package org.dromara.quant.paper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.execution.domain.QuantExecutionRecord;
import org.dromara.quant.execution.mapper.QuantExecutionRecordMapper;
import org.dromara.quant.paper.domain.QuantPaperAccount;
import org.dromara.quant.paper.mapper.PaperAtomicMapper;
import org.dromara.quant.paper.mapper.QuantPaperAccountMapper;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.position.mapper.QuantPositionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * PAPER 虚拟撮合账本。PAPER 不访问交易所私有账户，也不发送真实订单。
 */
@RequiredArgsConstructor
@Service
public class PaperTradingService {
    /**
     * 量化模块配置。
     */
    private final QuantProperties properties;
    /**
     * 账户相关 Mapper。
     */
    private final QuantPaperAccountMapper accountMapper;
    /**
     * 持仓 Mapper。
     */
    private final QuantPositionMapper positionMapper;
    /**
     * 执行记录 Mapper。
     */
    private final QuantExecutionRecordMapper executionRecordMapper;
    /**
     * 用于执行需要数据库原子语义的 SQL 操作。
     */
    private final PaperAtomicMapper atomicMapper;

    /**
     * 返回正数值；非法时使用默认值。
     */
    private static BigDecimal positive(BigDecimal value, BigDecimal fallback) {
        return value == null || value.signum() <= 0 ? fallback : value;
    }

    /**
     * 确保 PAPER 账户存在。
     */
    public QuantPaperAccount ensure(Long credentialId) {
        if (!properties.getPaper().isEnabled()) {
            throw new IllegalStateException("PAPER 模式已禁用");
        }
        QuantPaperAccount account = find(credentialId);
        if (account != null) {
            return account;
        }
        QuantPaperAccount entity = new QuantPaperAccount();
        entity.setCredentialId(credentialId);
        entity.setQuoteAsset(properties.getPaper().getQuoteAsset());
        BigDecimal initial = positive(properties.getPaper().getInitialBalance(), new BigDecimal("1000"));
        entity.setInitialBalance(initial);
        entity.setBalance(initial);
        entity.setAvailableBalance(initial);
        entity.setRealizedPnl(BigDecimal.ZERO);
        entity.setUnrealizedPnl(BigDecimal.ZERO);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            accountMapper.insert(entity);
            return entity;
        } catch (DuplicateKeyException ex) {
            return find(credentialId);
        }
    }

    /**
     * 查询符合条件的记录。
     */
    public QuantPaperAccount find(Long credentialId) {
        return accountMapper.selectOne(new LambdaQueryWrapper<QuantPaperAccount>().eq(QuantPaperAccount::getCredentialId, credentialId));
    }

    /**
     * 查询 PAPER 账户可用余额。
     */
    public BigDecimal availableBalance(Long credentialId) {
        QuantPaperAccount account = ensure(credentialId);
        return account.getAvailableBalance() == null ? BigDecimal.ZERO : account.getAvailableBalance();
    }

    /**
     * 在 PAPER 开仓前预留保证金。
     */
    @Transactional
    public void reserveMargin(Long credentialId, BigDecimal margin) {
        if (margin == null || margin.signum() <= 0) {
            throw new IllegalArgumentException("PAPER 保证金必须大于零");
        }
        ensure(credentialId);
        QuantPaperAccount account = atomicMapper.lockAccount(credentialId);
        if (account == null) {
            throw new IllegalStateException("PAPER 账户不存在");
        }
        if (account.getAvailableBalance().compareTo(margin) < 0) {
            throw new IllegalStateException("PAPER 可用余额不足");
        }
        if (atomicMapper.reserveMargin(account.getId(), margin) != 1) {
            throw new IllegalStateException("PAPER 可用余额不足或账户状态已变化");
        }
    }

    /**
     * 对尚未形成持仓且已被业务执行流程明确确认失败的 PAPER 保证金做原子退款。
     * LIVE/TESTNET 订单不得调用此方法；仅当订单明确未发送时由策略协调层调用。
     */
    @Transactional
    public void refundReservedMargin(Long credentialId, BigDecimal margin) {
        if (margin == null || margin.signum() <= 0) {
            return;
        }
        ensure(credentialId);
        QuantPaperAccount account = atomicMapper.lockAccount(credentialId);
        if (account == null || atomicMapper.refundMargin(account.getId(), margin) != 1) {
            throw new IllegalStateException("PAPER 预留保证金退款失败");
        }
    }

    /**
     * 处理 PAPER 开仓失败并回滚预留保证金。
     */
    @Transactional
    public boolean failPendingOpen(Long credentialId, String idempotencyKey, BigDecimal reservedMargin, String message) {
        QuantExecutionRecord record = lockExecution(credentialId, idempotencyKey);
        if (!"PENDING".equalsIgnoreCase(record.getExecutionStatus())) {
            return false;
        }
        ensure(credentialId);
        QuantPaperAccount account = atomicMapper.lockAccount(credentialId);
        if (account == null) {
            throw new IllegalStateException("PAPER 账户不存在");
        }
        BigDecimal refund = reservedMargin == null || reservedMargin.signum() <= 0 ? BigDecimal.ZERO : reservedMargin;
        if (refund.signum() > 0) {
            if (atomicMapper.refundMargin(account.getId(), refund) != 1) {
                throw new IllegalStateException("PAPER 保证金退款失败");
            }
        }
        record.setRecoveryAttempts((record.getRecoveryAttempts() == null ? 0 : record.getRecoveryAttempts()) + 1);
        record.setLastRecoveryAt(LocalDateTime.now(ZoneOffset.UTC));
        record.setExecutionStatus("FAILED");
        record.setErrorMessage(message == null ? "PAPER 执行未确认" : message);
        record.setUpdatedAt(record.getLastRecoveryAt());
        executionRecordMapper.updateById(record);
        return true;
    }

    /**
     * 记录 PAPER 开仓成交和持仓。
     */
    @Transactional
    public QuantPosition recordOpenFill(Long credentialId, String idempotencyKey, String symbol, String direction, BigDecimal quantity, BigDecimal entryPrice, int leverage) {
        QuantExecutionRecord record = lockExecution(credentialId, idempotencyKey);
        // 重放相同成交事件时只返回既有结果，不重复生成持仓或扣减保证金。
        if (Boolean.TRUE.equals(record.getPaperSettled())) {
            return positionMapper.selectOne(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credentialId).eq(QuantPosition::getSymbol, symbol).eq(QuantPosition::getPositionSide, direction));
        }
        QuantPosition position = positionMapper.selectOne(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credentialId).eq(QuantPosition::getSymbol,
            symbol).eq(QuantPosition::getPositionSide, direction));
        BigDecimal margin = entryPrice.multiply(quantity).divide(BigDecimal.valueOf(Math.max(1, leverage)), 16, RoundingMode.HALF_UP);
        if (position == null) {
            position = new QuantPosition();
            position.setCredentialId(credentialId);
            position.setSymbol(symbol);
            position.setPositionSide(direction);
            position.setQuantity(quantity);
            position.setEntryPrice(entryPrice);
            position.setMarkPrice(entryPrice);
            position.setMargin(margin);
            position.setLeverage(leverage);
            position.setUnrealizedPnl(BigDecimal.ZERO);
            position.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            positionMapper.insert(position);
        }
        record.setPaperSettled(true);
        record.setExecutionStatus("CONFIRMED");
        record.setExchangeOrderStatus("FILLED");
        record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        executionRecordMapper.updateById(record);
        return position;
    }

    /**
     * 结算 PAPER 平仓并返回结算结果。
     */
    @Transactional
    public PaperCloseSettlement settlePaperExecution(Long credentialId, String idempotencyKey, Long positionId, BigDecimal quantity, BigDecimal fillPrice) {
        QuantExecutionRecord record = lockExecution(credentialId, idempotencyKey);
        // 幂等保护：已结算的平仓不得再次释放保证金或累计已实现盈亏。
        if (Boolean.TRUE.equals(record.getPaperSettled())) {
            QuantPosition current = positionId == null ? null : positionMapper.selectById(positionId);
            return new PaperCloseSettlement(current, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (positionId == null || quantity == null || quantity.signum() <= 0 || fillPrice == null || fillPrice.signum() <= 0) {
            throw new IllegalArgumentException("PAPER 平仓参数无效");
        }
        if (atomicMapper.lockPosition(positionId) == null) {
            throw new IllegalStateException("PAPER 持仓不存在");
        }
        QuantPosition position = positionMapper.selectById(positionId);
        if (position == null || position.getQuantity() == null || position.getQuantity().signum() <= 0) {
            throw new IllegalStateException("PAPER 持仓不存在");
        }
        BigDecimal closeQty = quantity.min(position.getQuantity());
        BigDecimal oldUnrealizedPnl = position.getUnrealizedPnl() == null ? BigDecimal.ZERO : position.getUnrealizedPnl();
        BigDecimal delta = fillPrice.subtract(position.getEntryPrice());
        if ("SHORT".equalsIgnoreCase(position.getPositionSide())) {
            delta = delta.negate();
        }
        BigDecimal realized = delta.multiply(closeQty);
        BigDecimal oldQty = position.getQuantity();
        BigDecimal releasedMargin = (position.getMargin() == null ? BigDecimal.ZERO : position.getMargin()).multiply(closeQty).divide(oldQty, 16, RoundingMode.HALF_UP);
        BigDecimal remain = oldQty.subtract(closeQty).max(BigDecimal.ZERO);
        ensure(credentialId);
        QuantPaperAccount lockedAccount = atomicMapper.lockAccount(credentialId);
        if (lockedAccount == null) {
            throw new IllegalStateException("PAPER 账户不存在");
        }
        if (remain.signum() == 0) {
            positionMapper.deleteById(positionId);
        } else {
            position.setQuantity(remain);
            position.setMargin((position.getMargin() == null ? BigDecimal.ZERO : position.getMargin()).subtract(releasedMargin).max(BigDecimal.ZERO));
            position.setMarkPrice(fillPrice);
            position.setUnrealizedPnl(BigDecimal.ZERO);
            position.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            positionMapper.updateById(position);
        }
        if (atomicMapper.settleClose(lockedAccount.getId(), realized, releasedMargin, oldUnrealizedPnl) != 1) {
            throw new IllegalStateException("PAPER 平仓资金结算失败");
        }
        record.setPaperSettled(true);
        record.setExecutionStatus("CONFIRMED");
        record.setExchangeOrderStatus("FILLED");
        record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        executionRecordMapper.updateById(record);
        QuantPosition current = remain.signum() == 0 ? null : positionMapper.selectById(positionId);
        return new PaperCloseSettlement(current, releasedMargin, realized);
    }

    /**
     * 根据最新价格刷新 PAPER 未实现盈亏。
     */
    @Transactional
    public void refreshUnrealizedPnl(Long credentialId) {
        ensure(credentialId);
        List<QuantPosition> positions =
            positionMapper.selectList(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credentialId).gt(QuantPosition::getQuantity, BigDecimal.ZERO));
        BigDecimal total = BigDecimal.ZERO;
        for (QuantPosition p : positions) {
            BigDecimal mark = p.getMarkPrice() == null ? p.getEntryPrice() : p.getMarkPrice();
            BigDecimal pnl = mark.subtract(p.getEntryPrice()).multiply(p.getQuantity());
            if ("SHORT".equalsIgnoreCase(p.getPositionSide())) {
                pnl = pnl.negate();
            }
            p.setUnrealizedPnl(pnl);
            p.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            positionMapper.updateById(p);
            total = total.add(pnl);
        }
        if (atomicMapper.updateUnrealized(credentialId, total) != 1) {
            throw new IllegalStateException("PAPER 未实现盈亏更新失败");
        }
    }

    /**
     * 更新 PAPER 持仓标记价格。
     */
    @Transactional
    public void updateMarkPrice(QuantPosition position, BigDecimal markPrice) {
        if (position == null || position.getId() == null || markPrice == null || markPrice.signum() <= 0) {
            return;
        }
        BigDecimal oldPnl = position.getUnrealizedPnl() == null ? BigDecimal.ZERO : position.getUnrealizedPnl();
        BigDecimal pnl = markPrice.subtract(position.getEntryPrice()).multiply(position.getQuantity());
        if ("SHORT".equalsIgnoreCase(position.getPositionSide())) {
            pnl = pnl.negate();
        }
        position.setMarkPrice(markPrice);
        position.setUnrealizedPnl(pnl);
        position.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        positionMapper.updateById(position);
        if (atomicMapper.adjustUnrealized(position.getCredentialId(), pnl, oldPnl) != 1) {
            throw new IllegalStateException("PAPER 标记盈亏更新失败");
        }
    }

    /**
     * 锁定对应执行记录，保证同一幂等键的成交确认和资金结算串行执行。
     * <p>数据库锁与 {@code @Transactional} 必须处于同一事务；调用方须先确认执行模式为 PAPER。
     * 找不到记录时直接失败，避免在资金状态不明时重复结算。
     */
    private QuantExecutionRecord lockExecution(Long credentialId, String idempotencyKey) {
        Long id = atomicMapper.lockExecution(credentialId, idempotencyKey);
        if (id == null) {
            throw new IllegalStateException("PAPER 执行记录不存在: " + idempotencyKey);
        }
        QuantExecutionRecord record = executionRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalStateException("PAPER 执行记录不存在: " + idempotencyKey);
        }
        return record;
    }

    /**
     * PAPER 平仓结算结果。
     *
     * @param position       结算后的本地持仓
     * @param releasedMargin PAPER 平仓释放的保证金
     * @param realizedPnl    已实现盈亏
     */
    public record PaperCloseSettlement(QuantPosition position, BigDecimal releasedMargin, BigDecimal realizedPnl) {
    }
}

package org.dromara.quant.protection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.StrategyOrderIntentService;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.dromara.quant.protection.mapper.QuantProtectionOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/** 将已冻结的保护来源传递到 Binance 实际子订单；不按交易对或数量猜测成交归属。 */
@RequiredArgsConstructor
@Service
public class ProtectionOrderJournalService {
    private final QuantProtectionOrderMapper protections;
    private final QuantOrderMapper orders;
    private final StrategyOrderIntentService intents;

    /** 发送之前随保护意图保存来源；不能证明历史周期的记录保持未归属。 */
    public void captureOwner(QuantProtectionOrder local) {
        if (local.getStrategyId() != null || local.getStrategyInstanceId() != null) {
            if (local.getStrategyId() == null || local.getStrategyInstanceId() == null) {
                throw new IllegalStateException("保护单策略来源不完整");
            }
            return;
        }
        if (local.getPositionId() == null || local.getPositionCycleKey() == null) {
            return;
        }
        var owners = protections.cycleOwners(local.getCredentialId(), local.getPositionId(), local.getPositionCycleKey(), local.getSymbol(), local.getPositionSide());
        if (owners.size() != 1) {
            return;
        }
        Object strategy = owners.getFirst().get("strategy_id");
        Object instance = owners.getFirst().get("instance_id");
        if (strategy instanceof Number strategyId && strategyId.longValue() > 0 && instance instanceof Number instanceId && instanceId.longValue() > 0) {
            local.setStrategyId(strategyId.longValue());
            local.setStrategyInstanceId(instanceId.longValue());
        }
    }

    /** 精确算法单→实际子订单→冻结策略来源在同一事务入账，失败时保留原未知事实。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean bindChild(ExchangeGateway.Credential credential, QuantProtectionOrder parent,
                             ExchangeGateway.ProtectionOrderSnapshot actual, ExchangeGateway.OrderSnapshot child) {
        // 事务回滚不能把失败的来源绑定留在调用方实体里，随后被状态更新意外写回。
        QuantProtectionOrder local = new QuantProtectionOrder();
        org.springframework.beans.BeanUtils.copyProperties(parent, local);
        java.util.StringJoiner conflicts = new java.util.StringJoiner(",");
        if (actual == null) {
            conflicts.add("actualMissing");
        } else if (child == null) {
            conflicts.add("childMissing");
        } else {
            if (actual.actualOrderId() == null || !actual.actualOrderId().equals(child.orderId())) conflicts.add("actualOrderId");
            if (actual.algoId() == null) conflicts.add("algoIdMissing");
            if (!Objects.equals(local.getClientOrderId(), actual.clientAlgoId())) conflicts.add("clientAlgoId");
            if (local.getExchangeOrderId() != null && !local.getExchangeOrderId().equals(actual.algoId())) conflicts.add("algoIdMismatch");
            if (!local.getSymbol().equalsIgnoreCase(actual.symbol())) conflicts.add("algoSymbol");
            if (!local.getSymbol().equalsIgnoreCase(child.symbol())) conflicts.add("childSymbol");
            if (child.clientOrderId() == null || child.clientOrderId().isBlank()) conflicts.add("childClientOrderIdMissing");
            if (local.getQuantity() == null || actual.quantity() == null || local.getQuantity().compareTo(actual.quantity()) != 0) conflicts.add("quantity");
            if (local.getStopPrice() == null || actual.triggerPrice() == null || local.getStopPrice().compareTo(actual.triggerPrice()) != 0) conflicts.add("triggerPrice");
            if (local.getOrderType() == null || !local.getOrderType().equalsIgnoreCase(actual.type())) conflicts.add("orderType");
            if (!("SHORT".equalsIgnoreCase(local.getPositionSide()) ? "BUY" : "SELL").equalsIgnoreCase(child.side())) conflicts.add("childReduceSide");
            if (child.side() == null || actual.side() == null || !child.side().equalsIgnoreCase(actual.side())) conflicts.add("algoChildSide");
            if (!("BOTH".equalsIgnoreCase(actual.positionSide()) || local.getPositionSide().equalsIgnoreCase(actual.positionSide()))) conflicts.add("positionSide");
        }
        if (conflicts.length() > 0) {
            throw new IllegalStateException("保护单实际子订单身份或减仓方向冲突，禁止分配成交；conflicts=" + conflicts
                + "; localSide=" + local.getPositionSide()
                + "; algoSide=" + (actual == null ? null : actual.side())
                + "; algoPositionSide=" + (actual == null ? null : actual.positionSide())
                + "; childSide=" + (child == null ? null : child.side())
                + "; localQty=" + local.getQuantity()
                + "; algoQty=" + (actual == null ? null : actual.quantity())
                + "; localTrigger=" + local.getStopPrice()
                + "; algoTrigger=" + (actual == null ? null : actual.triggerPrice()));
        }
        if (local.getStrategyId() == null && local.getStrategyInstanceId() == null) {
            captureOwner(local);
            if (local.getStrategyId() == null) {
                return false;
            }
            if (protections.bindCycleOwner(local.getId(), local.getCredentialId(), local.getPositionCycleKey(), local.getStrategyId(), local.getStrategyInstanceId()) != 1) {
                var persisted = protections.selectById(local.getId());
                if (persisted == null || !Objects.equals(persisted.getStrategyId(), local.getStrategyId())
                    || !Objects.equals(persisted.getStrategyInstanceId(), local.getStrategyInstanceId())) {
                    throw new IllegalStateException("保护单来源并发变更，禁止覆盖");
                }
            }
        }
        if (local.getStrategyId() == null || local.getStrategyId() <= 0 || local.getStrategyInstanceId() == null || local.getStrategyInstanceId() <= 0) {
            throw new IllegalStateException("保护单来源不完整，禁止分配成交");
        }
        QuantOrder order = orders.selectOne(new LambdaQueryWrapper<QuantOrder>().eq(QuantOrder::getCredentialId, local.getCredentialId())
            .eq(QuantOrder::getSymbol, local.getSymbol()).eq(QuantOrder::getExchangeOrderId, child.orderId()).last("FOR UPDATE"));
        if (order == null) {
            order = new QuantOrder();
            order.setCredentialId(local.getCredentialId());
            order.setSymbol(local.getSymbol());
            order.setExchangeOrderId(child.orderId());
            order.setClientOrderId(child.clientOrderId());
            order.setIdempotencyKey("PROTECTION_CHILD:" + local.getId() + ":" + child.orderId());
            order.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        } else if (!Objects.equals(order.getClientOrderId(), child.clientOrderId())
            || order.getStrategyId() != null && !order.getStrategyId().equals(local.getStrategyId())
            || order.getStrategyInstanceId() != null && !order.getStrategyInstanceId().equals(local.getStrategyInstanceId())
            || order.getPositionSide() != null && !order.getPositionSide().equalsIgnoreCase(actual.positionSide())) {
            throw new IllegalStateException("保护子订单已有冲突的策略归属，禁止覆盖");
        }
        order.setStrategyId(local.getStrategyId());
        order.setStrategyInstanceId(local.getStrategyInstanceId());
        order.setPositionSide(actual.positionSide());
        order.setReduceOnly(true);
        order.setSide(child.side());
        order.setOrderType(child.type());
        // 延迟的子订单查询不能把已落库的成交终态退回 NEW/PARTIALLY_FILLED。
        boolean terminalAlreadyKnown = Set.of("FILLED", "CANCELED", "EXPIRED", "REJECTED").contains(Objects.toString(order.getStatus(), ""));
        if (!terminalAlreadyKnown) {
            order.setStatus(child.status());
        }
        order.setPrice(child.price());
        order.setOriginalQuantity(child.originalQuantity());
        order.setExecutedQuantity(order.getExecutedQuantity() == null ? child.executedQuantity()
            : child.executedQuantity() == null ? order.getExecutedQuantity() : order.getExecutedQuantity().max(child.executedQuantity()));
        order.setUpdatedAt(child.updateTime() == null ? LocalDateTime.now(ZoneOffset.UTC) : LocalDateTime.ofInstant(child.updateTime(), ZoneOffset.UTC));
        if ((order.getId() == null ? orders.insert(order) : orders.updateById(order)) != 1) {
            throw new IllegalStateException("保护单子订单未可靠入账");
        }
        if ("LIVE".equalsIgnoreCase(credential.runMode()) && Set.of("LONG", "SHORT").contains(actual.positionSide())) {
            var request = new ExchangeGateway.OrderRequest(credential, local.getSymbol(), child.side(), child.type(), child.originalQuantity(), child.price(), true,
                actual.positionSide(), child.clientOrderId(), true);
            intents.record(local.getCredentialId(), local.getStrategyId(), local.getStrategyInstanceId(), "CLOSE", request,
                new ExchangeGateway.OrderResult(child.orderId(), child.clientOrderId(), child.status()));
        }
        return true;
    }
}

package org.dromara.quant.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.execution.domain.QuantExecutionRecord;
import org.dromara.quant.execution.mapper.QuantExecutionRecordMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 数据库幂等执行记录服务。
 */
@RequiredArgsConstructor
@Service
public class ExecutionRecordService {
    /**
     * 当前领域 Mapper。
     */
    private final QuantExecutionRecordMapper mapper;

    /**
     * 查询符合条件的记录。
     */
    public QuantExecutionRecord find(Long credentialId, String idempotencyKey) {
        return mapper.selectOne(new LambdaQueryWrapper<QuantExecutionRecord>().eq(QuantExecutionRecord::getCredentialId, credentialId).eq(QuantExecutionRecord::getIdempotencyKey
            , idempotencyKey));
    }

    /**
     * 尝试创建待执行幂等记录；唯一键冲突时返回空。
     */
    public QuantExecutionRecord tryCreatePending(Long credentialId, Long strategyId, Long strategyInstanceId, String operationType, String symbol, String idempotencyKey,
                                                 String clientOrderId) {
        QuantExecutionRecord entity = new QuantExecutionRecord();
        entity.setCredentialId(credentialId);
        entity.setStrategyId(strategyId);
        entity.setStrategyInstanceId(strategyInstanceId);
        entity.setOperationType(operationType);
        entity.setSymbol(symbol);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setClientOrderId(clientOrderId == null || clientOrderId.isBlank() ? idempotencyKey : clientOrderId);
        entity.setExecutionStatus("PENDING");
        entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setRecoveryAttempts(0);
        entity.setPaperSettled(false);
        entity.setUpdatedAt(entity.getCreatedAt());
        try {
            mapper.insert(entity);
            return entity;
        } catch (DuplicateKeyException ex) {
            return null;
        }
    }

    /**
     * 把执行记录更新为已确认并保存交易所订单状态。
     */
    public void accepted(QuantExecutionRecord entity, Long exchangeOrderId, String exchangeStatus) {
        String status = exchangeStatus == null ? "ACCEPTED" : exchangeStatus;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (mapper.markAccepted(entity.getId(), exchangeOrderId, status, now) != 1) {
            // Binance has already returned an order result. A failed durable transition
            // must stop the caller before it can acknowledge/release the dispatch guard.
            throw new IllegalStateException("交易所已返回订单结果，但执行幂等记录未能可靠确认；保留派发阻断并等待恢复");
        }
        entity.setExchangeOrderId(exchangeOrderId);
        entity.setExecutionStatus("CONFIRMED");
        entity.setExchangeOrderStatus(status);
        entity.setUpdatedAt(now);
    }

    /**
     * 记录一次执行恢复尝试。
     */
    public void markRecoveryAttempt(QuantExecutionRecord entity) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (mapper.recordRecoveryAttempt(entity.getId(), now) == 1) {
            entity.setRecoveryAttempts((entity.getRecoveryAttempts() == null ? 0 : entity.getRecoveryAttempts()) + 1);
            entity.setLastRecoveryAt(now);
            entity.setUpdatedAt(now);
        }
    }

    /**
     * 把未触达交易所的执行记录标记为失败。
     */
    public void failed(QuantExecutionRecord entity, String message) {
        String reason = message == null ? "未知执行异常" : message;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (mapper.markFailedBeforeSend(entity.getId(), reason, now) == 1) {
            entity.setExecutionStatus("FAILED");
            entity.setErrorMessage(reason);
            entity.setUpdatedAt(now);
        }
    }

    /**
     * 把已触达交易所但响应未知的执行记录标记为 UNKNOWN。
     */
    public void markUnknown(QuantExecutionRecord entity, String message) {
        String reason = message == null ? "无法确认交易所订单事实" : message;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (mapper.markUnknownIfUnresolved(entity.getId(), reason, now) == 1) {
            entity.setExecutionStatus("UNKNOWN");
            entity.setErrorMessage(reason);
            entity.setUpdatedAt(now);
        }
    }

    /**
     * 把经过事实核实的执行记录标记为 RECOVERED。
     */
    public void markRecovered(QuantExecutionRecord entity, Long exchangeOrderId, String exchangeStatus) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (mapper.markRecoveredAfterFact(entity.getId(), exchangeOrderId, exchangeStatus, now) != 1) {
            // Never let a caller treat a non-persisted recovery fact as durable
            // and release the account-symbol dispatch guard.
            throw new IllegalStateException("已核实交易所订单，但执行恢复事实未能持久化；保留派发阻断");
        }
        entity.setExchangeOrderId(exchangeOrderId);
        entity.setExecutionStatus("RECOVERED");
        if (exchangeStatus != null) {
            entity.setExchangeOrderStatus(exchangeStatus);
        }
        entity.setUpdatedAt(now);
    }
}

package org.dromara.quant.execution;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 持久化策略订单意图及其派发状态。
 */
@Mapper
public interface StrategyOrderIntentMapper {

    int insert(@Param("credentialId") long credentialId, @Param("symbol") String symbol,
               @Param("orderId") long orderId, @Param("strategyId") long strategyId,
               @Param("instanceId") long instanceId, @Param("positionSide") String positionSide,
               @Param("role") String role, @Param("positionCycleKey") String positionCycleKey,
               @Param("exchangeSide") String exchangeSide, @Param("clientOrderId") String clientOrderId);

    Map<String, Object> find(@Param("credentialId") long credentialId,
                             @Param("symbol") String symbol, @Param("orderId") long orderId);

    /**
     * Resolve a close order only from immutable strategy-cycle identity and its accepted local order.
     */
    List<Long> findCloseOrderIdsByCycle(@Param("credentialId") long credentialId,
                                        @Param("symbol") String symbol,
                                        @Param("instanceId") long instanceId,
                                        @Param("positionCycleKey") String positionCycleKey,
                                        @Param("positionSide") String positionSide,
                                        @Param("exchangeSide") String exchangeSide,
                                        @Param("requestedQuantity") BigDecimal requestedQuantity);

    Map<String, Object> pending(@Param("credentialId") long credentialId, @Param("key") String key);

    Map<String, Object> pendingByClient(@Param("credentialId") long credentialId,
                                        @Param("symbol") String symbol, @Param("clientOrderId") String clientOrderId);

    /**
     * Promote only the immutable pre-send fact after exact exchange identity is verified.
     */
    int promote(@Param("credentialId") long credentialId, @Param("symbol") String symbol,
                @Param("key") String key, @Param("clientOrderId") String clientOrderId,
                @Param("orderId") long orderId);

    int bindRecoveredOrder(@Param("localId") long localId, @Param("credentialId") long credentialId,
                           @Param("symbol") String symbol, @Param("orderId") long orderId,
                           @Param("clientOrderId") String clientOrderId, @Param("strategyId") long strategyId,
                           @Param("instanceId") long instanceId, @Param("positionSide") String positionSide,
                           @Param("reduceOnly") boolean reduceOnly);

    /**
     * Verify an idempotent retry against every identity and ownership field.
     */
    int countExactRecoveredBinding(@Param("localId") long localId, @Param("credentialId") long credentialId,
                                   @Param("symbol") String symbol, @Param("orderId") long orderId,
                                   @Param("clientOrderId") String clientOrderId,
                                   @Param("strategyId") long strategyId, @Param("instanceId") long instanceId,
                                   @Param("positionSide") String positionSide,
                                   @Param("reduceOnly") boolean reduceOnly);
}

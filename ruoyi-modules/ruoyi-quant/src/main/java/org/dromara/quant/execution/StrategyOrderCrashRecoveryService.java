package org.dromara.quant.execution;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.domain.QuantExecutionRecord;
import org.dromara.quant.order.domain.QuantOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Crash recovery for exchange-accepted strategy orders whose local acceptance
 * journal did not commit. Uses ONLY immutable, committed PRE-send intent and
 * an exact Binance query result. No new Binance trade request is ever sent.
 */
@Service
@RequiredArgsConstructor
public class StrategyOrderCrashRecoveryService {
    @NonNull private final StrategyOrderIntentMapper intents;
    @NonNull private final OrderLedgerService ledger;



    private static boolean sameId(Object actual, Long expected) {
        return expected != null && actual instanceof Number n && n.longValue() == expected;
    }

    /**
     * 根据精确确认的交易所订单恢复本地策略订单记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuantOrder restore(QuantExecutionRecord record, ExchangeGateway.OrderSnapshot exact) {
        if (record == null || record.getCredentialId() == null || record.getCredentialId() <= 0 || record.getStrategyId() == null || record.getStrategyInstanceId() == null || record.getIdempotencyKey() == null || record.getSymbol() == null || record.getClientOrderId() == null || exact == null || exact.orderId() == null || exact.orderId() <= 0 || exact.symbol() == null || !record.getSymbol().equalsIgnoreCase(exact.symbol()) || !record.getClientOrderId().equals(exact.clientOrderId()) || record.getExchangeOrderId() != null && !record.getExchangeOrderId().equals(exact.orderId())) {
            throw new IllegalStateException("Cannot restore strategy ownership without exact order identity");
        }
        long credentialId = record.getCredentialId();
        String symbol = record.getSymbol().toUpperCase(Locale.ROOT);
        Map<String, Object> pre = intents.pending(credentialId, record.getIdempotencyKey());
        if (pre == null || !sameId(pre.get("strategy_id"), record.getStrategyId()) || !sameId(pre.get("strategy_instance_id"), record.getStrategyInstanceId()) || !symbol.equals(pre.get("symbol")) || !record.getClientOrderId().equals(pre.get("client_order_id")) || !record.getOperationType().equals(pre.get("operation_type")) || !Objects.equals(exact.side(), pre.get("exchange_side")) || !java.util.Set.of("LONG", "SHORT").contains(pre.get("position_side")) || !(pre.get("position_cycle_key") instanceof String cycleKey) || cycleKey.isBlank()) {
            throw new IllegalStateException("Immutable pre-send strategy intent missing or conflicting");
        }
        QuantOrder order = ledger.syncSnapshot(credentialId, exact);
        if (order == null || order.getId() == null) {
            throw new IllegalStateException("Cannot persist exact exchange order snapshot");
        }
        try {
            if (intents.promote(credentialId, symbol, record.getIdempotencyKey(), record.getClientOrderId(), exact.orderId()) != 1) {
                throw new IllegalStateException("Pre-dispatch strategy intent not promoted");
            }
        } catch (DuplicateKeyException duplicate) {
            Map<String, Object> existing = intents.find(credentialId, symbol, exact.orderId());
            if (existing == null || !sameId(existing.get("strategy_id"), record.getStrategyId()) || !sameId(existing.get("strategy_instance_id"), record.getStrategyInstanceId()) || !pre.get("position_side").equals(existing.get("position_side")) || !pre.get("operation_type").equals(existing.get("fill_role")) || !cycleKey.equals(existing.get("position_cycle_key")) || !record.getClientOrderId().equals(existing.get("client_order_id")) || !exact.side().equals(existing.get("exchange_side"))) {
                throw new IllegalStateException("Conflicting recovered strategy order intent", duplicate);
            }
        }
        boolean reduceOnly = "CLOSE".equals(record.getOperationType());
        String positionSide = (String) pre.get("position_side");
        int bound = intents.bindRecoveredOrder(order.getId(), credentialId, symbol, exact.orderId(),
            record.getClientOrderId(), record.getStrategyId(), record.getStrategyInstanceId(),
            positionSide, reduceOnly);
        int recoveredCount = intents.countExactRecoveredBinding(order.getId(), credentialId, symbol, exact.orderId(), record.getClientOrderId(), record.getStrategyId(), record.getStrategyInstanceId(), positionSide, reduceOnly);
        if (bound != 1 && recoveredCount != 1) {
            throw new IllegalStateException("Cannot bind exact recovered order to strategy: "
                + "localOrderId=" + order.getId() + ", credentialId=" + credentialId
                + ", symbol=" + symbol + ", exchangeOrderId=" + exact.orderId()
                + ", clientOrderId=" + record.getClientOrderId()
                + ", updateCount=" + bound);
        }
        return order;
    }
}

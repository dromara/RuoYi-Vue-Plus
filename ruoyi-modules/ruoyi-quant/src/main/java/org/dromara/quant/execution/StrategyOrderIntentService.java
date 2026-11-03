package org.dromara.quant.execution;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * Durable attribution of a proven exchange-accepted strategy order.
 * Never infers position ownership from BUY/SELL or mutable order state.
 * Exchange BOTH is accepted only when the exact immutable pre-dispatch record
 * supplies its logical LONG/SHORT side and persisted position-cycle key.
 * Missing identifiers, missing cycle identity, and conflicting replays fail closed.
 * <p>
 * This operation must run after Binance acceptance plus local order-ledger
 * persistence. Schema V09 and the position-cycle migration are prerequisites.
 */
@Service
@RequiredArgsConstructor
public class StrategyOrderIntentService {
    @NonNull private final StrategyOrderIntentMapper mapper;



    private static boolean equalsId(Object actual, long expected) {
        return actual instanceof Number n && n.longValue() == expected;
    }

    /**
     * 持久化策略订单意图和幂等键，供崩溃恢复精确对账。
     */
    public void record(long credentialId, long strategyId, long instanceId, String operation, ExchangeGateway.OrderRequest request, ExchangeGateway.OrderResult accepted) {
        if (credentialId <= 0 || strategyId <= 0 || instanceId <= 0 || request == null || accepted == null || accepted.orderId() == null || accepted.orderId() <= 0
            || request.symbol() == null || request.symbol().isBlank() || request.clientOrderId() == null || request.clientOrderId().isBlank()
            || accepted.clientOrderId() == null || !request.clientOrderId().equals(accepted.clientOrderId()) || request.positionSide() == null
            || !java.util.Set.of("LONG", "SHORT", "BOTH").contains(request.positionSide().toUpperCase(Locale.ROOT))
            || operation == null || !java.util.Set.of("OPEN", "CLOSE").contains(operation.toUpperCase(Locale.ROOT))
            || request.side() == null || !java.util.Set.of("BUY", "SELL").contains(request.side().toUpperCase(Locale.ROOT))) {
            throw new IllegalStateException("Cannot establish unambiguous immutable strategy order intent");
        }
        String role = operation.toUpperCase(Locale.ROOT);
        String side = request.side().toUpperCase(Locale.ROOT);
        String requestedPositionSide = request.positionSide().toUpperCase(Locale.ROOT);
        String symbol = request.symbol().toUpperCase(Locale.ROOT);
        Map<String, Object> pre = mapper.pendingByClient(credentialId, symbol, request.clientOrderId());
        if (pre == null || !equalsId(pre.get("strategy_id"), strategyId) || !equalsId(pre.get("strategy_instance_id"), instanceId)
            || !symbol.equals(pre.get("symbol")) || !role.equals(pre.get("operation_type"))
            || !side.equals(pre.get("exchange_side")) || !request.clientOrderId().equals(pre.get("client_order_id"))
            || !(pre.get("position_cycle_key") instanceof String cycleKey) || cycleKey.isBlank()
            || !(pre.get("position_side") instanceof String positionSide)
            || !java.util.Set.of("LONG", "SHORT").contains(positionSide)) {
            throw new IllegalStateException("Missing or conflicting immutable pre-dispatch strategy cycle identity");
        }
        if (!"BOTH".equals(requestedPositionSide) && !requestedPositionSide.equals(positionSide)) {
            throw new IllegalStateException("Exchange order position side conflicts with immutable strategy intent");
        }
        String expected = ((positionSide.equals("LONG")) == role.equals("OPEN")) ? "BUY" : "SELL";
        if (!side.equals(expected) || role.equals("CLOSE") && !request.reduceOnly() || role.equals("OPEN") && request.reduceOnly()) {
            throw new IllegalStateException("Order role, position side and execution direction conflict");
        }
        try {
            if (mapper.insert(credentialId, symbol, accepted.orderId(), strategyId, instanceId, positionSide, role, cycleKey, side, request.clientOrderId()) != 1) {
                throw new IllegalStateException("Strategy order intent was not persisted");
            }
        } catch (DuplicateKeyException replay) {
            Map<String, Object> prior = mapper.find(credentialId, symbol, accepted.orderId());
            if (prior == null || !equalsId(prior.get("strategy_id"), strategyId)
                || !equalsId(prior.get("strategy_instance_id"), instanceId) || !role.equals(prior.get("fill_role"))
                || !positionSide.equals(prior.get("position_side")) || !cycleKey.equals(prior.get("position_cycle_key"))
                || !side.equals(prior.get("exchange_side")) || !request.clientOrderId().equals(prior.get("client_order_id"))) {
                throw new IllegalStateException("Conflicting immutable strategy order identity", replay);
            }
        }
    }
}

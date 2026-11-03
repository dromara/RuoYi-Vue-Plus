package org.dromara.quant.execution;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.order.domain.QuantOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Binance-accepted order and its immutable strategy provenance commit as
 * a single local transaction. Remote HTTP MUST happen before this method.
 * A database failure rolls back both rows and leaves the caller's durable
 * dispatch ticket UNKNOWN, never "not sent".
 */
@Service
@RequiredArgsConstructor
public class StrategyOrderAcceptanceJournal {
    @NonNull private final OrderLedgerService ledger;
    @NonNull private final StrategyOrderIntentService intents;



    /**
     * 在提交订单前写入可恢复的接受意图，并持久化本地订单状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuantOrder persist(Long credentialId, Long strategyId, Long instanceId, String runMode, String operation, ExchangeGateway.OrderRequest request,
                              ExchangeGateway.OrderResult result) {
        return persist(credentialId, strategyId, instanceId, runMode, operation, request, result, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuantOrder persist(Long credentialId, Long strategyId, Long instanceId, String runMode, String operation, ExchangeGateway.OrderRequest request,
                              ExchangeGateway.OrderResult result, Integer leverageSnapshot, java.math.BigDecimal priceTickSizeSnapshot) {
        if (credentialId == null || credentialId <= 0 || request == null || result == null) {
            throw new IllegalArgumentException("Missing accepted order identity");
        }
        QuantOrder order = ledger.upsert(credentialId, strategyId, instanceId, request, result, leverageSnapshot, priceTickSizeSnapshot);
        if (order == null || order.getId() == null) {
            throw new IllegalStateException("Exchange order ledger was not persisted");
        }
        if (strategyId != null && instanceId != null && "LIVE".equalsIgnoreCase(runMode)
            && java.util.Set.of("LONG", "SHORT", "BOTH").contains(request.positionSide() == null ? ""
                : request.positionSide().toUpperCase(java.util.Locale.ROOT))) {
            // BOTH is resolved from the immutable logical LONG/SHORT pre-dispatch intent.
            // A failure rolls the ledger back and keeps the dispatch guard held.
            intents.record(credentialId, strategyId, instanceId, operation, request, result);
        }
        return order;
    }
}

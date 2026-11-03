package org.dromara.quant.execution;

import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.domain.QuantExecutionRecord;
import org.dromara.quant.order.domain.QuantOrder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class StrategyOrderCrashRecoveryServiceTest {
    private final StrategyOrderIntentMapper intents = mock(StrategyOrderIntentMapper.class);
    private final OrderLedgerService ledger = mock(OrderLedgerService.class);
    private final StrategyOrderCrashRecoveryService recovery = new StrategyOrderCrashRecoveryService(intents, ledger);

    private QuantExecutionRecord record() {
        var r = new QuantExecutionRecord();
        r.setCredentialId(1L);
        r.setSymbol("BTCUSDT");
        r.setStrategyId(2L);
        r.setStrategyInstanceId(3L);
        r.setOperationType("OPEN");
        r.setIdempotencyKey("dispatch-a");
        r.setClientOrderId("cid-a");
        return r;
    }

    private ExchangeGateway.OrderSnapshot exact(String clientId) {
        return new ExchangeGateway.OrderSnapshot("BTCUSDT", 9L, clientId, "BUY", "MARKET", "FILLED", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, Instant.parse("2026-10-14T02" +
            ":00:00Z"));
    }

    private Map<String, Object> pending() {
        return Map.of("strategy_id", 2L, "strategy_instance_id", 3L, "symbol", "BTCUSDT", "client_order_id", "cid-a", "operation_type", "OPEN", "exchange_side", "BUY",
            "position_side", "LONG", "position_cycle_key", "cycle-1");
    }

    @Test
    void matchingPresendFactRestoresProvenanceTransactionally() throws Exception {
        assertNotNull(StrategyOrderCrashRecoveryService.class.getMethod("restore", QuantExecutionRecord.class, ExchangeGateway.OrderSnapshot.class).getAnnotation(Transactional.class));
        when(intents.pending(1L, "dispatch-a")).thenReturn(pending());
        var order = new QuantOrder();
        order.setId(18L);
        when(ledger.syncSnapshot(1L, exact("cid-a"))).thenReturn(order);
        when(intents.promote(1L, "BTCUSDT", "dispatch-a", "cid-a", 9L)).thenReturn(1);
        when(intents.bindRecoveredOrder(18L, 1L, "BTCUSDT", 9L, "cid-a", 2L, 3L, "LONG", false)).thenReturn(1);
        assertSame(order, recovery.restore(record(), exact("cid-a")));
        verify(intents).promote(1L, "BTCUSDT", "dispatch-a", "cid-a", 9L);
    }

    @Test
    void alreadyBoundExactOrderIsAcceptedAsIdempotentRecovery() {
        when(intents.pending(1L, "dispatch-a")).thenReturn(pending());
        var order = new QuantOrder();
        order.setId(18L);
        when(ledger.syncSnapshot(1L, exact("cid-a"))).thenReturn(order);
        when(intents.promote(1L, "BTCUSDT", "dispatch-a", "cid-a", 9L)).thenReturn(1);
        when(intents.bindRecoveredOrder(18L, 1L, "BTCUSDT", 9L, "cid-a", 2L, 3L, "LONG", false))
            .thenReturn(0);
        when(intents.countExactRecoveredBinding(18L, 1L, "BTCUSDT", 9L, "cid-a", 2L, 3L, "LONG", false))
            .thenReturn(1);

        assertSame(order, recovery.restore(record(), exact("cid-a")));
        verify(intents).countExactRecoveredBinding(18L, 1L, "BTCUSDT", 9L, "cid-a", 2L, 3L, "LONG", false);
    }

    @Test
    void missingPresendIntentCannotAttributeExchangeOrder() {
        assertThrows(IllegalStateException.class, () -> recovery.restore(record(), exact("cid-a")));
        verifyNoInteractions(ledger);
    }

    @Test
    void exchangeClientIdConflictNeverWritesLocalLedger() {
        when(intents.pending(1L, "dispatch-a")).thenReturn(pending());
        assertThrows(IllegalStateException.class, () -> recovery.restore(record(), exact("foreign")));
        verifyNoInteractions(ledger);
    }
}

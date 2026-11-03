package org.dromara.quant.execution;

import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.order.domain.QuantOrder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("dev")
class StrategyOrderAcceptanceJournalTest {
    private final OrderLedgerService ledger = mock(OrderLedgerService.class);
    private final StrategyOrderIntentService intents = mock(StrategyOrderIntentService.class);
    private final StrategyOrderAcceptanceJournal journal = new StrategyOrderAcceptanceJournal(ledger, intents);
    private final ExchangeGateway.OrderResult result = new ExchangeGateway.OrderResult(17L, "cid", "NEW");

    private ExchangeGateway.OrderRequest request(String positionSide) {
        return new ExchangeGateway.OrderRequest(null, "BTCUSDT", "BUY", "MARKET", BigDecimal.ONE, null, false, positionSide, "cid", true);
    }

    @Test
    void transactionBoundaryAndVerifiedHedgeOrder() throws Exception {
        assertNotNull(StrategyOrderAcceptanceJournal.class.getMethod("persist", Long.class, Long.class, Long.class, String.class, String.class,
            ExchangeGateway.OrderRequest.class, ExchangeGateway.OrderResult.class).getAnnotation(Transactional.class));
        var order = new QuantOrder();
        order.setId(19L);
        when(ledger.upsert(any(), any(), any(), any(), any())).thenReturn(order);
        journal.persist(1L, 2L, 3L, "LIVE", "OPEN", request("LONG"), result);
        verify(intents).record(1L, 2L, 3L, "OPEN", request("LONG"), result);
    }

    @Test
    void oneWayOrderPersistsIntentFromImmutablePreDispatchRecord() {
        var order = new QuantOrder();
        order.setId(19L);
        when(ledger.upsert(any(), any(), any(), any(), any())).thenReturn(order);
        journal.persist(1L, 2L, 3L, "LIVE", "OPEN", request("BOTH"), result);
        verify(intents).record(1L, 2L, 3L, "OPEN", request("BOTH"), result);
    }

    @Test
    void failedLedgerCannotPersistUnbackedIntent() {
        assertThrows(IllegalStateException.class, () -> journal.persist(1L, 2L, 3L, "LIVE", "OPEN", request("LONG"), result));
        verifyNoInteractions(intents);
    }

    @Test
    void failedIntentPropagatesToTransactionalCaller() {
        var order = new QuantOrder();
        order.setId(19L);
        when(ledger.upsert(any(), any(), any(), any(), any())).thenReturn(order);
        doThrow(new IllegalStateException("intent conflict")).when(intents).record(anyLong(), anyLong(), anyLong(), anyString(), any(), any());
        assertThrows(IllegalStateException.class, () -> journal.persist(1L, 2L, 3L, "LIVE", "OPEN", request("LONG"), result));
    }
}

package org.dromara.quant.execution;

import org.dromara.quant.execution.mapper.LiveOrderDispatchMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class LivePreDispatchIntentTest {
    private final LiveOrderDispatchMapper mapper = mock(LiveOrderDispatchMapper.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus status = mock(TransactionStatus.class);
    private final LiveOrderDispatcher dispatcher;

    LivePreDispatchIntentTest() {
        when(manager.getTransaction(any())).thenReturn(status);
        when(mapper.claimWithLease(1L, "BTCUSDT", "dispatch-a", 3L, 4L)).thenReturn(1);
        when(mapper.insertTicket(1L, "BTCUSDT", "dispatch-a", "OPEN", "cid-a", 3L, 4L)).thenReturn(1);
        dispatcher = new LiveOrderDispatcher(mapper, manager);
    }

    @Test
    void ticketAndImmutablePresendIntentCommitTogether() {
        when(mapper.insertPreDispatchIntent(1L, "BTCUSDT", "dispatch-a", "cid-a", 2L, 3L, "OPEN", "LONG", "BUY", 4L)).thenReturn(1);
        var ticket = dispatcher.beginStrategy(1L, "BTCUSDT", "dispatch-a", "OPEN", "cid-a", 2L, 3L, 4L, "LONG", "BUY");
        assertEquals("dispatch-a", ticket.key());
        verify(mapper).insertPreDispatchIntent(1L, "BTCUSDT", "dispatch-a", "cid-a", 2L, 3L, "OPEN", "LONG", "BUY", 4L);
        verify(manager).commit(status);
    }

    @Test
    void oneWayExchangePositionSideIsPersistedAsLogicalLongForOpeningBuy() {
        when(mapper.insertPreDispatchIntent(1L, "BTCUSDT", "dispatch-a", "cid-a", 2L, 3L, "OPEN", "LONG", "BUY", 4L)).thenReturn(1);
        dispatcher.beginStrategy(1L, "BTCUSDT", "dispatch-a", "OPEN", "cid-a", 2L, 3L, 4L, "BOTH", "BUY");
        verify(mapper).insertPreDispatchIntent(1L, "BTCUSDT", "dispatch-a", "cid-a", 2L, 3L, "OPEN", "LONG", "BUY", 4L);
    }

    @Test
    void oneWayExchangePositionSideIsPersistedAsLogicalLongForClosingSell() {
        when(mapper.insertTicket(1L, "BTCUSDT", "dispatch-a", "CLOSE", "cid-a", 3L, 4L)).thenReturn(1);
        when(mapper.insertPreDispatchIntent(1L, "BTCUSDT", "dispatch-a", "cid-a", 2L, 3L, "CLOSE", "LONG", "SELL", 4L)).thenReturn(1);
        dispatcher.beginStrategy(1L, "BTCUSDT", "dispatch-a", "CLOSE", "cid-a", 2L, 3L, 4L, "BOTH", "SELL");
        verify(mapper).insertPreDispatchIntent(1L, "BTCUSDT", "dispatch-a", "cid-a", 2L, 3L, "CLOSE", "LONG", "SELL", 4L);
    }

    @Test
    void failedIntentInsertRollsBackOutboundTicketClaim() {
        assertThrows(IllegalStateException.class, () -> dispatcher.beginStrategy(1L, "BTCUSDT", "dispatch-a", "OPEN", "cid-a", 2L, 3L, 4L, "LONG", "BUY"));
        verify(mapper).insertTicket(1L, "BTCUSDT", "dispatch-a", "OPEN", "cid-a", 3L, 4L);
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
    }

    @Test
    void presendIntentSelectsThePersistedCycleKeyForTheExactPendingExecution() throws Exception {
        var parameters = new java.util.HashMap<String, Object>();
        parameters.put("credentialId", 1L);
        parameters.put("symbol", "BTCUSDT");
        parameters.put("key", "dispatch-a");
        parameters.put("clientOrderId", "cid-a");
        parameters.put("strategyId", 2L);
        parameters.put("instanceId", 3L);
        parameters.put("operation", "OPEN");
        parameters.put("positionSide", "LONG");
        parameters.put("exchangeSide", "BUY");
        parameters.put("epoch", 4L);
        var configuration = new org.apache.ibatis.session.Configuration();
        String resource = "mapper/quant/LiveOrderDispatchMapper.xml";
        try (java.io.InputStream input = org.apache.ibatis.io.Resources.getResourceAsStream(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String sql = configuration.getMappedStatement(LiveOrderDispatchMapper.class.getName() + ".insertPreDispatchIntent")
            .getBoundSql(parameters).getSql().toLowerCase(java.util.Locale.ROOT);
        assertTrue(sql.contains("s.position_cycle_key"));
        assertTrue(sql.contains("s.pending_idempotency_key"));
        assertTrue(sql.contains("quant_strategy_instance"));
        assertTrue(sql.contains("quant_strategy_symbol"));
        assertTrue(sql.contains("quant_symbol"));
    }

    @Test
    void contradictoryOpeningSideNeverAcquiresGuard() {
        assertThrows(IllegalArgumentException.class, () -> dispatcher.beginStrategy(1L, "BTCUSDT", "dispatch-a", "OPEN", "cid-a", 2L, 3L, 4L, "LONG", "SELL"));
        verifyNoInteractions(mapper);
    }
}

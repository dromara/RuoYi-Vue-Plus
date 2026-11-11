package org.dromara.quant.protection;

import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.StrategyOrderIntentService;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.dromara.quant.protection.mapper.QuantProtectionOrderMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ProtectionOrderJournalServiceTest {
    private final QuantProtectionOrderMapper protections = mock(QuantProtectionOrderMapper.class);
    private final QuantOrderMapper orders = mock(QuantOrderMapper.class);
    private final StrategyOrderIntentService intents = mock(StrategyOrderIntentService.class);
    private final ProtectionOrderJournalService journal = new ProtectionOrderJournalService(protections, orders, intents);
    private final ExchangeGateway.Credential credential = new ExchangeGateway.Credential("test", "test", "HMAC", "LIVE");

    private QuantProtectionOrder local() {
        var local = new QuantProtectionOrder();
        local.setId(71L);
        local.setCredentialId(1L);
        local.setPositionId(51L);
        local.setPositionCycleKey("cycle-a");
        local.setSymbol("BTCUSDT");
        local.setPositionSide("LONG");
        local.setExchangeOrderId(81L);
        local.setClientOrderId("QFTP_test");
        local.setQuantity(BigDecimal.ONE);
        local.setStopPrice(BigDecimal.TEN);
        local.setOrderType("TAKE_PROFIT_MARKET");
        local.setStrategyId(2L);
        local.setStrategyInstanceId(3L);
        return local;
    }

    private ExchangeGateway.ProtectionOrderSnapshot actual(String positionSide) {
        return new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 81L, "QFTP_test", "FINISHED", "SELL", positionSide,
            "TAKE_PROFIT_MARKET", BigDecimal.ONE, BigDecimal.TEN, Instant.now(), BigDecimal.ONE, true, false, 91L, BigDecimal.TEN);
    }

    private ExchangeGateway.OrderSnapshot child(Long id) {
        return new ExchangeGateway.OrderSnapshot("BTCUSDT", id, "child-client", "SELL", "MARKET", "FILLED", BigDecimal.ZERO,
            BigDecimal.ONE, BigDecimal.ONE, Instant.now(), BigDecimal.TEN);
    }

    @Test
    void childRetainsFrozenOwnerAfterRuntimeHasMovedToAnotherCycle() {
        when(orders.insert(any(QuantOrder.class))).thenReturn(1);
        assertTrue(journal.bindChild(credential, local(), actual("BOTH"), child(91L)));
        var saved = org.mockito.ArgumentCaptor.forClass(QuantOrder.class);
        verify(orders).insert(saved.capture());
        assertEquals(2L, saved.getValue().getStrategyId());
        assertEquals(3L, saved.getValue().getStrategyInstanceId());
        assertEquals(91L, saved.getValue().getExchangeOrderId());
        assertTrue(saved.getValue().getReduceOnly());
        verifyNoInteractions(protections);
        verify(intents).recordVerifiedProtectionChild(1L, 2L, 3L, "BTCUSDT", "cycle-a", "LONG", "SELL", 91L, "child-client");
    }

    @Test
    void hedgeChildAlsoRecordsImmutableCloseRole() {
        when(orders.insert(any(QuantOrder.class))).thenReturn(1);
        assertTrue(journal.bindChild(credential, local(), actual("LONG"), child(91L)));
        verify(intents).recordVerifiedProtectionChild(1L, 2L, 3L, "BTCUSDT", "cycle-a", "LONG", "SELL", 91L, "child-client");
    }

    @Test
    void reducedAlgoQuantityIsAcceptedOnlyWhenExactChildProvesFullFill() {
        var parent = local();
        parent.setQuantity(new BigDecimal("2"));
        assertTrue(journal.bindChild(credential, parent, actual("BOTH"), child(91L)));
        verify(orders).insert(any(QuantOrder.class));
        verify(intents).recordVerifiedProtectionChild(1L, 2L, 3L, "BTCUSDT", "cycle-a", "LONG", "SELL", 91L, "child-client");
    }

    @Test
    void reducedAlgoQuantityWithoutFilledChildProofIsRejected() {
        var parent = local();
        parent.setQuantity(new BigDecimal("2"));
        var partialChild = new ExchangeGateway.OrderSnapshot("BTCUSDT", 91L, "child-client", "SELL", "MARKET", "CANCELED",
            BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("0.5"), Instant.now(), BigDecimal.TEN);
        assertThrows(IllegalStateException.class, () -> journal.bindChild(credential, parent, actual("BOTH"), partialChild));
        verifyNoInteractions(orders);
    }

    @Test
    void conflictingExistingAttributionCannotBeOverwritten() {
        var existing = new QuantOrder();
        existing.setId(11L);
        existing.setClientOrderId("child-client");
        existing.setStrategyInstanceId(999L);
        when(orders.selectOne(any())).thenReturn(existing);
        assertThrows(IllegalStateException.class, () -> journal.bindChild(credential, local(), actual("BOTH"), child(91L)));
        verify(orders, never()).updateById(any(QuantOrder.class));
    }

    @Test
    void wrongChildIdentityCannotReceiveParentAttribution() {
        assertThrows(IllegalStateException.class, () -> journal.bindChild(credential, local(), actual("BOTH"), child(92L)));
        verifyNoInteractions(orders);
    }

    @Test
    void unknownHistoricalCycleRemainsUnattributed() {
        var local = local();
        local.setStrategyId(null);
        local.setStrategyInstanceId(null);
        when(protections.cycleOwners(any(), any(), any(), any(), any())).thenReturn(List.of());
        assertFalse(journal.bindChild(credential, local, actual("BOTH"), child(91L)));
        verifyNoInteractions(orders);
    }

    @Test
    void ownerIsCapturedOnlyFromExactPositionCycle() {
        var local = local();
        local.setStrategyId(null);
        local.setStrategyInstanceId(null);
        when(protections.cycleOwners(1L, 51L, "cycle-a", "BTCUSDT", "LONG")).thenReturn(List.of(Map.of("strategy_id", 2L, "instance_id", 3L)));
        journal.captureOwner(local);
        assertEquals(2L, local.getStrategyId());
        assertEquals(3L, local.getStrategyInstanceId());
        verifyNoInteractions(orders);
    }
}

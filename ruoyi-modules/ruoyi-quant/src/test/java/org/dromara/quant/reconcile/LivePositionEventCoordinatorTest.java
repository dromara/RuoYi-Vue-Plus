package org.dromara.quant.reconcile;

import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@Tag("dev")
class LivePositionEventCoordinatorTest {
    @Test
    void supersededSnapshotsStayQueuedWithoutNetworkFailureBackoff() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var live = new QuantCredential();
        live.setId(42L);
        live.setStatus("0");
        live.setRunMode("LIVE");
        when(credentials.selectById(42L)).thenReturn(live);
        doThrow(new TradingReconciliationService.SnapshotSupersededException()).when(reconciliation).refreshPositionsOnly(live);
        coordinator.request(42L);
        for (int attempt = 0; attempt < 4; attempt++) {
            coordinator.drain();
            var failures = (java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(coordinator, "failures");
            org.junit.jupiter.api.Assertions.assertFalse(failures.containsKey(42L));
            var lastAttempt = (java.util.Map<Long, Long>) org.springframework.test.util.ReflectionTestUtils.getField(coordinator, "lastAttempt");
            lastAttempt.put(42L, 0L);
        }
        verify(reconciliation, times(4)).refreshPositionsOnly(live);
    }

    @Test
    void duplicateEventsCoalesceToOneRestPositionRefresh() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        QuantCredential live = new QuantCredential();
        live.setId(42L);
        live.setStatus("0");
        live.setRunMode("LIVE");
        when(credentials.selectById(42L)).thenReturn(live);
        for (int i = 0; i < 100; i++) {
            coordinator.request(42L);
        }
        coordinator.drain();
        coordinator.drain();
        verify(reconciliation, times(1)).refreshPositionsOnly(live);
    }

    @Test
    void nonLiveAccountsNeverTriggerSignedRestPositionCalls() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        QuantCredential paper = new QuantCredential();
        paper.setId(8L);
        paper.setStatus("0");
        paper.setRunMode("PAPER");
        when(credentials.selectById(8L)).thenReturn(paper);
        coordinator.request(8L);
        coordinator.drain();
        verifyNoInteractions(reconciliation);
    }

    @Test
    void failedRestRefreshIsRetriedRatherThanLost() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        QuantCredential live = new QuantCredential();
        live.setId(43L);
        live.setStatus("0");
        live.setRunMode("LIVE");
        when(credentials.selectById(43L)).thenReturn(live);
        doThrow(new IllegalStateException("network failure")).when(reconciliation).refreshPositionsOnly(live);
        coordinator.request(43L);
        coordinator.drain();
        verify(reconciliation, times(1)).refreshPositionsOnly(live);
        // No immediate hot-loop retry; the failed request stays queued.
        coordinator.drain();
        verify(reconciliation, times(1)).refreshPositionsOnly(live);
    }

    @Test
    void newExchangeEventRevokesFreshnessUntilRestVerification() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        freshness.verified(99L);
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);
        org.junit.jupiter.api.Assertions.assertTrue(freshness.fresh(99L));
        coordinator.request(99L);
        org.junit.jupiter.api.Assertions.assertFalse(freshness.fresh(99L));
    }

    @Test
    void existingPrivateStreamCanForwardEventsWithoutOpeningAnotherSubscription() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        freshness.verified(71L);
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);
        coordinator.onUserDataEvent(71L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent("MARGIN_CALL", java.time.Instant.now(), "{}"));
        org.junit.jupiter.api.Assertions.assertTrue(freshness.fresh(71L));
        coordinator.onUserDataEvent(71L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent("ACCOUNT_UPDATE", java.time.Instant.now(), "{}"));
        org.junit.jupiter.api.Assertions.assertFalse(freshness.fresh(71L));
        verifyNoInteractions(credentials, reconciliation);
    }

    @Test
    void nonTradeOrderUpdatesDoNotInvalidatePositionSnapshot() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        freshness.verified(72L);
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);

        coordinator.onUserDataEvent(72L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent(
            "ORDER_TRADE_UPDATE", java.time.Instant.now(), "{\"e\":\"ORDER_TRADE_UPDATE\",\"o\":{\"x\":\"NEW\",\"X\":\"NEW\"}}"));

        org.junit.jupiter.api.Assertions.assertTrue(freshness.fresh(72L));
        coordinator.drain();
        verifyNoInteractions(credentials, reconciliation);
    }

    @Test
    void tradeExecutionInvalidatesPositionSnapshotAndSchedulesRefresh() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        freshness.verified(74L);
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);
        var account = new QuantCredential();
        account.setId(74L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(74L)).thenReturn(account);

        coordinator.onUserDataEvent(74L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent(
            "ORDER_TRADE_UPDATE", java.time.Instant.now(), "{\"e\":\"ORDER_TRADE_UPDATE\",\"o\":{\"x\":\"TRADE\",\"X\":\"PARTIALLY_FILLED\"}}"));
        coordinator.drain();

        org.junit.jupiter.api.Assertions.assertFalse(freshness.fresh(74L));
        verify(reconciliation).refreshPositionsOnly(account);
    }

    @Test
    void malformedOrderUpdateFailsClosedAndSchedulesRefresh() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        freshness.verified(75L);
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);
        var account = new QuantCredential();
        account.setId(75L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(75L)).thenReturn(account);

        coordinator.onUserDataEvent(75L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent(
            "ORDER_TRADE_UPDATE", java.time.Instant.now(), "not-json"));
        coordinator.drain();

        org.junit.jupiter.api.Assertions.assertFalse(freshness.fresh(75L));
        verify(reconciliation).refreshPositionsOnly(account);
    }

    @Test
    void disconnectInvalidatesSnapshotAndReconnectSchedulesFullRefresh() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        freshness.verified(73L);
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);
        var account = new QuantCredential();
        account.setId(73L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(73L)).thenReturn(account);
        coordinator.onUserDataEvent(73L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent("DISCONNECTED", java.time.Instant.now(), null));
        org.junit.jupiter.api.Assertions.assertFalse(freshness.fresh(73L));
        coordinator.onUserDataEvent(73L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent("CONNECTED", java.time.Instant.now(), null));
        coordinator.drain();
        verify(reconciliation).refreshPositionsOnly(account);
    }

    @Test
    void algoUpdateQueuesExactSymbolProtectionRecoveryOffTheStreamCallback() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var protection = mock(org.dromara.quant.protection.ProtectionOrderService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        coordinator.setProtectionOrderService(protection);
        QuantCredential account = new QuantCredential();
        account.setId(88L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(88L)).thenReturn(account);

        coordinator.onUserDataEvent(88L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent(
            "ALGO_UPDATE", java.time.Instant.now(), "{\"e\":\"ALGO_UPDATE\",\"o\":{\"s\":\"BTCUSDT\"}}"));

        verifyNoInteractions(protection);
        coordinator.drain();

        verify(protection).recoverUnresolvedProtectionDispatches(88L, "BTCUSDT");
        verifyNoInteractions(reconciliation);
    }

    @Test
    void reconnectQueuesAccountWideProtectionRecovery() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var protection = mock(org.dromara.quant.protection.ProtectionOrderService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        var freshness = new LivePositionFreshness();
        org.springframework.test.util.ReflectionTestUtils.setField(coordinator, "freshness", freshness);
        coordinator.setProtectionOrderService(protection);
        QuantCredential account = new QuantCredential();
        account.setId(89L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(89L)).thenReturn(account);

        coordinator.onUserDataEvent(89L, new org.dromara.quant.exchange.ExchangeGateway.UserDataEvent("CONNECTED", java.time.Instant.now(), null));
        coordinator.drain();

        org.junit.jupiter.api.Assertions.assertEquals(0L, freshness.generation(89L));
        verify(protection).recoverUnresolvedProtectionDispatches(89L, null);
    }

    @Test
    void dispatchGateBlockQueuesRecoveryForTheOccupiedAccountSymbolSlot() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var protection = mock(org.dromara.quant.protection.ProtectionOrderService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        coordinator.setProtectionOrderService(protection);
        QuantCredential account = new QuantCredential();
        account.setId(90L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(90L)).thenReturn(account);

        coordinator.onDispatchBlocked(new org.dromara.quant.execution.DispatchBlockedEvent(90L, "BTCUSDT", "OPEN"));
        verifyNoInteractions(protection);
        coordinator.drain();

        verify(protection).recoverUnresolvedProtectionDispatches(90L, "BTCUSDT");
    }

    @Test
    void unknownDispatchQueuesImmediateProtectionFactRecovery() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var protection = mock(org.dromara.quant.protection.ProtectionOrderService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        coordinator.setProtectionOrderService(protection);
        QuantCredential account = new QuantCredential();
        account.setId(92L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(92L)).thenReturn(account);

        coordinator.onDispatchRecoveryRequested(new org.dromara.quant.execution.DispatchRecoveryRequestedEvent(92L, "BTCUSDT"));
        coordinator.drain();

        verify(protection).recoverUnresolvedProtectionDispatches(92L, "BTCUSDT");
    }

    @Test
    void unresolvedProtectionRecoveryIsRetriedWithBackoffAfterPrivateStreamAttempt() {
        var credentials = mock(QuantCredentialMapper.class);
        var reconciliation = mock(TradingReconciliationService.class);
        var protection = mock(org.dromara.quant.protection.ProtectionOrderService.class);
        var coordinator = new LivePositionEventCoordinator(credentials, reconciliation);
        coordinator.setProtectionOrderService(protection);
        QuantCredential account = new QuantCredential();
        account.setId(91L);
        account.setStatus("0");
        account.setRunMode("LIVE");
        when(credentials.selectById(91L)).thenReturn(account);
        when(protection.recoverUnresolvedProtectionDispatches(91L, "BTCUSDT")).thenReturn(0);
        when(protection.hasUnresolvedProtectionDispatches(91L, "BTCUSDT")).thenReturn(true);

        coordinator.onDispatchBlocked(new org.dromara.quant.execution.DispatchBlockedEvent(91L, "BTCUSDT", "PROTECT_CREATE"));
        coordinator.drain();
        coordinator.drain();

        verify(protection, times(1)).recoverUnresolvedProtectionDispatches(91L, "BTCUSDT");
        var pending = (java.util.Set<?>) org.springframework.test.util.ReflectionTestUtils.getField(coordinator, "protectionRecoveryPending");
        org.junit.jupiter.api.Assertions.assertEquals(1, pending.size(), "unresolved exact dispatch must stay queued after one failed recovery");

        Object request = pending.iterator().next();
        var retryNotBefore = (java.util.Map<Object, Long>) org.springframework.test.util.ReflectionTestUtils.getField(coordinator, "protectionRetryNotBefore");
        retryNotBefore.put(request, 0L);
        coordinator.drain();

        verify(protection, times(2)).recoverUnresolvedProtectionDispatches(91L, "BTCUSDT");
    }

}

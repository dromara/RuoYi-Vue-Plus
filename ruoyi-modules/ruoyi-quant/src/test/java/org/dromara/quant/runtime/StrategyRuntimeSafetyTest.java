package org.dromara.quant.runtime;

import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.market.domain.QuantSymbol;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.protection.ProtectionOrderService;
import org.dromara.quant.runtime.domain.QuantStrategyPositionState;
import org.dromara.quant.strategy.*;
import org.dromara.quant.strategy.domain.QuantStrategy;
import org.dromara.quant.strategy.domain.QuantStrategyInstance;
import org.dromara.quant.strategy.domain.QuantStrategySymbol;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("dev")
class StrategyRuntimeSafetyTest {
    @Test
    void identifiesStaleOrUnavailableTradeFeedForProtectionFallback() {
        Instant now = Instant.parse("2026-11-09T12:00:00Z");

        assertTrue(StrategyRuntimeService.isMarketPriceStale(false, true, now, now, 5_000L));
        assertTrue(StrategyRuntimeService.isMarketPriceStale(true, false, now, now, 5_000L));
        assertTrue(StrategyRuntimeService.isMarketPriceStale(true, true, null, now, 5_000L));
        assertTrue(StrategyRuntimeService.isMarketPriceStale(true, true, now.minusMillis(5_001), now, 5_000L));
        assertTrue(StrategyRuntimeService.isMarketPriceStale(true, true, now.plusMillis(1), now, 5_000L));
        assertFalse(StrategyRuntimeService.isMarketPriceStale(true, true, now.minusMillis(5_000), now, 5_000L));
    }

    @Test
    void mapsExchangeCloseTimeToTheOwningKlineForSupportedIntervals() {
        assertEquals(Instant.parse("2026-10-17T08:00:00Z"), StrategyRuntimeService.klineOpenAt(Instant.parse("2026-10-17T08:14:59.999Z"), "15m"));
        assertEquals(Instant.parse("2026-10-17T00:00:00Z"), StrategyRuntimeService.klineOpenAt(Instant.parse("2026-10-17T23:59:59Z"), "1d"));
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), StrategyRuntimeService.klineOpenAt(Instant.parse("2026-10-19T00:00:00Z"), "1M"));
        assertNull(StrategyRuntimeService.klineOpenAt(Instant.now(), "unsupported"));
    }

    @Test
    void staleMarketRestPriceEvaluatesOnlyExistingPositionExitSignals() throws Exception {
        Fixture f = fixture();
        try {
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", false);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", false);
            ReflectionTestUtils.setField(f.runtime, "latestTradeEventAt", null);
            ReflectionTestUtils.setField(f.runtime, "nextRestProtectionPollAt", 0L);
            when(gateway.queryLastTradePrice("LIVE", "BTCUSDT")).thenReturn(
                new ExchangeGateway.PriceSnapshot("BTCUSDT", new BigDecimal("99"), null, Instant.now()));

            f.service.checkStaleMarketProtectionPrices();

            verify(gateway, timeout(2_000)).queryLastTradePrice("LIVE", "BTCUSDT");
            verify(f.plugin, timeout(2_000)).evaluateExit(any(), anyMap());
            verify(f.plugin, never()).evaluateEntry(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void publishesNewPositionPeakOnlyAfterStateSave() throws Exception {
        Fixture f = fixture();
        try {
            var stateService = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            StrategyPositionPerformanceHub hub = mock(StrategyPositionPerformanceHub.class);
            ReflectionTestUtils.setField(f.service, "positionPerformanceHub", hub);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setHighestPriceSinceEntry(new BigDecimal("110"));

            ReflectionTestUtils.invokeMethod(f.service, "updateRuntimeExtrema", f.runtime, new BigDecimal("120"));

            var order = inOrder(stateService, hub);
            order.verify(stateService).save(state);
            order.verify(hub).publish(argThat(event -> event.instanceId().equals(1L) && event.positionId().equals(4L) && event.highestPriceSinceEntry().equals(new BigDecimal("120"))));
            verifyNoMoreInteractions(hub);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void preparingNewOpenClearsPositionIdFromPreviousCycle() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setPositionId(4L);

            var pending = (QuantStrategyPositionState) ReflectionTestUtils.invokeMethod(f.service, "preparePendingStateForOpen", f.runtime, "LONG", BigDecimal.ONE,
                new BigDecimal("100"), Instant.parse("2026-10-17T08:00:00Z"), "open-key");

            assertEquals("OPEN_PENDING", pending.getStatus());
            assertEquals("OPEN", pending.getPendingAction());
            assertNull(pending.getPositionId());
            verify((org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService")).save(pending);
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LIVE", "TESTNET"})
    void marketRecoveryStatusKeepsTradingClosedUntilExplicitReady(String mode) throws Exception {
        Fixture f = fixture();
        try {
            var credential = (QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential");
            credential.setRunMode(mode);
            var connected = new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now());
            var recovering = new ExchangeGateway.MarketEvent("CONNECTING", "BTCUSDT", "1m", null, null, Instant.now());
            var disconnected = new ExchangeGateway.MarketEvent("DISCONNECTED", "BTCUSDT", "1m", null, null, Instant.now());

            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, connected);
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            // CONNECTING also means Kline REST recovery while WS remains open.
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, recovering);
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketStreamReady"));

            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, disconnected);
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, connected);
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            // CONNECTED event alone is insufficient: the independent hub health callback must also approve trading.
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketStreamReady"));
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LIVE", "TESTNET"})
    void freshTradeUpdatesSlopeTelemetryWithoutOpeningTradeGate(String mode) throws Exception {
        Fixture f = fixture();
        try {
            var credential = (QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential");
            credential.setRunMode(mode);
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            Object market = ReflectionTestUtils.getField(f.runtime, "market");
            ReflectionTestUtils.setField(market, "previousKlineOpen", new BigDecimal("100"));
            Instant open = Instant.now().minusSeconds(5);
            ReflectionTestUtils.setField(market, "currentKline", new ExchangeGateway.Kline(open, open.plusSeconds(60), new BigDecimal("101"), new BigDecimal("103"),
                new BigDecimal("99"), new BigDecimal("102"), BigDecimal.ONE, false));
            var event = new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("102"), null, Instant.now());
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, event);
            assertEquals(new BigDecimal("102"), ReflectionTestUtils.getField(f.runtime, "latestTradePrice"));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketStreamReady"));
            var telemetry = (StrategySlopeTelemetryHub) ReflectionTestUtils.getField(f.service, "slopeTelemetryHub");
            verify(telemetry, atLeastOnce()).publish(any());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LIVE", "TESTNET"})
    void readyRealtimeTradeDispatchesEntryEvaluationWithoutSendingAnOrder(String mode) throws Exception {
        Fixture f = fixture();
        try {
            var credential = (QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential");
            credential.setRunMode(mode);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            Object market = ReflectionTestUtils.getField(f.runtime, "market");
            Instant open = Instant.now().minusSeconds(5);
            ReflectionTestUtils.setField(market, "currentKline", new ExchangeGateway.Kline(open, open.plusSeconds(60), new BigDecimal("100"), new BigDecimal("103"),
                new BigDecimal("99"), new BigDecimal("101"), BigDecimal.ONE, false));
            ReflectionTestUtils.setField(market, "previousKlineOpen", new BigDecimal("100"));
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            when(f.plugin.evaluateEntry(any(), anyMap())).thenReturn(Optional.empty());

            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("102"), null,
                Instant.now()));

            verify(f.plugin, times(1)).evaluateEntry(argThat(ctx -> new BigDecimal("102").compareTo(ctx.currentPrice()) == 0), anyMap());
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            verify(execution, never()).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void unverifiedPositionFactsBlockNewEntryEvaluation() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "positionFactsReady", false);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            Object market = ReflectionTestUtils.getField(f.runtime, "market");
            Instant open = Instant.now().minusSeconds(5);
            ReflectionTestUtils.setField(market, "currentKline", new ExchangeGateway.Kline(open, open.plusSeconds(60), new BigDecimal("100"), new BigDecimal("103"),
                new BigDecimal("99"), new BigDecimal("101"), BigDecimal.ONE, false));
            ReflectionTestUtils.setField(market, "previousKlineOpen", new BigDecimal("100"));
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);

            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("102"), null,
                Instant.now()));

            verify(f.plugin, never()).evaluateEntry(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void flatSnapshotMustFenceTheFirstFreshCandleBeforeRecoveryCanReleaseEntry() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "closed", true);
            Instant closeCandle = Instant.parse("2026-10-17T08:15:00Z");
            var context = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE, closeCandle, false, Instant.now(),
                BigDecimal.ZERO, BigDecimal.ZERO);
            var guard = (org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService");

            ReflectionTestUtils.invokeMethod(f.service, "fenceSnapshotClosedCandle", f.runtime, context);

            verify(guard).blockClosedCandle(any(), any(), eq("BTCUSDT"), eq(closeCandle));
            verify(f.plugin, never()).evaluateEntry(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionDegradedClearsOnlyAfterFlatStateAndProtectionCleanupAreConfirmed() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "protectionDegraded", true);

            ReflectionTestUtils.invokeMethod(f.service, "clearProtectionDegradedAfterConfirmedFlat", f.runtime);
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"), "unresolved protection cleanup must keep entries blocked");

            state.setRecoveryRequired(false);
            state.setUnsafeRecovery(false);
            ReflectionTestUtils.invokeMethod(f.service, "clearProtectionDegradedAfterConfirmedFlat", f.runtime);
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void confirmedProtectionRecoveryClearsOnlyItsStaleInstanceError() throws Exception {
        Fixture f = fixture();
        try {
            var mapper = (org.dromara.quant.strategy.mapper.QuantStrategyInstanceMapper) ReflectionTestUtils.getField(f.service, "instanceMapper");
            when(mapper.updateById(f.instance)).thenReturn(1);
            f.instance.setLastError("交易所保护创建/同步失败，保留本地止损并等待恢复: timeout");

            ReflectionTestUtils.invokeMethod(f.service, "clearProtectionErrorAfterConfirmedSync", f.runtime);

            assertEquals("", f.instance.getLastError());
            verify(mapper).updateById(f.instance);

            f.instance.setLastError("strategy execution failed");
            clearInvocations(mapper);
            ReflectionTestUtils.invokeMethod(f.service, "clearProtectionErrorAfterConfirmedSync", f.runtime);

            assertEquals("strategy execution failed", f.instance.getLastError());
            verify(mapper, never()).updateById(any(QuantStrategyInstance.class));
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LIVE", "TESTNET"})
    void staleRealtimeTradeNeverReachesEntryEvaluationEvenWhenMarketIsReady(String mode) throws Exception {
        Fixture f = fixture();
        try {
            ((QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential")).setRunMode(mode);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            ReflectionTestUtils.setField(f.runtime, "latestTradePrice", new BigDecimal("90"));
            Instant receivedAt = Instant.now();
            var staleTrade = new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("102"), null, receivedAt.minusSeconds(120), 25L, receivedAt, false);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, staleTrade);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, staleTrade);
            verify(f.plugin, never()).evaluateEntry(any(), anyMap());
            assertEquals(new BigDecimal("90"), ReflectionTestUtils.getField(f.runtime, "latestTradePrice"), "stale events must not overwrite the runtime price");
            assertFalse(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconcileRequested")).get(),
                "stale market events must not enqueue repeated position reconciliation");
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            verify(execution, never()).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void exchangeFreshTradeIsNotDroppedBecauseLocalClockIsAhead() throws Exception {
        Fixture f = fixture();
        try {
            ExchangeGateway gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.exchangeTimeOffsetMillis("LIVE")).thenReturn(-2_979L);
            ((QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential")).setRunMode("LIVE");
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            Instant now = Instant.now();
            Object market = ReflectionTestUtils.getField(f.runtime, "market");
            Instant open = now.minusSeconds(5);
            ReflectionTestUtils.setField(market, "currentKline", new ExchangeGateway.Kline(open, open.plusSeconds(60), new BigDecimal("100"), new BigDecimal("103"),
                new BigDecimal("99"), new BigDecimal("101"), BigDecimal.ONE, false));
            ReflectionTestUtils.setField(market, "previousKlineOpen", new BigDecimal("100"));
            when(f.plugin.evaluateEntry(any(), anyMap())).thenReturn(Optional.empty());

            Instant receivedAt = now.minusMillis(40);
            var event = new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("102"), null, now.minusMillis(3_100), 25L, receivedAt, false);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, event);

            verify(f.plugin, times(1)).evaluateEntry(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LIVE", "TESTNET"})
    void fillReconciliationRequestIsRetainedDuringExitCoordination(String mode) throws Exception {
        Fixture f = fixture();
        try {
            ((QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential")).setRunMode(mode);
            var exitCoordinating = (java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "exitCoordinating");
            var reconcileRequested = (java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconcileRequested");
            exitCoordinating.set(true);
            // UserDataStreamHub's order-fill listener invokes this method; concurrent
            // exit coordination must coalesce the request instead of losing it.
            ReflectionTestUtils.invokeMethod(f.service, "requestReconciliation", f.runtime);
            assertTrue(reconcileRequested.get(), "fill-driven reconciliation must remain queued");
            assertFalse(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).get(), "exit coordinator owns the position until it " +
                "releases its gate");
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void newlyCreatedRuntimeMustNotAssumeMarketStreamIsReady() throws Exception {
        Fixture f = fixture();
        try {
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketStreamReady"), "LIVE and TESTNET must wait for an explicit healthy market-stream callback");
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"), "No trading decision is allowed before live market readiness");
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void marketEventLeaseGateDoesNotPerformSynchronousLeaseRenewal() throws Exception {
        Fixture f = fixture();
        try {
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            ReflectionTestUtils.setField(f.runtime, "leaseValid", false);
            assertFalse((boolean) ReflectionTestUtils.invokeMethod(f.service, "ensureEventLease", f.runtime),
                "an unverified lease must fail closed");
            verify(lease, never()).acquireOrRenew(any(), any(), any());

            ReflectionTestUtils.setField(f.runtime, "leaseValid", true);
            assertTrue((boolean) ReflectionTestUtils.invokeMethod(f.service, "ensureEventLease", f.runtime));
            verify(lease, never()).acquireOrRenew(any(), any(), any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void independentLeaseRenewalFailureFencesRuntimeDecisions() throws Exception {
        Fixture f = fixture();
        try {
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            when(lease.acquireOrRenew(any(), any(), any())).thenReturn(false);

            f.service.renewActiveRuntimeLeases();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
            while (((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "leaseRenewing")).get()
                && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }

            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "leaseValid"), "a failed renewal must immediately fence event decisions");
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"),
                "lease renewal failure must not poison the independent public-market health state");
            assertFalse((boolean) ReflectionTestUtils.invokeMethod(f.service, "ensureEventLease", f.runtime));
            verify(lease, atLeastOnce()).acquireOrRenew(any(), any(), any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void shutdownDoesNotWaitForeverOrReleaseLeaseWhileRenewalIsStuck() throws Exception {
        Fixture f = fixture();
        var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
        var renewalEntered = new java.util.concurrent.CountDownLatch(1);
        var allowRenewalToFinish = new java.util.concurrent.CountDownLatch(1);
        try {
            when(lease.acquireOrRenew(any(), any(), any())).thenAnswer(invocation -> {
                renewalEntered.countDown();
                assertTrue(allowRenewalToFinish.await(5, java.util.concurrent.TimeUnit.SECONDS));
                return true;
            });
            f.service.renewActiveRuntimeLeases();
            assertTrue(renewalEntered.await(2, java.util.concurrent.TimeUnit.SECONDS));
            @SuppressWarnings("unchecked")
            java.util.Map<Long, ?> runtimes = (java.util.Map<Long, ?>) ReflectionTestUtils.getField(f.service, "activeRuntimes");
            runtimes.clear();

            long started = System.nanoTime();
            ReflectionTestUtils.invokeMethod(f.service, "closeRuntime", f.runtime);
            long elapsedMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            assertTrue(elapsedMillis < 3_500L, "shutdown must be bounded when lease renewal is stuck");
            verify(lease, never()).release(1L, 3L, "BTCUSDT");
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "leaseValid"));
        } finally {
            allowRenewalToFinish.countDown();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
            while (((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "leaseRenewing")).get()
                && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            f.service.destroy();
        }
    }

    @Test
    void lateLeaseRenewalReleasesLeaseWhenRuntimeClosesInFlight() throws Exception {
        Fixture f = fixture();
        try {
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            var renewalEntered = new java.util.concurrent.CountDownLatch(1);
            var allowRenewalToFinish = new java.util.concurrent.CountDownLatch(1);
            when(lease.acquireOrRenew(any(), any(), any())).thenAnswer(invocation -> {
                renewalEntered.countDown();
                assertTrue(allowRenewalToFinish.await(2, java.util.concurrent.TimeUnit.SECONDS));
                return true;
            });

            f.service.renewActiveRuntimeLeases();
            assertTrue(renewalEntered.await(2, java.util.concurrent.TimeUnit.SECONDS), "renewal task should reach the database call");
            var closeFailure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread closer = Thread.ofVirtual().start(() -> {
                try {
                    ReflectionTestUtils.invokeMethod(f.service, "closeRuntime", f.runtime);
                } catch (Throwable ex) {
                    closeFailure.set(ex);
                }
            });
            long closeDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
            while (!(boolean) ReflectionTestUtils.getField(f.runtime, "closed") && System.nanoTime() < closeDeadline) {
                Thread.sleep(5);
            }
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "closed"), "shutdown should fence the runtime before waiting for lease operations");
            allowRenewalToFinish.countDown();
            closer.join(2_000);
            assertFalse(closer.isAlive(), "shutdown should release the lease after the in-flight renewal completes");
            assertNull(closeFailure.get(), "runtime shutdown must finish without exception");
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "leaseValid"));
            verify(lease).release(1L, 3L, "BTCUSDT");
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void shutdownRenewsLeaseWhileWaitingForInFlightProtectionTask() throws Exception {
        Fixture f = fixture();
        try {
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            java.util.concurrent.Future<?> protectionTask = mock(java.util.concurrent.Future.class);
            when(protectionTask.get(anyLong(), any(java.util.concurrent.TimeUnit.class)))
                .thenThrow(new java.util.concurrent.TimeoutException("still running"))
                .thenReturn(null);
            ReflectionTestUtils.setField(f.runtime, "protectionTask", protectionTask);

            ReflectionTestUtils.invokeMethod(f.service, "closeRuntime", f.runtime);

            verify(lease).acquireOrRenew(1L, 3L, "BTCUSDT");
            verify(lease).release(1L, 3L, "BTCUSDT");
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "leaseValid"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void leaseRenewalExceptionKeepsRuntimeFencedAndReleasesRenewalGuard() throws Exception {
        Fixture f = fixture();
        try {
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            when(lease.acquireOrRenew(any(), any(), any())).thenThrow(new IllegalStateException("database unavailable"));

            f.service.renewActiveRuntimeLeases();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
            while (((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "leaseRenewing")).get()
                && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }

            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "leaseValid"),
                "an exception makes lease ownership ambiguous and must fence decisions");
            assertFalse(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "leaseRenewing")).get(),
                "the renewal guard must be released after an exception");
            assertFalse((boolean) ReflectionTestUtils.invokeMethod(f.service, "ensureEventLease", f.runtime));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void persistedStepIsRestoredAndExtremaAdvanceItWithoutRegression() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setHighestStep(7);
            state.setHighestPriceSinceEntry(new BigDecimal("100"));
            var event = new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("100"), null, Instant.now());
            StrategyContext restored = ReflectionTestUtils.invokeMethod(f.service, "buildContext", f.runtime, new BigDecimal("100"), event);
            assertEquals(7, restored.highestStep());
            ReflectionTestUtils.invokeMethod(f.service, "updateRuntimeExtrema", f.runtime, new BigDecimal("110"));
            assertTrue(state.getHighestStep() > 7);
            int peak = state.getHighestStep();
            ReflectionTestUtils.invokeMethod(f.service, "updateRuntimeExtrema", f.runtime, new BigDecimal("100"));
            assertEquals(peak, state.getHighestStep());
            verify((org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService"), atLeastOnce()).save(state);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void extremaVersionConflictKeepsStreamConnectedAndBlocksDecisionsUntilReconciliation() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            doThrow(new org.dromara.quant.runtime.service.StrategyPositionStateService.StateConflictException(1L)).when(states).save(state);
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).set(true);
            var event = new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("120"), null, Instant.now());
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, event));
            assertEquals(true, ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            assertEquals(true, ReflectionTestUtils.getField(f.runtime, "stateRefreshRequired"));
            assertEquals(false, ReflectionTestUtils.getField(f.runtime, "positionFactsReady"));
            assertEquals(new BigDecimal("120"), state.getHighestPriceSinceEntry());
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime,
                new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("125"), null, Instant.now())));
            verify(states, times(1)).save(state);
            verify(f.plugin, never()).evaluateExit(any(), anyMap());
            assertEquals(new BigDecimal("125"), state.getHighestPriceSinceEntry());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"true,false,OPEN", "false,false,OPEN", "true,true,OPEN", "true,false,CLOSED"})
    void versionReloadPreservesUnpersistedExtremaOnlyWithinSamePositionCycle(boolean sameCycle, boolean costChanged, String status) throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setVersion(1);
            state.setHighestPriceSinceEntry(new BigDecimal("120"));
            state.setLowestPriceSinceEntry(new BigDecimal("90"));
            state.setHighestStep(4);
            ReflectionTestUtils.setField(f.runtime, "extremaPersistencePending", true);
            var latest = new QuantStrategyPositionState();
            org.springframework.beans.BeanUtils.copyProperties(state, latest);
            latest.setVersion(2);
            latest.setHighestPriceSinceEntry(new BigDecimal("110"));
            latest.setLowestPriceSinceEntry(new BigDecimal("95"));
            latest.setHighestStep(2);
            latest.setStatus(status);
            if (costChanged) {
                latest.setEntryPrice(new BigDecimal("101"));
            }
            if (!sameCycle) {
                latest.setPositionCycleKey("next-cycle");
            }
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            when(states.findByInstance(1L)).thenReturn(latest);
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new org.apache.ibatis.session.Configuration(), "extrema-reload-test"), QuantPosition.class);
            when(mapper.selectList(any())).thenReturn(List.of());
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(reconciliation.runtimePositionSnapshot(any())).thenReturn(List.of());

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);

            assertSame(latest, ReflectionTestUtils.getField(f.runtime, "state"));
            boolean merge = sameCycle && !"CLOSED".equals(status);
            assertEquals(new BigDecimal(merge ? "120" : "110"), latest.getHighestPriceSinceEntry());
            assertEquals(new BigDecimal(merge ? "90" : "95"), latest.getLowestPriceSinceEntry());
            assertEquals(merge && !costChanged ? 4 : 2, latest.getHighestStep());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void completedPositionVerificationReleasesStateConflictBarrier() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setVersion(2);
            ReflectionTestUtils.setField(f.runtime, "stateRefreshRequired", true);
            ReflectionTestUtils.setField(f.runtime, "positionFactsReady", false);
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            when(states.findByInstance(1L)).thenReturn(state);
            var position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new org.apache.ibatis.session.Configuration(), "extrema-resume-test"), QuantPosition.class);
            when(mapper.selectList(any())).thenReturn(List.of(position));
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(reconciliation.runtimePositionSnapshot(any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE,
                new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);

            verify(states).save(state);
            assertEquals(false, ReflectionTestUtils.getField(f.runtime, "stateRefreshRequired"));
            assertEquals(true, ReflectionTestUtils.getField(f.runtime, "positionFactsReady"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void confirmedStepOnlyAdvancesAfterSuccessfulCurrentCycleProtection() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setHighestStep(5);
            state.setConfirmedProtectionStep(2);
            ((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position")).setMarkPrice(new BigDecimal("100"));
            when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenReturn(new org.dromara.quant.protection.domain.QuantProtectionOrder());
            when(f.protection.confirmedStep(any(), any())).thenReturn(5);
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            verify(f.protection, timeout(2_000)).confirmedStep(any(), any());
            for (int i = 0; i < 100 && ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionSyncing")).get(); i++) {
                Thread.sleep(20);
            }
            assertEquals(5, state.getConfirmedProtectionStep());
            assertEquals(5, state.getHighestStep());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void failedStepPersistenceIsRetriedAfterPriceRetraces() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            doThrow(new IllegalStateException("database unavailable")).doNothing().when(states).save(state);
            assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(f.service, "updateRuntimeExtrema", f.runtime, new BigDecimal("110")));
            int peak = state.getHighestStep();
            assertTrue(peak > 0);
            ReflectionTestUtils.invokeMethod(f.service, "updateRuntimeExtrema", f.runtime, new BigDecimal("100"));
            verify(states, times(2)).save(state);
            assertEquals(peak, state.getHighestStep());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void failedProtectionCannotAdvanceConfirmedStep() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setHighestStep(5);
            state.setConfirmedProtectionStep(2);
            doThrow(new IllegalStateException("query timeout")).when(f.protection).syncStrategyProtections(any(), any(), any(), anyList(), any(), any(), anyBoolean());
            when(f.protection.confirmedStep(any(), any())).thenReturn(5);
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            for (int i = 0; i < 100 && ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionSyncing")).get(); i++) {
                Thread.sleep(20);
            }
            assertEquals(2, state.getConfirmedProtectionStep());
            assertEquals(5, state.getHighestStep());
            verify(f.protection, never()).confirmedStep(any(), any());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void slowPendingOrderRecoveryMustNotBlockMarketOrOverwriteChangedPending(boolean fullReconciliation) throws Exception {
        Fixture f = fixture();
        var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
        state.setPendingAction("OPEN");
        state.setPendingOrderId(55L);
        state.setPendingOrderTime(System.currentTimeMillis());
        var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        when(gateway.queryOrder(any(), any(), any(), any())).thenAnswer(invocation -> {
            entered.countDown();
            release.await(3, java.util.concurrent.TimeUnit.SECONDS);
            return new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "old", "BUY", "MARKET", "CANCELED", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, Instant.now());
        });
        try {
            var recovery = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, fullReconciliation ? "reconcilePosition" : "reconcilePendingExecution", f.runtime));
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var update = executor.submit(() -> {
                synchronized (f.runtime) {
                    state.setPendingOrderId(66L);
                    ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("110"), null
                        , Instant.now()));
                }
            });
            assertDoesNotThrow(() -> update.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            release.countDown();
            recovery.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(66L, state.getPendingOrderId());
            assertEquals("OPEN", state.getPendingAction());
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void missingOrderOnlyClearsConfirmedFailedFlatOpen(boolean failed) throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            var instance = (QuantStrategyInstance) ReflectionTestUtils.getField(f.runtime, "instance");
            state.setPendingIdempotencyKey("STRATEGY:" + instance.getId() + ":OPEN:1797000000000");
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setPendingOrderId(55L);
            state.setPendingNotFoundConfirmations(2);
            var record = new org.dromara.quant.execution.domain.QuantExecutionRecord();
            record.setExecutionStatus(failed ? "FAILED" : "UNKNOWN");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            ReflectionTestUtils.invokeMethod(f.service, "applyPendingExecution", f.runtime, record, null, List.of());
            if (failed) {
                verify(states).markClosed(state);
                verify((org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService"))
                    .release(any(), any(), any(), any());
            } else {
                verify(states, never()).markClosed(any());
                verify((org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService"), never())
                    .release(any(), any(), any(), any());
                assertEquals("OPEN", state.getPendingAction());
                assertTrue(Boolean.TRUE.equals(state.getRecoveryRequired()));
            }
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void confirmedFailedFlatOpenReleasesItsExactCandleGuard() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            var instance = (QuantStrategyInstance) ReflectionTestUtils.getField(f.runtime, "instance");
            var credential = (org.dromara.quant.credential.domain.QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential");
            var symbol = (org.dromara.quant.market.domain.QuantSymbol) ReflectionTestUtils.getField(f.runtime, "symbol");
            long candleOpenMillis = 1_797_000_000_000L;
            state.setPendingAction("OPEN");
            state.setPendingIdempotencyKey("STRATEGY:" + instance.getId() + ":OPEN:" + candleOpenMillis);
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setPendingOrderId(55L);
            state.setPendingNotFoundConfirmations(2);
            var record = new org.dromara.quant.execution.domain.QuantExecutionRecord();
            record.setExecutionStatus("FAILED");
            var guard = (org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService");

            ReflectionTestUtils.invokeMethod(f.service, "applyPendingExecution", f.runtime, record, null, List.of());

            verify(guard).release(instance.getId(), credential.getId(), symbol.getSymbol(),
                java.time.Instant.ofEpochMilli(candleOpenMillis));
            verify((org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService")).markClosed(state);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void missingOrderRetainsAndRefreshesConfirmedExposure() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setPendingOrderId(55L);
            state.setPendingNotFoundConfirmations(2);
            var record = new org.dromara.quant.execution.domain.QuantExecutionRecord();
            record.setExecutionStatus("FAILED");
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "test"), QuantPosition.class);
            QuantPosition position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
            position.setQuantity(new BigDecimal("0.5"));
            when(mapper.selectList(any())).thenReturn(List.of(position));
            var snapshots = List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", new BigDecimal("0.5"), new BigDecimal("100"), new BigDecimal("101"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, 10, null, null));
            ReflectionTestUtils.invokeMethod(f.service, "applyPendingExecution", f.runtime, record, null, snapshots);
            assertEquals(new BigDecimal("0.5"), ((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position")).getQuantity());
            assertEquals("OPEN", state.getPendingAction());
            verify((org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService"), never()).markClosed(any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void slopeWarmupRejectsMissingAndDiscontinuousReferenceCandles() throws Exception {
        Fixture f = fixture();
        try {
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryKlines(any(), any(), anyInt())).thenReturn(List.of());
            assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(f.service, "loadInitialMarket", "LIVE", "BTCUSDT", "1m", Map.of(), true));
            var now = Instant.now();
            var older = new ExchangeGateway.Kline(now.minusSeconds(120), now.minusSeconds(60).minusMillis(1), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.ONE, true);
            var current = new ExchangeGateway.Kline(now, now.plusSeconds(60).minusMillis(1), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, false);
            when(gateway.queryKlines(any(), any(), anyInt())).thenReturn(List.of(older, current));
            assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(f.service, "loadInitialMarket", "LIVE", "BTCUSDT", "1m", Map.of(), true));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void identicalProtectionTargetsAreMergedButChangesAndPeriodicChecksRemain() throws Exception {
        Fixture f = fixture();
        try {
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionSyncing")).set(true);
            var requested = (java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionRequested");
            StrategyContext context = ReflectionTestUtils.invokeMethod(f.service, "buildContext", f.runtime, new BigDecimal("100"), new ExchangeGateway.MarketEvent("TRADE",
                "BTCUSDT", "1m", new BigDecimal("100"), null, Instant.now()));
            when(f.plugin.evaluateProtection(any(), anyMap())).thenReturn(List.of(new ProtectionSignal("STEP_TP", "STOP_MARKET", BigDecimal.ONE,
                ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("99"), "test")));
            ReflectionTestUtils.invokeMethod(f.service, "syncPluginProtections", f.runtime, context);
            assertTrue(requested.getAndSet(false));
            ReflectionTestUtils.invokeMethod(f.service, "syncPluginProtections", f.runtime, context);
            assertFalse(requested.get());
            ReflectionTestUtils.setField(f.runtime, "lastProtectionRequestNanos", System.nanoTime() - java.util.concurrent.TimeUnit.SECONDS.toNanos(16));
            ReflectionTestUtils.invokeMethod(f.service, "syncPluginProtections", f.runtime, context);
            assertTrue(requested.getAndSet(false));
            when(f.plugin.evaluateProtection(any(), anyMap())).thenReturn(List.of(new ProtectionSignal("STEP_TP", "STOP_MARKET", BigDecimal.ONE,
                ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("100"), "test")));
            ReflectionTestUtils.invokeMethod(f.service, "syncPluginProtections", f.runtime, context);
            assertTrue(requested.get());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionFailureKeepsLocalExitAndSupervisionAlive() throws Exception {
        Fixture f = fixture();
        when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenThrow(new IllegalStateException("timeout"));
        ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
        assertEquals("RUNNING", f.instance.getStatus(), "must retain protection supervision");
        ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("90"), null,
            Instant.now()));
        verify(f.plugin).evaluateExit(any(), anyMap());
    }

    @Test
    void routinePositionReconciliationDoesNotSuppressRiskReducingExitEvaluation() throws Exception {
        Fixture f = fixture();
        try {
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).set(true);
            ReflectionTestUtils.setField(f.runtime, "positionReconciling", true);
            var properties = (QuantProperties) ReflectionTestUtils.getField(f.service, "properties");
            properties.getProtection().setConfirmationWaitMillis(10_000);
            when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("LOCAL_STOP_LOSS", BigDecimal.ONE, "test", false, "LOCAL_STOP",
                ExitSignal.QuantityBase.CURRENT)));
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("90"), null, null, Instant.now(), false, Instant.now(), BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.TEN, 10, null, null, false);

            ReflectionTestUtils.invokeMethod(f.service, "evaluateExit", f.runtime, context);

            verify(f.plugin).evaluateExit(any(), anyMap());
            assertTrue(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "exitCoordinating")).get());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void historicalReplayOnlyUpdatesMarketAndMustNotTrade() throws Exception {
        Fixture f = fixture();
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
        var candle = new ExchangeGateway.Kline(Instant.now().minusSeconds(60), Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, true);
        var replay = new ExchangeGateway.MarketEvent("KLINE", "BTCUSDT", "1m", null, candle, Instant.now(), null, Instant.now(), true);
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, replay);
        verify(f.plugin, never()).evaluateExit(any(), anyMap());
        verify(f.plugin, never()).evaluateEntry(any(), anyMap());
    }

    @Test
    void filledExitWithStalePositionMustRemainPending() throws Exception {
        Fixture f = fixture();
        var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
        state.setStatus("EXIT_PENDING");
        state.setPendingAction("FIRST_TAKE_PROFIT");
        state.setPendingOrderId(55L);
        state.setPendingOrderTime(System.currentTimeMillis());
        state.setDirection("LONG");
        var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
        var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
        var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
            "test"), QuantPosition.class);
        when(mapper.selectList(any())).thenReturn(List.of((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position")));
        when(reconciliation.refreshSymbolPosition(any(), any(), any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE,
            new BigDecimal("100"), new BigDecimal("101"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
        when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "close", "SELL", "MARKET", "FILLED", BigDecimal.ZERO,
            new BigDecimal("0.5"), new BigDecimal("0.5"), Instant.now()));
        when(gateway.queryPositions(any(), any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE, new BigDecimal("100"),
            new BigDecimal("101"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
        ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime);
        assertEquals("EXIT_PENDING", state.getStatus());
        assertEquals(55L, state.getPendingOrderId());
        assertFalse(Boolean.TRUE.equals(state.getFirstTakeProfitTriggered()));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void partiallyFilledExitRetainsPendingEvenWhenPositionIsEmpty(boolean emptyPosition) throws Exception {
        Fixture f = fixture();
        var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
        var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
        var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
        var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
            "test"), QuantPosition.class);
        when(mapper.selectOne(any())).thenReturn((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position"));
        when(gateway.queryPositions(any(), any())).thenReturn(emptyPosition ? List.of() : List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", new BigDecimal("0.9"),
            new BigDecimal("100"), new BigDecimal("101"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
        when(execution.closeByRuntime(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new org.dromara.quant.execution.ExecutionResult(true, "PARTIALLY_FILLED"
            , 55L, "close"));
        when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("FIRST_TAKE_PROFIT", new BigDecimal("0.5"), "test", true)));
        ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("101"), null,
            Instant.now()));
        ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "exitTask")).get(3, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals("EXIT_PENDING", state.getStatus());
        assertEquals(55L, state.getPendingOrderId());
        assertFalse(Boolean.TRUE.equals(state.getFirstTakeProfitTriggered()));
        f.service.destroy();
    }

    @Test
    void staleTradeDoesNotRestoreExtremaOrTrade() throws Exception {
        Fixture f = fixture();
        var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
        try {
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("110"), null,
                Instant.now().minusSeconds(4), 42L, Instant.now().minusSeconds(4), false));
            assertNull(state.getHighestPriceSinceEntry(), "expired events must not mutate position facts");
            verify(f.plugin, never()).evaluateExit(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void slowProtectionMustNotBlockMarketUpdates() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
            entered.countDown();
            release.await(3, java.util.concurrent.TimeUnit.SECONDS);
            return new org.dromara.quant.protection.domain.QuantProtectionOrder();
        });
        try {
            executor.submit(() -> {
                synchronized (f.runtime) {
                    ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
                }
            });
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var update = executor.submit(() -> {
                ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("110"), null,
                    Instant.now()));
            });
            assertDoesNotThrow(() -> update.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            assertEquals(new BigDecimal("110"), state.getHighestPriceSinceEntry());
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void lateProtectionForClosedPositionIsCleanedWithoutConfirmingCurrentPosition() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
            entered.countDown();
            release.await(3, java.util.concurrent.TimeUnit.SECONDS);
            return new org.dromara.quant.protection.domain.QuantProtectionOrder();
        });
        try {
            var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
            executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime));
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            synchronized (f.runtime) {
                ReflectionTestUtils.setField(f.runtime, "position", null);
            }
            release.countDown();
            verify(f.protection, timeout(1000).atLeastOnce()).cancelActiveProtectionOrders(eq(3L), eq(4L), eq("cycle"), eq(true));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "hardStopConfirmed"));
            executor.shutdownNow();
        } finally {
            release.countDown();
            f.service.destroy();
        }
    }

    @Test
    void slowPositionReconciliationMustNotBlockMarketUpdates() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
        var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
        when(lease.acquireOrRenew(any(), any(), any())).thenReturn(true);
        when(reconciliation.runtimePositionSnapshot(any())).thenAnswer(invocation -> {
            entered.countDown();
            release.await(3, java.util.concurrent.TimeUnit.SECONDS);
            return List.of();
        });
        try {
            executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime));
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var update = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m",
                new BigDecimal("110"), null, Instant.now())));
            assertDoesNotThrow(() -> update.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void partialExitCannotRaceWithInFlightProtectionPlacement() throws Exception {
        Fixture f = fixture();
        ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionSyncing")).set(true);
        when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("FIRST_TAKE_PROFIT", new BigDecimal("0.5"), "test", true)));
        var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
        ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("110"), null,
            Instant.now()));
        verify(execution, never()).closeByRuntime(any(), any(), any(), any(), any(), any(), any(), any());
        f.service.destroy();
    }

    @Test
    void pendingOpenUsesLaterPositionInsteadOfEarlierEmptySnapshot() throws Exception {
        Fixture f = fixture();
        var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
        state.setStatus("OPEN_PENDING");
        state.setPendingAction("OPEN");
        state.setPendingOrderId(55L);
        state.setPendingOrderTime(System.currentTimeMillis());
        state.setDirection("LONG");
        var position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
        ReflectionTestUtils.setField(f.runtime, "position", null);
        var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
        var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
        var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
            "test"), QuantPosition.class);
        when(mapper.selectList(any())).thenReturn(List.of(position));
        when(reconciliation.refreshSymbolPosition(any(), any(), any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE, new BigDecimal("100"),
            new BigDecimal("101"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
        when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "FILLED", BigDecimal.ZERO,
            BigDecimal.ONE, BigDecimal.ONE, Instant.now()));
        try {
            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);
            assertNotNull(ReflectionTestUtils.getField(f.runtime, "position"));
            var reads = inOrder(gateway);
            reads.verify(gateway).queryOrder(any(), any(), any(), any());
            verify(reconciliation).refreshSymbolPosition(any(), eq("BTCUSDT"), any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void partialOpenWithoutDirectionRemainsBlockedForRecovery() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("OPEN_PENDING");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(55L);
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection(null);
            var position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
            position.setQuantity(new BigDecimal("0.5"));
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
                "test"), QuantPosition.class);
            when(mapper.selectList(any())).thenReturn(List.of(position));
            var partialOrder = new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "CANCELED", BigDecimal.ZERO, new BigDecimal("0.5"),
                new BigDecimal("0.5"), Instant.now());

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "applyPendingExecution", f.runtime, null, partialOrder, List.of()));

            assertEquals("OPEN", state.getPendingAction());
            assertTrue(Boolean.TRUE.equals(state.getRecoveryRequired()));
            assertTrue(Boolean.TRUE.equals(state.getUnsafeRecovery()));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void closingRuntimeCannotReleaseLeaseBeforeProtectionRestFinishes() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
        when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
            entered.countDown();
            release.await(3, java.util.concurrent.TimeUnit.SECONDS);
            return new org.dromara.quant.protection.domain.QuantProtectionOrder();
        });
        try {
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var closing = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "closeRuntime", f.runtime));
            Thread.sleep(100);
            verify(lease, never()).release(any(), any(), any());
            assertFalse(closing.isDone());
            release.countDown();
            closing.get(1, java.util.concurrent.TimeUnit.SECONDS);
            verify(lease).release(eq(1L), eq(3L), eq("BTCUSDT"));
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void delayedExchangeEventMustNotTradeEvenWhenJustReceived() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("110"), null,
                Instant.now().minusSeconds(10), 42L, Instant.now(), false));
            verify(f.plugin, never()).evaluateExit(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void staleTradePriceCannotOverrideFreshKline() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
            // 只设置旧价格缓存，避免过期事件触发异步持仓对账干扰本测试。
            ReflectionTestUtils.setField(f.runtime, "latestTradePrice", new BigDecimal("90"));
            ReflectionTestUtils.setField(f.runtime, "latestTradeEventAt", Instant.now().minusSeconds(10));
            var now = Instant.now();
            var candle = new ExchangeGateway.Kline(now.minusSeconds(30), now.plusSeconds(30), new BigDecimal("100"), new BigDecimal("111"), new BigDecimal("100"),
                new BigDecimal("110"), BigDecimal.ONE, false);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("KLINE", "BTCUSDT", "1m", null, candle, now));
            verify(f.plugin).evaluateExit(argThat(context -> new BigDecimal("110").compareTo(context.currentPrice()) == 0), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void externalHealthGateStopsDecisionsBeforeQueuedDisconnectArrives() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", false);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal("110"), null,
                Instant.now()));
            verify(f.plugin, never()).evaluateExit(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void disconnectDuringBalanceAdmissionCannotClaimAnEntry() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var strategy = (QuantStrategy) ReflectionTestUtils.getField(f.runtime, "strategy");
            strategy.setDefaultOpenRatio(new BigDecimal("0.1"));
            when(f.plugin.evaluateEntry(any(), anyMap())).thenReturn(Optional.of(new EntrySignal(EntrySignal.Direction.LONG, "test")));
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var capacity = (org.dromara.quant.risk.OpenCapacityService) ReflectionTestUtils.getField(f.service, "openCapacityService");
            when(capacity.availableBalance(any(), any())).thenAnswer(i -> {
                ReflectionTestUtils.setField(f.runtime, "marketStreamReady", false);
                return BigDecimal.TEN;
            });
            var context = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE, Instant.now(), false, Instant.now(), BigDecimal.ZERO,
                BigDecimal.ZERO);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);
            var guard = (org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService");
            verify(guard, never()).tryClaim(any(), any(), any(), any());
        } finally {
            f.service.destroy();
        }
    }


    @Test
    void rejectedOpenReleasesClaimAndDoesNotFailMarketConsumer() throws Exception {
        verifyOpenFailure(false);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"LIVE,FILLED", "TESTNET,FILLED", "LIVE,NEW", "LIVE,PARTIALLY_FILLED"})
    void synchronousOpenReportsQueriedOrderFactWithoutPrivateStream(String mode, String status) throws Exception {
        Fixture f = fixture();
        try {
            var position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var credential = (QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential");
            credential.setRunMode(mode);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            ((QuantStrategySymbol) ReflectionTestUtils.getField(f.runtime, "strategySymbol")).setOpenRatio(new BigDecimal("0.1"));
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).set(true);
            ReflectionTestUtils.setField(f.runtime, "marketHealthy", true);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            var capacity = (org.dromara.quant.risk.OpenCapacityService) ReflectionTestUtils.getField(f.service, "openCapacityService");
            when(capacity.availableBalance(any(), any())).thenReturn(new BigDecimal("100"));
            when(f.plugin.evaluateEntry(any(), anyMap())).thenReturn(Optional.of(new EntrySignal(EntrySignal.Direction.LONG, "test slope")));
            var guard = (org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService");
            when(guard.tryClaim(any(), any(), any(), any())).thenReturn(true);
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            when(execution.openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(new org.dromara.quant.execution.ExecutionResult(true, status, 55L, "confirmed-open"));
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(reconciliation.refreshSymbolPosition(any(), any(), any())).thenReturn(List.of());
            var positions = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new org.apache.ibatis.session.Configuration(), "open-log-test"), QuantPosition.class);
            when(positions.selectList(any())).thenReturn(List.of(position));
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            BigDecimal filled = "NEW".equals(status) ? BigDecimal.ZERO : "PARTIALLY_FILLED".equals(status) ? new BigDecimal("0.5") : BigDecimal.ONE;
            var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "confirmed-open", "BUY", "MARKET", status,
                BigDecimal.ZERO, BigDecimal.ONE, filled, Instant.now(), new BigDecimal("101"));
            when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(order);
            var logs = mock(org.dromara.quant.decision.TradeDecisionLogService.class);
            ReflectionTestUtils.setField(f.service, "decisionLogs", logs);
            var context = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE,
                Instant.now(), false, Instant.now(), BigDecimal.ZERO, BigDecimal.ZERO);

            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);

            verify(gateway).queryOrder(any(), eq("BTCUSDT"), eq(55L), eq("confirmed-open"));
            verify(logs).order(3L, order);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void unknownOpenRetainsClaimAndCannotRepeatOrder() throws Exception {
        verifyOpenFailure(true);
    }

    @Test
    void capacityFailureWaitsForReleasedSlotBeforeRetrying() throws Exception {
        verifyOpenFailure(false, true);
    }

    private void verifyOpenFailure(boolean unknown) throws Exception {
        verifyOpenFailure(unknown, false);
    }

    private void verifyOpenFailure(boolean unknown, boolean capacityBlocked) throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            var symbolConfig = (QuantStrategySymbol) ReflectionTestUtils.getField(f.runtime, "strategySymbol");
            symbolConfig.setOpenRatio(new BigDecimal("0.1"));
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).set(true);
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var capacity = (org.dromara.quant.risk.OpenCapacityService) ReflectionTestUtils.getField(f.service, "openCapacityService");
            when(capacity.availableBalance(any(), any())).thenReturn(new BigDecimal("100"));
            when(f.plugin.evaluateEntry(any(), anyMap())).thenReturn(Optional.of(new EntrySignal(EntrySignal.Direction.LONG, "test")));
            var guard = (org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService");
            when(guard.tryClaim(any(), any(), any(), any())).thenReturn(true);
            var records = (org.dromara.quant.execution.ExecutionRecordService) ReflectionTestUtils.getField(f.service, "executionRecordService");
            var attempted = new java.util.concurrent.atomic.AtomicBoolean();
            var record = new org.dromara.quant.execution.domain.QuantExecutionRecord();
            record.setExecutionStatus(unknown ? "UNKNOWN" : "FAILED");
            when(records.find(any(), any())).thenAnswer(i -> attempted.get() ? record : null);
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            doAnswer(i -> {
                assertEquals("CLOSED", state.getStatus(), "cleanup must run before pending overwrites close evidence");
                assertNull(state.getPendingAction());
                return null;
            }).when(execution).releaseClosedOpenReservationsForRuntime(any(), any(), any(), any());
            var capacityState = new java.util.concurrent.atomic.AtomicReference<>("occupied");
            doAnswer(i -> {
                if ("occupied".equals(capacityState.get())) {
                    throw new org.dromara.quant.risk.OpenCapacityService.CapacityBlockedException("账户最大持仓数已达到", true);
                }
                return null;
            }).when(execution).requireOpenCapacityForRuntime(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(),
                nullable(org.dromara.quant.market.AccountBalanceSnapshot.class));
            when(execution.openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any())).thenAnswer(i -> {
                attempted.set(true);
                if (unknown) {
                    throw new IllegalStateException("timeout", new java.net.SocketTimeoutException());
                }
                if (capacityBlocked) {
                    throw new org.dromara.quant.risk.OpenCapacityService.CapacityBlockedException("账户最大持仓数已达到", true);
                }
                throw new org.dromara.quant.exchange.ExchangeTradingPort.OrderRejectedException(-4023, "step size");
            });
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            doAnswer(i -> {
                state.setStatus("CLOSED");
                state.setPositionQty(BigDecimal.ZERO);
                state.setPendingAction(null);
                return null;
            }).when(states).markClosed(state);
            ReflectionTestUtils.setField(f.runtime, "marketStreamReady", true);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
            var context = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE, Instant.now(), false, Instant.now(), BigDecimal.ZERO,
                BigDecimal.ZERO);
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context));
            assertTrue(attempted.get(), "test must actually attempt execution");
            var order = inOrder(execution);
            order.verify(execution).releaseClosedOpenReservationsForRuntime(any(), any(), any(), any());
            order.verify(execution).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "marketHealthy"));
            assertEquals(unknown ? "OPEN_PENDING" : "CLOSED", state.getStatus());
            if (unknown) {
                assertTrue(Boolean.TRUE.equals(state.getRecoveryRequired()));
                verify(guard, never()).release(any(), any(), any(), any());
            } else {
                verify(guard).release(any(), any(), any(), any());
                assertNull(state.getPendingAction());
                assertEquals(0, state.getPositionQty().signum());
                verify((org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService")).markClosed(state);
            }
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);
            verify(execution, times(1)).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
            // 价格或行情时间变化不能绕过执行退避，策略仍会评估信号。
            var changedPrice = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("101"), BigDecimal.ONE, context.klineOpenTime(), false, context.eventTime()
                , BigDecimal.ZERO, BigDecimal.ZERO);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, changedPrice);
            verify(execution, times(1)).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
            var fresh = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("102"), BigDecimal.ONE, context.klineOpenTime(), false,
                context.eventTime().plusMillis(1), BigDecimal.ZERO, BigDecimal.ZERO);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, fresh);
            verify(execution, times(1)).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
            if (!unknown) {
                var retryGate = ReflectionTestUtils.getField(f.runtime, "entryRetryGate");
                ReflectionTestUtils.setField(retryGate, "nextCheckAt", 0L);
                if (capacityBlocked) {
                    ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, fresh);
                    verify(execution, times(1)).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
                    capacityState.set("released");
                    ReflectionTestUtils.setField(retryGate, "nextCheckAt", 0L);
                }
                ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, fresh);
                verify(execution, times(2)).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
            }
            verify(gateway, never()).queryAccount(any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void failedReconciliationBacksOffInsteadOfImmediatelyQueueingAgain() throws Exception {
        Fixture f = fixture();
        try {
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(reconciliation.runtimePositionSnapshot(any())).thenThrow(new IllegalStateException("REST queue timeout"));
            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);
            assertTrue((long) ReflectionTestUtils.getField(f.runtime, "nextReconcileAt") > System.currentTimeMillis());
            ReflectionTestUtils.invokeMethod(f.service, "requestReconciliation", f.runtime);
            verify(reconciliation, times(1)).runtimePositionSnapshot(any());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void pendingProtectionExecutionWaitsForReconciliationWithoutRecordingRuntimeError() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            var position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            position.setCredentialId(3L);
            f.instance.setLastError("previous-error");
            when(reconciliation.runtimePositionSnapshot(any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE,
                new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
            when(mapper.selectList(any())).thenReturn(List.of(position));
            when(f.protection.firstTakeProfitExecutedQuantity(any(), any())).thenThrow(new ProtectionOrderService.ProtectionExecutionPendingException());

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);

            assertEquals("previous-error", f.instance.getLastError(), "transient protection execution is not a strategy runtime failure");
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "positionFactsReady"), "unresolved protection facts must continue blocking decisions");
            assertTrue((long) ReflectionTestUtils.getField(f.runtime, "nextReconcileAt") > System.currentTimeMillis(), "reconciliation must back off before retrying");
            verify(f.protection).firstTakeProfitExecutedQuantity(3L, position);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void pendingProtectionTargetKeepsEntryBlockedWithoutPersistingRuntimeFailure() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            var mapper = (org.dromara.quant.strategy.mapper.QuantStrategyInstanceMapper) ReflectionTestUtils.getField(f.service, "instanceMapper");
            f.instance.setLastError("previous-error");

            ReflectionTestUtils.invokeMethod(f.service, "markProtectionDegraded", f.runtime, "交易所保护创建/同步失败",
                new org.dromara.quant.protection.ProtectionTargetCoordinator.PendingTargetException(null, 77L));

            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
            assertTrue(state.getRecoveryRequired());
            assertFalse(state.getUnsafeRecovery(), "保护未确认只阻断新开仓，不能否定可信持仓归属");
            assertEquals("previous-error", f.instance.getLastError());
            verify(mapper, never()).updateById(any(QuantStrategyInstance.class));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void pendingOrderStillPublishesChangingSlopeWithoutTrading() throws Exception {
        Fixture f = fixture();
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(StrategyRuntimeService.class);
        var originalLevel = logger.getLevel();
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", mock(KlineSlopeRateStrategy.class));
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderTime(System.currentTimeMillis());
            var hub = (StrategySlopeTelemetryHub) ReflectionTestUtils.getField(f.service, "slopeTelemetryHub");
            var now = Instant.now();
            var candle = new ExchangeGateway.Kline(now.minusSeconds(60), now, new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
                BigDecimal.ONE, true);
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT", "1m", null, null, now));
            ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("KLINE", "BTCUSDT", "1m", null, candle, now));
            var market = ReflectionTestUtils.getField(f.runtime, "market");
            for (String field : List.of("previousKlineOpen", "secondPreviousKlineOpen", "thirdPreviousKlineOpen")) {
                ReflectionTestUtils.setField(market, field, new BigDecimal("100"));
            }
            clearInvocations(hub);
            for (String price : List.of("101", "102")) {
                ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", new BigDecimal(price), null,
                    Instant.now()));
            }
            var updates = org.mockito.ArgumentCaptor.forClass(StrategySlopeTelemetry.class);
            verify(hub, times(2)).publish(updates.capture());
            assertNotEquals(updates.getAllValues().get(0).degrees(), updates.getAllValues().get(1).degrees());
            var plugin = (StrategyPlugin) ReflectionTestUtils.getField(f.runtime, "plugin");
            verify(plugin, never()).evaluateEntry(any(), anyMap());
            verify(plugin, never()).evaluateExit(any(), anyMap());
            assertEquals("OPEN", state.getPendingAction());
            assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("策略实时斜率") && event.getFormattedMessage().contains("pendingAction=OPEN")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLevel);
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void slowPositionReconciliationMustNotHoldMarketMonitor(boolean slowDatabase) throws Exception {
        Fixture f = fixture();
        var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
            "test"), QuantPosition.class);
        var existing = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
        existing.setCredentialId(3L);
        when(mapper.selectList(any())).thenReturn(List.of(existing));
        when(mapper.update(any(QuantPosition.class), any())).thenReturn(1);
        var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
        when(reconciliation.runtimePositionSnapshot(any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE, new BigDecimal("100"),
            new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        if (slowDatabase) {
            var readCount = new java.util.concurrent.atomic.AtomicInteger();
            when(mapper.selectList(any())).thenAnswer(i -> {
                if (readCount.incrementAndGet() == 2) {
                    entered.countDown();
                    release.await(3, java.util.concurrent.TimeUnit.SECONDS);
                }
                return List.of(existing);
            });
        } else {
            when(f.protection.firstTakeProfitExecutedQuantity(any(), any())).thenAnswer(i -> {
                entered.countDown();
                release.await(3, java.util.concurrent.TimeUnit.SECONDS);
                return BigDecimal.ZERO;
            });
        }
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var recovery = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime));
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var update = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("CONNECTED", "BTCUSDT",
                "1m", null, null, Instant.now())));
            assertDoesNotThrow(() -> update.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            synchronized (f.runtime) {
                state.setPendingAction("CLOSE");
                state.setPendingOrderId(66L);
            }
            release.countDown();
            recovery.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals("CLOSE", state.getPendingAction(), "旧对账不能清理新待执行动作");
            assertEquals(66L, state.getPendingOrderId());
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void changedDatabasePositionRejectsStaleReconciliationBeforeRuntimeCommit() throws Exception {
        Fixture f = fixture();
        try {
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "test"), QuantPosition.class);
            var existing = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
            existing.setCredentialId(3L);
            when(mapper.selectList(any())).thenReturn(List.of(existing));
            when(mapper.update(any(QuantPosition.class), any())).thenReturn(0);
            when(gateway.queryPositions(any(), any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", new BigDecimal("0.5"), new BigDecimal("100"),
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);
            assertSame(existing, ReflectionTestUtils.getField(f.runtime, "position"));
            assertEquals(BigDecimal.ONE, existing.getQuantity());
            verify(f.protection, never()).isFirstTakeProfitFilled(any(), any(), any());
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "positionReconciling"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void persistedRecoveryFlagsRehydrateProtectionCleanupRetryAfterRestart() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setPendingAction(null);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).set(true);

            ReflectionTestUtils.invokeMethod(f.service, "resumeRecoveryIfNeeded", f.runtime);

            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
            assertTrue(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconcileRequested")).get());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void persistedPendingActionWithoutTimestampStartsFailClosedRecovery() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("CLOSE");
            state.setPendingIdempotencyKey("STRATEGY:1:CLOSE:1792300500000");
            state.setRecoveryRequired(false);
            state.setUnsafeRecovery(false);
            ((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconciling")).set(true);

            ReflectionTestUtils.invokeMethod(f.service, "resumeRecoveryIfNeeded", f.runtime);

            assertTrue(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "reconcileRequested")).get());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void failedProtectionCleanupKeepsRecoveryFlagsPersistedAfterFlatReconciliation() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("PROTECTION_CLEANUP");
            state.setPositionQty(new BigDecimal("30043.5"));
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            state.setPendingAction(null);
            ReflectionTestUtils.setField(f.runtime, "position", null);

            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(mapper.selectList(any())).thenReturn(List.of());
            when(reconciliation.runtimePositionSnapshot(any())).thenReturn(List.of());
            when(f.protection.cancelActiveProtectionOrders(3L, 4L, true)).thenReturn(false);
            when(states.findByInstance(1L)).thenReturn(state);
            doAnswer(invocation -> {
                state.setStatus("CLOSED");
                state.setPositionQty(BigDecimal.ZERO);
                state.setPendingAction(null);
                state.setRecoveryRequired(false);
                state.setUnsafeRecovery(false);
                return null;
            }).when(states).markClosed(state);

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);

            assertEquals("CLOSED", state.getStatus());
            assertEquals(BigDecimal.ZERO, state.getPositionQty());
            assertTrue(state.getRecoveryRequired());
            assertTrue(state.getUnsafeRecovery());
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
            verify(states).markClosed(state);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void completedFlatReconciliationLogsEntryReevaluationReadiness() throws Exception {
        Fixture f = fixture();
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(StrategyRuntimeService.class);
        var originalLevel = logger.getLevel();
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(mapper.selectList(any())).thenReturn(List.of());
            when(reconciliation.runtimePositionSnapshot(any())).thenReturn(List.of());
            when(f.protection.cancelActiveProtectionOrders(3L, 4L, true)).thenReturn(true);
            when(states.findByInstance(1L)).thenReturn(state);
            doAnswer(invocation -> {
                state.setStatus("CLOSED");
                state.setPositionQty(BigDecimal.ZERO);
                state.setPendingAction(null);
                state.setRecoveryRequired(false);
                state.setUnsafeRecovery(false);
                return null;
            }).when(states).markClosed(state);

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);

            assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("持仓核实完成")
                && event.getFormattedMessage().contains("恢复阻断已解除") && event.getFormattedMessage().contains("instanceId=1")
                && event.getFormattedMessage().contains("symbol=BTCUSDT")));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "positionReconciling"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(originalLevel);
            f.service.destroy();
        }
    }

    @Test
    void missingPositionIdRequiresFlatExchangeProofAndCancelsProtectionOrdersBySymbol() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPositionId(null);
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);

            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryPositions(nullable(ExchangeGateway.Credential.class), eq("BTCUSDT"))).thenReturn(List.of());
            when(f.protection.cancelActiveProtectionOrdersForSymbol(3L, "BTCUSDT", true)).thenReturn(true);

            assertTrue((boolean) ReflectionTestUtils.invokeMethod(f.service, "cancelStaleProtection", f.runtime));
            verify(f.protection).cancelActiveProtectionOrdersForSymbol(3L, "BTCUSDT", true);
            assertFalse(state.getRecoveryRequired());
            assertFalse(state.getUnsafeRecovery());
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void missingPositionIdAndUnknownExchangePositionMustKeepRecoveryBlocked() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPositionId(null);
            state.setStatus("EXIT_PENDING");
            state.setPendingAction("CLOSE");
            state.setRecoveryRequired(false);
            state.setUnsafeRecovery(false);
            ReflectionTestUtils.setField(f.runtime, "position", null);

            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryPositions(nullable(ExchangeGateway.Credential.class), eq("BTCUSDT")))
                .thenThrow(new IllegalStateException("REST unavailable"));

            assertFalse((boolean) ReflectionTestUtils.invokeMethod(f.service, "cancelStaleProtection", f.runtime));
            verify(f.protection, never()).cancelActiveProtectionOrdersForSymbol(any(), anyString(), anyBoolean());
            assertEquals("EXIT_PENDING", state.getStatus());
            assertEquals("CLOSE", state.getPendingAction());
            assertTrue(state.getRecoveryRequired());
            assertTrue(state.getUnsafeRecovery());
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void confirmedFlatClosedStateClearsStaleRecoveryFlags() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            state.setPendingAction(null);
            ReflectionTestUtils.setField(f.runtime, "position", null);

            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "test"), QuantPosition.class);
            when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of());
            when(mapper.selectList(any())).thenReturn(List.of());
            when(f.protection.cancelActiveProtectionOrders(any(), eq(4L), anyBoolean())).thenReturn(true);
            doAnswer(invocation -> {
                state.setRecoveryRequired(false);
                state.setUnsafeRecovery(false);
                return null;
            }).when(states).markClosed(state);

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePosition", f.runtime);

            verify(states).markClosed(state);
            assertFalse(state.getRecoveryRequired());
            assertFalse(state.getUnsafeRecovery());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void filledOpenFlatOnlyClosesWithCompleteTradeProof() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setPendingOrderId(55L);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "FILLED", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, Instant.now());
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of());
            ReflectionTestUtils.invokeMethod(f.service, "applyPendingExecution", f.runtime, null, order, List.of());
            assertEquals("OPEN", state.getPendingAction());
            verify(states, never()).markClosed(any());
            when(f.protection.cancelActiveProtectionOrders(any(), any(), anyBoolean())).thenReturn(false);
            ReflectionTestUtils.invokeMethod(f.service, "applyPendingExecution", f.runtime, null, order, List.of(), true);
            verify(states, never()).markClosed(any());
            assertEquals("CLOSED", state.getStatus());
            assertNull(state.getPendingAction());
            assertTrue(Boolean.TRUE.equals(state.getRecoveryRequired()), "failed protection cleanup must keep opening blocked");
            when(f.protection.cancelActiveProtectionOrders(any(), any(), anyBoolean())).thenReturn(true);
            when(f.protection.cancelActiveProtectionOrders(any(), any(), any(), anyBoolean())).thenReturn(true);
            ReflectionTestUtils.invokeMethod(f.service, "retryProtectionCleanupIfNeeded", f.runtime);
            assertFalse(Boolean.TRUE.equals(state.getRecoveryRequired()));
            assertFalse(Boolean.TRUE.equals(state.getUnsafeRecovery()));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionCleanupRetryPreservesOrdersWhileExchangePositionIsOpen() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "protectionCleanupPending", true);
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", BigDecimal.ONE, new BigDecimal("100"),
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));

            ReflectionTestUtils.invokeMethod(f.service, "retryProtectionCleanupIfNeeded", f.runtime);

            verify(f.protection, never()).cancelActiveProtectionOrders(any(), any(), any(), anyBoolean());
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionCleanupRetryAcceptsNewerStateWhenConcurrentWriterAlreadyClearedRecovery() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "protectionCleanupPending", true);

            var latest = new QuantStrategyPositionState();
            latest.setInstanceId(state.getInstanceId());
            latest.setPositionId(state.getPositionId());
            latest.setPositionCycleKey(state.getPositionCycleKey());
            latest.setStatus("CLOSED");
            latest.setPositionQty(BigDecimal.ZERO);
            latest.setRecoveryRequired(false);
            latest.setUnsafeRecovery(false);
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            doThrow(new org.dromara.quant.runtime.service.StrategyPositionStateService.StateConflictException(1L)).when(states).save(state);
            when(states.findByInstance(1L)).thenReturn(latest);
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of());
            when(f.protection.cancelActiveProtectionOrders(any(), eq(4L), eq("cycle"), anyBoolean())).thenReturn(true);

            ReflectionTestUtils.invokeMethod(f.service, "retryProtectionCleanupIfNeeded", f.runtime);

            assertSame(latest, ReflectionTestUtils.getField(f.runtime, "state"));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
            verify(states, never()).save(latest);
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionCleanupRetryKeepsEntryBlockedWhenConcurrentStateStillRequiresRecovery() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "protectionCleanupPending", true);

            var latest = new QuantStrategyPositionState();
            latest.setInstanceId(state.getInstanceId());
            latest.setPositionId(state.getPositionId());
            latest.setPositionCycleKey(state.getPositionCycleKey());
            latest.setVersion(2);
            latest.setStatus("CLOSED");
            latest.setPositionQty(BigDecimal.ZERO);
            latest.setRecoveryRequired(true);
            latest.setUnsafeRecovery(true);
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            doThrow(new org.dromara.quant.runtime.service.StrategyPositionStateService.StateConflictException(1L)).when(states).save(state);
            when(states.findByInstance(1L)).thenReturn(latest);
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of());
            when(f.protection.cancelActiveProtectionOrders(any(), eq(4L), eq("cycle"), anyBoolean())).thenReturn(true);

            assertTrue((boolean) ReflectionTestUtils.invokeMethod(f.service, "retryProtectionCleanupIfNeeded", f.runtime));

            assertSame(latest, ReflectionTestUtils.getField(f.runtime, "state"));
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionCleanupPending"));
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "stateRefreshRequired"));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "positionFactsReady"));
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"complete", "partial", "missingOpen", "duplicate", "otherOpen", "missingTime", "truncated"})
    void closedOpenProofRejectsIncompleteOrAmbiguousTrades(String scenario) throws Exception {
        Fixture f = fixture();
        try {
            Instant time = Instant.now();
            var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "FILLED", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, time);
            var open = new ExchangeGateway.TradeSnapshot("BTCUSDT", 1L, 55L, "BUY", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, time);
            var close = new ExchangeGateway.TradeSnapshot("BTCUSDT", 2L, 66L, "SELL", scenario.equals("partial") ? new BigDecimal("0.5") : BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, scenario.equals("missingTime") ? null : time.plusSeconds(1));
            List<ExchangeGateway.TradeSnapshot> trades = new ArrayList<>(List.of(open, close));
            if (scenario.equals("missingOpen")) {
                trades.remove(0);
            }
            if (scenario.equals("duplicate")) {
                trades.add(close);
            }
            if (scenario.equals("otherOpen")) {
                trades.add(new ExchangeGateway.TradeSnapshot("BTCUSDT", 3L, 77L, "BUY", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, time.plusSeconds(2)));
            }
            if (scenario.equals("truncated")) {
                trades = Collections.nCopies(1000, open);
            }
            boolean proof = ReflectionTestUtils.invokeMethod(f.service, "closedPendingOpenProven", order, "LONG", trades);
            assertEquals(scenario.equals("complete"), proof);
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void pendingRecoveryRequiresSingleDirectionAndRechecksFlatAfterTrades(boolean hedgeMode) throws Exception {
        Fixture f = fixture();
        try {
            Instant time = Instant.now().minusSeconds(2);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderTime(time.toEpochMilli());
            state.setPendingOrderId(55L);
            state.setDirection("LONG");
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            var states = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "FILLED", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, time);
            when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(order);
            when(reconciliation.refreshSymbolPosition(any(), any(), any())).thenReturn(List.of());
            when(gateway.queryPositionMode(any())).thenReturn(new ExchangeGateway.PositionMode(hedgeMode));
            when(gateway.queryTrades(any(), any(), any())).thenAnswer(invocation -> {
                assertFalse(Thread.holdsLock(f.runtime), "REST must remain outside the market lock");
                return List.of(new ExchangeGateway.TradeSnapshot("BTCUSDT", 1L, 55L, "BUY", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, time),
                    new ExchangeGateway.TradeSnapshot("BTCUSDT", 2L, 66L, "SELL", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, time.plusSeconds(1)));
            });
            when(f.protection.cancelActiveProtectionOrders(any(), any(), anyBoolean())).thenReturn(true);
            when(f.protection.cancelActiveProtectionOrders(any(), any(), any(), anyBoolean())).thenReturn(true);
            ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime);
            if (hedgeMode) {
                verify(states, never()).markClosed(any());
                verify(gateway, never()).queryTrades(any(), any(), any());
                assertEquals("OPEN", state.getPendingAction());
            } else {
                verify(states).markClosed(state);
                verify(reconciliation, times(2)).refreshSymbolPosition(any(), eq("BTCUSDT"), any());
            }
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void missingPositionModeKeepsFilledFlatOpenPending() throws Exception {
        Fixture f = fixture();
        try {
            Instant time = Instant.now().minusSeconds(2);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderTime(time.toEpochMilli());
            state.setPendingOrderId(55L);
            state.setDirection("LONG");
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
                "test"), QuantPosition.class);
            when(mapper.selectList(any())).thenReturn(List.of());
            when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "FILLED",
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, time));
            when(gateway.queryPositions(any(), any())).thenReturn(List.of());
            when(gateway.queryPositionMode(any())).thenReturn(null);

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime));

            assertEquals("OPEN", state.getPendingAction());
            assertTrue(Boolean.TRUE.equals(state.getRecoveryRequired()));
            assertTrue(Boolean.TRUE.equals(state.getUnsafeRecovery()));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void preparationNoticeMustNotBlockMarketAndMustBeRateLimited() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var notifications = (org.dromara.quant.notification.QuantTradeNotificationService) ReflectionTestUtils.getField(f.service, "notificationService");
            doAnswer(invocation -> {
                entered.countDown();
                release.await(2, java.util.concurrent.TimeUnit.SECONDS);
                return null;
            }).when(notifications).notifyLiveBlocked(any(), any(), any());
            var dispatch = executor.submit(() -> {
                ReflectionTestUtils.invokeMethod(f.service, "notifyPreparationBlocked", f.runtime, "系统未开启实盘");
                ReflectionTestUtils.invokeMethod(f.service, "notifyPreparationBlocked", f.runtime, "系统未开启实盘");
            });
            dispatch.get(300, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            verify(notifications, times(1)).notifyLiveBlocked(any(), eq("BTCUSDT"), eq("系统未开启实盘"));
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void thresholdReachedWhileCapacityFullRecordsReasonAndCanRetryAfterCapacityFrees() throws Exception {
        Fixture f = fixture();
        try {
            var logs = mock(org.dromara.quant.decision.TradeDecisionLogService.class);
            ReflectionTestUtils.setField(f.service, "decisionLogs", logs);
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            var strategy = (QuantStrategy) ReflectionTestUtils.getField(f.runtime, "strategy");
            strategy.setMaxOpenCount(1);
            strategy.setDefaultOpenRatio(new BigDecimal("0.1"));
            strategy.setStrategyName("当前策略快照");
            var ledger = (org.dromara.quant.runtime.coordination.mapper.StrategyRuntimeLedgerMapper) ReflectionTestUtils.getField(f.service, "runtimeLedgerMapper");
            when(ledger.countStrategyExposures(anyLong(), anyLong())).thenReturn(1L, 0L);
            var context = new StrategyContext("BTCUSDT", null, null, new BigDecimal("102"), null, new BigDecimal("100"), Instant.now(), false, Instant.now(), null, null, null,
                10, null, null, false);
            var balance = (org.dromara.quant.market.AccountBalanceCache) ReflectionTestUtils.getField(f.service, "balanceCache");
            when(balance.requireSnapshot(any())).thenThrow(new org.dromara.quant.risk.OpenCapacityService.CapacityBlockedException("账户余额未就绪"));
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);
            var captured = org.mockito.ArgumentCaptor.forClass(org.dromara.quant.decision.DecisionLogEvent.class);
            verify(logs).record(captured.capture());
            assertEquals("MAX_POSITIONS", captured.getValue().reasonCode());
            assertEquals("当前策略快照", captured.getValue().strategyName());
            verify((org.dromara.quant.runtime.coordination.StrategyEntryGuardService) ReflectionTestUtils.getField(f.service, "entryGuardService"), never()).tryClaim(any(),
                any(), any(), any());
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);
            verify(logs, times(2)).record(captured.capture());
            assertEquals("ACCOUNT_NOT_READY", captured.getValue().reasonCode());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionDegradedStatePreventsEntryEvaluation() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "protectionDegraded", true);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            var context = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE,
                Instant.parse("2026-10-16T00:00:00Z"), false, Instant.now(), BigDecimal.ONE, BigDecimal.ONE);

            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);

            verify(f.plugin, never()).evaluateEntry(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"TESTNET,LONG,10", "LIVE,LONG,10", "TESTNET,SHORT,20", "LIVE,SHORT,20"})
    void eachStopLayerUsesSameNormalizedBoundary(String mode, String direction, int leverage) throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            var credential = (QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential");
            credential.setRunMode(mode);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setFrozenLeverage(leverage);
            var rule = new ExchangeGateway.SymbolRule("BTCUSDT", "USDT", 2, 3, new BigDecimal("0.01"), new BigDecimal("0.001"), new BigDecimal("0.001"), BigDecimal.ONE, "TRADING");
            ReflectionTestUtils.setField(f.runtime, "protectionRule", rule);
            for (String expected : List.of("HARD_STOP", "SYMBOL_STOP", "LOCAL_STOP")) {
                state.setFrozenStopLossRatio(new BigDecimal(expected.equals("HARD_STOP") ? "0.05" : "0.3"));
                state.setFrozenSymbolStopLossRatio(new BigDecimal(expected.equals("SYMBOL_STOP") ? "0.05" : "0.3"));
                ReflectionTestUtils.setField(f.runtime, "params", Map.of("localStopLossRate", expected.equals("LOCAL_STOP") ? "0.05" : "0.3"));
                BigDecimal trigger = org.dromara.quant.exchange.QuantityNormalizer.price(KlineSlopeRateStrategy.priceForReturn(new BigDecimal("100"), direction, new BigDecimal(
                    "-0.05"), leverage), rule);
                var context = new StrategyContext("BTCUSDT", direction, new BigDecimal("100"), trigger, null, BigDecimal.TEN, Instant.now(), false, Instant.now(), BigDecimal.ONE
                    , BigDecimal.ONE, BigDecimal.TEN, leverage, new BigDecimal("100"), new BigDecimal("100"), false);
                List<ExitSignal> exits = ReflectionTestUtils.invokeMethod(f.service, "protectionExits", f.runtime, context);
                assertEquals(expected, exits.getFirst().exchangeProtectionType());
                List<ProtectionSignal> signals = ReflectionTestUtils.invokeMethod(f.service, "protectionSignals", f.runtime, context, Map.of("localStopLossRate",
                    expected.equals("LOCAL_STOP") ? "0.05" : "0.3"));
                assertTrue(signals.stream().anyMatch(x -> x.type().equals("LOCAL_STOP")));
                assertTrue(signals.stream().anyMatch(x -> x.type().equals("SYMBOL_STOP")));
                assertFalse(signals.stream().anyMatch(x -> x.type().equals("STEP_TP")));
            }
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void exchangeProtectionSignalsDeduplicateSameStopAcrossLayers() {
        var position = new org.dromara.quant.position.domain.QuantPosition();
        position.setEntryPrice(new BigDecimal("0.1740736842105263157894736843"));
        position.setPositionSide("LONG");
        position.setQuantity(new BigDecimal("492"));
        var rule = new ExchangeGateway.SymbolRule("SYNUSDT", "USDT", 5, 0, new BigDecimal("0.00001"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "TRADING");
        var duplicateLocalStop = new ProtectionSignal("LOCAL_STOP", "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("0.16537"), "local");
        var duplicateSymbolStop = new ProtectionSignal("SYMBOL_STOP", "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("0.1653700"), "symbol");
        var uniqueTakeProfit = new ProtectionSignal("FIRST_TP", "TAKE_PROFIT_MARKET", new BigDecimal("0.5"), ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("0.1629"),
            "take profit");
        var samePriceStep = new ProtectionSignal("STEP_TP", "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("0.16537"), "step state must be " +
            "confirmed", 1);

        var filtered = StrategyRuntimeService.deduplicateExchangeProtectionSignals(List.of(duplicateLocalStop, duplicateSymbolStop, samePriceStep, uniqueTakeProfit), position,
            BigDecimal.ONE, new BigDecimal("0.05"), rule, 1);

        assertEquals(List.of("STEP_TP", "FIRST_TP"), filtered.stream().map(ProtectionSignal::type).toList());
        var withoutConfirmedHardStop = StrategyRuntimeService.deduplicateExchangeProtectionSignals(List.of(duplicateLocalStop, duplicateSymbolStop, samePriceStep,
            uniqueTakeProfit), position, BigDecimal.ONE, null, rule, 1);
        assertEquals(List.of("LOCAL_STOP", "STEP_TP", "FIRST_TP"), withoutConfirmedHardStop.stream().map(ProtectionSignal::type).toList());
    }

    @Test
    void slowProtectionCancellationDoesNotBlockMarketOrSendUnknownExit() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var properties = (QuantProperties) ReflectionTestUtils.getField(f.service, "properties");
            properties.getProtection().setConfirmationWaitMillis(0);
            when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("LOCAL_STOP_LOSS", BigDecimal.ONE, "test", false, "LOCAL_STOP",
                ExitSignal.QuantityBase.CURRENT)));
            when(f.protection.prepareLocalExit(any(), any(), anyString())).thenAnswer(i -> {
                assertFalse(Thread.holdsLock(f.runtime));
                entered.countDown();
                release.await(3, java.util.concurrent.TimeUnit.SECONDS);
                return false;
            });
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("99"), null, null, Instant.now(), false, Instant.now(), BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.TEN, 10, null, null, false);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateExit", f.runtime, context);
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var future = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m",
                new BigDecimal("98"), null, Instant.now())));
            assertDoesNotThrow(() -> future.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            ReflectionTestUtils.invokeMethod(f.service, "evaluateExit", f.runtime, context);
            verify(f.protection, times(1)).prepareLocalExit(any(), any(), anyString());
            release.countDown();
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "exitTask")).get(2, java.util.concurrent.TimeUnit.SECONDS);
            verify((org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService"), never()).closeByRuntime(any(),
                any(), any(), any(), any(), any(), any(), any());
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void exitClaimsCycleWhileProtectionSyncDrainsWithoutBlockingMarket() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var properties = (QuantProperties) ReflectionTestUtils.getField(f.service, "properties");
            properties.getProtection().setConfirmationWaitMillis(0);
            when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("LOCAL_STOP_LOSS", BigDecimal.ONE, "test", false, "LOCAL_STOP",
                ExitSignal.QuantityBase.CURRENT)));
            var syncing = (java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionSyncing");
            syncing.set(true);
            var protection = executor.submit(() -> {
                entered.countDown();
                release.await();
                syncing.set(false);
                return null;
            });
            ReflectionTestUtils.setField(f.runtime, "protectionTask", protection);
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("99"), null, null, Instant.now(), false, Instant.now(), BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.TEN, 10, null, null, false);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateExit", f.runtime, context);
            assertTrue(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "exitCoordinating")).get());
            verify(f.protection, never()).prepareLocalExit(any(), any(), anyString());
            var event = executor.submit(() -> ReflectionTestUtils.invokeMethod(f.service, "onMarketEvent", f.runtime, new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m",
                new BigDecimal("98"), null, Instant.now())));
            assertDoesNotThrow(() -> event.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            release.countDown();
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "exitTask")).get(2, java.util.concurrent.TimeUnit.SECONDS);
            verify(f.protection).prepareLocalExit(any(), any(), anyString());
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"TESTNET", "LIVE"})
    void dynamicProtectionFailureStillConfirmsHardStop(String mode) throws Exception {
        Fixture f = fixture();
        try {
            ((QuantCredential) ReflectionTestUtils.getField(f.runtime, "credential")).setRunMode(mode);
            when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenReturn(new org.dromara.quant.protection.domain.QuantProtectionOrder());
            doThrow(new IllegalStateException("take profit rejected")).when(f.protection).syncStrategyProtections(any(), any(), any(), anyList(), any(), any(), anyBoolean());
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask")).get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "hardStopConfirmed"));
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            assertTrue(state.getRecoveryRequired());
            assertFalse(state.getUnsafeRecovery(), "动态保护失败仍须允许核实后的本地安全减仓");
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void protectionSignalComputationFailureStillConfirmsHardStopAndPreservesOldTargets() throws Exception {
        Fixture f = fixture();
        try {
            ((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position")).setMarkPrice(new BigDecimal("100"));
            when(f.plugin.evaluateProtection(any(), anyMap())).thenThrow(new IllegalStateException("invalid protection parameter"));
            when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenReturn(new org.dromara.quant.protection.domain.QuantProtectionOrder());
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask")).get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "hardStopConfirmed"));
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            assertTrue(state.getRecoveryRequired());
            verify(f.protection, never()).syncStrategyProtections(any(), any(), any(), anyList(), any(), any(), anyBoolean());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void missingProtectionPriceDoesNotCancelOrTreatStrategyTargetsAsConfirmed() throws Exception {
        Fixture f = fixture();
        try {
            when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenReturn(new org.dromara.quant.protection.domain.QuantProtectionOrder());
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask")).get(2, java.util.concurrent.TimeUnit.SECONDS);

            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "hardStopConfirmed"));
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
            verify(f.protection, never()).syncStrategyProtections(any(), any(), any(), anyList(), any(), any(), anyBoolean());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void hardStopFailureDoesNotSkipOtherProtectionTargets() throws Exception {
        Fixture f = fixture();
        try {
            ((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position")).setMarkPrice(new BigDecimal("100"));
            doThrow(new IllegalStateException("hard stop rejected")).when(f.protection).ensureMarginHardStop(any(), any(), any(), anyBoolean());
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask")).get(2, java.util.concurrent.TimeUnit.SECONDS);
            verify(f.protection).syncStrategyProtections(any(), any(), any(), anyList(), any(), any(), eq(true));
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "hardStopConfirmed"));
            assertTrue((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void stalledProtectionWaitReleasesExitCoordinatorWithoutSendingDuplicateClose() throws Exception {
        Fixture f = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var properties = (QuantProperties) ReflectionTestUtils.getField(f.service, "properties");
            properties.getProtection().setConfirmationWaitMillis(0);
            when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("LOCAL_STOP_LOSS", BigDecimal.ONE, "test", false, "LOCAL_STOP",
                ExitSignal.QuantityBase.CURRENT)));
            var syncing = (java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "protectionSyncing");
            syncing.set(true);
            var protection = executor.submit(() -> {
                entered.countDown();
                release.await();
                syncing.set(false);
                return null;
            });
            ReflectionTestUtils.setField(f.runtime, "protectionTask", protection);
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("99"), null, null, Instant.now(), false, Instant.now(), BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.TEN, 10, null, null, false);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateExit", f.runtime, context);
            var exit = (java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "exitTask");
            assertDoesNotThrow(() -> exit.get(7, java.util.concurrent.TimeUnit.SECONDS), "exit coordinator waited indefinitely for protection");
            assertFalse(((java.util.concurrent.atomic.AtomicBoolean) ReflectionTestUtils.getField(f.runtime, "exitCoordinating")).get());
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            assertTrue(state.getRecoveryRequired());
            assertFalse(state.getUnsafeRecovery(), "在途保护仍由精确停单门禁阻断，不能永久否定持仓归属");
            assertFalse(protection.isCancelled(), "timeout cannot cancel an uncertain exchange request and assume it was not sent");
            verify(f.protection, never()).prepareLocalExit(any(), any(), anyString());
            verify((org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService"), never()).closeByRuntime(any(),
                any(), any(), any(), any(), any(), any(), any());
        } finally {
            release.countDown();
            executor.shutdownNow();
            f.service.destroy();
        }
    }

    @Test
    void missingExactOrderUsesStrictlyMatchedOpenOrdersFallbackWithoutClearingUnknown() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(55L);
            state.setPendingClientOrderId("open-client");
            state.setPendingIdempotencyKey("open-key");
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection("LONG");
            state.setStatus("OPEN_PENDING");
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);

            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            var matching = new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open-client", "BUY", "MARKET", "NEW",
                new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, Instant.now());
            when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(55L), eq("open-client"))).thenReturn(null);
            when(gateway.queryOrders(any(), eq("BTCUSDT"))).thenReturn(List.of(matching));
            when(reconciliation.refreshSymbolPosition(any(), eq("BTCUSDT"), any())).thenReturn(List.of());

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime));

            verify(gateway).queryOrder(any(), eq("BTCUSDT"), eq(55L), eq("open-client"));
            verify(gateway).queryOrders(any(), eq("BTCUSDT"));
            assertEquals("OPEN", state.getPendingAction(), "活动订单只能证明订单仍在交易所，不能清除待确认动作");
            assertEquals(55L, state.getPendingOrderId());
            assertTrue(state.getRecoveryRequired());
            assertTrue(state.getUnsafeRecovery());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void exactOrderQueryFailureKeepsUnknownAndDoesNotCompoundWithOpenOrdersQuery() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(56L);
            state.setPendingClientOrderId("open-client-56");
            state.setPendingIdempotencyKey("open-key-56");
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection("LONG");
            state.setStatus("OPEN_PENDING");
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);

            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(56L), eq("open-client-56")))
                .thenThrow(new IllegalStateException("simulated REST rate limit"));
            when(reconciliation.refreshSymbolPosition(any(), eq("BTCUSDT"), any())).thenReturn(List.of());

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime));

            verify(gateway).queryOrder(any(), eq("BTCUSDT"), eq(56L), eq("open-client-56"));
            verify(gateway, never()).queryOrders(any(), anyString());
            assertEquals("OPEN", state.getPendingAction(), "查询异常不能被解释为订单不存在");
            assertEquals(56L, state.getPendingOrderId());
            assertEquals("open-client-56", state.getPendingClientOrderId());
            assertTrue(state.getRecoveryRequired());
            assertTrue(state.getUnsafeRecovery());
            assertEquals("IllegalStateException", ReflectionTestUtils.getField(f.runtime, "lastPendingOrderQueryFailure"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void openOrdersFallbackRejectsMismatchedClientOrderIdAndKeepsUnknown() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(57L);
            state.setPendingClientOrderId("expected-client-57");
            state.setPendingIdempotencyKey("open-key-57");
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection("LONG");
            state.setStatus("OPEN_PENDING");
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);

            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
            var wrongIdentity = new ExchangeGateway.OrderSnapshot("BTCUSDT", 57L, "different-client-id", "BUY", "MARKET", "NEW",
                new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, Instant.now());
            when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(57L), eq("expected-client-57"))).thenReturn(null);
            when(gateway.queryOrders(any(), eq("BTCUSDT"))).thenReturn(List.of(wrongIdentity));
            when(reconciliation.refreshSymbolPosition(any(), eq("BTCUSDT"), any())).thenReturn(List.of());

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime));

            verify(gateway).queryOrders(any(), eq("BTCUSDT"));
            assertEquals("OPEN", state.getPendingAction(), "订单号相同但客户订单号不符时不得接受模糊匹配");
            assertEquals(57L, state.getPendingOrderId());
            assertEquals("expected-client-57", state.getPendingClientOrderId());
            assertTrue(state.getRecoveryRequired());
            assertTrue(state.getUnsafeRecovery());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"CANCELED", "EXPIRED", "REJECTED"})
    void terminalPartialOpenReadsFreshPositionBeforeApplyingExecution(String status) throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(55L);
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection("LONG");
            state.setUnsafeRecovery(true);
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            var reconciliation = configureFreshPosition(f, new BigDecimal("0.5"));
            when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", status,
                new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("0.5"), Instant.now()));

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime));

            var reads = inOrder(gateway, reconciliation);
            reads.verify(gateway).queryOrder(any(), any(), any(), any());
            reads.verify(reconciliation).refreshSymbolPosition(any(), eq("BTCUSDT"), any());
            assertNull(state.getPendingAction());
            assertEquals("OPEN", state.getStatus());
            assertFalse(state.getUnsafeRecovery());
            assertEquals(new BigDecimal("0.5"), state.getInitialPositionQty());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void confirmedPartialOpenGetsHardStopWithoutFreezingFinalTargets() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", mock(KlineSlopeRateStrategy.class));
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(55L);
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection("LONG");
            state.setFirstTakeProfitTargetQty(null);
            state.setHighestPriceSinceEntry(new BigDecimal("110"));
            state.setLowestPriceSinceEntry(new BigDecimal("90"));
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            configureFreshPosition(f, new BigDecimal("0.5"));
            when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenReturn(new org.dromara.quant.protection.domain.QuantProtectionOrder());
            when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "PARTIALLY_FILLED",
                new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("0.5"), Instant.now()));

            ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime);
            ReflectionTestUtils.invokeMethod(f.service, "ensureHardStop", f.runtime);
            var task = (java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask");
            assertNotNull(task, "已确认部分成交必须请求硬止损");
            task.get(3, java.util.concurrent.TimeUnit.SECONDS);
            ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime);

            verify(f.protection, atLeastOnce()).ensureMarginHardStop(any(), argThat(p -> p.getQuantity().compareTo(new BigDecimal("0.5")) == 0), any(), eq(true));
            verify(f.protection, never()).syncStrategyProtections(any(), any(), any(), anyList(), any(), any(), anyBoolean());
            assertNull(state.getFirstTakeProfitTargetQty());
            assertEquals(BigDecimal.ONE, state.getInitialPositionQty());
            assertEquals(new BigDecimal("110"), state.getHighestPriceSinceEntry());
            assertEquals(new BigDecimal("90"), state.getLowestPriceSinceEntry());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void canceledZeroFillOpenCannotAdoptAnUnrelatedAccountPosition() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setPendingAction("OPEN");
            state.setPendingOrderId(55L);
            state.setPendingOrderTime(System.currentTimeMillis());
            state.setDirection("LONG");
            configureFreshPosition(f, BigDecimal.ONE);
            var gateway = (ExchangeGateway) ReflectionTestUtils.getField(f.service, "gateway");
            when(gateway.queryOrder(any(), any(), any(), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 55L, "open", "BUY", "MARKET", "CANCELED",
                new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, Instant.now()));
            ReflectionTestUtils.invokeMethod(f.service, "reconcilePendingExecution", f.runtime);
            assertEquals("OPEN", state.getPendingAction());
            assertTrue(state.getUnsafeRecovery());
            assertTrue(state.getRecoveryRequired());
            verify(f.protection, never()).cancelActiveProtectionOrders(any(), any(), anyBoolean());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void failedProtectedExitUsesNewAttemptInSameCandle() throws Exception {
        Fixture f = fixture();
        try {
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("99"), null, null,
                Instant.parse("2026-10-20T00:00:00Z"), false, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, 10, null, null, false);
            when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("LOCAL_STOP_LOSS", BigDecimal.ONE, "test", false)));
            var records = (org.dromara.quant.execution.ExecutionRecordService) ReflectionTestUtils.getField(f.service, "executionRecordService");
            var failed = new org.dromara.quant.execution.domain.QuantExecutionRecord();
            failed.setExecutionStatus("FAILED");
            String base = "STRATEGY:1:LOCAL_STOP_LOSS:" + context.klineOpenTime().toEpochMilli();
            when(records.find(3L, base)).thenReturn(failed);
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            doThrow(new IllegalStateException("not sent")).when(execution).closeByRuntime(any(), any(), any(), any(), any(), any(), any(), any());

            assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(f.service, "executeProtectedExit", f.runtime, context,
                new java.util.concurrent.atomic.AtomicBoolean(true)));

            verify(execution).closeByRuntime(eq(3L), eq(2L), eq(1L), eq("BTCUSDT"), eq("SELL"), eq("LONG"), any(), startsWith(base + ":RETRY:"));
        } finally {
            f.service.destroy();
        }
    }

    private org.dromara.quant.reconcile.TradingReconciliationService configureFreshPosition(Fixture f, BigDecimal quantity) {
        var position = (QuantPosition) ReflectionTestUtils.getField(f.runtime, "position");
        position.setQuantity(quantity);
        var mapper = (org.dromara.quant.position.mapper.QuantPositionMapper) ReflectionTestUtils.getField(f.service, "positionMapper");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
            "test"), QuantPosition.class);
        when(mapper.selectList(any())).thenReturn(List.of(position));
        var reconciliation = (org.dromara.quant.reconcile.TradingReconciliationService) ReflectionTestUtils.getField(f.service, "reconciliationService");
        when(reconciliation.refreshSymbolPosition(any(), any(), any())).thenReturn(List.of(new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", quantity,
            new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 10, null, null)));
        return reconciliation;
    }

    @Test
    void protectionFailureMustNotInvalidatePreviouslyVerifiedPositionOwnership() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            ReflectionTestUtils.invokeMethod(f.service, "markProtectionDegraded", f.runtime, "test", new IllegalStateException("timeout"));
            assertTrue(state.getRecoveryRequired());
            assertFalse(state.getUnsafeRecovery(), "保护失败不能使 LIVE 的本地安全减仓被归属门禁拒绝");
            state.setUnsafeRecovery(true);
            ReflectionTestUtils.invokeMethod(f.service, "markProtectionDegraded", f.runtime, "test", new IllegalStateException("timeout"));
            assertTrue(state.getUnsafeRecovery(), "既有归属未知状态不能因保护失败而被清除");
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void successfulProtectionSyncCannotClearUnknownPositionOwnership() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setUnsafeRecovery(true);
            state.setRecoveryRequired(true);
            ReflectionTestUtils.setField(f.runtime, "protectionDegraded", true);
            ReflectionTestUtils.setField(f.runtime, "latestTradePrice", new BigDecimal("100"));
            ReflectionTestUtils.setField(f.runtime, "latestTradeEventAt", Instant.now());
            ((QuantPosition) ReflectionTestUtils.getField(f.runtime, "position")).setMarkPrice(new BigDecimal("100"));
            when(f.protection.ensureMarginHardStop(any(), any(), any(), anyBoolean())).thenReturn(new org.dromara.quant.protection.domain.QuantProtectionOrder());
            ReflectionTestUtils.invokeMethod(f.service, "requestProtectionSync", f.runtime, true);
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask")).get(3, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(state.getUnsafeRecovery());
            assertTrue(state.getRecoveryRequired());
            assertFalse((boolean) ReflectionTestUtils.getField(f.runtime, "protectionDegraded"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void liveFallbackAfterProtectionFailurePassesRealOwnershipGateOnlyAfterQuiesce() throws Exception {
        Fixture f = fixture();
        try {
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            configureFreshPosition(f, BigDecimal.ONE);
            when(f.protection.prepareLocalExit(any(), any(), anyString())).thenReturn(true);
            when(f.plugin.evaluateExit(any(), anyMap())).thenReturn(List.of(new ExitSignal("LOCAL_STOP_LOSS", BigDecimal.ONE, "test", false,
                "LOCAL_STOP", ExitSignal.QuantityBase.CURRENT)));
            ReflectionTestUtils.invokeMethod(f.service, "markProtectionDegraded", f.runtime, "test", new IllegalStateException("timeout"));
            var ownership = mock(org.dromara.quant.runtime.coordination.mapper.StrategyCloseOwnershipMapper.class);
            var guard = new org.dromara.quant.runtime.coordination.StrategyCloseOwnershipGuard(ownership);
            when(ownership.pendingOwnedExit(any(), any(), any(), any())).thenAnswer(call -> Map.of("status", state.getStatus(), "direction", state.getDirection(),
                "position_qty", state.getPositionQty(), "pending_action", state.getPendingAction(), "pending_idempotency_key", state.getPendingIdempotencyKey(),
                "unsafe_recovery", state.getUnsafeRecovery()));
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            doAnswer(call -> {
                guard.requireOwnedClose(3L, 2L, 1L, "BTCUSDT", "SELL", call.getArgument(6), call.getArgument(7));
                throw new org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException("test stop before network");
            }).when(execution).closeByRuntime(any(), any(), any(), any(), any(), any(), any(), any());
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("90"), null, null,
                Instant.now(), false, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, 10, null, null, false);

            assertThrows(org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException.class,
                () -> ReflectionTestUtils.invokeMethod(f.service, "executeProtectedExit", f.runtime, context, new java.util.concurrent.atomic.AtomicBoolean(true)));
            var sequence = inOrder(f.protection, execution);
            sequence.verify(f.protection).prepareLocalExit(any(), any(), anyString());
            sequence.verify(execution).closeByRuntime(any(), any(), any(), any(), any(), any(), any(), any());
            verify(ownership).pendingOwnedExit(1L, 3L, 2L, "BTCUSDT");
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PENDING", "UNKNOWN"})
    void uncertainCloseCannotGenerateAnotherAttempt(String status) throws Exception {
        Fixture f = fixture();
        try {
            Instant candle = Instant.now();
            String base = "STRATEGY:1:LOCAL_STOP_LOSS:" + candle.toEpochMilli();
            var record = new org.dromara.quant.execution.domain.QuantExecutionRecord();
            record.setExecutionStatus(status);
            var records = (org.dromara.quant.execution.ExecutionRecordService) ReflectionTestUtils.getField(f.service, "executionRecordService");
            when(records.find(3L, base)).thenReturn(record);
            assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(f.service, "nextExitAttemptKey", f.runtime, "LOCAL_STOP_LOSS", candle));
            verify(records, never()).find(3L, base + ":RETRY:1");
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void newCycleFreezesServerPolicyAndLocalStopInsteadOfLegacyDefault() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            ReflectionTestUtils.setField(f.runtime, "params", Map.of("localStopLossRate", "0.05", "_quantProtectionPolicyVersion", 999));
            ReflectionTestUtils.setField(f.runtime, "position", null);
            org.dromara.quant.runtime.domain.QuantStrategyPositionState state = ReflectionTestUtils.invokeMethod(f.service,
                "preparePendingStateForOpen", f.runtime, "LONG", BigDecimal.ONE, new BigDecimal("100"), Instant.now(), "open-policy-v2");
            assertEquals(0, new BigDecimal("0.05").compareTo(state.getFrozenStopLossRatio()));
            assertTrue(state.getStrategyParamsSnapshot().contains("\"_quantProtectionPolicyVersion\":2"));
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void entryCleanupIsAsyncCoalescedAndPrivateEventsInvalidateItsProof() throws Exception {
        Fixture f = fixture();
        var release = new java.util.concurrent.CountDownLatch(1);
        var entered = new java.util.concurrent.CountDownLatch(1);
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            when(lease.requireCurrentEpoch(any(), any(), any())).thenReturn(1L);
            when(f.protection.prepareEntryProtectionCleanup(any(), any(), anyBoolean())).thenAnswer(call -> {
                entered.countDown();
                release.await(3, java.util.concurrent.TimeUnit.SECONDS);
                return true;
            });
            long start = System.nanoTime();
            assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(f.service, "entryProtectionReady", f.runtime));
            assertTrue(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 500);
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(f.service, "entryProtectionReady", f.runtime));
            ReflectionTestUtils.invokeMethod(f.service, "invalidateEntryProtectionCleanup", f.runtime);
            release.countDown();
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "entryProtectionCleanupTask")).get(3, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(false, ReflectionTestUtils.getField(f.runtime, "entryProtectionCleanupReady"));
            ReflectionTestUtils.setField(f.runtime, "nextEntryProtectionCleanupAt", 0L);
            assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(f.service, "entryProtectionReady", f.runtime));
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "entryProtectionCleanupTask")).get(3, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(f.service, "entryProtectionReady", f.runtime));
            verify(f.protection, times(2)).prepareEntryProtectionCleanup(any(), any(), anyBoolean());
        } finally {
            release.countDown();
            f.service.destroy();
        }
    }

    @Test
    void unifiedLocalHardStopUsesPersistedTriggerAfterEntryAverageChanges() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStrategyParamsSnapshot("{\"_quantProtectionPolicyVersion\":2,\"localStopLossRate\":0.05}");
            state.setFrozenStopLossRatio(new BigDecimal("0.05"));
            state.setStopLossPrice(new BigDecimal("99"));
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("101"), new BigDecimal("99.5"), null, new BigDecimal("100"),
                Instant.now(), false, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, 10, new BigDecimal("101"), new BigDecimal("99.5"), false);
            List<ExitSignal> exits = ReflectionTestUtils.invokeMethod(f.service, "protectionExits", f.runtime, context);
            assertTrue(exits.isEmpty());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void privateEventInvalidatesEntryProofWithoutWaitingForMarketLock() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "entryProtectionCleanupReady", true);
            var task = new java.util.concurrent.FutureTask<Void>(() -> {
                ReflectionTestUtils.invokeMethod(f.service, "invalidateEntryProtectionCleanup", f.runtime);
                return null;
            });
            synchronized (f.runtime) {
                Thread.ofVirtual().start(task);
                task.get(1, java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(false, ReflectionTestUtils.getField(f.runtime, "entryProtectionCleanupReady"));
            }
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void failedHardStopFreezeDoesNotLeaveAnUnpersistedTriggerInRuntime() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStrategyParamsSnapshot("{\"_quantProtectionPolicyVersion\":2,\"localStopLossRate\":0.05}");
            state.setFrozenStopLossRatio(new BigDecimal("0.05"));
            var stateService = (org.dromara.quant.runtime.service.StrategyPositionStateService) ReflectionTestUtils.getField(f.service, "stateService");
            doThrow(new org.dromara.quant.runtime.service.StrategyPositionStateService.StateConflictException(1L)).doNothing().when(stateService).save(state);
            ReflectionTestUtils.invokeMethod(f.service, "requestProtectionSync", f.runtime, false);
            ((java.util.concurrent.Future<?>) ReflectionTestUtils.getField(f.runtime, "protectionTask")).get(3, java.util.concurrent.TimeUnit.SECONDS);
            assertNull(state.getStopLossPrice());
            verify(f.protection, never()).ensureMarginHardStop(any(), any(), any(), anyBoolean());
        } finally {
            f.service.destroy();
        }
    }

    @Test
    void cleanupCompletionRequiresAnExchangeEventStrictlyAfterItsProof() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "plugin", new KlineSlopeRateStrategy());
            ReflectionTestUtils.setField(f.runtime, "position", null);
            ReflectionTestUtils.setField(f.runtime, "entryProtectionCleanupReady", true);
            ReflectionTestUtils.setField(f.runtime, "entryProtectionCleanupEpoch", 1L);
            var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(f.service, "leaseService");
            when(lease.requireCurrentEpoch(any(), any(), any())).thenReturn(1L);
            Instant completed = Instant.now();
            ReflectionTestUtils.setField(f.runtime, "entryProtectionCleanupConfirmedAt", completed);
            for (int delta : new int[] {-1, 0, 1}) {
                var context = new StrategyContext("BTCUSDT", null, null, new BigDecimal("100"), null, Instant.now(), false,
                    completed.plusMillis(delta), BigDecimal.ONE, BigDecimal.TEN);
                assertEquals(delta > 0, ReflectionTestUtils.invokeMethod(f.service, "entryProtectionReadyForEvent", f.runtime, context));
            }
            verify(f.protection, never()).prepareEntryProtectionCleanup(any(), any(), anyBoolean());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.api.Test
    void disabledStrategySymbolBlocksEntryEvaluation() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "runtimeEnabled", false);
            ReflectionTestUtils.setField(f.runtime, "position", null);
            var state = (QuantStrategyPositionState) ReflectionTestUtils.getField(f.runtime, "state");
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);

            var context = new StrategyContext("BTCUSDT", "LONG", BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ONE,
                Instant.now(), false, Instant.now(), BigDecimal.ZERO, BigDecimal.ZERO);
            ReflectionTestUtils.invokeMethod(f.service, "evaluateEntry", f.runtime, context);

            verify(f.plugin, never()).evaluateEntry(any(), anyMap());
            var execution = (org.dromara.quant.execution.TradeExecutionApplicationService) ReflectionTestUtils.getField(f.service, "executionService");
            verify(execution, never()).openByRuntime(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.api.Test
    void disabledStrategySymbolStillEvaluatesExitProtectionForExistingPosition() throws Exception {
        Fixture f = fixture();
        try {
            ReflectionTestUtils.setField(f.runtime, "runtimeEnabled", false);
            var context = new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ONE,
                Instant.now(), false, Instant.now(), BigDecimal.ZERO, BigDecimal.ZERO);

            ReflectionTestUtils.invokeMethod(f.service, "evaluateExit", f.runtime, context);

            verify(f.plugin, times(1)).evaluateExit(any(), anyMap());
        } finally {
            f.service.destroy();
        }
    }

    @org.junit.jupiter.api.Test
    void symbolRuntimeTogglePersistsStateUnderDistributedLock() throws Exception {
        Fixture f = fixture();
        try {
            var binding = (QuantStrategySymbol) ReflectionTestUtils.getField(f.runtime, "strategySymbol");
            binding.setId(10L);
            var mapper = (org.dromara.quant.strategy.mapper.QuantStrategySymbolMapper) ReflectionTestUtils.getField(f.service, "strategySymbolMapper");
            var redisson = (org.redisson.api.RedissonClient) ReflectionTestUtils.getField(f.service, "redissonClient");
            var lock = mock(org.redisson.api.RLock.class);
            when(redisson.getLock("quant:strategy-symbol-runtime:10")).thenReturn(lock);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            when(mapper.selectById(10L)).thenReturn(binding);
            when(mapper.updateById(binding)).thenReturn(1);

            f.service.setStrategySymbolRuntimeEnabled(10L, false);
            f.service.setStrategySymbolRuntimeEnabled(10L, false);

            assertEquals(Boolean.FALSE, binding.getRuntimeEnabled());
            assertEquals(false, ReflectionTestUtils.getField(f.runtime, "runtimeEnabled"));
            verify(lock, times(2)).lock();
            verify(lock, times(2)).unlock();
            verify(mapper, times(1)).updateById(binding);
        } finally {
            f.service.destroy();
        }
    }

    private Fixture fixture() throws Exception {
        var constructor = StrategyRuntimeService.class.getConstructors()[0];
        Object[] args = Arrays.stream(constructor.getParameterTypes()).map(type -> type == QuantProperties.class ? new QuantProperties() : mock(type)).toArray();
        var service = (StrategyRuntimeService) constructor.newInstance(args);
        var gateway = (ExchangeGateway) ReflectionTestUtils.getField(service, "gateway");
        var lease = (org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService) ReflectionTestUtils.getField(service, "leaseService");
        when(lease.acquireOrRenew(any(), any(), any())).thenReturn(true);
        when(gateway.queryKlines(any(), any(), anyInt())).thenReturn(List.of());
        var instance = new QuantStrategyInstance();
        instance.setId(1L);
        instance.setStatus("RUNNING");
        var strategy = new QuantStrategy();
        strategy.setId(2L);
        strategy.setDefaultLeverage(10);
        strategy.setStopLossRatio(new BigDecimal("0.1"));
        var symbolConfig = new QuantStrategySymbol();
        symbolConfig.setIntervalCode("1m");
        symbolConfig.setLeverage(10);
        var credential = new QuantCredential();
        credential.setId(3L);
        credential.setRunMode("LIVE");
        var symbol = new QuantSymbol();
        symbol.setSymbol("BTCUSDT");
        var plugin = mock(StrategyPlugin.class);
        when(plugin.evaluateExit(any(), anyMap())).thenReturn(List.of());
        when(plugin.evaluateProtection(any(), anyMap())).thenReturn(List.of());
        Class<?> runtimeClass =
            Arrays.stream(StrategyRuntimeService.class.getDeclaredClasses()).filter(type -> type.getSimpleName().equals("ActiveRuntime")).findFirst().orElseThrow();
        var runtimeConstructor = runtimeClass.getDeclaredConstructors()[0];
        runtimeConstructor.setAccessible(true);
        Object runtime = runtimeConstructor.newInstance(service, instance, strategy, symbolConfig, credential, symbol, plugin, Map.of());
        var position = new QuantPosition();
        position.setId(4L);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        position.setPositionSide("LONG");
        position.setSymbol("BTCUSDT");
        position.setLeverage(10);
        var state = new QuantStrategyPositionState();
        state.setInstanceId(1L);
        state.setPositionId(4L);
        state.setStatus("OPEN");
        state.setPositionQty(BigDecimal.ONE);
        state.setInitialPositionQty(BigDecimal.ONE);
        state.setUnsafeRecovery(false);
        state.setRecoveryRequired(false);
        state.setFirstTakeProfitTriggered(false);
        state.setPositionCycleKey("cycle");
        state.setFrozenLeverage(10);
        state.setEntryPrice(new BigDecimal("100"));
        state.setFrozenStopLossRatio(new BigDecimal("0.1"));
        state.setFirstTakeProfitTargetQty(new BigDecimal("0.5"));
        state.setFirstTakeProfitExecutedQty(BigDecimal.ZERO);
        state.setFirstTakeProfitLocalExecutedQty(BigDecimal.ZERO);
        when(((ProtectionOrderService) ReflectionTestUtils.getField(service, "protectionOrderService")).firstTakeProfitExecutedQuantity(any(), any())).thenReturn(BigDecimal.ZERO);
        ReflectionTestUtils.setField(runtime, "position", position);
        ReflectionTestUtils.setField(runtime, "positionFactsReady", true);
        ReflectionTestUtils.setField(runtime, "positionFactsVerifiedAt", System.currentTimeMillis());
        ReflectionTestUtils.setField(runtime, "state", state);
        ReflectionTestUtils.setField(runtime, "lastEvaluatedKlineOpenTime", Instant.now());
        ReflectionTestUtils.setField(runtime, "leaseValid", true);
        ((java.util.Map) ReflectionTestUtils.getField(service, "activeRuntimes")).put(1L, runtime);
        return new Fixture(service, runtime, plugin, instance, (ProtectionOrderService) ReflectionTestUtils.getField(service, "protectionOrderService"));
    }

    private record Fixture(StrategyRuntimeService service, Object runtime, StrategyPlugin plugin, QuantStrategyInstance instance, ProtectionOrderService protection) {
    }
}

package org.dromara.quant.market;

import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeMarketPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class MarketStreamHubRecoveryTest {
    @Test
    void reconnectDelayUsesJitterWindowToAvoidSynchronizedRestRecovery() {
        assertEquals(3_000L, MarketStreamHub.reconnectDelayMillis(3, 0));
        assertEquals(6_000L, MarketStreamHub.reconnectDelayMillis(3, 3_000));
    }

    private final ExchangeGateway gateway = mock(ExchangeGateway.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<Consumer<ExchangeGateway.MarketEvent>> callback = new AtomicReference<>();
    // 使用稳定的形成中周期，避免500ms重试恰好跨整分钟导致测试误失败。
    private final Instant open = Instant.now().minusSeconds(5);
    private final QuantProperties properties = new QuantProperties();

    private MarketStreamHub hub(List<ExchangeGateway.Kline> history) {
        when(gateway.subscribeMarket(any(), any(), any())).thenAnswer(i -> {
            callback.set(i.getArgument(2));
            return mock(ExchangeGateway.MarketStreamSession.class);
        });
        when(gateway.queryKlines(any(), any(), anyInt())).thenReturn(history);
        return new MarketStreamHub(gateway, scheduler, properties);
    }

    private ExchangeGateway.Kline candle(Instant time, boolean closed) {
        return new ExchangeGateway.Kline(time, time.plusSeconds(60).minusMillis(1), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, closed);
    }

    private void send(String type) {
        callback.get().accept(new ExchangeGateway.MarketEvent(type, "BTCUSDT", "1m", "MARK".equals(type) ? BigDecimal.TEN : null, "KLINE".equals(type) ? candle(open, false) :
            null, Instant.now()));
    }

    private boolean await(AtomicInteger value) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (value.get() == 0 && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        return value.get() > 0;
    }

    @Test
    void marketReadinessUsesBinanceClockOffsetInsteadOfRejectingFreshMarkAsStale() throws Exception {
        Instant localNow = Instant.now();
        when(gateway.exchangeTimeOffsetMillis("LIVE")).thenReturn(-2_979L);
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(120), true), candle(open.minusSeconds(60), true), candle(open, false)));
        CountDownLatch ready = new CountDownLatch(1);
        try {
            hub.subscribe("LIVE", "BTCUSDT", "1m", ignored -> {
            }, isReady -> {
                if (isReady) {
                    ready.countDown();
                }
            });
            callback.get().accept(new ExchangeGateway.MarketEvent("MARK", "BTCUSDT", "1m", BigDecimal.TEN, null, localNow.minusMillis(3_100), null,
                localNow.minusMillis(40), false));

            assertTrue(ready.await(2, TimeUnit.SECONDS), "a fresh Binance MARK should pass readiness when the local clock is ahead by the measured offset");
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void slowClockOffsetLookupDoesNotHoldSharedStreamMonitor() throws Exception {
        AtomicInteger clockCalls = new AtomicInteger();
        AtomicInteger blockAtCall = new AtomicInteger(Integer.MAX_VALUE);
        CountDownLatch slowLookupStarted = new CountDownLatch(1);
        CountDownLatch releaseSlowLookup = new CountDownLatch(1);
        when(gateway.exchangeTimeOffsetMillis("LIVE")).thenAnswer(invocation -> {
            if (clockCalls.incrementAndGet() == blockAtCall.get()) {
                slowLookupStarted.countDown();
                assertTrue(releaseSlowLookup.await(3, TimeUnit.SECONDS));
            }
            return 0L;
        });
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(120), true), candle(open.minusSeconds(60), true), candle(open, false)));
        CountDownLatch ready = new CountDownLatch(1);
        AutoCloseable firstSubscription = null;
        AutoCloseable secondSubscription = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                firstSubscription = hub.subscribe("LIVE", "BTCUSDT", "1m", ignored -> {
                }, isReady -> {
                    if (isReady) {
                        ready.countDown();
                    }
                });
                callback.get().accept(new ExchangeGateway.MarketEvent("MARK", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now()));
                assertTrue(ready.await(2, TimeUnit.SECONDS));

                blockAtCall.set(clockCalls.get() + 1);
                Future<?> slowHealthCheck = executor.submit(() -> checkHealth(hub));
                assertTrue(slowLookupStarted.await(2, TimeUnit.SECONDS));

                Future<AutoCloseable> secondSubscriptionFuture = executor.submit(() -> hub.subscribe("LIVE", "BTCUSDT", "1m", ignored -> {
                }));
                secondSubscription = secondSubscriptionFuture.get(1, TimeUnit.SECONDS);
                assertNotNull(secondSubscription, "a slow exchange-time lookup must not hold the shared-stream monitor");
            } finally {
                releaseSlowLookup.countDown();
                if (secondSubscription != null) {
                    secondSubscription.close();
                }
                if (firstSubscription != null) {
                    firstSubscription.close();
                }
                hub.close();
                scheduler.shutdownNow();
            }
        }
    }

    @Test
    void unsupportedSymbolPublishesErrorAndDoesNotReconnectForever() throws Exception {
        properties.getMarket().setReconnectDelaySeconds(1);
        when(gateway.subscribeMarket(anyString(), anyString(), anyString(), any())).thenReturn(mock(ExchangeGateway.MarketStreamSession.class));
        when(gateway.queryKlines(eq("TESTNET"), eq("BTWUSDT"), eq("15m"), anyInt())).thenThrow(new ExchangeMarketPort.UnsupportedMarketSymbolException("TESTNET", "BTWUSDT"));
        MarketStreamHub hub = new MarketStreamHub(gateway, scheduler, properties);
        CountDownLatch failed = new CountDownLatch(1);
        AutoCloseable subscription = null;
        try {
            subscription = hub.subscribe("TESTNET", "BTWUSDT", "15m", event -> {
                if ("ERROR".equals(event.type())) {
                    failed.countDown();
                }
            }, ignored -> {
            });

            assertTrue(failed.await(2, TimeUnit.SECONDS));
            Thread.sleep(1_200);
            verify(gateway, times(1)).subscribeMarket(anyString(), anyString(), anyString(), any());
            verify(gateway, times(1)).queryKlines(eq("TESTNET"), eq("BTWUSDT"), eq("15m"), anyInt());
        } finally {
            if (subscription != null) {
                subscription.close();
            }
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void lateSubscriberAfterPeriodTransitionReceivesLatestReference() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(120), true), candle(open.minusSeconds(60), true)));
        CountDownLatch firstReady = new CountDownLatch(1), secondReady = new CountDownLatch(1);
        java.util.List<Instant> references = new CopyOnWriteArrayList<>();
        try {
            hub.subscribe("BTCUSDT", "1m", event -> {
                if ("CONNECTED".equals(event.type())) {
                    firstReady.countDown();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(firstReady.await(2, TimeUnit.SECONDS));
            hub.subscribe("BTCUSDT", "1m", event -> {
                if (event.replay() && event.kline() != null) {
                    references.add(event.kline().openTime());
                }
                if ("CONNECTED".equals(event.type())) {
                    secondReady.countDown();
                }
            });
            assertTrue(secondReady.await(2, TimeUnit.SECONDS));
            assertEquals(List.of(open.minusSeconds(120), open.minusSeconds(60), open), references);
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void lateSubscriberReceivesReferenceCandlesBeforeReady() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        CountDownLatch firstReady = new CountDownLatch(1), secondReady = new CountDownLatch(1);
        java.util.List<Instant> references = new CopyOnWriteArrayList<>();
        try {
            hub.subscribe("BTCUSDT", "1m", event -> {
                if ("CONNECTED".equals(event.type())) {
                    firstReady.countDown();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(firstReady.await(2, TimeUnit.SECONDS));
            hub.subscribe("BTCUSDT", "1m", event -> {
                if (event.replay() && event.kline() != null) {
                    references.add(event.kline().openTime());
                }
                if ("CONNECTED".equals(event.type())) {
                    secondReady.countDown();
                }
            });
            assertTrue(secondReady.await(2, TimeUnit.SECONDS));
            assertEquals(List.of(open.minusSeconds(60), open), references);
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void localRestCongestionRetriesWithoutReplacingWebSocket() throws Exception {
        properties.getMarket().setReconnectDelaySeconds(1);
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger queries = new AtomicInteger();
        when(gateway.queryKlines(any(), any(), anyInt())).thenAnswer(i -> {
            if (queries.incrementAndGet() <= 3) {
                throw new IllegalStateException("query failed", new org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException());
            }
            return List.of(candle(open.minusSeconds(60), true), candle(open, false));
        });
        CountDownLatch replay = new CountDownLatch(1);
        try {
            hub.subscribe("BTCUSDT", "1m", event -> {
                if (event.replay()) {
                    replay.countDown();
                }
            });
            assertTrue(replay.await(5, TimeUnit.SECONDS));
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
            assertTrue(queries.get() >= 4);
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void binanceRateLimitDefersHistoryRecoveryWithoutReplacingWebSocket() throws Exception {
        properties.getMarket().setReconnectDelaySeconds(1);
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        when(gateway.queryKlines(any(), any(), anyInt())).thenThrow(new ExchangeMarketPort.RateLimitException("QUERY_KLINES", 418, -1003, "banned", 3_000));
        try {
            hub.subscribe("BTCUSDT", "1m", ignored -> {
            });

            Thread.sleep(1_200);

            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
            verify(gateway, times(1)).queryKlines(any(), any(), anyInt());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void clockSyncRateLimitKeepsWebSocketConnectedAndFailsClosed() throws Exception {
        properties.getMarket().setReconnectDelaySeconds(1);
        when(gateway.exchangeTimeOffsetMillis("LIVE")).thenThrow(new ExchangeMarketPort.RequestDeferredException(10_000));
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicBoolean healthy = new AtomicBoolean(true);
        try {
            hub.subscribe("LIVE", "BTCUSDT", "1m", ignored -> {
            }, healthy::set);

            Thread.sleep(150);
            checkHealth(hub);
            Thread.sleep(1_200);

            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
            assertFalse(healthy.get(), "clock calibration unavailable must keep the strategy gate closed");
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void disconnectImmediatelyClosesHealthGateWhileConsumerIsBlocked() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicBoolean healthy = new AtomicBoolean();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger staleDeliveries = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("TRADE".equals(e.type()) && e.sequenceId() == 10L) {
                    entered.countDown();
                    try {
                        release.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                if ("TRADE".equals(e.type()) && e.sequenceId() == 11L) {
                    staleDeliveries.incrementAndGet();
                }
            }, healthy::set);
            send("MARK");
            send("KLINE");
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (!healthy.get() && System.nanoTime() < end) {
                Thread.sleep(10);
            }
            assertTrue(healthy.get());
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 10L, Instant.now(), false));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 11L, Instant.now(), false));
            callback.get().accept(new ExchangeGateway.MarketEvent("DISCONNECTED", "BTCUSDT", "1m", null, null, Instant.now()));
            assertFalse(healthy.get());
            Thread.sleep(100);
            release.countDown();
            Thread.sleep(100);
            assertEquals(0, staleDeliveries.get());
        } finally {
            release.countDown();
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void failedConsumerReplaysTheFailedTradeInsteadOfSkippingIt() throws Exception {
        properties.getMarket().setReconnectDelaySeconds(1);
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicBoolean failOnce = new AtomicBoolean(true);
        CountDownLatch replayed = new CountDownLatch(1);
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenReturn(List.of(candle(open, false)));
        when(gateway.queryAggregateTradesFrom(any(), any(), eq(10L), anyInt())).thenReturn(List.of(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null
            , Instant.now(), 10L, Instant.now(), true)));
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("TRADE".equals(e.type())) {
                    if (failOnce.getAndSet(false)) {
                        throw new IllegalStateException("failed trade");
                    }
                    if (e.replay() && e.sequenceId() == 10L) {
                        replayed.countDown();
                    }
                }
            });
            send("MARK");
            send("KLINE");
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 10L, Instant.now(), false));
            assertTrue(replayed.await(3, TimeUnit.SECONDS));
            verify(gateway).queryAggregateTradesFrom(any(), any(), eq(10L), anyInt());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void oldEventsAfterRestOnlyRestoreStateAndFreshEventsMakeReady() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), replayed = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if ("TRADE".equals(e.type()) && e.replay()) {
                    replayed.incrementAndGet();
                }
            });
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now().minusSeconds(10), 10L, Instant.now(), false));
            assertTrue(await(replayed));
            assertEquals(0, connected.get());
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void smallFutureExchangeTimestampIsNormalizedWithoutReconnecting() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), disconnected = new AtomicInteger();
        AtomicBoolean healthy = new AtomicBoolean();
        AtomicReference<ExchangeGateway.MarketEvent> receivedTrade = new AtomicReference<>();
        CountDownLatch tradeReceived = new CountDownLatch(1);
        try {
            hub.subscribe("BTCUSDT", "1m", event -> {
                if ("CONNECTED".equals(event.type())) {
                    connected.incrementAndGet();
                }
                if ("DISCONNECTED".equals(event.type())) {
                    disconnected.incrementAndGet();
                }
                if ("TRADE".equals(event.type())) {
                    receivedTrade.set(event);
                    tradeReceived.countDown();
                }
            }, healthy::set);
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            assertTrue(healthy.get());

            Instant receivedAt = Instant.now();
            Instant eventTime = receivedAt.plusMillis(1_500);
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, eventTime, 1L, receivedAt, false));
            assertTrue(tradeReceived.await(2, TimeUnit.SECONDS));
            assertEquals(1, connected.get());
            assertEquals(0, disconnected.get());
            assertTrue(healthy.get());
            assertEquals(receivedAt, receivedTrade.get().eventTime(), "strategy timestamps must use local receipt time when exchange time leads the local clock");
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void restEndingAtPreviousClosedCandleWaitsForCurrentWsWithoutReconnecting() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(120), true), candle(open.minusSeconds(60), true)));
        AtomicInteger connected = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void transientRestTimeoutRetriesWithoutClosingWebsocket() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        when(gateway.queryKlines(any(), any(), anyInt())).thenThrow(new IllegalStateException("timeout", new java.net.SocketTimeoutException())).thenReturn(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            verify(gateway, times(2)).queryKlines(any(), any(), anyInt());
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void emptyHistoryCannotBecomeReady() throws Exception {
        MarketStreamHub hub = hub(List.of());
        AtomicInteger connected = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
            });
            send("MARK");
            send("KLINE");
            Thread.sleep(150);
            assertEquals(0, connected.get());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void verifiedCurrentRestAndFreshMarkCanDeclareReadyWithoutLiveKline() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
            });
            send("MARK");
            assertTrue(await(connected));
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void slowRestMustNotBlockSubscriptionOrOtherSchedulerTasks() throws Exception {
        MarketStreamHub hub = hub(List.of());
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(gateway.queryKlines(any(), any(), anyInt())).thenAnswer(i -> {
            entered.countDown();
            release.await(3, TimeUnit.SECONDS);
            return List.of(candle(open.minusSeconds(60), true), candle(open, false));
        });
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<?> subscribing = executor.submit(() -> hub.subscribe("BTCUSDT", "1m", e -> {
            }));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertDoesNotThrow(() -> subscribing.get(200, TimeUnit.MILLISECONDS));
            assertDoesNotThrow(() -> scheduler.submit(() -> {
            }).get(200, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            hub.close();
            scheduler.shutdownNow();
            executor.shutdownNow();
        }
    }

    @Test
    void missingFinalCandleMustBeCorrectedBeforeNextLiveCandle() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger corrected = new AtomicInteger();
        CountDownLatch next = new CountDownLatch(1);
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenReturn(List.of(candle(open, true), candle(open.plusSeconds(60), false)));
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if (e.kline() != null && e.replay() && e.kline().openTime().equals(open) && e.kline().closed()) {
                    corrected.incrementAndGet();
                }
                if (e.kline() != null && e.kline().openTime().equals(open.plusSeconds(60))) {
                    next.countDown();
                }
            });
            send("MARK");
            send("KLINE");
            callback.get().accept(new ExchangeGateway.MarketEvent("KLINE", "BTCUSDT", "1m", null, candle(open.plusSeconds(60), false), Instant.now()));
            assertTrue(next.await(2, TimeUnit.SECONDS));
            assertEquals(1, corrected.get());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    private Object stream(MarketStreamHub hub) throws Exception {
        var field = MarketStreamHub.class.getDeclaredField("streams");
        field.setAccessible(true);
        return ((java.util.Map<?, ?>) field.get(hub)).values().iterator().next();
    }

    @SuppressWarnings("unchecked")
    private void expireReceived(MarketStreamHub hub, String type) throws Exception {
        Object stream = stream(hub);
        var field = stream.getClass().getDeclaredField("lastReceived");
        field.setAccessible(true);
        ((java.util.Map<String, Instant>) field.get(stream)).put(type, Instant.now().minusSeconds(30));
    }

    private void checkHealth(MarketStreamHub hub) throws Exception {
        Object stream = stream(hub);
        var method = stream.getClass().getDeclaredMethod("checkHealth");
        method.setAccessible(true);
        method.invoke(stream);
    }

    @Test
    void previousClosedRestAndMarkCannotDeclareCurrentPeriodReady() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(120), true), candle(open.minusSeconds(60), true)));
        AtomicInteger connected = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
            });
            send("MARK");
            Thread.sleep(150);
            assertEquals(0, connected.get());
            send("KLINE");
            assertTrue(await(connected));
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void quietKlineWithFreshMarkRevalidatesRestWithoutClosingWebsocket() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), disconnected = new AtomicInteger();
        AtomicBoolean healthy = new AtomicBoolean();
        CountDownLatch checked = new CountDownLatch(1), release = new CountDownLatch(1);
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenAnswer(i -> {
            checked.countDown();
            release.await(2, TimeUnit.SECONDS);
            return List.of(candle(open, false));
        });
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if ("DISCONNECTED".equals(e.type())) {
                    disconnected.incrementAndGet();
                }
            }, healthy::set);
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            expireReceived(hub, "KLINE");
            checkHealth(hub);
            assertTrue(checked.await(1, TimeUnit.SECONDS));
            assertTrue(healthy.get(), "有效周期内的K线静默校验不应暂停健康行情");
            assertEquals(0, disconnected.get());
            release.countDown();
            send("MARK");
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (connected.get() < 1 && System.nanoTime() < end) {
                Thread.sleep(10);
            }
            assertEquals(1, connected.get());
            checkHealth(hub);
            verify(gateway, times(1)).queryKlinesFrom(any(), any(), any(), anyInt());
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            release.countDown();
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void quietRestResultCannotOverwriteNewerWebsocketKline() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger();
        CountDownLatch checked = new CountDownLatch(1), release = new CountDownLatch(1), fresh = new CountDownLatch(1);
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenAnswer(i -> {
            checked.countDown();
            release.await(2, TimeUnit.SECONDS);
            return List.of(candle(open, false));
        });
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if (e.kline() != null && !e.replay() && e.kline().close().compareTo(new BigDecimal("12")) == 0) {
                    fresh.countDown();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            expireReceived(hub, "KLINE");
            checkHealth(hub);
            assertTrue(checked.await(1, TimeUnit.SECONDS));
            var updated = new ExchangeGateway.Kline(open, open.plusSeconds(60).minusMillis(1), BigDecimal.TEN, new BigDecimal("12"), BigDecimal.TEN, new BigDecimal("12"),
                BigDecimal.ONE, false);
            callback.get().accept(new ExchangeGateway.MarketEvent("KLINE", "BTCUSDT", "1m", null, updated, Instant.now()));
            assertTrue(fresh.await(1, TimeUnit.SECONDS));
            release.countDown();
            var taskField = stream(hub).getClass().getDeclaredField("quietCheckTask");
            taskField.setAccessible(true);
            ((java.util.concurrent.Future<?>) taskField.get(stream(hub))).get(1, TimeUnit.SECONDS);
            var refs = stream(hub).getClass().getDeclaredField("referenceKlines");
            refs.setAccessible(true);
            @SuppressWarnings("unchecked") var candles = (java.util.NavigableMap<Instant, ExchangeGateway.Kline>) refs.get(stream(hub));
            synchronized (stream(hub)) {
                assertEquals(new BigDecimal("12"), candles.get(open).close());
            }
            assertEquals(1, connected.get());
        } finally {
            release.countDown();
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void missingMarkStillDisconnectsEvenWhenKlineExists() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), disconnected = new AtomicInteger();
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if ("DISCONNECTED".equals(e.type())) {
                    disconnected.incrementAndGet();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            expireReceived(hub, "MARK");
            checkHealth(hub);
            assertTrue(await(disconnected));
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    void aggregateTradeGapIsReplayedInOrderWithoutClosingWebsocket() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), disconnected = new AtomicInteger();
        java.util.List<Long> replayIds = new CopyOnWriteArrayList<>();
        CountDownLatch first = new CountDownLatch(1), restored = new CountDownLatch(1);
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenReturn(List.of(candle(open, false)));
        when(gateway.queryAggregateTradesFrom(any(), any(), eq(11L), anyInt())).thenReturn(List.of(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null
            , Instant.now(), 11L, Instant.now(), true), new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 12L, Instant.now(), true)));
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if ("DISCONNECTED".equals(e.type())) {
                    disconnected.incrementAndGet();
                }
                if ("TRADE".equals(e.type()) && e.sequenceId() == 10L) {
                    first.countDown();
                }
                if ("TRADE".equals(e.type()) && e.replay()) {
                    replayIds.add(e.sequenceId());
                    if (e.sequenceId() == 12L) {
                        restored.countDown();
                    }
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 10L, Instant.now(), false));
            assertTrue(first.await(1, TimeUnit.SECONDS));
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 12L, Instant.now(), false));
            assertTrue(restored.await(2, TimeUnit.SECONDS));
            assertEquals(List.of(11L, 12L), replayIds);
            send("MARK");
            Thread.sleep(100);
            assertEquals(0, disconnected.get());
            verify(gateway, times(1)).subscribeMarket(any(), any(), any());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }


    @Test
    void incompleteTradeGapCannotResumeDecisions() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), disconnected = new AtomicInteger();
        CountDownLatch first = new CountDownLatch(1);
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenReturn(List.of(candle(open, false)));
        when(gateway.queryAggregateTradesFrom(any(), any(), eq(11L), anyInt())).thenReturn(List.of());
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if ("DISCONNECTED".equals(e.type())) {
                    disconnected.incrementAndGet();
                }
                if ("TRADE".equals(e.type()) && e.sequenceId() == 10L) {
                    first.countDown();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 10L, Instant.now(), false));
            assertTrue(first.await(1, TimeUnit.SECONDS));
            callback.get().accept(new ExchangeGateway.MarketEvent("TRADE", "BTCUSDT", "1m", BigDecimal.TEN, null, Instant.now(), 12L, Instant.now(), false));
            assertTrue(await(disconnected));
            assertEquals(1, connected.get());
        } finally {
            hub.close();
            scheduler.shutdownNow();
        }
    }


    @Test
    void restRefreshMustNotRefreshMissingMarkHeartbeat() throws Exception {
        MarketStreamHub hub = hub(List.of(candle(open.minusSeconds(60), true), candle(open, false)));
        AtomicInteger connected = new AtomicInteger(), disconnected = new AtomicInteger();
        CountDownLatch checked = new CountDownLatch(1), release = new CountDownLatch(1), restored = new CountDownLatch(1);
        AtomicInteger replayed = new AtomicInteger();
        when(gateway.queryKlinesFrom(any(), any(), any(), anyInt())).thenAnswer(i -> {
            checked.countDown();
            release.await(2, TimeUnit.SECONDS);
            return List.of(candle(open, false));
        });
        try {
            hub.subscribe("BTCUSDT", "1m", e -> {
                if ("CONNECTED".equals(e.type())) {
                    connected.incrementAndGet();
                }
                if ("DISCONNECTED".equals(e.type())) {
                    disconnected.incrementAndGet();
                }
                if (e.kline() != null && e.replay() && replayed.incrementAndGet() >= 3) {
                    restored.countDown();
                }
            });
            send("MARK");
            send("KLINE");
            assertTrue(await(connected));
            expireReceived(hub, "KLINE");
            checkHealth(hub);
            assertTrue(checked.await(1, TimeUnit.SECONDS));
            expireReceived(hub, "MARK");
            release.countDown();
            assertTrue(restored.await(1, TimeUnit.SECONDS));
            Thread.sleep(50);
            checkHealth(hub);
            assertTrue(await(disconnected));
        } finally {
            release.countDown();
            hub.close();
            scheduler.shutdownNow();
        }
    }

}

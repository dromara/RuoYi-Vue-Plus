package org.dromara.quant.exchange.binance;

import org.dromara.quant.exchange.ExchangeGateway;

import org.junit.jupiter.api.Test;
import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BinanceGatewayAdapterTest {
    @Test
    void usesCachedClockOffsetDuringTheThirtySecondToFiveMinuteRefreshWindow() {
        long now = 1_800_000_000_000L;

        assertFalse(BinanceGatewayAdapter.isClockOffsetRefreshDue(now - 29_999L, now));
        assertTrue(BinanceGatewayAdapter.isClockOffsetRefreshDue(now - 30_000L, now));
        assertTrue(BinanceGatewayAdapter.isMarketClockOffsetCacheUsable(now - 30_000L, now));
        assertTrue(BinanceGatewayAdapter.isMarketClockOffsetCacheUsable(now - 5 * 60_000L, now));
    }

    @Test
    void refusesClockOffsetCacheOlderThanFiveMinutesOrFromFuture() {
        long now = 1_800_000_000_000L;

        assertFalse(BinanceGatewayAdapter.isMarketClockOffsetCacheUsable(now - 5 * 60_000L - 1L, now));
        assertFalse(BinanceGatewayAdapter.isMarketClockOffsetCacheUsable(now + 1L, now));
    }

    @Test
    void onlyProvesAlgoOrderAbsenceInsideBinanceThreeDayHistoryWindow() {
        long now = 1_800_000_000_000L;
        long threeDays = java.time.Duration.ofDays(3).toMillis();

        assertTrue(BinanceGatewayAdapter.isProtectionHistoryAbsenceProvable(now - threeDays + 1, now));
        assertFalse(BinanceGatewayAdapter.isProtectionHistoryAbsenceProvable(now - threeDays, now));
        assertFalse(BinanceGatewayAdapter.isProtectionHistoryAbsenceProvable(now - threeDays - 1, now));
        assertFalse(BinanceGatewayAdapter.isProtectionHistoryAbsenceProvable(now + 1, now));
        assertFalse(BinanceGatewayAdapter.isProtectionHistoryAbsenceProvable(0, now));
    }

    @Test
    void mapsPositionSideFromBinanceAccountTradeResponse() {
        var raw = new com.binance.connector.client.derivatives_trading_usds_futures.rest.model.AccountTradeListResponseInner()
            .symbol("BTCUSDT").id(123L).orderId(456L).side("SELL").qty("0.5").price("100")
            .commission("0.01").commissionAsset("USDT").realizedPnl("0").time(1_762_000_000_000L)
            .positionSide("SHORT");

        ExchangeGateway.TradeSnapshot snapshot = BinanceGatewayAdapter.tradeSnapshot(raw);

        assertEquals("SHORT", snapshot.positionSide());
        assertEquals(456L, snapshot.orderId());
    }

    @Test
    void localRestDeferralIsClassifiedAsDefinitelyNotSent() {
        var deferred = new org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException(5_000L);
        RuntimeException classified = BinanceGatewayAdapter.classifyOrderDispatchFailure(deferred);

        assertInstanceOf(org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException.class, classified);
        assertTrue(classified.getMessage().contains("5000ms"));
    }

    @Test
    void ambiguousRuntimeOrderFailureRemainsAmbiguous() {
        var ambiguous = new IllegalStateException("transport failed after dispatch may have started");

        assertSame(ambiguous, BinanceGatewayAdapter.classifyOrderDispatchFailure(ambiguous));
    }

    @Test
    void exactOrderIdentityRequiresExchangeReturnedFieldsAndMatchingQueryIdentity() {
        assertTrue(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", 123L, "client-123", "ETHUSDT"));
        assertFalse(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", null, "client-123", "ETHUSDT"));
        assertFalse(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", 123L, null, "ETHUSDT"));
        assertFalse(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", 123L, "client-123", null));
        assertFalse(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", 124L, "client-123", "ETHUSDT"));
        assertFalse(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", 123L, "other-client", "ETHUSDT"));
        assertFalse(BinanceGatewayAdapter.hasExactOrderIdentity("ETHUSDT", 123L, "client-123", 123L, "client-123", "BTCUSDT"));
    }
    @Test
    void rateLimitResponseBlocksFurtherRestRequestsAndPreservesRetryAfter() {
        var adapter = new BinanceGatewayAdapter(new org.dromara.quant.config.QuantProperties());
        var apiException = new ApiException(418, "I'm a teapot", java.util.Map.of("retry-after", List.of("5")),
            "{\"code\":-1003,\"msg\":\"Way too many requests\"}");

        RuntimeException mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(adapter, "requestFailure", "QUERY_KLINES", apiException);

        var rateLimit = assertInstanceOf(org.dromara.quant.exchange.ExchangeMarketPort.RateLimitException.class, mapped);
        assertTrue(rateLimit.retryAfterMillis() >= 4_000);
        assertTrue(rateLimit.retryAfterMillis() <= 5_000);
        var scheduler = (RestRequestScheduler) org.springframework.test.util.ReflectionTestUtils.getField(adapter, "restScheduler");
        assertThrows(org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException.class, () -> scheduler.acquire(RestRequestScheduler.Kind.BACKGROUND));
    }

    @Test
    void classifiesBinanceInvalidSymbolResponseFromWrappedSdkException() {
        ApiException invalidSymbol = new ApiException(400, "Bad Request", java.util.Map.of(), "{\"code\":-1121,\"msg\":\"Invalid symbol.\"}");
        ApiException otherError = new ApiException(400, "Bad Request", java.util.Map.of(), "{\"code\":-1003,\"msg\":\"Too many requests.\"}");

        assertTrue(BinanceGatewayAdapter.isInvalidSymbolError(new IllegalStateException("SDK", invalidSymbol)));
        assertFalse(BinanceGatewayAdapter.isInvalidSymbolError(new IllegalStateException("SDK", otherError)));
    }

    @Test
    void doesNotExpireExchangeFreshSignalWhenLocalClockIsAhead() {
        Instant localNow = Instant.parse("2026-10-17T16:03:23Z");
        Instant signalDeadline = Instant.parse("2026-10-17T16:03:22.695Z");

        assertFalse(BinanceGatewayAdapter.isOrderDeadlineExpired(signalDeadline, localNow, -3_050));
    }

    @Test
    void expiresSignalThatIsStaleByExchangeTime() {
        Instant localNow = Instant.parse("2026-10-17T16:03:23Z");
        Instant signalDeadline = Instant.parse("2026-10-17T16:03:19Z");

        assertTrue(BinanceGatewayAdapter.isOrderDeadlineExpired(signalDeadline, localNow, -3_050));
    }

    @Test
    void normalizesAlgoStatusesWithoutTreatingUnknownValuesAsKnownStates() {
        assertEquals("NEW", BinanceGatewayAdapter.normalizeAlgoStatus(" new "));
        assertEquals("TRIGGERED", BinanceGatewayAdapter.normalizeAlgoStatus("triggered"));
        assertEquals("FUTURE_STATUS", BinanceGatewayAdapter.normalizeAlgoStatus(" future_status "));
        assertNull(BinanceGatewayAdapter.normalizeAlgoStatus(null));
    }

    @Test
    void treatsUntriggeredTerminalAlgoOrderWithoutActualQuantityAsZeroFill() {
        assertEquals(java.math.BigDecimal.ZERO, BinanceGatewayAdapter.protectionExecutedQuantity("CANCELED", null));
        assertEquals(java.math.BigDecimal.ZERO, BinanceGatewayAdapter.protectionExecutedQuantity("EXPIRED", null));
        assertNull(BinanceGatewayAdapter.protectionExecutedQuantity("FINISHED", null));
        assertEquals(new java.math.BigDecimal("0.25"), BinanceGatewayAdapter.protectionExecutedQuantity("FINISHED", new java.math.BigDecimal("0.25")));
    }

    @Test
    void mapsOnlyExactRecentAlgoHistoryRecordAndConfirmsUntriggeredTerminalAsZeroFill() {
        QueryAllAlgoOrdersResponseInner other = new QueryAllAlgoOrdersResponseInner().symbol("BTCUSDT").algoId(11L).clientAlgoId("OTHER")
            .algoStatus("CANCELED").updateTime(1_760_760_000_000L);
        QueryAllAlgoOrdersResponseInner match = new QueryAllAlgoOrdersResponseInner().symbol("BTCUSDT").algoId(12L).clientAlgoId("QFTP_exact")
            .orderType("TAKE_PROFIT_MARKET").side("SELL").positionSide("BOTH").quantity("5").triggerPrice("100")
            .algoStatus("CANCELED").updateTime(1_760_760_001_000L).reduceOnly(true).closePosition(false);

        var snapshot = BinanceGatewayAdapter.findExactAlgoHistory(List.of(other, match), "BTCUSDT", null, "QFTP_exact");

        assertNotNull(snapshot);
        assertEquals(12L, snapshot.algoId());
        assertEquals("QFTP_exact", snapshot.clientAlgoId());
        assertEquals("CANCELED", snapshot.status());
        assertEquals(0, snapshot.executedQuantity().compareTo(java.math.BigDecimal.ZERO));
        assertNull(BinanceGatewayAdapter.findExactAlgoHistory(List.of(match), "BTCUSDT", null, "QFTP_other"));
    }

    @Test
    void splitsSaturatedAlgoHistoryWindowsUntilEveryWindowIsComplete() {
        var queried = new java.util.ArrayList<String>();
        var result = BinanceGatewayAdapter.collectCompleteAlgoHistory(0L, 3L, (start, end) -> {
            queried.add(start + ":" + end);
            if (end - start >= 1) {
                return java.util.Collections.nCopies(1000, new QueryAllAlgoOrdersResponseInner());
            }
            return List.of(new QueryAllAlgoOrdersResponseInner().symbol("BTCUSDT").algoId(start + 1).clientAlgoId("QFTP_" + start));
        });

        assertTrue(result.complete());
        assertEquals(4, result.orders().size());
        assertEquals(List.of("0:3", "0:1", "0:0", "1:1", "2:3", "2:2", "3:3"), queried);
    }

    @Test
    void marksAlgoHistoryIncompleteWhenOneMillisecondWindowStillHitsLimit() {
        var result = BinanceGatewayAdapter.collectCompleteAlgoHistory(100L, 100L, (start, end) ->
            java.util.Collections.nCopies(1000, new QueryAllAlgoOrdersResponseInner()));

        assertFalse(result.complete());
        assertTrue(result.orders().isEmpty());
    }

    @Test
    void latchesRestClosedWhenHttp418HasNoTrustworthyBanExpiry() {
        var adapter = new BinanceGatewayAdapter(new org.dromara.quant.config.QuantProperties());
        var apiException = new ApiException(418, "I'm a teapot", java.util.Map.of(),
            "{\"code\":-1003,\"msg\":\"Way too many requests\"}");

        RuntimeException mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(adapter, "requestFailure", "QUERY_KLINES", apiException);

        var rateLimit = assertInstanceOf(org.dromara.quant.exchange.ExchangeMarketPort.RateLimitException.class, mapped);
        assertEquals(86_400_000L, rateLimit.retryAfterMillis());
        var scheduler = (RestRequestScheduler) org.springframework.test.util.ReflectionTestUtils.getField(adapter, "restScheduler");
        assertTrue(scheduler.isManuallyBlocked());
        assertThrows(org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException.class, () -> scheduler.acquire(RestRequestScheduler.Kind.TRADE));
    }

    @Test
    void defaults429ToBoundedDelayWhenRetryAfterHeaderIsMissing() {
        var adapter = new BinanceGatewayAdapter(new org.dromara.quant.config.QuantProperties());
        var apiException = new ApiException(429, "Too Many Requests", java.util.Map.of(),
            "{\"code\":-1003,\"msg\":\"Too many requests\"}");

        RuntimeException mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(adapter, "requestFailure", "QUERY_ACCOUNT", apiException);

        var rateLimit = assertInstanceOf(org.dromara.quant.exchange.ExchangeMarketPort.RateLimitException.class, mapped);
        assertEquals(60_000L, rateLimit.retryAfterMillis());
        var scheduler = (RestRequestScheduler) org.springframework.test.util.ReflectionTestUtils.getField(adapter, "restScheduler");
        assertThrows(org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException.class, () -> scheduler.acquire(RestRequestScheduler.Kind.ACCOUNT));
    }


    @Test
    void retriesTransientTlsHandshakeEofAndSucceeds() {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        String result = BinanceGatewayAdapter.executeWithNetworkRetries(() -> {
            if (attempts.incrementAndGet() <= 2) {
                var handshake = new javax.net.ssl.SSLHandshakeException("Remote host terminated the handshake");
                handshake.initCause(new java.io.EOFException("SSL peer shut down incorrectly"));
                throw new IllegalStateException("SDK transport failure", handshake);
            }
            return "ok";
        }, RestRequestScheduler.Kind.ACCOUNT, "TESTNET");

        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void doesNotRetryCertificateValidationFailure() {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        var failure = new IllegalStateException("TLS certificate validation failed",
            new java.security.cert.CertificateException("certificate expired"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            BinanceGatewayAdapter.executeWithNetworkRetries(() -> {
                attempts.incrementAndGet();
                throw failure;
            }, RestRequestScheduler.Kind.ACCOUNT, "TESTNET"));

        assertSame(failure, thrown);
        assertEquals(1, attempts.get());
    }

    @Test
    void retriesTransientNetworkFailureAtMostThreeTimes() {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        var failure = new IllegalStateException("connection reset", new java.net.SocketException("Connection reset"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            BinanceGatewayAdapter.executeWithNetworkRetries(() -> {
                attempts.incrementAndGet();
                throw failure;
            }, RestRequestScheduler.Kind.ACCOUNT, "TESTNET"));

        assertSame(failure, thrown);
        assertEquals(4, attempts.get());
    }

    @Test
    void doesNotRetryLocallyDeferredRestRequest() {
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        var deferred = new org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException(5_000L);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            BinanceGatewayAdapter.executeWithNetworkRetries(() -> {
                attempts.incrementAndGet();
                throw deferred;
            }, RestRequestScheduler.Kind.ACCOUNT, "TESTNET"));

        assertSame(deferred, thrown);
        assertEquals(1, attempts.get());
    }
}

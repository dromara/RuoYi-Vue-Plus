package org.dromara.quant.exchange.binance;

import com.sun.net.httpserver.HttpServer;
import org.dromara.quant.config.QuantProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class BinanceGatewayClockOffsetTest {
    private HttpServer server(AtomicInteger calls, AtomicInteger status) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            String response = status.get() == 200 ? "{\"serverTime\":" + System.currentTimeMillis() + "}" : "{\"code\":-1000,\"msg\":\"test failure\"}";
            byte[] data = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.start();
        return server;
    }

    private long clock(BinanceGatewayAdapter adapter, HttpServer server) {
        return ReflectionTestUtils.invokeMethod(adapter, "clockOffset", "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void requestBodyDetectionDoesNotRelyOnInternalOkHttpApi() {
        for (String method : new String[]{"POST", "PUT", "PATCH", "PROPPATCH", "REPORT"}) {
            assertTrue(BinanceGatewayAdapter.requiresRequestBody(method), method);
        }
        for (String method : new String[]{"GET", "DELETE", "HEAD"}) {
            assertFalse(BinanceGatewayAdapter.requiresRequestBody(method), method);
        }
        assertFalse(BinanceGatewayAdapter.requiresRequestBody(null));
    }

    @Test
    void concurrentRequestsShareOneServerTimeRefresh() throws Exception {
        var calls = new AtomicInteger();
        var server = server(calls, new AtomicInteger(200));
        var adapter = new BinanceGatewayAdapter(new QuantProperties());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var results = new ArrayList<Future<Long>>();
            for (int i = 0; i < 4; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return clock(adapter, server);
                }));
            }
            start.countDown();
            long first = results.getFirst().get(4, TimeUnit.SECONDS);
            for (var result : results) {
                assertEquals(first, result.get(4, TimeUnit.SECONDS));
            }
            assertEquals(first, clock(adapter, server));
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void staleButUsableMarketClockOffsetReturnsImmediatelyAndRefreshesOnceInBackground() throws Exception {
        var calls = new AtomicInteger();
        var server = server(calls, new AtomicInteger(200));
        var properties = new QuantProperties();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        properties.getBinance().setBaseUrl(baseUrl);
        var adapter = new BinanceGatewayAdapter(properties);
        try {
            long cachedValue = clock(adapter, server);
            @SuppressWarnings("unchecked")
            var offsets = (java.util.Map<String, Object>) ReflectionTestUtils.getField(adapter, "clockOffsets");
            long staleUpdatedAt = System.currentTimeMillis() - 31_000L;
            Class<?> offsetType = Class.forName("org.dromara.quant.exchange.binance.BinanceGatewayAdapter$ClockOffset");
            var constructor = offsetType.getDeclaredConstructor(long.class, long.class);
            constructor.setAccessible(true);
            offsets.put(baseUrl, constructor.newInstance(cachedValue, staleUpdatedAt));

            long startedAt = System.nanoTime();
            for (int i = 0; i < 8; i++) {
                assertEquals(cachedValue, adapter.exchangeTimeOffsetMillis("LIVE"));
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            assertTrue(elapsedMillis < 150L, "a usable cached offset must not wait for the refresh HTTP request");

            var updatedAtAccessor = offsetType.getDeclaredMethod("updatedAt");
            updatedAtAccessor.setAccessible(true);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            long refreshedAt = staleUpdatedAt;
            while (System.nanoTime() < deadline && refreshedAt <= staleUpdatedAt) {
                Thread.sleep(10L);
                refreshedAt = (long) updatedAtAccessor.invoke(offsets.get(baseUrl));
            }

            assertTrue(refreshedAt > staleUpdatedAt, "the background refresh should replace the stale cache");
            assertEquals(2, calls.get(), "concurrent market callers should share one background refresh");
        } finally {
            adapter.shutdownClockOffsetRefreshExecutor();
            server.stop(0);
        }
    }

    @Test
    void liveAndTestnetEndpointsHaveIndependentClockCaches() throws Exception {
        var liveCalls = new AtomicInteger();
        var testCalls = new AtomicInteger();
        var live = server(liveCalls, new AtomicInteger(200));
        var testnet = server(testCalls, new AtomicInteger(200));
        try {
            var adapter = new BinanceGatewayAdapter(new QuantProperties());
            clock(adapter, live);
            clock(adapter, testnet);
            clock(adapter, live);
            clock(adapter, testnet);
            assertEquals(1, liveCalls.get());
            assertEquals(1, testCalls.get());
        } finally {
            live.stop(0);
            testnet.stop(0);
        }
    }

    @Test
    void failedRefreshIsNotCachedAndCanRecover() throws Exception {
        var calls = new AtomicInteger();
        var status = new AtomicInteger(500);
        var server = server(calls, status);
        try {
            var adapter = new BinanceGatewayAdapter(new QuantProperties());
            assertThrows(RuntimeException.class, () -> clock(adapter, server));
            status.set(200);
            clock(adapter, server);
            clock(adapter, server);
            assertEquals(2, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void signedAccountReadRefreshesClockAndRetriesOnceAfterInvalidTimestamp() throws Exception {
        var timeCalls = new AtomicInteger();
        var accountCalls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fapi/v1/time", exchange -> {
            long skewMillis = timeCalls.incrementAndGet() == 1 ? 1500 : 0;
            byte[] body = ("{\"serverTime\":" + (System.currentTimeMillis() + skewMillis) + "}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/fapi/v3/account", exchange -> {
            accountCalls.incrementAndGet();
            long timestamp = java.util.Arrays.stream(exchange.getRequestURI().getRawQuery().split("&"))
                .filter(value -> value.startsWith("timestamp="))
                .map(value -> value.substring("timestamp=".length()))
                .mapToLong(Long::parseLong)
                .findFirst()
                .orElseThrow();
            int status = timestamp > System.currentTimeMillis() + 999 ? 400 : 200;
            byte[] body = (status == 400
                ? "{\"code\":-1021,\"msg\":\"Timestamp for this request was 1000ms ahead of the server's time.\"}"
                : "{\"totalWalletBalance\":\"100\",\"availableBalance\":\"90\",\"totalMarginBalance\":\"100\",\"totalUnrealizedProfit\":\"0\"}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var properties = new QuantProperties();
            properties.getBinance().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var adapter = new BinanceGatewayAdapter(properties);

            var snapshot = adapter.queryAccountFresh(new org.dromara.quant.exchange.ExchangeGateway.Credential("test-key", "test-secret", "HMAC", "LIVE"));

            assertEquals(100, snapshot.walletBalance().intValueExact());
            assertEquals(2, timeCalls.get());
            assertEquals(2, accountCalls.get());
        } finally {
            server.stop(0);
        }
    }
}

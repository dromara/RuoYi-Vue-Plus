package org.dromara.quant.exchange.binance;

import com.sun.net.httpserver.HttpServer;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeTradingPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class BinanceGatewayTradingRuleTest {
    private HttpServer server(String step, AtomicInteger calls, int status) throws Exception {
        return server(step, calls, status, -4023);
    }

    private HttpServer server(String step, AtomicInteger calls, int status, int errorCode) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body;
            int code = 200;
            if (path.endsWith("/time")) {
                body = "{\"serverTime\":" + System.currentTimeMillis() + "}";
            } else if (path.endsWith("/exchangeInfo")) {
                calls.incrementAndGet();
                body = "{\"symbols\":[{\"symbol\":\"ONDOUSDT\",\"quoteAsset\":\"USDT\",\"status\":\"TRADING\",\"pricePrecision\":2,\"quantityPrecision\":2," +
                    "\"filters\":[{\"filterType\":\"PRICE_FILTER\",\"tickSize\":\"0.01\"},{\"filterType\":\"LOT_SIZE\",\"minQty\":\"0.01\",\"maxQty\":\"100\",\"stepSize\":\"" + step + "\"},{\"filterType" +
                    "\":\"MARKET_LOT_SIZE\",\"minQty\":\"0.01\",\"maxQty\":\"50\",\"stepSize\":\"" + step + "\"},{\"filterType\":\"MIN_NOTIONAL\",\"notional\":\"5\"}]}]}";
            } else {
                code = status;
                body = status == 400 ? "{\"code\":" + errorCode + ",\"msg\":\"No need to change is not a confirmation\"}" : "{\"code\":-1007,\"msg\":\"Timeout\"}";
            }
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.start();
        return server;
    }

    @Test
    void fairnessDoesNotLetBackgroundTrafficStarveAccountCalibration() throws Exception {
        var scheduler = new RestRequestScheduler(50);
        scheduler.acquire(RestRequestScheduler.Kind.BACKGROUND);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var background = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 8; i++) {
                background.add(executor.submit(() -> scheduler.acquire(RestRequestScheduler.Kind.BACKGROUND)));
            }
            var account = executor.submit(() -> scheduler.acquire(RestRequestScheduler.Kind.ACCOUNT));
            account.get(300, java.util.concurrent.TimeUnit.MILLISECONDS);
            for (var task : background) {
                task.get(2, java.util.concurrent.TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void equivalentAccountReadsShareOnlyInflightRequestAndFreshReadsBypassIt() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var calls = new AtomicInteger();
        var gateway = new BinanceGatewayAdapter(new QuantProperties()) {
            @Override
            public ExchangeGateway.AccountSnapshot queryAccountFresh(ExchangeGateway.Credential credential) {
                calls.incrementAndGet();
                entered.countDown();
                try {
                    assertTrue(release.await(2, java.util.concurrent.TimeUnit.SECONDS));
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                }
                return new ExchangeGateway.AccountSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);
            }
        };
        var credential = new ExchangeGateway.Credential("test", "test", "HMAC", "TESTNET");
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        try {
            var first = executor.submit(() -> gateway.queryAccount(credential));
            assertTrue(entered.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var second = executor.submit(() -> gateway.queryAccount(credential));
            Thread.sleep(50);
            release.countDown();
            assertSame(first.get(2, java.util.concurrent.TimeUnit.SECONDS), second.get(2, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            gateway.queryAccountFresh(credential);
            assertEquals(2, calls.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void rulesAreCachedSeparatelyForLiveAndTestnet() throws Exception {
        AtomicInteger liveCalls = new AtomicInteger(), testCalls = new AtomicInteger();
        HttpServer live = server("0.1", liveCalls, 400), test = server("1", testCalls, 400);
        try {
            QuantProperties props = new QuantProperties();
            props.getBinance().setBaseUrl("http://127.0.0.1:" + live.getAddress().getPort());
            props.getBinance().setTestnetUrl("http://127.0.0.1:" + test.getAddress().getPort());
            var gateway = new BinanceGatewayAdapter(props);
            var testCredential = new ExchangeGateway.Credential("test", "test", "HMAC", "TESTNET");
            ExchangeGateway.SymbolRule testRule = gateway.queryTradingRule(testCredential, "ONDOUSDT");
            assertEquals(0, BigDecimal.ONE.compareTo(testRule.stepSize()));
            assertEquals(0, new BigDecimal("100").compareTo(testRule.maxQuantity()));
            assertEquals(0, new BigDecimal("50").compareTo(testRule.marketMaxQuantity()));
            gateway.queryTradingRule(testCredential, "ONDOUSDT");
            assertEquals(0, new BigDecimal("0.1").compareTo(gateway.queryTradingRule(new ExchangeGateway.Credential("test", "test", "HMAC", "LIVE"), "ONDOUSDT").stepSize()));
            assertEquals(1, liveCalls.get());
            assertEquals(1, testCalls.get());
        } finally {
            live.stop(0);
            test.stop(0);
        }
    }

    @Test
    void explicitQuantityErrorIsRejectedBut503IsNot() throws Exception {
        for (int status : new int[]{400, 503}) {
            HttpServer server = server("1", new AtomicInteger(), status);
            try {
                QuantProperties props = new QuantProperties();
                props.getBinance().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
                var gateway = new BinanceGatewayAdapter(props);
                var request = new ExchangeGateway.OrderRequest(new ExchangeGateway.Credential("test", "test", "HMAC", "LIVE"), "ONDOUSDT", "BUY", "MARKET", BigDecimal.ONE, null,
                    false, null, "test", true);
                RuntimeException error = assertThrows(RuntimeException.class, () -> gateway.placeOrder(request));
                if (status == 400) {
                    assertInstanceOf(ExchangeTradingPort.OrderRejectedException.class, error);
                } else {
                    assertFalse(error instanceof ExchangeTradingPort.OrderRejectedException);
                }
                assertTrue(error.getMessage().contains("stage=PLACE_ORDER httpStatus=" + status));
                assertTrue(error.getMessage().contains("binanceCode=" + (status == 400 ? -4023 : -1007)));
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void onlyExplicit4046ConfirmsUnchangedMarginMode() throws Exception {
        for (int code : new int[]{-4046, -4047}) {
            HttpServer server = server("1", new AtomicInteger(), 400, code);
            try {
                QuantProperties props = new QuantProperties();
                props.getBinance().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
                var gateway = new BinanceGatewayAdapter(props);
                var credential = new ExchangeGateway.Credential("test", "test", "HMAC", "LIVE");
                if (code == -4046) {
                    assertDoesNotThrow(() -> gateway.setMarginType(credential, "ONDOUSDT", "ISOLATED"));
                } else {
                    var error = assertThrows(ExchangeTradingPort.ExchangeRequestException.class, () -> gateway.setMarginType(credential, "ONDOUSDT", "ISOLATED"));
                    assertTrue(error.getMessage().contains("stage=SET_MARGIN_TYPE httpStatus=400 binanceCode=-4047"));
                    assertNull(error.getCause());
                }
            } finally {
                server.stop(0);
            }
        }
    }


    @Test
    void leverageFailureIncludesStageAndExchangeCode() throws Exception {
        HttpServer server = server("1", new AtomicInteger(), 400, -4028);
        try {
            QuantProperties props = new QuantProperties();
            props.getBinance().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var gateway = new BinanceGatewayAdapter(props);
            var error = assertThrows(ExchangeTradingPort.ExchangeRequestException.class, () -> gateway.setLeverage(new ExchangeGateway.Credential("test", "test", "HMAC", "LIVE")
                , "ONDOUSDT", 5));
            assertTrue(error.getMessage().contains("stage=SET_LEVERAGE httpStatus=400 binanceCode=-4028"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void diagnosticsRemoveUrlsSecretsAndLineBreaks() {
        var error = new ExchangeTradingPort.ExchangeRequestException("PLACE_ORDER", 400, -4023, "signature=fakeSignature token=fakeToken api_key=fakeKey Authorization: Bearer " +
            "fakeBearer https://example.test/order?signature=fakeUrlSecret\nQty invalid");
        assertFalse(error.getMessage().contains("fake"));
        assertFalse(error.getMessage().contains("https://"));
        assertFalse(error.getMessage().contains("\n"));
        assertTrue(error.getMessage().contains("Qty invalid"));
        assertNull(error.getCause());
    }
}

package org.dromara.quant.exchange.binance;

import com.sun.net.httpserver.HttpServer;
import org.dromara.quant.config.QuantProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("dev")
class BinanceGatewayPriceQueryTest {

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    @Test
    void queriesOnlyLastTradePriceForProtectionFallback() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fapi/v2/ticker/price", exchange -> respond(exchange, "{\"symbol\":\"BTCUSDT\",\"price\":\"112345.6\",\"time\":1750000000000}"));
        server.start();
        try {
            QuantProperties properties = new QuantProperties();
            properties.getBinance().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var snapshot = new BinanceGatewayAdapter(properties).queryLastTradePrice("LIVE", "BTCUSDT");
            assertEquals("112345.6", snapshot.lastTradePrice().toPlainString());
            assertNull(snapshot.markPrice());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void readsTickerV2AndMarkPriceThroughOfficialSdkModels() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fapi/v2/ticker/price", exchange -> respond(exchange, "{\"symbol\":\"BTCUSDT\",\"price\":\"112345.6\",\"time\":1750000000000}"));
        server.createContext("/fapi/v1/premiumIndex", exchange -> respond(exchange, "{\"symbol\":\"BTCUSDT\",\"markPrice\":\"112340.5\",\"indexPrice\":\"112340.0\"," +
            "\"estimatedSettlePrice\":\"112340.0\",\"lastFundingRate\":\"0.0001\",\"interestRate\":\"0.0001\",\"nextFundingTime\":1750000000000,\"time\":1750000000000}"));
        server.start();
        try {
            QuantProperties properties = new QuantProperties();
            properties.getBinance().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var snapshot = new BinanceGatewayAdapter(properties).queryPrice("BTCUSDT");
            assertEquals("112345.6", snapshot.lastTradePrice().toPlainString());
            assertEquals("112340.5", snapshot.markPrice().toPlainString());
        } finally {
            server.stop(0);
        }
    }
}

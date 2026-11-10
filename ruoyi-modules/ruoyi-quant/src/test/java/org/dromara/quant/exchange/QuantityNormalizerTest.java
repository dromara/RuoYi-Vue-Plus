package org.dromara.quant.exchange;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class QuantityNormalizerTest {
    private ExchangeGateway.SymbolRule rule(String lotMax, String marketMax) {
        return new ExchangeGateway.SymbolRule("TACUSDT", "USDT", 8, 8, new BigDecimal("0.00000001"),
            new BigDecimal("1"), new BigDecimal("1"), new BigDecimal(lotMax), new BigDecimal(marketMax),
            new BigDecimal("5"), "TRADING");
    }

    @Test
    void protectionPriceRoundsTowardEarlierTriggerForEachSideAndOrderType() {
        var rule = new ExchangeGateway.SymbolRule("TACUSDT", "USDT", 2, 8, new BigDecimal("0.01"),
            BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("1000"), new BigDecimal("1000"),
            new BigDecimal("5"), "TRADING");
        BigDecimal raw = new BigDecimal("100.003");
        assertEquals(0, new BigDecimal("100.01").compareTo(QuantityNormalizer.protectionPrice(raw, rule, "LONG", "STOP_MARKET")));
        assertEquals(0, new BigDecimal("100.00").compareTo(QuantityNormalizer.protectionPrice(raw, rule, "SHORT", "STOP_MARKET")));
        assertEquals(0, new BigDecimal("100.00").compareTo(QuantityNormalizer.protectionPrice(raw, rule, "LONG", "TAKE_PROFIT_MARKET")));
        assertEquals(0, new BigDecimal("100.01").compareTo(QuantityNormalizer.protectionPrice(raw, rule, "SHORT", "TAKE_PROFIT_MARKET")));
    }

    @Test
    void rejectsUnknownProtectionOrderType() {
        var rule = rule("1000", "500");
        assertThrows(IllegalArgumentException.class,
            () -> QuantityNormalizer.protectionPrice(BigDecimal.ONE, rule, "LONG", "LIMIT"));
    }

    @Test
    void rejectsLimitQuantityAboveLotSizeMaximum() {
        var rule = rule("1000", "500");
        var error = assertThrows(IllegalArgumentException.class,
            () -> QuantityNormalizer.quantity(new BigDecimal("1001"), rule));
        assertTrue(error.getMessage().contains("TACUSDT"));
        assertTrue(error.getMessage().contains("1000"));
    }

    @Test
    void marketQuantityUsesSmallerPositiveMaximumFromBothFilters() {
        var rule = rule("1000", "500");
        var error = assertThrows(IllegalArgumentException.class,
            () -> QuantityNormalizer.quantity(new BigDecimal("501"), rule, "MARKET"));
        assertTrue(error.getMessage().contains("500"));
    }

    @Test
    void marketQuantityUsesLotMaximumWhenMarketMaximumIsNotConfigured() {
        var rule = rule("1000", "0");
        assertEquals(0, new BigDecimal("999").compareTo(
            QuantityNormalizer.quantity(new BigDecimal("999"), rule, "MARKET")));
        assertThrows(IllegalArgumentException.class,
            () -> QuantityNormalizer.quantity(new BigDecimal("1001"), rule, "MARKET"));
    }

    @Test
    void floorsQuantityBeforeApplyingMaximumCheck() {
        var rule = rule("1000.5", "1000.5");
        assertEquals(0, new BigDecimal("1000").compareTo(
            QuantityNormalizer.quantity(new BigDecimal("1000.9"), rule, "MARKET")));
    }

    @Test
    void missingMaximumDoesNotRejectValidQuantity() {
        var rule = new ExchangeGateway.SymbolRule("TACUSDT", "USDT", 8, 8, new BigDecimal("0.00000001"),
            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5"), "TRADING");
        assertEquals(0, BigDecimal.TEN.compareTo(QuantityNormalizer.quantity(BigDecimal.TEN, rule, "MARKET")));
    }
}

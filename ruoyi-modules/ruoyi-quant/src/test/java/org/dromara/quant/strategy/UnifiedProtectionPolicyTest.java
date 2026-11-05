package org.dromara.quant.strategy;

import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.QuantityNormalizer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class UnifiedProtectionPolicyTest {
    private final KlineSlopeRateStrategy strategy = new KlineSlopeRateStrategy();
    private Map<String, Object> params(String first, String step) {
        return Map.of("_quantProtectionPolicyVersion", 2, "firstProfitTakingRate", first, "stepRatio", step);
    }
    private StrategyContext context(String price, String peak, boolean firstFilled, int highest) {
        return new StrategyContext("BTCUSDT", "LONG", new BigDecimal("100"), new BigDecimal(price), null, new BigDecimal("100"),
            Instant.now(), false, Instant.now(), new BigDecimal("5"), BigDecimal.TEN, BigDecimal.TEN, 10,
            new BigDecimal(peak), new BigDecimal("100"), firstFilled, highest);
    }
    @ParameterizedTest
    @CsvSource({"0.10,0.20,101", "0.10,0.10,101", "0.30,0.10,103"})
    void keepsOnlyFirstTpUntilItsFullTargetIsConfirmed(String first, String ratio, String price) {
        var signals = strategy.evaluateProtection(context(price, price, false, 0), params(first, ratio));
        assertEquals(java.util.List.of("FIRST_TP"), signals.stream().map(ProtectionSignal::type).toList());
        assertEquals(ProtectionSignal.QuantityBase.INITIAL, signals.getFirst().quantityBase());
    }
    @Test
    void activatesHighestConfirmedStepAfterFirstProfitFill() {
        var signals = strategy.evaluateProtection(context("100.5", "102.5", true, 2), params("0.10", "0.10"));
        assertEquals(java.util.List.of("STEP_TP"), signals.stream().map(ProtectionSignal::type).toList());
        assertEquals(2, signals.getFirst().stepIndex());
        assertEquals(0, new BigDecimal("101").compareTo(signals.getFirst().triggerPrice()));
    }
    @Test
    void legacySnapshotKeepsLocalStopAndOldStepActivation() {
        var signals = strategy.evaluateProtection(context("101", "101", false, 0), Map.of("firstProfitTakingRate", "0.10", "stepRatio", "0.20"));
        assertEquals(java.util.List.of("LOCAL_STOP", "FIRST_TP"), signals.stream().map(ProtectionSignal::type).toList());
    }
    @ParameterizedTest
    @CsvSource({"LONG,-0.05", "LONG,0.10", "SHORT,-0.05", "SHORT,0.10"})
    void triggerRoundingPreservesTargetReturn(String direction, String rate) {
        var entry = new BigDecimal("100.003");
        var rule = new ExchangeGateway.SymbolRule("BTCUSDT", "USDT", 2, 3, new BigDecimal("0.01"), new BigDecimal("0.001"),
            new BigDecimal("0.001"), BigDecimal.ONE, "TRADING");
        var raw = KlineSlopeRateStrategy.priceForReturn(entry, direction, new BigDecimal(rate), 5);
        BigDecimal actual = ReflectionTestUtils.invokeMethod(QuantityNormalizer.class, "protectionPrice", raw, rule, direction);
        assertTrue(KlineSlopeRateStrategy.leveragedReturn(direction, actual, entry, 5).compareTo(new BigDecimal(rate)) >= 0);
        assertEquals(0, QuantityNormalizer.price(actual, rule).compareTo(actual));
    }
}

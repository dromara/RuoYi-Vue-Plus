package org.dromara.quant.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("dev")
class KlineSlopeRateStrategyTest {
    private final KlineSlopeRateStrategy strategy = new KlineSlopeRateStrategy();

    @Test
    void slopeDiagnosticLogIsRateLimitedPerSymbol() {
        String symbol = "TEST-" + java.util.UUID.randomUUID();
        assertTrue(KlineSlopeRateStrategy.shouldLogSlope(symbol, 1_000));
        org.junit.jupiter.api.Assertions.assertFalse(KlineSlopeRateStrategy.shouldLogSlope(symbol, 1_001));
        assertTrue(KlineSlopeRateStrategy.shouldLogSlope(symbol, 6_000));
    }

    @Test
    void opensLongWhenRealtimeSlopeExceedsThreshold() {
        StrategyContext c = context(null, null, new BigDecimal("100"), new BigDecimal("102"), false, null, null);
        assertEquals(EntrySignal.Direction.LONG, strategy.evaluateEntry(c, Map.of("entryAngleDegrees", 60)).orElseThrow().direction());
    }

    @Test
    void oneMinuteMaDirectionFilterAllowsLongOnlyWhenMa5AboveMa7() {
        StrategyContext c = context(null, null, new BigDecimal("100"), new BigDecimal("102"), false, null, null);
        var signal = strategy.evaluateEntry(c, Map.of(), new BigDecimal("101.5"), new BigDecimal("101.2")).orElseThrow();
        assertEquals(EntrySignal.Direction.LONG, signal.direction());
        assertTrue(signal.reason().contains("1分钟5MA高于7MA"));
    }

    @Test
    void oneMinuteMaDirectionFilterRejectsLongWhenMa5BelowMa7() {
        StrategyContext c = context(null, null, new BigDecimal("100"), new BigDecimal("102"), false, null, null);
        assertTrue(strategy.evaluateEntry(c, Map.of(), new BigDecimal("101.0"), new BigDecimal("101.2")).isEmpty());
    }

    @Test
    void oneMinuteMaDirectionFilterAllowsShortOnlyWhenMa5BelowMa7() {
        StrategyContext c = context(null, null, new BigDecimal("100"), new BigDecimal("98"), false, null, null);
        var signal = strategy.evaluateEntry(c, Map.of(), new BigDecimal("98.8"), new BigDecimal("99.0")).orElseThrow();
        assertEquals(EntrySignal.Direction.SHORT, signal.direction());
    }

    @Test
    void oneMinuteMaDirectionFilterWaitsForSevenClosedCandlesUnlessDisabled() {
        StrategyContext c = context(null, null, new BigDecimal("100"), new BigDecimal("102"), false, null, null);
        assertTrue(strategy.evaluateEntry(c, Map.of(), null, null).isEmpty());
        assertEquals(EntrySignal.Direction.LONG, strategy.evaluateEntry(c, Map.of("entryMaDirectionFilterEnabled", false), null, null)
            .orElseThrow().direction());
    }

    @Test
    void firstTakeProfitClosesConfiguredHalf() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("101"), false, new BigDecimal("101"), null);
        var exit = strategy.evaluateExit(c, Map.of("firstProfitTakingRate", "0.10", "firstClosePositionRate", "0.50")).getFirst();
        assertEquals(KlineSlopeRateStrategy.EXIT_FIRST_TP, exit.type());
        assertEquals(0, new BigDecimal("0.50").compareTo(exit.quantityRate()));
        assertTrue(exit.reason().contains("首次止盈阈值=10%"));
    }

    @Test
    void localStopUsesFivePercentReturnByDefault() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("99.4"), false, new BigDecimal("100"), null);
        var exit = strategy.evaluateExit(c, Map.of()).getFirst();
        assertEquals(KlineSlopeRateStrategy.EXIT_LOCAL_STOP, exit.type());
        assertTrue(exit.reason().contains("止损阈值=-5%"));
    }

    @Test
    void trailingClosesAtPreviousStepAfterFirstTakeProfit() {
        // 10x 杠杆：最高价 102.1 => 21% 回报率，处于 n=2；回撤到 101 => 10% 锁盈线，触发剩余全平。
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("101"), true, new BigDecimal("102.1"), null);
        var exit = strategy.evaluateExit(c, Map.of()).getFirst();
        assertEquals(KlineSlopeRateStrategy.EXIT_STEP_TP, exit.type());
        assertTrue(exit.reason().contains("持仓期间最大回报率=21%"));
        assertTrue(exit.reason().contains("回撤保护比例=10%"));
    }

    @Test
    void protectionContainsBinanceFirstTpAndLocalStopBeforePartialClose() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("100.5"), false, new BigDecimal("100.5"), null);
        var protections = strategy.evaluateProtection(c, Map.of());
        assertEquals(2, protections.size());
        assertTrue(protections.stream().anyMatch(p -> p.type().equals(KlineSlopeRateStrategy.PROTECTION_FIRST_TP) && p.orderType().equals("TAKE_PROFIT_MARKET")));
        assertTrue(protections.stream().anyMatch(p -> p.type().equals(KlineSlopeRateStrategy.PROTECTION_LOCAL_STOP) && p.orderType().equals("STOP_MARKET")));
    }


    @Test
    void shortProtectionPricesUseLeveragedReturnSemantics() {
        StrategyContext c = new StrategyContext("BTCUSDT", "SHORT", new BigDecimal("100"), new BigDecimal("99.5"), new BigDecimal("99.5"), new BigDecimal("100"),
            Instant.ofEpochMilli(1_000), false, Instant.ofEpochMilli(2_000), BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("10"), 10, null, new BigDecimal("99.5"), false);
        var protections = strategy.evaluateProtection(c, Map.of());
        var firstTp = protections.stream().filter(p -> p.type().equals(KlineSlopeRateStrategy.PROTECTION_FIRST_TP)).findFirst().orElseThrow();
        var localStop = protections.stream().filter(p -> p.type().equals(KlineSlopeRateStrategy.PROTECTION_LOCAL_STOP)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("99.000000000000").compareTo(firstTp.triggerPrice().setScale(12)));
        assertEquals(0, new BigDecimal("100.500000000000").compareTo(localStop.triggerPrice().setScale(12)));
    }

    @Test
    void localStopMirrorRemainsAfterFirstTpBeforeNextStep() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("100.5"), true, new BigDecimal("100.5"), null);
        var protections = strategy.evaluateProtection(c, Map.of("stepRatio", "0.20"));
        assertTrue(protections.stream().anyMatch(p -> p.type().equals(KlineSlopeRateStrategy.PROTECTION_LOCAL_STOP)));
    }

    @Test
    void stepProtectionWaitsForFirstTpFillConfirmation() {
        StrategyContext pending = context("LONG", new BigDecimal("100"), null, new BigDecimal("101"), false, new BigDecimal("101"), null);
        var pendingSignals = strategy.evaluateProtection(pending, Map.of("firstProfitTakingRate", "0.10", "stepRatio", "0.10"));
        assertTrue(pendingSignals.stream().anyMatch(p -> p.type().equals("FIRST_TP")));
        assertTrue(pendingSignals.stream().noneMatch(p -> p.type().equals("STEP_TP")));

        StrategyContext confirmed = context("LONG", new BigDecimal("100"), null, new BigDecimal("101"), true, new BigDecimal("101"), null);
        var confirmedSignals = strategy.evaluateProtection(confirmed, Map.of("firstProfitTakingRate", "0.10", "stepRatio", "0.10"));
        var step = confirmedSignals.stream().filter(p -> p.type().equals("STEP_TP")).findFirst().orElseThrow();
        assertEquals(1, step.stepIndex());
        assertEquals(0, new BigDecimal("100").compareTo(step.triggerPrice()));
    }

    @Test
    void stepExitWaitsForFirstTpFillConfirmation() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("100"), false, new BigDecimal("101"), null);
        assertTrue(strategy.evaluateExit(c, Map.of("firstProfitTakingRate", "0.10", "stepRatio", "0.10")).isEmpty());
    }

    @Test
    void retracingBelowReachedStepKeepsSameStepProtectionAfterFirstTpFill() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("100.5"), true, new BigDecimal("101"), null);
        var step =
            strategy.evaluateProtection(c, Map.of("firstProfitTakingRate", "0.10", "stepRatio", "0.10")).stream().filter(p -> p.type().equals("STEP_TP")).findFirst().orElseThrow();
        assertEquals(1, step.stepIndex());
        assertEquals(0, new BigDecimal("100").compareTo(step.triggerPrice()));
    }

    @Test
    void twentyFivePercentPeakLocksTenPercentFromZeroAnchor() {
        StrategyContext c = context("LONG", new BigDecimal("100"), null, new BigDecimal("101"), true, new BigDecimal("102.5"), null);
        var step = strategy.evaluateProtection(c, Map.of()).stream().filter(p -> p.type().equals("STEP_TP")).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("101").compareTo(step.triggerPrice()));
        assertEquals(KlineSlopeRateStrategy.EXIT_STEP_TP, strategy.evaluateExit(c, Map.of()).getFirst().type());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"LONG,5", "SHORT,5", "LONG,20", "SHORT,20"})
    void stopThresholdEqualsBoundaryAndProfitConversionUsesSameLeverage(String direction, int leverage) {
        BigDecimal entry = new BigDecimal("100");
        BigDecimal stop = KlineSlopeRateStrategy.priceForReturn(entry, direction, new BigDecimal("-0.05"), leverage);
        var c = new StrategyContext("BTCUSDT", direction, entry, stop, null, entry, Instant.now(), false, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, leverage
            , entry, entry, false);
        assertEquals(KlineSlopeRateStrategy.EXIT_LOCAL_STOP, strategy.evaluateExit(c, Map.of()).getFirst().type());
        var mirror = strategy.evaluateProtection(c, Map.of()).stream().filter(p -> p.type().equals("LOCAL_STOP")).findFirst().orElseThrow();
        assertEquals(0, stop.compareTo(mirror.triggerPrice()));
    }

    private StrategyContext context(String direction, BigDecimal entry, BigDecimal previousOpen, BigDecimal current, boolean firstTp, BigDecimal high, BigDecimal low) {
        return new StrategyContext("BTCUSDT", direction, entry, current, current, previousOpen == null ? new BigDecimal("100") : previousOpen, Instant.ofEpochMilli(1_000), false
            , Instant.ofEpochMilli(2_000), direction == null ? BigDecimal.ZERO : new BigDecimal("1"), direction == null ? BigDecimal.ZERO : new BigDecimal("1"),
            direction == null ? BigDecimal.ZERO : new BigDecimal("10"), 10, high, low, firstTp);
    }
}

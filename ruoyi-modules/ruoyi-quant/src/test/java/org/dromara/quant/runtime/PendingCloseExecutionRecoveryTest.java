package org.dromara.quant.runtime;

import org.dromara.quant.exchange.ExchangeGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingCloseExecutionRecoveryTest {
    private static final Instant FROM = Instant.parse("2026-11-09T02:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-09T03:00:00Z");

    @Test
    void provesShortPositionClosedByExactBuyFillQuantity() {
        var trades = List.of(trade(1L, "BUY", "0.4", "BOTH"), trade(2L, "BUY", "0.6", "BOTH"));

        assertTrue(StrategyRuntimeService.closedPendingCloseProven(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), trades, FROM, TO));
    }

    @Test
    void provesLongPositionClosedByExactSellFillQuantity() {
        var trades = List.of(trade(1L, "SELL", "1.0", "BOTH"));

        assertTrue(StrategyRuntimeService.closedPendingCloseProven(
            "JCTUSDT", "LONG", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), trades, FROM, TO));
    }

    @Test
    void rejectsQuantityMismatchOrWrongCloseSide() {
        assertFalse(proven(List.of(trade(1L, "BUY", "0.9", "BOTH")), "SHORT", "1.0"));
        assertFalse(proven(List.of(trade(1L, "SELL", "1.0", "BOTH")), "SHORT", "1.0"));
    }

    @Test
    void rejectsDuplicateTradeIdsAndHedgeTradesWithoutPositionSide() {
        var duplicate = List.of(trade(1L, "BUY", "0.5", "BOTH"), trade(1L, "BUY", "0.5", "BOTH"));
        assertFalse(proven(duplicate, "SHORT", "1.0"));
        var missingSide = List.of(trade(2L, "BUY", "1.0", null));
        assertFalse(StrategyRuntimeService.closedPendingCloseProven(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(true), missingSide, FROM, TO));
    }

    @Test
    void rejectsTruncatedOrOutOfWindowTradeHistory() {
        var truncated = java.util.stream.LongStream.range(0, 1000)
            .mapToObj(id -> trade(id, "BUY", "0.001", "BOTH")).toList();
        assertFalse(proven(truncated, "SHORT", "1.0"));
        var outOfWindow = List.of(new ExchangeGateway.TradeSnapshot(
            "JCTUSDT", 3L, 30L, "BUY", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO,
            BigDecimal.ZERO, FROM.minusMillis(1), null, "BOTH"));
        assertFalse(proven(outOfWindow, "SHORT", "1.0"));
    }

    private static boolean proven(List<ExchangeGateway.TradeSnapshot> trades, String direction, String quantity) {
        return StrategyRuntimeService.closedPendingCloseProven(
            "JCTUSDT", direction, new BigDecimal(quantity), new ExchangeGateway.PositionMode(false), trades, FROM, TO);
    }

    private static ExchangeGateway.TradeSnapshot trade(Long id, String side, String quantity, String positionSide) {
        return new ExchangeGateway.TradeSnapshot("JCTUSDT", id, id + 100L, side, new BigDecimal(quantity),
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, FROM.plusSeconds(10), null, positionSide);
    }
}

package org.dromara.quant.runtime;

import org.dromara.quant.exchange.ExchangeGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingCloseExecutionRecoveryTest {
    private static final Instant FROM = Instant.parse("2026-11-09T02:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-09T03:00:00Z");

    @Test
    void startsTradeHistoryBeforePositionOpenedAtToIncludeOpeningFill() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 11, 9, 2, 46, 0);
        Instant expected = openedAt.minusSeconds(60).atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant();

        assertEquals(expected, StrategyRuntimeService.resolveCloseProofStartTime(
            openedAt, 1794163595403L, LocalDateTime.of(2026, 11, 8, 18, 46, 36)));
    }

    @Test
    void usesEarlierPendingEpochWhenItPrecedesPositionOpenTimeMargin() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 11, 9, 2, 46, 0);
        long pendingEpoch = Instant.parse("2026-11-08T18:44:00Z").toEpochMilli();

        assertEquals(Instant.ofEpochMilli(pendingEpoch),
            StrategyRuntimeService.resolveCloseProofStartTime(openedAt, pendingEpoch, null));
    }

    @Test
    void fallsBackToPendingEpochWhenPositionOpenTimeIsMissing() {
        Instant expected = Instant.ofEpochMilli(1794163595403L);

        assertEquals(expected, StrategyRuntimeService.resolveCloseProofStartTime(null, 1794163595403L, null));
    }

    @Test
    void fallsBackToUtcExecutionCreatedAtWhenBothEpochTimesAreMissing() {
        LocalDateTime executionCreatedAt = LocalDateTime.of(2026, 11, 8, 18, 46, 36);

        assertEquals(executionCreatedAt.toInstant(ZoneOffset.UTC),
            StrategyRuntimeService.resolveCloseProofStartTime(null, null, executionCreatedAt));
    }

    @Test
    void provesShortPositionClosedByExactBuyFillQuantity() {
        var trades = List.of(tradeAt(1L, "SELL", "1.0", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "BUY", "0.4", "BOTH", FROM.plusSeconds(2)),
            tradeAt(3L, "BUY", "0.6", "BOTH", FROM.plusSeconds(3)));

        assertTrue(StrategyRuntimeService.closedPendingCloseProven(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), trades, FROM, TO));
    }

    @Test
    void provesLongPositionClosedByExactSellFillQuantity() {
        var trades = List.of(tradeAt(1L, "BUY", "1.0", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "SELL", "1.0", "BOTH", FROM.plusSeconds(2)));

        assertTrue(StrategyRuntimeService.closedPendingCloseProven(
            "JCTUSDT", "LONG", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), trades, FROM, TO));
    }

    @Test
    void rejectsQuantityMismatchOrWrongCloseSide() {
        assertFalse(proven(List.of(tradeAt(1L, "SELL", "1.0", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "BUY", "0.9", "BOTH", FROM.plusSeconds(2))), "SHORT", "1.0"));
        assertFalse(proven(List.of(tradeAt(1L, "SELL", "1.0", "BOTH", FROM.plusSeconds(1))), "SHORT", "1.0"));
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
    void netsMixedDirectionTradesInChronologicalOrder() {
        var mixed = List.of(
            tradeAt(1L, "SELL", "1.0", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "BUY", "0.5", "BOTH", FROM.plusSeconds(2)),
            tradeAt(3L, "SELL", "0.2", "BOTH", FROM.plusSeconds(3)),
            tradeAt(4L, "BUY", "0.7", "BOTH", FROM.plusSeconds(4)));

        var proof = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), mixed, FROM, TO);

        assertTrue(proof.proven());
        assertEquals(0, proof.remainingQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, proof.closeQuantity().compareTo(new BigDecimal("1.2")));
        assertEquals(0, proof.increaseQuantity().compareTo(new BigDecimal("1.2")));
    }

    @Test
    void rejectsMixedDirectionHistoryThatLeavesNetExposure() {
        var mixed = List.of(
            tradeAt(1L, "SELL", "1.0", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "BUY", "0.5", "BOTH", FROM.plusSeconds(2)),
            tradeAt(3L, "SELL", "0.2", "BOTH", FROM.plusSeconds(3)));

        var proof = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), mixed, FROM, TO);

        assertFalse(proof.proven());
        assertEquals("NET_POSITION_NOT_ZERO", proof.reason());
        assertEquals(0, proof.remainingQuantity().compareTo(new BigDecimal("0.7")));
    }

    @Test
    void rejectsTradeSequenceThatCrossesThroughZeroPosition() {
        var mixed = List.of(
            tradeAt(1L, "SELL", "1.0", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "BUY", "1.1", "BOTH", FROM.plusSeconds(2)),
            tradeAt(3L, "SELL", "0.1", "BOTH", FROM.plusSeconds(3)));

        var proof = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(false), mixed, FROM, TO);

        assertFalse(proof.proven());
        assertEquals("CLOSE_QUANTITY_EXCEEDS_POSITION", proof.reason());
    }

    @Test
    void ignoresTheOtherPositionSideInHedgeMode() {
        var mixed = List.of(
            tradeAt(1L, "SELL", "1.0", "SHORT", FROM.plusSeconds(1)),
            tradeAt(2L, "SELL", "4.0", "LONG", FROM.plusSeconds(2)),
            tradeAt(3L, "BUY", "1.0", "SHORT", FROM.plusSeconds(3)));

        var proof = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", new BigDecimal("1.0"), new ExchangeGateway.PositionMode(true), mixed, FROM, TO);

        assertTrue(proof.proven());
    }

    @Test
    void distinguishesEmptyAndNullTradeHistory() {
        var empty = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", BigDecimal.ONE, new ExchangeGateway.PositionMode(false), List.of(), FROM, TO);
        var missing = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", BigDecimal.ONE, new ExchangeGateway.PositionMode(false), null, FROM, TO);

        assertEquals("TRADE_HISTORY_EMPTY", empty.reason());
        assertEquals("TRADE_HISTORY_NULL", missing.reason());
    }

    @Test
    void rejectsHistoryThatNeverReachesTheRecordedInitialPositionQuantity() {
        var trades = List.of(
            tradeAt(1L, "SELL", "0.5", "BOTH", FROM.plusSeconds(1)),
            tradeAt(2L, "BUY", "0.5", "BOTH", FROM.plusSeconds(2)));

        var proof = StrategyRuntimeService.evaluatePendingCloseProof(
            "JCTUSDT", "SHORT", BigDecimal.ONE, new ExchangeGateway.PositionMode(false), trades, FROM, TO);

        assertFalse(proof.proven());
        assertEquals("OPEN_QUANTITY_NOT_REACHED", proof.reason());
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

    private static ExchangeGateway.TradeSnapshot tradeAt(Long id, String side, String quantity, String positionSide, Instant time) {
        return new ExchangeGateway.TradeSnapshot("JCTUSDT", id, id + 100L, side, new BigDecimal(quantity),
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, time, null, positionSide);
    }
}

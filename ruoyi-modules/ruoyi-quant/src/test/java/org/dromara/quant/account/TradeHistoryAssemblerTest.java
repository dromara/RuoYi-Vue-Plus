package org.dromara.quant.account;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class TradeHistoryAssemblerTest {
    private TradeHistoryAssembler.Fill fill(long id, long account, String symbol, String side, String position, String operation, String qty, String price, String fee,
                                            String pnl) {
        return new TradeHistoryAssembler.Fill(id, account, symbol, side, position, operation, null, new BigDecimal(qty), new BigDecimal(price), fee == null ? null :
            new BigDecimal(fee), pnl == null ? null : new BigDecimal(pnl), LocalDateTime.of(2026, 10, 9, 1, 0).plusSeconds(id));
    }

    @Test
    void aggregatesPartialFillsAndPartialClosesUsingWeightedPrices() {
        var result = TradeHistoryAssembler.assemble(List.of(fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100", "0.1", "0"), fill(2, 10, "BTCUSDT", "BUY", "LONG", "OPEN",
            "3", "120", "0.3", "0"), fill(3, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "1", "130", "0.1", "15"), fill(4, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "3", "140", "0.3",
            "75")), Map.of(10L, "模拟1"));
        assertEquals(1, result.rows().size());
        var row = result.rows().getFirst();
        assertEquals("115", row.get("entryPrice"));
        assertEquals("137.5", row.get("closePrice"));
        assertEquals("90", row.get("realizedPnl"));
        assertEquals("0.8", row.get("commission"));
        assertEquals("模拟1", row.get("accountName"));
        assertEquals("CLOSED", row.get("status"));
        assertNotNull(row.get("closedAt"));
        assertNull(row.get("openingMargin"));
        assertNull(row.get("leverage"));
    }

    @Test
    void reportsOpeningNotionalAndUsesOpeningTickSizeForBothPrices() {
        var opening = fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "2", "100.125", "0", "0");
        var closing = fill(2, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "2", "110.37549", "0", "20");
        var result = TradeHistoryAssembler.assemble(List.of(
            new TradeHistoryAssembler.Fill(opening.id(), opening.credentialId(), opening.symbol(), opening.side(), opening.positionSide(), opening.operationType(), false,
                opening.quantity(), opening.price(), opening.commission(), opening.realizedPnl(), opening.tradeTime(), 5, new BigDecimal("0.001"), new BigDecimal("1000"),
                LocalDateTime.of(2026, 10, 9, 1, 0).plusSeconds(1), null),
            new TradeHistoryAssembler.Fill(closing.id(), closing.credentialId(), closing.symbol(), closing.side(), closing.positionSide(), closing.operationType(), true,
                closing.quantity(), closing.price(), closing.commission(), closing.realizedPnl(), closing.tradeTime(), null, null, new BigDecimal("125"),
                LocalDateTime.of(2026, 10, 9, 1, 0).plusSeconds(2), null)
        ), Map.of());

        var row = result.rows().getFirst();
        assertEquals("100.125", row.get("entryPrice"));
        assertEquals("110.375", row.get("closePrice"));
        assertEquals("200.25", row.get("openingNotional"));
        assertEquals(5, row.get("leverage"));
        assertEquals("1000.0000", row.get("openingBalance"));
        assertEquals("125.0000", row.get("closingBalance"));
    }

    @Test
    void roundsBalancesToFourDecimalsAndCommissionToContractPriceScale() {
        var openedAt = LocalDateTime.of(2026, 10, 9, 1, 0);
        var result = TradeHistoryAssembler.assemble(List.of(
            new TradeHistoryAssembler.Fill(1L, 10L, "BTCUSDT", "BUY", "LONG", "OPEN", false, new BigDecimal("1"), new BigDecimal("100.123"),
                BigDecimal.ZERO, BigDecimal.ZERO, openedAt, 5, new BigDecimal("0.001"), new BigDecimal("4811.63172"), openedAt, null),
            new TradeHistoryAssembler.Fill(2L, 10L, "BTCUSDT", "SELL", "LONG", "CLOSE", true, new BigDecimal("1"), new BigDecimal("101.123"),
                new BigDecimal("0.12345"), BigDecimal.ONE, openedAt.plusMinutes(1), null, null, new BigDecimal("5561.00831248"), openedAt.plusMinutes(1), null)
        ), Map.of());

        var row = result.rows().getFirst();
        assertEquals("4811.6317", row.get("openingBalance"));
        assertEquals("5561.0083", row.get("closingBalance"));
        assertEquals("0.123", row.get("commission"));
    }

    @Test
    void legacyPricesUseCurrentSymbolTickFallbackWithoutInventingLeverage() {
        var opening = fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100.123456", "0", "0");
        var closing = fill(2, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "1", "101.23455", "0", "1");
        var result = TradeHistoryAssembler.assemble(List.of(
            new TradeHistoryAssembler.Fill(opening.id(), opening.credentialId(), opening.symbol(), opening.side(), opening.positionSide(), opening.operationType(), false,
                opening.quantity(), opening.price(), opening.commission(), opening.realizedPnl(), opening.tradeTime(), null, null, null, null, new BigDecimal("0.0001")),
            new TradeHistoryAssembler.Fill(closing.id(), closing.credentialId(), closing.symbol(), closing.side(), closing.positionSide(), closing.operationType(), true,
                closing.quantity(), closing.price(), closing.commission(), closing.realizedPnl(), closing.tradeTime(), null, null, null, null, new BigDecimal("0.0001"))
        ), Map.of());

        var row = result.rows().getFirst();
        assertEquals("100.1235", row.get("entryPrice"));
        assertEquals("101.2346", row.get("closePrice"));
        assertNull(row.get("leverage"));
    }

    @Test
    void separatesAccountsSymbolsDirectionsAndRepeatedCycles() {
        var result = TradeHistoryAssembler.assemble(List.of(fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100", "0", "0"), fill(2, 10, "BTCUSDT", "SELL", "LONG", "CLOSE",
            "1", "110", "0", "10"), fill(3, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "120", "0", "0"), fill(4, 11, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100", "0", "0"),
            fill(5, 10, "PUMPUSDT", "BUY", "LONG", "OPEN", "1", "1", "0", "0"), fill(6, 10, "BTCUSDT", "SELL", "SHORT", "OPEN", "1", "100", "0", "0")), Map.of());
        assertEquals(5, result.rows().size());
        assertEquals(4, result.rows().stream().filter(r -> "OPEN".equals(r.get("status"))).count());
        result.rows().stream().filter(r -> "OPEN".equals(r.get("status"))).forEach(r -> {
            assertNull(r.get("closePrice"));
            assertNull(r.get("closedAt"));
        });
    }

    @Test
    void excludesLegacyFillsWithoutConfirmedDirectionAndOperation() {
        var result = TradeHistoryAssembler.assemble(List.of(
            fill(1, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "1", "100", "0", "0"),
            fill(2, 10, "PUMPUSDT", "BUY", null, null, "1", "1", "0", "0")
        ), Map.of());
        assertTrue(result.rows().isEmpty());
        assertEquals(2, result.incompleteCount());
    }

    @Test
    void preservesCompletedCyclesBeforeAnUnreconstructableHistoryBoundary() {
        var result = TradeHistoryAssembler.assemble(List.of(
            fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100", "0", "0"),
            fill(2, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "1", "110", "0", "10"),
            fill(3, 10, "BTCUSDT", "BUY", null, null, "1", "120", "0", "0"),
            fill(4, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "130", "0", "0")
        ), Map.of());

        assertEquals(1, result.rows().size());
        assertEquals("CLOSED", result.rows().getFirst().get("status"));
        assertEquals("110", result.rows().getFirst().get("closePrice"));
        assertEquals(1, result.incompleteCount());
    }

    @Test
    void excludesOverCloseAndDoesNotTreatMissingFeesAsZero() {
        var result = TradeHistoryAssembler.assemble(List.of(fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100", "0", "0"), fill(2, 10, "BTCUSDT", "SELL", "LONG", "CLOSE",
            "2", "110", "0", "0"), fill(3, 10, "PUMPUSDT", "BUY", "LONG", "OPEN", "1", "1", null, "0")), Map.of());
        assertEquals(1, result.rows().size());
        assertNull(result.rows().getFirst().get("commission"));
        assertEquals(1, result.incompleteCount());
    }

    @Test
    void identifiesOneWayShortFromConfirmedOperation() {
        var result = TradeHistoryAssembler.assemble(List.of(fill(1, 10, "BTCUSDT", "SELL", "BOTH", "OPEN", "2", "100", "0", "0"), fill(2, 10, "BTCUSDT", "BUY", "BOTH", "CLOSE",
            "2", "90", "0", "20")), Map.of());
        assertEquals("SHORT", result.rows().getFirst().get("direction"));
        assertEquals("20", result.rows().getFirst().get("realizedPnl"));
    }

    @Test
    void excludesFlatOneWayFillsWithoutExchangeOrderFacts() {
        var result = TradeHistoryAssembler.assemble(List.of(
            fill(1, 10, "ZROUSDT", "SELL", null, null, "3", "100", "0", "0"),
            fill(2, 10, "ZROUSDT", "BUY", null, null, "1", "90", "0", "1"),
            fill(3, 10, "ZROUSDT", "BUY", null, null, "2", "80", "0", "2"),
            fill(4, 10, "ZROUSDT", "BUY", null, null, "2", "70", "0", "0")
        ), Map.of());
        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.incompleteCount());
    }

    @Test
    void keepsCyclesBuiltFromConfirmedExchangeFacts() {
        var result = TradeHistoryAssembler.assemble(List.of(
            fill(1, 10, "BTCUSDT", "BUY", "LONG", "OPEN", "1", "100", "0", "0"),
            fill(2, 10, "BTCUSDT", "SELL", "LONG", "CLOSE", "1", "110", "0", "10")
        ), Map.of());
        assertFalse(result.rows().getFirst().containsKey("reconstructionSource"));
    }

    @Test
    void doesNotInferAcrossAZeroCrossingFill() {
        var result = TradeHistoryAssembler.assemble(List.of(
            fill(1, 10, "BTCUSDT", "SELL", null, null, "1", "100", "0", "0"),
            fill(2, 10, "BTCUSDT", "BUY", null, null, "2", "90", "0", "0")
        ), Map.of());

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.incompleteCount());
    }

    @Test
    void groupsExchangeOrderAndTradeIdsWithTheirOpeningAndClosingFills() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 9, 1, 0);
        var result = TradeHistoryAssembler.assemble(List.of(
            new TradeHistoryAssembler.Fill(1L, 10L, "BTCUSDT", "BUY", "LONG", "OPEN", false, BigDecimal.ONE, new BigDecimal("100"),
                BigDecimal.ZERO, BigDecimal.ZERO, time, null, null, null, null, null, 101L, 201L),
            new TradeHistoryAssembler.Fill(2L, 10L, "BTCUSDT", "SELL", "LONG", "CLOSE", true, BigDecimal.ONE, new BigDecimal("110"),
                BigDecimal.ZERO, BigDecimal.TEN, time.plusMinutes(1), null, null, null, null, null, 102L, 202L)
        ), Map.of());
        var row = result.rows().getFirst();
        assertEquals(List.of("201"), row.get("openingOrderIds"));
        assertEquals(List.of("202"), row.get("closingOrderIds"));
        assertEquals(List.of("101"), row.get("openingTradeIds"));
        assertEquals(List.of("102"), row.get("closingTradeIds"));
        assertEquals(1, row.get("openingFillCount"));
        assertEquals(1, row.get("closingFillCount"));
    }

    @Test
    void doesNotInferOneWayOpeningFromReduceOnlyFalse() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 9, 1, 0);
        var result = TradeHistoryAssembler.assemble(List.of(
            new TradeHistoryAssembler.Fill(1L, 10L, "BTCUSDT", "SELL", "BOTH", null, false, BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                time),
            new TradeHistoryAssembler.Fill(2L, 10L, "BTCUSDT", "BUY", "BOTH", null, true, BigDecimal.ONE, new BigDecimal("90"), BigDecimal.ZERO, BigDecimal.TEN,
                time.plusMinutes(1))
        ), Map.of());

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.incompleteCount());
    }
}

package org.dromara.quant.account;

import org.dromara.quant.account.domain.vo.TradeCycleRecord;
import org.dromara.quant.account.domain.vo.TradeCycleSourceFill;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class TradeHistoryAssemblerTest {
    private static TradeCycleSourceFill fill(long tradeId, String side, String positionSide,
                                              String exchangePositionSide, String role, String cycleKey,
                                              String quantity, String price, String fee, String pnl,
                                              boolean reduceOnly, boolean conflict) {
        TradeCycleSourceFill fill = new TradeCycleSourceFill();
        fill.setId(tradeId + 1000);
        fill.setCredentialId(10L);
        fill.setSymbol("BTCUSDT");
        fill.setExchangeTradeId(tradeId);
        fill.setExchangeOrderId(tradeId + 100);
        fill.setSide(side);
        fill.setPositionSide(positionSide);
        fill.setExchangePositionSide(exchangePositionSide);
        fill.setFillRole(role);
        fill.setPositionCycleKey(cycleKey);
        fill.setReduceOnly(reduceOnly);
        fill.setQuantity(new BigDecimal(quantity));
        fill.setPrice(new BigDecimal(price));
        fill.setCommission(fee == null ? null : new BigDecimal(fee));
        fill.setCommissionAsset(fee == null ? null : "USDT");
        fill.setRealizedPnl(pnl == null ? null : new BigDecimal(pnl));
        fill.setTradeTime(LocalDateTime.of(2026, 11, 1, 0, 0).plusSeconds(tradeId));
        fill.setSourceConflict(conflict);
        return fill;
    }

    @Test
    void combinesMultipleOpenAndPartialCloseFillsIntoOneWeightedCycle() {
        var result = TradeCycleAssembler.assemble(List.of(
            fill(1, "BUY", "LONG", "LONG", "OPEN", "cycle-1", "1", "100", "0.1", "0", false, false),
            fill(2, "BUY", "LONG", "LONG", "OPEN", "cycle-1", "3", "120", "0.3", "0", false, false),
            fill(3, "SELL", "LONG", "LONG", "CLOSE", "cycle-1", "1", "130", "0.1", "15", true, false),
            fill(4, "SELL", "LONG", "LONG", "CLOSE", "cycle-1", "3", "140", "0.3", "75", true, false)
        ));

        assertEquals(1, result.cycles().size());
        TradeCycleRecord cycle = result.cycles().getFirst();
        assertEquals("LONG", cycle.getPositionSide());
        assertEquals("CLOSED", cycle.getStatus());
        assertEquals(0, cycle.getOpeningQuantity().compareTo(new BigDecimal("4")));
        assertEquals(0, cycle.getOpeningNotional().compareTo(new BigDecimal("460")));
        assertEquals(0, cycle.getEntryPrice().compareTo(new BigDecimal("115")));
        assertEquals(0, cycle.getClosingQuantity().compareTo(new BigDecimal("4")));
        assertEquals(0, cycle.getClosePrice().compareTo(new BigDecimal("137.5")));
        assertEquals(0, cycle.getRealizedPnl().compareTo(new BigDecimal("90")));
        assertEquals("0.8 USDT", cycle.getCommissionDisplay());
        assertEquals(4, cycle.getFillCount());
        assertEquals(4, result.allocations().size());
        assertEquals("VERIFIED", cycle.getReconciliationStatus());
    }

    @Test
    void aggregatesDifferentStrategyCycleKeysIntoOneAccountPositionCycle() {
        var result = TradeCycleAssembler.assemble(List.of(
            fill(1, "BUY", "LONG", "LONG", "OPEN", "strategy-cycle-a", "1", "100", "0.1", "0", false, false),
            fill(2, "BUY", "LONG", "LONG", "OPEN", "strategy-cycle-b", "2", "110", "0.2", "0", false, false),
            fill(3, "SELL", "LONG", "LONG", "CLOSE", "strategy-cycle-b", "3", "120", "0.3", "30", true, false)
        ));

        assertEquals(1, result.cycles().size());
        TradeCycleRecord cycle = result.cycles().getFirst();
        assertEquals("CLOSED", cycle.getStatus());
        assertEquals(0, cycle.getOpeningQuantity().compareTo(new BigDecimal("3")));
        assertEquals(0, cycle.getEntryPrice().compareTo(new BigDecimal("106.666666666666666667")));
        assertEquals(0, cycle.getClosingQuantity().compareTo(new BigDecimal("3")));
        assertEquals("0.6 USDT", cycle.getCommissionDisplay());
    }

    @Test
    void allocatesOneWayReversalFillBetweenClosingOldCycleAndOpeningNewCycle() {
        var result = TradeCycleAssembler.assemble(List.of(
            fill(1, "SELL", null, "BOTH", "OPEN", null, "2", "100", "0.2", "0", false, false),
            fill(2, "BUY", null, "BOTH", null, null, "3", "90", "0.3", "20", false, false)
        ));

        assertEquals(2, result.cycles().size());
        TradeCycleRecord shortCycle = result.cycles().stream()
            .filter(cycle -> "SHORT".equals(cycle.getPositionSide())).findFirst().orElseThrow();
        TradeCycleRecord longCycle = result.cycles().stream()
            .filter(cycle -> "LONG".equals(cycle.getPositionSide())).findFirst().orElseThrow();

        assertEquals("CLOSED", shortCycle.getStatus());
        assertEquals(0, shortCycle.getOpeningQuantity().compareTo(new BigDecimal("2")));
        assertEquals(0, shortCycle.getClosingQuantity().compareTo(new BigDecimal("2")));
        assertEquals(0, shortCycle.getRealizedPnl().compareTo(new BigDecimal("20")));
        assertEquals("0.4 USDT", shortCycle.getCommissionDisplay());

        assertEquals("OPEN", longCycle.getStatus());
        assertEquals(0, longCycle.getOpeningQuantity().compareTo(BigDecimal.ONE));
        assertEquals(0, longCycle.getRemainingQuantity().compareTo(BigDecimal.ONE));
        assertNull(longCycle.getClosedAt());
        assertEquals("0.1 USDT", longCycle.getCommissionDisplay());

        var reversalAllocations = result.allocations().stream()
            .filter(allocation -> Long.valueOf(2).equals(allocation.getExchangeTradeId())).toList();
        assertEquals(2, reversalAllocations.size());
        assertEquals(0, reversalAllocations.stream().map(a -> a.getAllocatedQuantity())
            .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(new BigDecimal("3")));
        assertEquals(0, reversalAllocations.stream().map(a -> a.getCommission())
            .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(new BigDecimal("0.3")));
    }

    @Test
    void infersInitialOneWayPositionWhenHistoricalBaselineIsFlat() {
        TradeCycleSourceFill opening = fill(1, "SELL", null, "BOTH", null, null,
            "2", "100", "0.2", "0", false, false);
        opening.setReduceOnly(null);

        var result = TradeCycleAssembler.assemble(List.of(
            opening,
            fill(2, "BUY", null, "BOTH", "CLOSE", null, "2", "90", "0.2", "20", true, false)
        ));

        assertEquals(0, result.incompleteCount());
        assertEquals(1, result.cycles().size());
        TradeCycleRecord cycle = result.cycles().getFirst();
        assertEquals("SHORT", cycle.getPositionSide());
        assertEquals("CLOSED", cycle.getStatus());
        assertEquals("INFERRED", cycle.getReconstructionSource());
        assertEquals("INFERRED", cycle.getReconciliationStatus());
        assertEquals(0, cycle.getOpeningQuantity().compareTo(new BigDecimal("2")));
        assertEquals(0, cycle.getEntryPrice().compareTo(new BigDecimal("100")));
        assertEquals(0, cycle.getClosingQuantity().compareTo(new BigDecimal("2")));
        assertEquals(0, cycle.getClosePrice().compareTo(new BigDecimal("90")));
        assertEquals(0, cycle.getRealizedPnl().compareTo(new BigDecimal("20")));
        assertEquals(2, cycle.getFillCount());
        assertEquals(2, result.allocations().size());
    }

    @Test
    void retainsPartialPositionWithWeightedEntryAndNoFinalCloseTime() {
        var result = TradeCycleAssembler.assemble(List.of(
            fill(1, "BUY", "LONG", "LONG", "OPEN", "cycle-2", "2", "100", "0.2", "0", false, false),
            fill(2, "SELL", "LONG", "LONG", "CLOSE", "cycle-2", "0.5", "110", "0.05", "5", true, false)
        ));

        TradeCycleRecord cycle = result.cycles().getFirst();
        assertEquals("PARTIAL", cycle.getStatus());
        assertEquals(0, cycle.getEntryPrice().compareTo(new BigDecimal("100")));
        assertEquals(0, cycle.getClosePrice().compareTo(new BigDecimal("110")));
        assertEquals(0, cycle.getRemainingQuantity().compareTo(new BigDecimal("1.5")));
        assertNull(cycle.getClosedAt());
    }

    @Test
    void acceptsMultiplePartialFillsWhenTheyMatchTheOrderCumulativeQuantity() {
        TradeCycleSourceFill first = fill(1, "BUY", "LONG", "LONG", "OPEN", "cycle-5",
            "1", "100", "0.1", "0", false, false);
        TradeCycleSourceFill second = fill(2, "BUY", "LONG", "LONG", "OPEN", "cycle-5",
            "1", "102", "0.1", "0", false, false);
        first.setExchangeOrderId(901L);
        second.setExchangeOrderId(901L);
        first.setOrderExecutedQuantity(new BigDecimal("2"));
        second.setOrderExecutedQuantity(new BigDecimal("2"));

        var result = TradeCycleAssembler.assemble(List.of(first, second));

        assertEquals(0, result.incompleteCount());
        assertEquals(1, result.cycles().size());
        assertEquals(0, result.cycles().getFirst().getOpeningQuantity().compareTo(new BigDecimal("2")));
        assertEquals(0, result.cycles().getFirst().getEntryPrice().compareTo(new BigDecimal("101")));
    }

    @Test
    void quarantinesOrderWhenCumulativeExecutedQuantityExceedsPersistedFills() {
        TradeCycleSourceFill first = fill(1, "BUY", "LONG", "LONG", "OPEN", "cycle-4",
            "1", "100", "0.1", "0", false, false);
        TradeCycleSourceFill second = fill(2, "BUY", "LONG", "LONG", "OPEN", "cycle-4",
            "1", "101", "0.1", "0", false, false);
        first.setExchangeOrderId(900L);
        second.setExchangeOrderId(900L);
        first.setOrderExecutedQuantity(new BigDecimal("3"));
        second.setOrderExecutedQuantity(new BigDecimal("3"));

        var result = TradeCycleAssembler.assemble(List.of(first, second));

        assertEquals(1, result.incompleteCount());
        assertEquals(1, result.cycles().size());
        assertEquals("RECONCILE_REQUIRED", result.cycles().getFirst().getStatus());
        assertEquals(1, result.allocations().size());
    }

    @Test
    void quarantinesConflictingSourceAndDoesNotInferSubsequentFills() {
        var result = TradeCycleAssembler.assemble(List.of(
            fill(1, "BUY", "LONG", "LONG", "OPEN", "cycle-3", "1", "100", "0.1", "0", false, true),
            fill(2, "SELL", "LONG", "LONG", "CLOSE", "cycle-3", "1", "110", "0.1", "10", true, false)
        ));

        assertEquals(1, result.incompleteCount());
        assertEquals(1, result.cycles().size());
        assertEquals("RECONCILE_REQUIRED", result.cycles().getFirst().getReconciliationStatus());
        assertEquals(1, result.allocations().size());
    }
}

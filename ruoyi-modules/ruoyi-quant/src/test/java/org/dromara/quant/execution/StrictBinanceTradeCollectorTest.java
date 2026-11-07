package org.dromara.quant.execution;

import org.dromara.quant.exchange.ExchangeGateway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class StrictBinanceTradeCollectorTest {
    private final ExchangeGateway gateway = mock(ExchangeGateway.class);
    private final StrictBinanceTradeCollector collector = new StrictBinanceTradeCollector(gateway);
    private final ExchangeGateway.Credential credential = new ExchangeGateway.Credential("test", "secret", "HMAC", "LIVE");
    private final Instant start = Instant.parse("2026-10-01T00:00:00Z");
    private final Instant end = Instant.parse("2026-10-02T00:00:00Z");

    private ExchangeGateway.TradeSnapshot trade(long id) {
        return new ExchangeGateway.TradeSnapshot("BTCUSDT", id, 11L, "BUY", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, start.plusSeconds(id), "USDT");
    }

    @Test
    void splitsSaturatedPerOrderFillWindowAndCollectsEveryFill() {
        var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 11L, "client-11", "BUY", "MARKET", "FILLED",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, end, BigDecimal.TEN, "BOTH", false, start);
        var fullPage = java.util.stream.LongStream.rangeClosed(1, 1000).mapToObj(this::trade).toList();
        when(gateway.queryTradesByOrder(eq(credential), eq("BTCUSDT"), eq(11L), any(), eq(1000L))).thenAnswer(call -> {
            var range = call.<ExchangeGateway.TimeRange>getArgument(3);
            return fullPage.stream().filter(value -> !value.time().isBefore(range.from()) && !value.time().isAfter(range.to())).toList();
        });

        var result = collector.collectByOrders(credential, "BTCUSDT", List.of(order), start, end);

        assertEquals(1000, result.size());
        assertEquals(1L, result.getFirst().tradeId());
        assertEquals(1000L, result.getLast().tradeId());
    }

    @Test
    void skipsOrdersThatCannotHaveFills() {
        var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 12L, "client-12", "BUY", "LIMIT", "CANCELED",
            BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, end);
        assertEquals(List.of(), collector.collectByOrders(credential, "BTCUSDT", List.of(order), start, end));
        org.mockito.Mockito.verifyNoInteractions(gateway);
    }

    @Test
    void rejectsFillsReturnedForAnotherOrder() {
        var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 11L, "client-11", "BUY", "MARKET", "FILLED",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, end, BigDecimal.TEN);
        when(gateway.queryTradesByOrder(any(), eq("BTCUSDT"), eq(11L), any(), anyLong())).thenReturn(List.of(
            new ExchangeGateway.TradeSnapshot("BTCUSDT", 1L, 12L, "BUY", BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, start, "USDT")));
        assertThrows(IllegalStateException.class, () -> collector.collectByOrders(credential, "BTCUSDT", List.of(order)));
    }

    @Test
    void splitsSaturatedPagesAndDeduplicatesInclusiveBoundaryFacts() {
        when(gateway.queryTrades(any(), eq("BTCUSDT"), argThat(range -> range.from().equals(start) && range.to().equals(end))))
            .thenReturn(java.util.Collections.nCopies(1000, trade(1L)));
        when(gateway.queryTrades(any(), eq("BTCUSDT"), argThat(range -> range.from().isAfter(start) || range.to().isBefore(end))))
            .thenAnswer(call -> {
                var range = call.<ExchangeGateway.TimeRange>getArgument(2);
                return List.of(trade(1L), trade(3L)).stream().filter(value -> !value.time().isBefore(range.from()) && !value.time().isAfter(range.to())).toList();
            });
        var result = collector.collect(credential, "BTCUSDT", start, end);
        assertEquals(List.of(1L, 3L), result.stream().map(ExchangeGateway.TradeSnapshot::tradeId).toList());
    }

    @Test
    void retainsIncompleteFinancialFactsForRawJournalInsteadOfConvertingThemToZero() {
        var incomplete = new ExchangeGateway.TradeSnapshot("BTCUSDT", 5L, 11L, "BUY", BigDecimal.ONE, BigDecimal.TEN,
            null, BigDecimal.ZERO, start.plusSeconds(5), null);
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenReturn(List.of(incomplete));
        var result = collector.collect(credential, "BTCUSDT", start, end);
        assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertNull(result.getFirst().commission());
        org.junit.jupiter.api.Assertions.assertNull(result.getFirst().commissionAsset());
    }

    @Test
    void rejectsAOneMillisecondWindowThatRemainsSaturated() {
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenReturn(java.util.Collections.nCopies(1000, trade(1L)));
        assertThrows(IllegalStateException.class, () -> collector.collect(credential, "BTCUSDT", start, end));
    }

    @Test
    void rejectsConflictingReplayAndEmitsChronologicalFacts() {
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenReturn(List.of(trade(2L), trade(1L)));
        var sorted = collector.collect(credential, "BTCUSDT", start, end);
        assertEquals(List.of(1L, 2L), sorted.stream().map(ExchangeGateway.TradeSnapshot::tradeId).toList());
        assertThrows(IllegalArgumentException.class, () -> collector.collect(credential, "BTCUSDT", end, start));
    }
}

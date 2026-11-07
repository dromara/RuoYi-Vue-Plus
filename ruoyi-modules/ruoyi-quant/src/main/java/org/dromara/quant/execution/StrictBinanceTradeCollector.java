package org.dromara.quant.execution;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeHistoryCollector;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Collect a bounded authenticated Binance trade-history interval before
 * opening a position-book transaction.
 * <p>
 * Binance accountTradeList is capped at 1000 rows and limited to a 7-day
 * request interval. Full pages are split by time; a saturated millisecond
 * interval remains incomplete and is rejected rather than silently truncated.
 */
@Service
@RequiredArgsConstructor
public class StrictBinanceTradeCollector {
    private static final Duration WINDOW = Duration.ofDays(6);
    private static final int LIMIT = 1000;
    private static final int MAX_QUERIES = 10_000;
    @NonNull private final ExchangeGateway gateway;



    /**
     * 分页收集交易所成交并验证游标连续性和请求边界。
     */
    public List<ExchangeGateway.TradeSnapshot> collect(ExchangeGateway.Credential credential, String symbol, Instant from, Instant to) {
        if (credential == null || symbol == null || symbol.isBlank() || from == null || to == null || !from.isBefore(to) || Duration.between(from, to).compareTo(Duration.ofDays(180)) > 0) {
            throw new IllegalArgumentException("Unbounded or unverified historical fill interval");
        }
        Map<Long, ExchangeGateway.TradeSnapshot> unique = new HashMap<>();
        long left = from.toEpochMilli();
        long end = to.toEpochMilli();
        int[] queryCount = {0};
        while (left <= end) {
            long right = Math.min(end, left + WINDOW.toMillis());
            collectWindow(credential, symbol, left, right, unique, queryCount);
            if (right == Long.MAX_VALUE) {
                break;
            }
            left = right + 1;
        }
        List<ExchangeGateway.TradeSnapshot> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparing(ExchangeGateway.TradeSnapshot::time).thenComparing(ExchangeGateway.TradeSnapshot::tradeId));
        return List.copyOf(sorted);
    }

    /**
     * 先取得历史订单，再逐个按 symbol + orderId 收集其成交。
     * 单个订单成交达到单页上限时，使用 fromId 继续分页，避免静默截断。
     */
    public List<ExchangeGateway.TradeSnapshot> collectByOrders(ExchangeGateway.Credential credential, String symbol,
                                                                 List<ExchangeGateway.OrderSnapshot> orders, Instant historyStart, Instant historyEnd) {
        if (credential == null || symbol == null || symbol.isBlank() || orders == null || historyStart == null
            || historyEnd == null || historyStart.isAfter(historyEnd)) {
            throw new IllegalArgumentException("按历史订单收集成交的参数无效");
        }
        Map<Long, ExchangeGateway.TradeSnapshot> unique = new HashMap<>();
        int[] queryCount = {0};
        for (ExchangeGateway.OrderSnapshot order : orders) {
            if (order == null || order.orderId() == null || order.orderId() <= 0
                || order.symbol() == null || !symbol.equalsIgnoreCase(order.symbol())) {
                throw new IllegalStateException("历史订单缺少有效身份，无法按订单收集成交");
            }
            boolean mayHaveFills = order.executedQuantity() != null && order.executedQuantity().signum() > 0
                || "FILLED".equalsIgnoreCase(order.status()) || "PARTIALLY_FILLED".equalsIgnoreCase(order.status());
            if (!mayHaveFills) {
                continue;
            }
            Instant orderStart = order.createTime() == null ? historyStart : order.createTime();
            Instant orderEnd = order.updateTime() == null ? historyEnd : order.updateTime();
            if (orderStart.isBefore(historyStart)) {
                orderStart = historyStart;
            }
            if (orderEnd.isAfter(historyEnd)) {
                orderEnd = historyEnd;
            }
            if (orderStart.isAfter(orderEnd)) {
                throw new IllegalStateException("历史订单创建时间晚于更新时间，无法安全查询其成交");
            }
            Instant queryStart = orderStart;
            Instant queryEnd = orderEnd;
            List<ExchangeGateway.TradeSnapshot> orderTrades = ExchangeHistoryCollector.collect(queryStart, queryEnd, range -> {
                if (++queryCount[0] > MAX_QUERIES) {
                    throw new IllegalStateException("Binance per-order trade query budget exceeded; history completeness unproven");
                }
                List<ExchangeGateway.TradeSnapshot> page =
                    gateway.queryTradesByOrder(credential, symbol, order.orderId(), range, (long) LIMIT);
                if (page == null) {
                    throw new IllegalStateException("Binance per-order trade response unavailable; history completeness unproven");
                }
                return page;
            });
            for (ExchangeGateway.TradeSnapshot trade : orderTrades) {
                if (trade == null || trade.tradeId() == null || trade.tradeId() <= 0
                    || trade.orderId() == null || !order.orderId().equals(trade.orderId())
                    || trade.symbol() == null || !symbol.equalsIgnoreCase(trade.symbol()) || trade.time() == null
                    || trade.time().isBefore(queryStart) || trade.time().isAfter(queryEnd)) {
                    throw new IllegalStateException("Binance per-order trade page contains invalid or mismatched identity");
                }
                ExchangeGateway.TradeSnapshot prior = unique.putIfAbsent(trade.tradeId(), trade);
                if (prior != null && !prior.equals(trade)) {
                    throw new IllegalStateException("Conflicting Binance TradeId across per-order queries");
                }
            }
        }
        List<ExchangeGateway.TradeSnapshot> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparing(ExchangeGateway.TradeSnapshot::time).thenComparing(ExchangeGateway.TradeSnapshot::tradeId));
        return List.copyOf(sorted);
    }

    private void collectWindow(ExchangeGateway.Credential credential, String symbol, long leftMillis, long rightMillis,
                               Map<Long, ExchangeGateway.TradeSnapshot> unique, int[] queryCount) {
        if (++queryCount[0] > MAX_QUERIES) {
            throw new IllegalStateException("Binance trade history query budget exceeded; history completeness unproven");
        }
        Instant left = Instant.ofEpochMilli(leftMillis);
        Instant right = Instant.ofEpochMilli(rightMillis);
        var page = gateway.queryTrades(credential, symbol, new ExchangeGateway.TimeRange(left, right));
        if (page == null) {
            throw new IllegalStateException("Binance trade history page unavailable; history completeness unproven");
        }
        if (page.size() >= LIMIT) {
            if (rightMillis - leftMillis <= 1) {
                throw new IllegalStateException("Binance trade history remains saturated at millisecond precision; history completeness unproven");
            }
            long middle = leftMillis + (rightMillis - leftMillis) / 2;
            collectWindow(credential, symbol, leftMillis, middle, unique, queryCount);
            collectWindow(credential, symbol, middle + 1, rightMillis, unique, queryCount);
            return;
        }
        for (var trade : page) {
            if (trade == null || trade.tradeId() == null || trade.tradeId() <= 0
                || trade.time() == null || !symbol.equalsIgnoreCase(trade.symbol())
                || trade.time().toEpochMilli() < leftMillis || trade.time().toEpochMilli() > rightMillis
                || trade.orderId() != null && trade.orderId() <= 0) {
                throw new IllegalStateException("Exchange fill outside requested historical interval or has invalid identity");
            }
            // Do not discard a history row merely because money/side/quantity fields are incomplete.
            // The LIVE raw-fill journal retains those UNKNOWN facts for review; it must never coerce
            // them to zero. Pagination completeness and allocation completeness are separate gates.
            var prior = unique.putIfAbsent(trade.tradeId(), trade);
            if (prior != null && !prior.equals(trade)) {
                throw new IllegalStateException("Conflicting Binance TradeId across history windows");
            }
        }
    }
}

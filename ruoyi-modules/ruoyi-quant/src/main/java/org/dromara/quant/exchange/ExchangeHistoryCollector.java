package org.dromara.quant.exchange;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 按 USDⓈ-M Futures 接口限制分段收集最近 90 天的完整历史数据。
 */
public final class ExchangeHistoryCollector {
    private static final long LOOKBACK_MILLIS = Duration.ofDays(90).toMillis();
    private static final long START_SAFETY_MARGIN_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long WINDOW_MILLIS = Duration.ofDays(7).toMillis();
    private static final int PAGE_LIMIT = 1000;
    private static final int MAX_REQUESTS = 256;
    private static final int MAX_DEFERRED_RETRIES = 5;
    private static final long MAX_DEFERRED_RETRY_DELAY_MILLIS = Duration.ofSeconds(5).toMillis();

    private ExchangeHistoryCollector() {
    }

    public static Instant recent90DaysStart(Instant end) {
        if (end == null) {
            throw new IllegalArgumentException("交易所历史查询结束时间不能为空");
        }
        return Instant.ofEpochMilli(end.toEpochMilli() - LOOKBACK_MILLIS + START_SAFETY_MARGIN_MILLIS);
    }

    public static <T> List<T> collectLast90Days(Function<ExchangeGateway.TimeRange, List<T>> query) {
        Instant end = Instant.now();
        return collect(recent90DaysStart(end), end, query);
    }

    public static <T> List<T> collect(Instant from, Instant to, Function<ExchangeGateway.TimeRange, List<T>> query) {
        if (from == null || to == null || query == null || from.isAfter(to)) {
            throw new IllegalArgumentException("交易所历史查询范围无效");
        }
        long start = from.toEpochMilli();
        long end = to.toEpochMilli();
        List<T> result = new ArrayList<>();
        int[] requests = {0};
        while (start <= end) {
            long windowEnd = Math.min(end, start + WINDOW_MILLIS - 1);
            collectRange(start, windowEnd, query, result, requests);
            start = windowEnd + 1;
        }
        return List.copyOf(result);
    }

    /**
     * 本地调度器延后请求时，GET 历史查询尚未发送且没有副作用，可以在短暂退避后重试。
     * 长冷却（例如 Binance 限流或人工封禁）不在同步 HTTP 请求中等待，直接交由上层处理。
     */
    private static <T> List<T> queryWithDeferredRetry(Function<ExchangeGateway.TimeRange, List<T>> query,
                                                       ExchangeGateway.TimeRange range) {
        for (int attempt = 0; ; attempt++) {
            try {
                return query.apply(range);
            } catch (ExchangeMarketPort.RequestDeferredException deferred) {
                if (attempt >= MAX_DEFERRED_RETRIES
                    || deferred.retryAfterMillis() > MAX_DEFERRED_RETRY_DELAY_MILLIS) {
                    throw deferred;
                }
                try {
                    Thread.sleep(deferred.retryAfterMillis());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw deferred;
                }
            }
        }
    }

    private static <T> void collectRange(long start, long end, Function<ExchangeGateway.TimeRange, List<T>> query, List<T> result, int[] requests) {
        if (++requests[0] > MAX_REQUESTS) {
            throw new IllegalStateException("交易所历史查询超过请求预算，拒绝使用不完整结果更新本地账本");
        }
        List<T> page = queryWithDeferredRetry(query, new ExchangeGateway.TimeRange(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)));
        if (page == null || page.size() < PAGE_LIMIT) {
            if (page != null) result.addAll(page);
            return;
        }
        if (end - start <= 1) {
            throw new IllegalStateException("同一毫秒内历史记录达到交易所单页上限，无法确认查询完整性");
        }
        long middle = start + (end - start) / 2;
        collectRange(start, middle, query, result, requests);
        collectRange(middle + 1, end, query, result, requests);
    }
}

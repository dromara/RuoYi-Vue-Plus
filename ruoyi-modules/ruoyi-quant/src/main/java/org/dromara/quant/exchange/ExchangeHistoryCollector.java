package org.dromara.quant.exchange;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Collects complete recent exchange history within the query limits of USDⓈ-M Futures endpoints.
 */
public final class ExchangeHistoryCollector {
    private static final long LOOKBACK_MILLIS = Duration.ofDays(90).toMillis();
    private static final long WINDOW_MILLIS = Duration.ofDays(7).toMillis();
    private static final int PAGE_LIMIT = 1000;
    private static final int MAX_REQUESTS = 128;

    private ExchangeHistoryCollector() {
    }

    public static <T> List<T> collectLast90Days(Function<ExchangeGateway.TimeRange, List<T>> query) {
        long end = Instant.now().toEpochMilli();
        long start = end - LOOKBACK_MILLIS;
        List<T> result = new ArrayList<>();
        int[] requests = {0};
        while (start <= end) {
            long windowEnd = Math.min(end, start + WINDOW_MILLIS - 1);
            collectRange(start, windowEnd, query, result, requests);
            start = windowEnd + 1;
        }
        return List.copyOf(result);
    }

    private static <T> void collectRange(long start, long end, Function<ExchangeGateway.TimeRange, List<T>> query, List<T> result, int[] requests) {
        if (++requests[0] > MAX_REQUESTS) {
            throw new IllegalStateException("交易所历史查询超过请求预算，拒绝使用不完整结果更新本地账本");
        }
        List<T> page = query.apply(new ExchangeGateway.TimeRange(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)));
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

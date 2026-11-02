package org.dromara.quant.exchange;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeHistoryCollectorTest {
    @Test
    void splitsFullSevenDayPagesUntilEveryRangeIsComplete() {
        AtomicInteger calls = new AtomicInteger();
        List<Integer> result = ExchangeHistoryCollector.collectLast90Days(range -> {
            calls.incrementAndGet();
            return range.to().toEpochMilli() - range.from().toEpochMilli() >= Duration.ofDays(3).toMillis()
                ? java.util.Collections.nCopies(1000, 1)
                : List.of(1, 2);
        });

        assertFalse(result.isEmpty());
        assertTrue(calls.get() > 13);
    }

    @Test
    void neverQueriesMoreThanSevenDaysAtOnce() {
        AtomicInteger calls = new AtomicInteger();
        ExchangeHistoryCollector.collectLast90Days(range -> {
            calls.incrementAndGet();
            assertTrue(Duration.between(range.from(), range.to()).toMillis() < Duration.ofDays(7).toMillis());
            return List.of();
        });
        assertTrue(calls.get() >= 13);
    }
}

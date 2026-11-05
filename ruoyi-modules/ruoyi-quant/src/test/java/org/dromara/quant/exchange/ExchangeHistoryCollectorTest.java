package org.dromara.quant.exchange;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
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
    @Test
    void explicitHistoryBoundsAreUsedAcrossSevenDayWindows() {
        java.time.Instant from = java.time.Instant.parse("2026-08-07T10:22:30Z");
        java.time.Instant to = java.time.Instant.parse("2026-11-05T10:22:30Z");
        java.util.List<ExchangeGateway.TimeRange> ranges = new java.util.ArrayList<>();

        ExchangeHistoryCollector.collect(from, to, range -> {
            ranges.add(range);
            return List.of();
        });

        assertFalse(ranges.isEmpty());
        assertEquals(from.toEpochMilli(), ranges.getFirst().from().toEpochMilli());
        assertEquals(to.toEpochMilli(), ranges.getLast().to().toEpochMilli());
        assertTrue(ranges.size() >= 13);
        for (int i = 0; i < ranges.size(); i++) {
            assertTrue(java.time.Duration.between(ranges.get(i).from(), ranges.get(i).to()).toMillis()
                < java.time.Duration.ofDays(7).toMillis());
            if (i > 0) {
                assertEquals(ranges.get(i - 1).to().toEpochMilli() + 1, ranges.get(i).from().toEpochMilli());
            }
        }
    }

    @Test
    void rejectsHistoryWhenRequestBudgetCannotProveCompleteness() {
        assertThrows(IllegalStateException.class, () -> ExchangeHistoryCollector.collectLast90Days(range -> java.util.Collections.nCopies(1000, 1)));
    }

}

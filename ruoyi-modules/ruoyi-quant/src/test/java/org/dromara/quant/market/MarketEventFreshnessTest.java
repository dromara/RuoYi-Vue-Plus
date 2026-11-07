package org.dromara.quant.market;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketEventFreshnessTest {
    @Test
    void acceptsRecentExchangeEventWhenLocalClockIsThreeSecondsAhead() {
        Instant localNow = Instant.parse("2026-10-17T07:21:33Z");
        Instant eventTime = localNow.minusMillis(3_100);
        Instant receivedAt = localNow.minusMillis(40);

        assertTrue(MarketEventFreshness.isFresh(eventTime, receivedAt, localNow, localNow.minusMillis(2_979), 3_000));
    }

    @Test
    void rejectsEventDelayedInLocalProcessingQueueEvenWhenExchangeClockIsAdjusted() {
        Instant localNow = Instant.parse("2026-10-17T07:21:33Z");
        Instant eventTime = localNow.minusMillis(3_100);
        Instant receivedAt = localNow.minusMillis(3_100);

        assertFalse(MarketEventFreshness.isFresh(eventTime, receivedAt, localNow, localNow.minusMillis(2_979), 3_000));
    }

    @Test
    void rejectsExchangeEventThatIsActuallyStaleAfterClockAdjustment() {
        Instant localNow = Instant.parse("2026-10-17T07:21:33Z");
        Instant eventTime = localNow.minusMillis(6_500);
        Instant receivedAt = localNow.minusMillis(40);

        assertFalse(MarketEventFreshness.isFresh(eventTime, receivedAt, localNow, localNow.minusMillis(2_979), 3_000));
    }

    @Test
    void acceptsKlineEventAtPeriodOpenAndBeforeNextPeriod() {
        Instant open = Instant.parse("2026-11-08T00:45:00Z");
        Instant nextOpen = Instant.parse("2026-11-08T01:00:00Z");

        assertTrue(MarketStreamHub.isEventTimeWithinKlinePeriod(open, open, nextOpen));
        assertTrue(MarketStreamHub.isEventTimeWithinKlinePeriod(nextOpen.minusMillis(1), open, nextOpen));
    }

    @Test
    void rejectsKlineEventAtOrOutsidePeriodBoundaries() {
        Instant open = Instant.parse("2026-11-08T00:45:00Z");
        Instant nextOpen = Instant.parse("2026-11-08T01:00:00Z");

        assertFalse(MarketStreamHub.isEventTimeWithinKlinePeriod(open.minusMillis(1), open, nextOpen));
        assertFalse(MarketStreamHub.isEventTimeWithinKlinePeriod(nextOpen, open, nextOpen));
    }

    @Test
    void rejectsExchangeTimestampMoreThanOneSecondInTheFuture() {
        Instant localNow = Instant.parse("2026-10-17T07:21:33Z");

        assertFalse(MarketEventFreshness.isExchangeTimeFresh(localNow.plusSeconds(2), localNow, 3_000));
    }
}

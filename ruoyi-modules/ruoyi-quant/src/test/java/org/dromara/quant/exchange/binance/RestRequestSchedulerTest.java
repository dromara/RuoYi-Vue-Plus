package org.dromara.quant.exchange.binance;

import org.dromara.quant.exchange.ExchangeMarketPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestRequestSchedulerTest {
    @Test
    void manualBlockReportsReasonAndDoesNotSuggestAutomaticRetry() {
        var scheduler = new RestRequestScheduler(1);
        scheduler.blockUntilManualReset();

        var failure = assertThrows(ExchangeMarketPort.RequestDeferredException.class,
            () -> scheduler.acquire(RestRequestScheduler.Kind.POSITION_VERIFY));

        assertEquals("MANUAL_BLOCK", failure.reason());
        assertEquals("POSITION_VERIFY", failure.kind());
        assertEquals(0L, failure.retryAfterMillis());
        assertEquals(4, failure.queueSizes().length);
    }

    @Test
    void cooldownReportsRemainingDelay() {
        var scheduler = new RestRequestScheduler(1);
        scheduler.deferFor(2_000L);

        var failure = assertThrows(ExchangeMarketPort.RequestDeferredException.class,
            () -> scheduler.acquire(RestRequestScheduler.Kind.ACCOUNT));

        assertEquals("BLOCKED_UNTIL", failure.reason());
        assertEquals("ACCOUNT", failure.kind());
        assertTrue(failure.retryAfterMillis() > 0L);
        assertEquals(0, failure.queueSize());
    }

    @Test
    void queueKindsKeepPositionVerificationAheadOfInformationalReads() {
        assertEquals(0, RestRequestScheduler.Kind.TRADE.ordinal());
        assertEquals(1, RestRequestScheduler.Kind.POSITION_VERIFY.ordinal());
        assertEquals(2, RestRequestScheduler.Kind.ACCOUNT.ordinal());
        assertEquals(3, RestRequestScheduler.Kind.BACKGROUND.ordinal());
    }
}

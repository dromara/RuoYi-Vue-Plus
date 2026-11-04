package org.dromara.quant.account.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantAccountQueryTimeTest {

    @Test
    void convertsBeijingDateRangeToUtcBoundaries() {
        assertEquals(LocalDateTime.of(2026, 11, 3, 16, 0),
            QuantAccountQueryController.tradeDateBoundary("2026-11-04", false));
        assertEquals(LocalDateTime.of(2026, 11, 4, 16, 0),
            QuantAccountQueryController.tradeDateBoundary("2026-11-04", true));
    }
}

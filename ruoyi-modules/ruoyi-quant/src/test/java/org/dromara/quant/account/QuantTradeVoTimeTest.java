package org.dromara.quant.account;

import org.dromara.quant.account.domain.vo.QuantTradeVo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantTradeVoTimeTest {

    @Test
    void convertsUtcFillTimeToExplicitBeijingOffset() {
        QuantTradeVo vo = new QuantTradeVo();
        vo.setTradeTime(LocalDateTime.of(2026, 11, 4, 13, 37, 56));

        assertEquals(OffsetDateTime.parse("2026-11-04T21:37:56+08:00"), vo.getTradeTimeBeijing());
    }
}

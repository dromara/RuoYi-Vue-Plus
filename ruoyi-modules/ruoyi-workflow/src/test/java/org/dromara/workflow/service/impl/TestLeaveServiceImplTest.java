package org.dromara.workflow.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestLeaveServiceImplTest {

    @Test
    void ignoresEngineGeneratedBusinessIds() {
        assertNull(TestLeaveServiceImpl.parseLeaveId("T2114648233624309762_1793006432000"));
        assertNull(TestLeaveServiceImpl.parseLeaveId("9223372036854775808"));
        assertNull(TestLeaveServiceImpl.parseLeaveId(null));
        assertEquals(123L, TestLeaveServiceImpl.parseLeaveId("123"));
    }
}

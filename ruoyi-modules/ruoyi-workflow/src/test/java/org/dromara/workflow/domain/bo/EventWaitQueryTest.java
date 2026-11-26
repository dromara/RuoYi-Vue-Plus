package org.dromara.workflow.domain.bo;

import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EventWaitQueryTest {
    @Test
    void invalidPaginationUsesTimerMonitorDefaults() {
        var request = new FlowEventWaitBo();
        request.setPageNum(0);
        request.setPageSize(-1);
        request.setStatuses("  ");
        var query = request.toQuery();
        assertEquals(1, query.getPageNum());
        assertEquals(10, query.getPageSize());
        assertTrue(query.isTimerOnly());
        assertNull(query.getStatuses());
    }

    @Test
    void preservesMultipleFiltersAcrossPages() {
        var request = new FlowEventWaitBo();
        request.setPageNum(2);
        request.setPageSize(25);
        request.setStatuses(" DEAD, , RUNNING ");
        request.setEventTypes("TIMER_START, MESSAGE_CATCH");
        request.setTimerOnly(false);
        request.setBusinessId("leave-3");
        request.setNextTriggerAtFrom(new Date(100));
        request.setNextTriggerAtTo(new Date(200));
        request.setOrderByColumn("id");
        request.setIsAsc("desc");
        var query = request.toQuery();
        assertEquals(List.of("DEAD", "RUNNING"), query.getStatuses());
        assertEquals(List.of("TIMER_START", "MESSAGE_CATCH"), query.getEventTypes());
        assertFalse(query.isTimerOnly());
        assertEquals("leave-3", query.getBusinessId());
        assertEquals(2, query.getPageNum());
        assertEquals(25, query.getPageSize());
        assertEquals(new Date(100), query.getNextTriggerAtFrom());
        assertEquals(new Date(200), query.getNextTriggerAtTo());
        assertEquals("id", query.getOrderBy());
        assertEquals("desc", query.getIsAsc());
    }
}

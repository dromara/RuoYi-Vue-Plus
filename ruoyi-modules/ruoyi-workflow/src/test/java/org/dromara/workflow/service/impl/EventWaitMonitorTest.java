package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.EventWaitQuery;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.service.EventWaitService;
import org.dromara.warm.flow.core.workflow.WorkflowServiceImpl;
import org.dromara.workflow.domain.bo.FlowEventWaitBo;
import org.dromara.workflow.domain.bo.FlowTimerTriggerBo;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventWaitMonitorTest {
    private final FlwEventWaitServiceImpl service = new FlwEventWaitServiceImpl();

    @Test
    void rejectsMissingOrNonTimerStartRecord() {
        var waits = mock(EventWaitService.class);
        var request = new FlowTimerTriggerBo();
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::eventWaitService).thenReturn(waits);
            assertThrows(ServiceException.class, () -> service.manualTrigger(request));
            verifyNoInteractions(waits);
            request.setWaitId(7L);
            assertNull(service.queryById(7L));
            assertThrows(ServiceException.class, () -> service.manualTrigger(request));
            EventWait wait = mock(EventWait.class);
            when(waits.getById(7L)).thenReturn(wait);
            when(wait.getEventType()).thenReturn(EventType.MESSAGE_CATCH.getValue());
            assertThrows(ServiceException.class, () -> service.manualTrigger(request));
            engine.verify(FlowEngine::insService, never());
        }
    }

    @Test
    void manualTriggerUsesRecordDefinitionAndCurrentOperator() {
        var request = new FlowTimerTriggerBo();
        request.setWaitId(8L);
        request.setVariables(Map.of("days", 3));
        var waits = mock(EventWaitService.class);
        var wait = mock(EventWait.class);
        when(waits.getById(8L)).thenReturn(wait);
        when(wait.getEventType()).thenReturn(EventType.TIMER_START.getValue());
        when(wait.getDefinitionId()).thenReturn(12L);
        when(wait.getNodeCode()).thenReturn("monthly");
        var instances = mock(org.dromara.warm.flow.core.service.InsService.class);
        var instance = mock(org.dromara.warm.flow.core.entity.Instance.class);
        when(instance.getId()).thenReturn(72L);
        when(instances.startByTimer(anyString(), eq(12L), eq("monthly"), eq(request.getVariables()),
            argThat(context -> "17".equals(context.getHandler())))).thenReturn(instance);
        try (var engine = mockStatic(FlowEngine.class);
             var login = mockStatic(org.dromara.common.satoken.utils.LoginHelper.class)) {
            login.when(org.dromara.common.satoken.utils.LoginHelper::getUserIdStr).thenReturn("17");
            engine.when(FlowEngine::eventWaitService).thenReturn(waits);
            engine.when(FlowEngine::insService).thenReturn(instances);
            request.setBusinessId("  order-2  ");
            assertEquals(72L, service.manualTrigger(request));
            request.setBusinessId(" ");
            assertEquals(72L, service.manualTrigger(request));
            var ids = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(instances, times(2)).startByTimer(ids.capture(), eq(12L), eq("monthly"), eq(request.getVariables()), any());
            assertEquals("order-2", ids.getAllValues().getFirst());
            assertTrue(Long.parseLong(ids.getAllValues().getLast()) > 0);
        }
    }

    @Test
    void pageRetainsEngineTotalFilterAndStatusLabels() {
        var request = new FlowEventWaitBo();
        request.setBusinessId("order-7");
        request.setStatuses("DEAD");
        request.setPageNum(2);
        request.setPageSize(1);
        var workflow = mock(WorkflowServiceImpl.class);
        var config = mock(org.dromara.warm.flow.core.config.WarmFlow.class);
        when(config.isTimerEnabled()).thenReturn(true);
        var wait = mock(EventWait.class);
        when(wait.getId()).thenReturn(83L);
        when(wait.getDefinitionId()).thenReturn(null);
        when(wait.getInstanceId()).thenReturn(null);
        when(wait.getStatus()).thenReturn("DEAD");
        when(wait.getEventType()).thenReturn(EventType.TIMER_CATCH.getValue());
        when(workflow.pageEventWaits(any())).thenAnswer(call -> {
            EventWaitQuery query = call.getArgument(0);
            assertEquals("order-7", query.getBusinessId());
            assertEquals(java.util.List.of("DEAD"), query.getStatuses());
            assertEquals(2, query.getPageNum());
            assertEquals(1, query.getPageSize());
            return new org.dromara.warm.flow.core.utils.page.Page<>(java.util.List.of(wait), 4L);
        });
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            engine.when(FlowEngine::getFlowConfig).thenReturn(config);
            var page = service.queryPageList(request);
            assertEquals(4L, page.getTotal());
            assertEquals(1, page.getRows().size());
            var row = page.getRows().iterator().next();
            assertEquals(83L, row.getId());
            assertEquals("已死", row.getStatusName());
            assertEquals("定时捕获", row.getEventTypeName());
            assertTrue(row.getTimerEnabled());
        }
    }

    @Test
    void summaryOverridesOnlyStatusForEveryBucket() {
        var workflow = mock(WorkflowServiceImpl.class);
        var request = new FlowEventWaitBo();
        request.setBusinessId("A");
        request.setStatuses("SUCCESS");
        Map<String, Long> counts = Map.of("WAITING", 1L, "RETRY", 2L, "RUNNING", 3L, "DEAD", 4L, "CANCELLED", 5L);
        when(workflow.countEventWaits(any())).thenAnswer(call -> {
            EventWaitQuery query = call.getArgument(0);
            assertEquals("A", query.getBusinessId());
            return counts.get(query.getStatuses().getFirst());
        });
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            assertEquals(Map.of("waiting", 1L, "retry", 2L, "running", 3L, "dead", 4L, "cancelled", 5L), service.summary(request));
            assertEquals("SUCCESS", request.getStatuses());
        }
    }
}

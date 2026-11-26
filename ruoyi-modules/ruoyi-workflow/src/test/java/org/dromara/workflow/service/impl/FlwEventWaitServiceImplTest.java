package org.dromara.workflow.service.impl;

import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.constant.EventCons;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.service.EventWaitService;
import org.dromara.warm.flow.core.service.impl.EventWaitServiceImpl;
import org.dromara.warm.flow.core.workflow.WorkflowServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class FlwEventWaitServiceImplTest {

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "RETRY", "RUNNING", "CANCELLED", "SUCCESS"})
    void recoveryRejectsRecordsThatAreNotDead(String status) {
        FlowException error = recover(status, EventType.TIMER_CATCH);
        assertTrue(error.getMessage().contains("DEAD"), error.getMessage());
    }

    @Test
    void revivalUsesDeadRecordVersionAndRejectsConcurrentUpdate() {
        var dao = mock(org.dromara.warm.flow.core.orm.dao.FlowEventWaitDao.class);
        var service = spy(new EventWaitServiceImpl());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "warmDao", dao);
        var wait = mock(EventWait.class);
        when(wait.getStatus()).thenReturn("DEAD");
        when(wait.getId()).thenReturn(12L);
        when(wait.getVersion()).thenReturn(5);
        when(wait.getInstanceId()).thenReturn(8L);
        when(wait.getAttachedTaskId()).thenReturn(null);
        when(wait.getEventType()).thenReturn(EventType.MESSAGE_CATCH.getValue());
        when(wait.getNodeCode()).thenReturn("receive");
        doReturn(wait).when(service).getById(12L);
        doReturn(java.util.List.of(wait)).when(service).getWaitingByInstanceId(8L);
        var instances = mock(org.dromara.warm.flow.core.service.InsService.class);
        var tasks = mock(org.dromara.warm.flow.core.service.TaskService.class);
        var instance = mock(org.dromara.warm.flow.core.entity.Instance.class);
        when(instance.getId()).thenReturn(8L);
        when(instance.getActivityStatus()).thenReturn(org.dromara.warm.flow.core.enums.ActivityStatus.ACTIVITY.getKey());
        when(instances.getById(8L)).thenReturn(instance);
        when(dao.revive(org.mockito.Mockito.eq(12L), org.mockito.Mockito.eq(5),
            org.mockito.Mockito.any(java.util.Date.class), org.mockito.Mockito.isNull())).thenReturn(true, false);
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(new WorkflowServiceImpl());
            engine.when(FlowEngine::eventWaitService).thenReturn(service);
            engine.when(FlowEngine::insService).thenReturn(instances);
            engine.when(FlowEngine::taskService).thenReturn(tasks);
            var monitor = new FlwEventWaitServiceImpl();
            monitor.revive(12L);
            assertThrows(FlowException.class, () -> monitor.revive(12L));
        }
    }

    @Test
    void deadTimerStartMustBeRebuiltFromDefinition() {
        FlowException error = recover(EventCons.STATUS_DEAD, EventType.TIMER_START);
        assertTrue(error.getMessage().contains("定时启动"), error.getMessage());
    }

    @Test
    void deadEventCannotResumeWithoutItsInstance() {
        FlowException error = recover(EventCons.STATUS_DEAD, EventType.MESSAGE_CATCH);
        assertTrue(error.getMessage().contains("实例"), error.getMessage());
    }

    /**
     * 使用实际 2.1.1 门面和事件服务验证恢复约束，避免宿主绕过引擎直接改状态。
     */
    private FlowException recover(String status, EventType type) {
        EventWait wait = mock(EventWait.class);
        when(wait.getStatus()).thenReturn(status);
        when(wait.getEventType()).thenReturn(type.getValue());
        when(wait.getAttachedTaskId()).thenReturn(null);
        when(wait.getInstanceId()).thenReturn(null);
        EventWaitService eventService = spy(new EventWaitServiceImpl());
        doReturn(wait).when(eventService).getById(12L);
        try (MockedStatic<FlowEngine> engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(new WorkflowServiceImpl());
            engine.when(FlowEngine::eventWaitService).thenReturn(eventService);
            return assertThrows(FlowException.class, () -> new FlwEventWaitServiceImpl().revive(12L));
        }
    }
}

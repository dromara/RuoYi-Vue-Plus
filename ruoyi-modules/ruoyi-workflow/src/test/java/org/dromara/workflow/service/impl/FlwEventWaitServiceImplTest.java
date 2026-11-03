package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
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
        ServiceException error = recover(status, EventType.TIMER_CATCH);
        assertTrue(error.getMessage().contains("DEAD"), error.getMessage());
    }

    @Test
    void deadTimerStartMustBeRebuiltFromDefinition() {
        ServiceException error = recover(EventCons.STATUS_DEAD, EventType.TIMER_START);
        assertTrue(error.getMessage().contains("定时启动"), error.getMessage());
    }

    @Test
    void deadEventCannotResumeWithoutItsInstance() {
        ServiceException error = recover(EventCons.STATUS_DEAD, EventType.MESSAGE_CATCH);
        assertTrue(error.getMessage().contains("实例"), error.getMessage());
    }

    /**
     * 使用实际 2.1.1 门面和事件服务验证恢复约束，避免宿主绕过引擎直接改状态。
     */
    private ServiceException recover(String status, EventType type) {
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
            return assertThrows(ServiceException.class, () -> new FlwEventWaitServiceImpl().revive(12L));
        }
    }
}

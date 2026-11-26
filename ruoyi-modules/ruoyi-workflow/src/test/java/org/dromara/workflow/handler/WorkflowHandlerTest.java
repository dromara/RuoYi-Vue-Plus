package org.dromara.workflow.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.workflow.api.event.ProcessEvent;
import org.dromara.workflow.api.event.ProcessTaskEvent;
import org.dromara.workflow.api.event.ProcessDeleteEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowHandlerTest {
    @Test
    void exposesEngineFailureReason() {
        var response = new FlowExceptionHandler().handleFlowException(new FlowException("任务已结束"), mock(HttpServletRequest.class));
        assertFalse(R.isSuccess(response));
        assertEquals("任务已结束", response.getMsg());
    }

    @Test
    void eventsKeepInstanceAndNextTaskIdentitiesDistinct() {
        ApplicationContext spring = mock(ApplicationContext.class);
        Instance instance = mock(Instance.class);
        Task next = mock(Task.class);
        when(instance.getId()).thenReturn(8L);
        when(instance.getBusinessId()).thenReturn("order");
        when(instance.getNodeCode()).thenReturn("apply");
        when(next.getNodeCode()).thenReturn("review");
        when(next.getId()).thenReturn(19L);
        Map<String, Object> variables = Map.of("days", 2);
        try (var utils = mockStatic(SpringUtils.class)) {
            utils.when(SpringUtils::context).thenReturn(spring);
            var handler = new FlowProcessEventHandler();
            handler.processHandler("leave", instance, "waiting", variables, true);
            handler.processTaskHandler("leave", instance, next, variables);
            handler.processDeleteHandler("leave", "order");
        }
        var published = ArgumentCaptor.forClass(Object.class);
        verify(spring, times(3)).publishEvent(published.capture());
        var events = published.getAllValues();
        ProcessEvent state = assertInstanceOf(ProcessEvent.class, events.get(0));
        assertEquals("order", state.getBusinessId());
        assertEquals(8L, state.getInstanceId());
        assertEquals(variables, state.getParams());
        assertEquals("apply", state.getNodeCode());
        assertEquals("waiting", state.getStatus());
        assertTrue(state.getSubmit());
        ProcessTaskEvent task = assertInstanceOf(ProcessTaskEvent.class, events.get(1));
        assertEquals("order", task.getBusinessId());
        assertEquals("review", task.getNodeCode());
        assertEquals(19L, task.getTaskId());
        assertEquals(8L, task.getInstanceId());
        assertEquals(variables, task.getParams());
        ProcessDeleteEvent deleted = assertInstanceOf(ProcessDeleteEvent.class, events.get(2));
        assertEquals("leave", deleted.getFlowCode());
        assertEquals("order", deleted.getBusinessId());
    }
}

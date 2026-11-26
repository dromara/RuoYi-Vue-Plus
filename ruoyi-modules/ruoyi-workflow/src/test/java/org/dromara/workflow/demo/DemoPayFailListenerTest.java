package org.dromara.workflow.demo;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.spi.ListenerVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.mockito.Mockito.*;

class DemoPayFailListenerTest {
    @org.junit.jupiter.api.Test
    void missingTaskOrAmountDoesNotRaisePaymentError() {
        var event = mock(ListenerVariable.class);
        try (var engine = mockStatic(FlowEngine.class)) {
            var listener = new DemoPayFailListener();
            listener.notify(event);
            var task = mock(Task.class);
            when(task.getId()).thenReturn(18L);
            when(event.getTask()).thenReturn(task);
            when(event.getVariable()).thenReturn(Map.of());
            listener.notify(event);
            engine.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource({"1000,2000,false", "1001,0,true", "1000.01,0,true", ",1001,true", "invalid,0,false"})
    void checksCurrentAmountBeforePersistedAmount(String current, String saved, boolean exceeded) {
        ListenerVariable event = mock(ListenerVariable.class);
        Task task = mock(Task.class);
        Instance instance = mock(Instance.class);
        when(event.getTask()).thenReturn(task);
        when(task.getId()).thenReturn(18L);
        when(event.getVariable()).thenReturn(current == null ? Map.of() : Map.of("payAmount", current));
        when(event.getInstance()).thenReturn(instance);
        when(instance.getVariableMap()).thenReturn(Map.of("payAmount", saved));
        TaskService tasks = mock(TaskService.class);
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::taskService).thenReturn(tasks);
            new DemoPayFailListener().notify(event);
            if (exceeded) {
                verify(tasks).throwError(eq(18L), eq("PAY_FAILED"), contains(current == null ? saved : current));
            } else {
                verifyNoInteractions(tasks);
            }
        }
    }
}

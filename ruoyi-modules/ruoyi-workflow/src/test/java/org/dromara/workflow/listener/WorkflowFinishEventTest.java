package org.dromara.workflow.listener;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.spi.ListenerVariable;
import org.dromara.warm.flow.core.workflow.context.WorkflowContext;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.event.*;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.service.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;

import java.util.*;
import static org.mockito.Mockito.*;

class WorkflowFinishEventTest {
    @ParameterizedTest
    @ValueSource(strings = {"submit", "forward", "back", "end"})
    void publishesBusinessAndTaskEventsForCurrentTransition(String transition) {
        var tasks = mock(IFlwTaskService.class);
        var instances = mock(IFlwInstanceService.class);
        var events = mock(FlowProcessEventHandler.class);
        var common = mock(IFlwCommonService.class);
        var listener = new WorkflowGlobalListener(tasks, instances, events, common, null);
        var event = mock(ListenerVariable.class);
        var instance = new FlowInstance().setId(31L).setFlowStatus("waiting").setCreateBy("7");
        var definition = new FlowDefinition().setFlowCode("leave").setFlowName("请假").setId(4L);
        var variables = new HashMap<String, Object>();
        variables.put(FlowConstant.MESSAGE_TYPE, List.of("1"));
        variables.put(FlowConstant.SUBMIT, transition.equals("submit"));
        var operation = WorkflowContext.create();
        operation.setTaskStatus(transition.equals("back") ? "back" : "pass");
        operation.setVariables(variables);
        List<Task> next = transition.equals("end") ? List.of()
            : List.of(new FlowTask().setInstanceId(31L).setNodeCode(transition.equals("back") ? "apply" : "review"));
        when(event.getDefinition()).thenReturn(definition);
        when(event.getInstance()).thenReturn(instance);
        when(event.getTask()).thenReturn(new FlowTask().setInstanceId(31L));
        when(event.getContext()).thenReturn(operation);
        when(event.getNextTasks()).thenReturn(next);
        when(common.applyNodeCode(4L)).thenReturn("apply");
        when(tasks.isTaskEnd(31L)).thenReturn(next.isEmpty());
        var publisher = mock(ApplicationContext.class);
        var engineInstances = mock(InsService.class);
        try (var spring = mockStatic(SpringUtils.class); var engine = mockStatic(FlowEngine.class)) {
            spring.when(SpringUtils::context).thenReturn(publisher);
            engine.when(FlowEngine::insService).thenReturn(engineInstances);
            listener.finish(event);
            for (Task task : next) {
                verify(events).processTaskHandler(eq("leave"), same(instance), same(task), any());
            }
            switch (transition) {
                case "submit" -> verify(events).processHandler("leave", instance, null, variables, true);
                case "back" -> verify(events).processHandler(eq("leave"), same(instance), eq("back"), any(), eq(false));
                case "end" -> {
                    verify(instances).updateStatus(31L, "finish");
                    verify(events).processHandler(eq("leave"), same(instance), eq("finish"), any(), eq(false));
                }
            }
            verifyNoMoreInteractions(events);
            if (transition.equals("back") || transition.equals("end")) {
                verify(publisher).publishEvent(new WorkflowResultMessageEvent("请假", transition.equals("end") ? "finish" : "back", "7", List.of("1")));
            } else {
                verify(publisher).publishEvent(new WorkflowTaskMessageEvent("请假", 31L, List.of("1"), null));
            }
            verifyNoMoreInteractions(publisher);
        }
    }
}

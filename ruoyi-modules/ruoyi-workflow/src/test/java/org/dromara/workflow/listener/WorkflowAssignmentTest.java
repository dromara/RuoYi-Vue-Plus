package org.dromara.workflow.listener;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.TaskStatus;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.spi.ListenerVariable;
import org.dromara.warm.flow.core.workflow.context.WorkflowContext;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.service.IFlwCommonService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowAssignmentTest {
    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"PASS", "BACK"})
    void assignsEachNodeAndConsumesOnlyForwardSelection(TaskStatus status) {
        IFlwCommonService common = mock(IFlwCommonService.class);
        var listener = new WorkflowGlobalListener(null, null, null, common, null);
        var event = mock(ListenerVariable.class);
        var definition = mock(Definition.class);
        var instance = mock(Instance.class);
        var operator = mock(WorkflowContext.class);
        when(operator.getTaskStatus()).thenReturn(status.getKey());
        when(event.getContext()).thenReturn(operator);
        when(event.getDefinition()).thenReturn(definition);
        when(definition.getId()).thenReturn(7L);
        when(common.applyNodeCode(7L)).thenReturn("apply");
        when(event.getInstance()).thenReturn(instance);
        when(instance.getCreateBy()).thenReturn("18");
        Task review = new FlowTask().setNodeCode("review").setInstanceId(3L);
        Task apply = new FlowTask().setNodeCode("apply").setInstanceId(3L);
        when(event.getNextTasks()).thenReturn(List.of(review, apply));
        String key = status.getKey() + ":review";
        Map<String, Object> variables = new HashMap<>(Map.of(key, "12,13", "unrelated", 5));
        when(event.getVariable()).thenReturn(variables);
        InsService instances = mock(InsService.class);
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::insService).thenReturn(instances);
            listener.assignment(event);
            assertEquals(List.of("18"), apply.getPermissionList());
            assertEquals(List.of("12", "13"), review.getPermissionList());
            assertEquals(5, variables.get("unrelated"));
            assertEquals(status == TaskStatus.BACK, variables.containsKey(key));
            verify(instances, times(status == TaskStatus.PASS ? 1 : 0)).removeVariables(3L, key);
        }
    }
}

package org.dromara.workflow.service.impl;

import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.workflow.domain.bo.FlowNextNodeBo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NextApprovalPreviewTest {
    @ParameterizedTest
    @ValueSource(strings = {"small", "large", "unassigned"})
    void requestVariablesSelectBranchAndPreviewItsCandidates(String amount) {
        var tasks = mock(TaskService.class);
        var instances = mock(InsService.class);
        var definitions = mock(DefService.class);
        var nodes = mock(NodeService.class);
        var service = new FlwTaskServiceImpl(tasks, instances, definitions, null, nodes, null, null, null,
            null, null, null, null, null, null);
        var task = new FlowTask().setDefinitionId(4L).setInstanceId(31L).setNodeCode("apply");
        var instance = mock(FlowInstance.class);
        var definition = new FlowDefinition().setId(4L);
        when(tasks.getById(11L)).thenReturn(task);
        when(instances.getById(31L)).thenReturn(instance);
        when(instance.getVariableMap()).thenReturn(new HashMap<>(Map.of("amount", "original", "dept", "sales")));
        when(definitions.getById(4L)).thenReturn(definition);
        String target = "small".equals(amount) ? "manager" : "director";
        var approval = new FlowNode().setNodeCode(target).setNodeType(NodeType.BETWEEN.getKey());
        var end = new FlowNode().setNodeCode("end").setNodeType(NodeType.END.getKey());
        when(nodes.getNextNodeList(4L, "apply", null, SkipType.PASS.getKey(), Map.of("amount", amount, "dept", "sales")))
            .thenReturn(List.of(approval, end));
        List<String> candidates = switch (amount) {
            case "small" -> List.of("7");
            case "large" -> List.of("8", "9");
            default -> List.of();
        };
        var preview = new FlowTask().setNodeCode(target);
        preview.setPermissionList(new ArrayList<>(candidates));
        when(tasks.addTask(same(approval), same(instance), same(definition), any(), eq(SkipType.PASS.getKey()))).thenReturn(preview);
        var request = new FlowNextNodeBo();
        request.setTaskId(11L);
        request.setVariables(Map.of("amount", amount));
        var result = service.getNextNodeList(request);
        assertEquals(List.of(target), result.stream().map(FlowNode::getNodeCode).toList());
        assertEquals(String.join(",", candidates), result.getFirst().getPermissionFlag());
    }
}

package org.dromara.workflow.service.impl;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowCombine;
import org.dromara.warm.flow.core.entity.*;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskReturnPathTest {
    @Test
    void delegatedTaskReturnsToCurrentNodeEvenWhenRejectionTargetIsConfigured() {
        var tasks = mock(FlowTaskMapper.class);
        var nodes = mock(NodeService.class);
        var service = new FlwTaskServiceImpl(null, null, null, null, nodes, tasks,
            null, null, null, null, null, null, null, null);
        var task = new FlowTask().setDefinitionId(7L);
        task.setId(9L);
        when(tasks.selectById(9L)).thenReturn(task);
        var review = node("review", NodeType.BETWEEN);
        when(review.getAnyNodeSkip()).thenReturn("apply");
        when(nodes.getByNodeCodes(List.of("review"), 7L)).thenReturn(List.of(review));
        try (var engine = mockStatic(FlowEngine.class)) {
            var users = mock(UserService.class);
            engine.when(FlowEngine::userService).thenReturn(users);
            when(users.getByAssociateds(List.of(9L), org.dromara.warm.flow.core.enums.UserType.DEPUTE.getKey()))
                .thenReturn(List.of(mock(User.class)));
            assertEquals(List.of(review), service.getBackTaskNode(9L, "review"));
        }
    }

    @Test
    void cyclicGraphOnlyOffersPreviousApprovalNodesOnce() {
        var tasks = mock(FlowTaskMapper.class);
        var nodes = mock(NodeService.class);
        var history = mock(HisTaskService.class);
        var definitions = mock(DefService.class);
        var service = new FlwTaskServiceImpl(null, null, definitions, history, nodes, tasks,
            null, null, null, null, null, null, null, null);
        var task = new FlowTask().setDefinitionId(7L).setInstanceId(8L);
        task.setId(9L);
        when(tasks.selectById(9L)).thenReturn(task);
        Node current = node("review", NodeType.BETWEEN);
        Node previous = node("apply", NodeType.BETWEEN);
        Node start = node("start", NodeType.START);
        Node unrelated = node("other", NodeType.BETWEEN);
        when(nodes.getByNodeCodes(List.of("review"), 7L)).thenReturn(List.of(current));
        var graph = mock(FlowCombine.class);
        when(definitions.getFlowCombineNoDef(7L)).thenReturn(graph);
        when(graph.getAllNodes()).thenReturn(List.of(start, previous, current, unrelated));
        var edges = List.of(edge("start", "apply"), edge("apply", "review"), edge("review", "apply"));
        when(graph.getAllSkips()).thenReturn(edges);
        try (var engine = mockStatic(FlowEngine.class)) {
            var users = mock(UserService.class);
            engine.when(FlowEngine::userService).thenReturn(users);
            assertEquals(List.of(previous), service.getBackTaskNode(9L, "review"));
            when(current.getAnyNodeSkip()).thenReturn("other");
            when(nodes.getByNodeCodes(List.of("other"), 7L)).thenReturn(List.of(unrelated));
            assertEquals(List.of(unrelated), service.getBackTaskNode(9L, "review"));
        }
    }

    @Test
    void mergedBranchesOfferOnlyVisitedApprovalsOnCurrentRoute() {
        var tasks = mock(FlowTaskMapper.class);
        var nodes = mock(NodeService.class);
        var history = mock(HisTaskService.class);
        var definitions = mock(DefService.class);
        var instances = mock(InsService.class);
        var service = new FlwTaskServiceImpl(null, instances, definitions, history, nodes, tasks,
            null, null, null, null, null, null, null, null);
        var task = new FlowTask().setDefinitionId(7L).setInstanceId(8L);
        task.setId(9L);
        when(tasks.selectById(9L)).thenReturn(task);
        Node start = node("start", NodeType.START);
        Node left = node("left", NodeType.BETWEEN);
        Node right = node("right", NodeType.BETWEEN);
        Node end = node("review", NodeType.BETWEEN);
        var graph = mock(FlowCombine.class);
        when(graph.getAllNodes()).thenReturn(List.of(start, left, right, end));
        var edges = List.of(edge("start", "left"), edge("start", "right"), edge("left", "review"), edge("right", "review"));
        when(graph.getAllSkips()).thenReturn(edges);
        when(definitions.getFlowCombineNoDef(7L)).thenReturn(graph);
        when(nodes.getByNodeCodes(List.of("review"), 7L)).thenReturn(List.of(end));
        var instance = mock(Instance.class);
        when(instances.getById(8L)).thenReturn(instance);
        var variables = java.util.Map.<String, Object>of("route", "left");
        when(instance.getVariableMap()).thenReturn(variables);
        var leftVisit = mock(HisTask.class);
        var rightVisit = mock(HisTask.class);
        when(leftVisit.getNodeCode()).thenReturn("left");
        when(rightVisit.getNodeCode()).thenReturn("right");
        when(history.getByInsId(8L)).thenReturn(List.of(leftVisit, rightVisit, leftVisit));
        when(nodes.getNextNodeList(eq(start), isNull(), anyString(), eq(variables), isNull(), eq(graph))).thenReturn(List.of(left));
        when(nodes.getNextNodeList(eq(left), isNull(), anyString(), eq(variables), isNull(), eq(graph))).thenReturn(List.of(end));
        try (var engine = mockStatic(FlowEngine.class)) {
            var users = mock(UserService.class);
            engine.when(FlowEngine::userService).thenReturn(users);
            assertEquals(List.of(left), service.getBackTaskNode(9L, "review"));
        }
    }

    private Node node(String code, NodeType type) {
        Node node = mock(Node.class);
        when(node.getNodeCode()).thenReturn(code);
        when(node.getNodeType()).thenReturn(type.getKey());
        return node;
    }

    private Skip edge(String from, String to) {
        Skip skip = mock(Skip.class);
        when(skip.getNowNodeCode()).thenReturn(from);
        when(skip.getNextNodeCode()).thenReturn(to);
        return skip;
    }
}

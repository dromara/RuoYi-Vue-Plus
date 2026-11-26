package org.dromara.workflow.liteflow.operation;

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.workflow.WorkflowServiceImpl;
import org.dromara.warm.flow.core.workflow.command.*;
import org.dromara.warm.flow.core.workflow.context.OperatorContext;
import org.dromara.warm.flow.core.workflow.result.WorkflowResult;
import org.dromara.workflow.common.enums.TaskOperationEnum;
import org.dromara.workflow.domain.bo.TaskOperationBo;
import org.dromara.workflow.domain.context.TaskOperationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskOperationCommandTest {
    @ParameterizedTest
    @EnumSource(value = TaskOperationEnum.class, names = {"ADD_SIGNATURE", "REDUCTION_SIGNATURE"})
    void cannotChangeSignersOnMissingTaskOrOrdinaryNode(TaskOperationEnum operation) {
        var tasks = mock(org.dromara.warm.flow.core.service.TaskService.class);
        var nodes = mock(org.dromara.warm.flow.orm.mapper.FlowNodeMapper.class);
        var component = spy(new TaskOpLoadComponent(tasks, nodes));
        var request = new TaskOperationBo();
        request.setTaskId(18L);
        var context = new TaskOperationContext(request, operation.getCode());
        context.setOperation(operation);
        doReturn(context).when(component).getContextBean(TaskOperationContext.class);
        assertThrows(org.dromara.common.core.exception.ServiceException.class, component::process);
        verifyNoInteractions(nodes);
        var task = new org.dromara.warm.flow.orm.entity.FlowTask().setDefinitionId(11L).setNodeCode("review");
        when(tasks.getById(18L)).thenReturn(task);
        assertThrows(org.dromara.common.core.exception.ServiceException.class, component::process);
        var node = new org.dromara.warm.flow.orm.entity.FlowNode().setNodeRatio("0");
        when(nodes.selectOne(any())).thenReturn(node);
        assertThrows(org.dromara.common.core.exception.ServiceException.class, component::process);
        assertNull(context.getFlowNode());
        node.setNodeRatio("100");
        component.process();
        assertSame(node, context.getFlowNode());
        assertSame(task, context.getTask());
    }

    @ParameterizedTest
    @EnumSource(TaskOperationEnum.class)
    void operationRequiresANonemptyTarget(TaskOperationEnum operation) {
        var request = new TaskOperationBo();
        request.setTaskId(18L);
        request.setUserId(" ");
        request.setUserIds(List.of());
        boolean single = operation == TaskOperationEnum.DELEGATE_TASK || operation == TaskOperationEnum.TRANSFER_TASK;
        var group = single ? org.dromara.common.core.validate.AddGroup.class : org.dromara.common.core.validate.EditGroup.class;
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request, group);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(single ? "userId" : "userIds")));
        }
    }

    @ParameterizedTest
    @EnumSource(TaskOperationEnum.class)
    void deliversOperationToSelectedHandlers(TaskOperationEnum operation) {
        var request = new TaskOperationBo();
        request.setTaskId(18L);
        request.setUserId("4");
        request.setUserIds(List.of("5", "6"));
        var context = new TaskOperationContext(request, operation.getCode());
        context.setOperation(operation);
        context.setMessage("复核");
        context.setOperator(new OperatorContext("7"));
        var component = spy(new TaskOpExecuteComponent());
        doReturn(context).when(component).getContextBean(TaskOperationContext.class);
        var workflow = mock(WorkflowServiceImpl.class);
        var result = mock(WorkflowResult.class);
        when(result.isSuccess()).thenReturn(true);
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            switch (operation) {
                case DELEGATE_TASK -> when(workflow.delegate(argThat(c -> c.getOperator() == context.getOperator() && c.getTaskId() == 18L && "4".equals(c.getTargetHandler())))).thenReturn(result);
                case TRANSFER_TASK -> when(workflow.transfer(argThat(c -> c.getOperator() == context.getOperator() && c.getTaskId() == 18L && "4".equals(c.getTargetHandler())))).thenReturn(result);
                case ADD_SIGNATURE -> when(workflow.addSigner(argThat(c -> c.getOperator() == context.getOperator() && c.getTaskId() == 18L && request.getUserIds().equals(c.getTargetHandlers())))).thenReturn(result);
                case REDUCTION_SIGNATURE -> when(workflow.removeSigner(argThat(c -> c.getOperator() == context.getOperator() && c.getTaskId() == 18L && request.getUserIds().equals(c.getTargetHandlers())))).thenReturn(result);
            }
            component.process();
            assertTrue(context.isResult());
        }
    }
}

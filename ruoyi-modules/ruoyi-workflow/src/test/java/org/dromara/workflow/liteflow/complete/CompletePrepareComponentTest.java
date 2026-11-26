package org.dromara.workflow.liteflow.complete;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.TaskStatus;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.context.CompleteTaskContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompletePrepareComponentTest {
    private final FlowTaskMapper tasks = mock(FlowTaskMapper.class);
    private final InsService instances = mock(InsService.class);
    private final CompletePrepareComponent component = spy(new CompletePrepareComponent(tasks, instances));
    private final CompleteTaskBo request = new CompleteTaskBo();

    CompletePrepareComponentTest() {
        request.setTaskId(12L);
        doReturn(new CompleteTaskContext(request)).when(component).getContextBean(CompleteTaskContext.class);
    }

    @Test
    void missingTaskStopsBeforeLoadingInstance() {
        assertThrows(ServiceException.class, component::process);
        verifyNoInteractions(instances);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = org.dromara.common.core.enums.BusinessStatusEnum.class,
        names = {"DRAFT", "CANCEL", "BACK", "WAITING"})
    void passesSubmissionFlagAndNotificationDataToEngine(org.dromara.common.core.enums.BusinessStatusEnum status) {
        var task = new FlowTask().setInstanceId(8L);
        task.setId(12L);
        when(tasks.selectById(12L)).thenReturn(task);
        var instance = mock(Instance.class);
        when(instances.getById(8L)).thenReturn(instance);
        when(instance.getFlowStatus()).thenReturn(status.getStatus());
        request.getVariables().put("days", 2);
        request.setMessage("同意");
        request.setNotice("通知申请人");
        request.setMessageType(java.util.List.of("1"));
        var recipient = new org.dromara.workflow.domain.bo.FlowCopyBo();
        recipient.setUserId(7L);
        request.setFlowCopyList(java.util.List.of(recipient));
        component.process();
        var execute = spy(new CompleteExecuteComponent());
        doReturn(component.getContextBean(CompleteTaskContext.class)).when(execute).getContextBean(CompleteTaskContext.class);
        var workflow = mock(org.dromara.warm.flow.core.workflow.WorkflowServiceImpl.class);
        try (var engine = mockStatic(org.dromara.warm.flow.core.FlowEngine.class)) {
            engine.when(org.dromara.warm.flow.core.FlowEngine::workflow).thenReturn(workflow);
            execute.process();
            var command = org.mockito.ArgumentCaptor.forClass(org.dromara.warm.flow.core.workflow.command.CompleteCommand.class);
            verify(workflow).complete(command.capture());
            var variables = command.getValue().getVariables();
            assertEquals(status != org.dromara.common.core.enums.BusinessStatusEnum.WAITING,
                Boolean.TRUE.equals(variables.get(org.dromara.workflow.common.constant.FlowConstant.SUBMIT)));
            assertEquals(2, variables.get("days"));
            assertEquals(request.getMessageType(), variables.get(org.dromara.workflow.common.constant.FlowConstant.MESSAGE_TYPE));
            assertEquals(request.getNotice(), variables.get(org.dromara.workflow.common.constant.FlowConstant.MESSAGE_NOTICE));
            assertEquals(request.getFlowCopyList(), variables.get(org.dromara.workflow.common.constant.FlowConstant.FLOW_COPY_LIST));
            assertEquals("同意", command.getValue().getMessage());
            assertEquals(12L, command.getValue().getTaskId());
        }
    }

    @Test
    void emptyPopupLeavesNodeAssignmentsUntouched() {
        when(tasks.selectById(12L)).thenReturn(new FlowTask().setInstanceId(8L));
        Instance instance = mock(Instance.class);
        when(instances.getById(8L)).thenReturn(instance);
        when(instance.getVariableMap()).thenReturn(Map.of("review", "42"));
        request.setAssigneeMap(Map.of());
        component.process();
        for (TaskStatus status : Set.of(TaskStatus.PASS, TaskStatus.BACK)) {
            assertFalse(request.getVariables().containsKey(status.getKey() + ":review"));
        }
    }

    @Test
    void missingInstanceStopsPreparation() {
        when(tasks.selectById(12L)).thenReturn(new FlowTask().setInstanceId(8L));
        assertThrows(ServiceException.class, component::process);
        assertTrue(request.getVariables().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1,2", "", " "})
    void selectedAssigneesApplyToBothPassAndBack(String previous) {
        when(tasks.selectById(12L)).thenReturn(new FlowTask().setInstanceId(8L));
        Instance instance = mock(Instance.class);
        when(instances.getById(8L)).thenReturn(instance);
        when(instance.getVariableMap()).thenReturn(Map.of("review", previous));
        request.setAssigneeMap(Map.of("review", "2,3", "audit", "4"));
        component.process();
        for (TaskStatus status : Set.of(TaskStatus.PASS, TaskStatus.BACK)) {
            String prefix = status.getKey() + ":";
            assertEquals("4", request.getVariables().get(prefix + "audit"));
            String reviewers = (String) request.getVariables().get(prefix + "review");
            assertNotNull(reviewers, "本次弹窗选择的办理人必须保留");
            assertEquals(previous.isBlank() ? Set.of("2", "3") : Set.of("1", "2", "3"),
                Set.of(reviewers.split(",")));
        }
    }
}

package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.service.UserService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.domain.bo.FlowNextNodeBo;
import org.dromara.workflow.domain.bo.FlowUrgeTaskBo;
import org.dromara.workflow.domain.bo.FlowTerminationBo;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskParticipantRulesTest {
    private final TaskService tasks = mock(TaskService.class);
    private final FlwTaskServiceImpl service = spy(new FlwTaskServiceImpl(tasks, null, null, null, null,
        null, null, null, null, null, null, null, null, null));

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void terminationAndReturnExposeEngineOutcome(boolean success) {
        var instances = mock(org.dromara.warm.flow.core.service.InsService.class);
        var taskMapper = mock(org.dromara.warm.flow.orm.mapper.FlowTaskMapper.class);
        var common = mock(org.dromara.workflow.service.IFlwCommonService.class);
        var actions = new FlwTaskServiceImpl(tasks, instances, null, null, null, taskMapper,
            null, null, null, null, null, null, common, null);
        var task = new FlowTask().setInstanceId(3L).setDefinitionId(4L);
        task.setId(8L);
        var instance = new org.dromara.warm.flow.orm.entity.FlowInstance().setFlowStatus("waiting");
        when(tasks.getById(8L)).thenReturn(task);
        when(taskMapper.selectById(8L)).thenReturn(task);
        when(instances.getById(3L)).thenReturn(instance);
        when(common.applyNodeCode(4L)).thenReturn("apply");
        var workflow = mock(org.dromara.warm.flow.core.workflow.WorkflowServiceImpl.class);
        var outcome = new org.dromara.warm.flow.core.workflow.result.WorkflowResult();
        outcome.setSuccess(success);
        when(workflow.reject(any())).thenReturn(outcome);
        when(workflow.terminate(any())).thenReturn(outcome);
        var back = new org.dromara.workflow.domain.bo.BackProcessBo();
        back.setTaskId(8L);
        back.setNodeCode("apply");
        back.setMessage("补充材料");
        try (var engine = mockStatic(FlowEngine.class); var login = mockStatic(org.dromara.common.satoken.utils.LoginHelper.class)) {
            engine.when(FlowEngine::workflow).thenReturn(workflow);
            login.when(org.dromara.common.satoken.utils.LoginHelper::getUserIdStr).thenReturn("7");
            boolean rejected = actions.backProcess(back);
            boolean terminated = actions.terminationTask(new FlowTerminationBo(8L, "已撤项"));
            verify(workflow).reject(argThat(c -> c.getTaskId() == 8L && "apply".equals(c.getTargetNodeCode())
                && "7".equals(c.getOperator().getHandler()) && "补充材料".equals(c.getMessage())));
            verify(workflow).terminate(argThat(c -> c.getTaskId() == 8L && "7".equals(c.getOperator().getHandler())
                && "已撤项".equals(c.getMessage())));
            assertAll(() -> assertEquals(success, rejected), () -> assertEquals(success, terminated));
        }
    }

    @Test
    void replacesOnlyExistingTasksApprovers() {
        var ids = List.of(4L, 9L, 99L);
        FlowTask first = new FlowTask();
        first.setId(4L);
        FlowTask second = new FlowTask();
        second.setId(9L);
        doReturn(List.of(first, second)).when(service).selectByIdList(ids);
        var users = mock(UserService.class);
        try (var engine = mockStatic(FlowEngine.class)) {
            engine.when(FlowEngine::userService).thenReturn(users);
            assertTrue(service.updateAssignee(ids, "27"));
            verify(users).deleteByTaskIds(List.of(4L, 9L));
            verify(users).saveBatch(argThat(rows -> rows.size() == 2
                && rows.stream().map(User::getAssociated).toList().equals(List.of(4L, 9L))
                && rows.stream().allMatch(user -> "27".equals(user.getProcessedBy())
                && TaskAssigneeType.APPROVER.getCode().equals(user.getType()))));
        }
    }

    @Test
    void emptyTaskListCannotChangeUsersOrSendReminders() {
        assertFalse(service.updateAssignee(List.of(), "27"));
        assertFalse(service.urgeTask(new FlowUrgeTaskBo()));
        var request = new FlowUrgeTaskBo();
        request.setTaskIdList(List.of(99L));
        assertFalse(service.urgeTask(request));
    }

    @Test
    void missingTaskCannotBeTerminatedOrPreviewed() {
        var next = new FlowNextNodeBo();
        next.setTaskId(99L);
        assertThrows(ServiceException.class, () -> service.getNextNodeList(next));
        assertThrows(ServiceException.class, () -> service.terminationTask(new FlowTerminationBo(99L, "终止")));
        assertNull(service.selectById(99L));
    }
}

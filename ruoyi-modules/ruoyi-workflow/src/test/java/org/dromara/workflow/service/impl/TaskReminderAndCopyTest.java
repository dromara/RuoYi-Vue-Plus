package org.dromara.workflow.service.impl;

import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.domain.bo.FlowCopyBo;
import org.dromara.workflow.domain.bo.FlowUrgeTaskBo;
import org.dromara.workflow.service.IFlwCommonService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskReminderAndCopyTest {
    private final TaskService tasks = mock(TaskService.class);
    private final UserService users = mock(UserService.class);
    private final IFlwCommonService messages = mock(IFlwCommonService.class);
    private final FlwTaskServiceImpl service = new FlwTaskServiceImpl(tasks, null, null, null, null,
        null, null, users, null, null, null, null, messages, null);

    @Test
    void reminderUsesExistingTasksAndDeduplicatesSharedAssignees() {
        var relations = mock(org.dromara.warm.flow.core.service.UserService.class);
        var request = new FlowUrgeTaskBo();
        request.setTaskIdList(List.of(11L, 99L, 12L));
        request.setMessage("请复核");
        request.setMessageType(List.of("1", "2"));
        when(tasks.getById(11L)).thenReturn(new FlowTask().setId(11L));
        when(tasks.getById(12L)).thenReturn(new FlowTask().setId(12L));
        var recipients = List.of(new UserDTO(), new UserDTO());
        when(users.selectListByIds(Set.of(7L, 8L))).thenReturn(recipients);
        try (var engine = mockStatic(FlowEngine.class); var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            engine.when(FlowEngine::userService).thenReturn(relations);
            assertFalse(service.urgeTask(request));
            verifyNoInteractions(messages);
            when(relations.getByAssociateds(List.of(11L, 12L))).thenReturn(List.of(
                new FlowUser().setAssociated(11L).setProcessedBy("7"),
                new FlowUser().setAssociated(12L).setProcessedBy("7"),
                new FlowUser().setAssociated(12L).setProcessedBy("8")));
            assertTrue(service.urgeTask(request));
            verify(messages).sendMessage(List.of("1", "2"), "请复核", "单据审批提醒", recipients, "/task/taskWaiting");
        }
    }

    @Test
    void copyOnlyNotifiesNewRecipientsReturnedByEngine() {
        var task = new FlowTask().setId(11L);
        var first = new FlowCopyBo();
        first.setUserId(7L);
        first.setNickName("张三");
        var second = new FlowCopyBo();
        second.setUserId(8L);
        second.setNickName("李四");
        service.setCopy(task, List.of());
        verifyNoInteractions(tasks, messages);
        var recipients = List.of(new UserDTO());
        when(users.selectListByIds(List.of(8L))).thenReturn(recipients);
        var copy = new FlowHisTask().setCollaborator("8");
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("2");
            when(tasks.copy(eq(11L), eq(List.of("7", "8")), any())).thenReturn(copy, null);
            service.setCopy(task, List.of(first, second));
            service.setCopy(task, List.of(first, second));
            verify(messages).sendMessage(List.of("1"), "您收到一条新的流程抄送，请及时查看。", "单据抄送提醒",
                recipients, "/task/taskCopyList");
            verifyNoMoreInteractions(messages);
        }
    }
}

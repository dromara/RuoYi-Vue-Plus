package org.dromara.workflow.service.impl;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.enums.UserType;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskReadAccessTest {
    @ParameterizedTest
    @ValueSource(strings = {"admin", "list", "edit", "approver", "copy", "initiator", "stranger"})
    void participantListUsesTaskAndCopyHistoryRelationships(String role) {
        var tasks = mock(TaskService.class);
        var instances = mock(InsService.class);
        var history = mock(HisTaskService.class);
        var relations = mock(UserService.class);
        var users = mock(org.dromara.system.api.UserService.class);
        var service = new FlwTaskServiceImpl(tasks, instances, null, null, null, null, null, users,
            null, null, null, null, null, null);
        FlowTask task = new FlowTask().setInstanceId(7L);
        task.setId(12L);
        when(tasks.getById(12L)).thenReturn(task);
        var reviewer = new FlowUser().setProcessedBy(role.equals("approver") ? "4" : "9")
            .setType(TaskAssigneeType.APPROVER.getCode());
        when(relations.getByAssociateds(List.of(12L))).thenReturn(List.of(reviewer));
        var copy = new FlowHisTask();
        copy.setId(15L);
        when(history.list(any())).thenReturn(List.of(copy));
        when(relations.getByAssociateds(List.of(15L))).thenReturn(List.of(new FlowUser()
            .setProcessedBy(role.equals("copy") ? "4" : "8").setType(UserType.COPY.getKey())));
        var instance = new FlowInstance();
        instance.setCreateBy(role.equals("initiator") ? "4" : "6");
        when(instances.getById(7L)).thenReturn(instance);
        try (var engine = mockStatic(FlowEngine.class); var login = mockStatic(LoginHelper.class);
             var permissions = mockStatic(StpUtil.class)) {
            engine.when(FlowEngine::newHisTask).thenAnswer(call -> new FlowHisTask());
            engine.when(FlowEngine::userService).thenReturn(relations);
            engine.when(FlowEngine::hisTaskService).thenReturn(history);
            login.when(LoginHelper::isSuperAdmin).thenReturn(role.equals("admin"));
            login.when(LoginHelper::getUserIdStr).thenReturn("4");
            permissions.when(() -> StpUtil.hasPermission("workflow:task:list")).thenReturn(role.equals("list"));
            permissions.when(() -> StpUtil.hasPermission("workflow:task:edit")).thenReturn(role.equals("edit"));
            if (role.equals("stranger")) {
                assertThrows(NotPermissionException.class, () -> service.currentTaskAllUser(List.of(12L)));
                verifyNoInteractions(users);
            } else {
                service.currentTaskAllUser(List.of(12L));
                verify(users).selectListByIds(java.util.Set.of(role.equals("approver") ? 4L : 9L));
            }
        }
    }
}

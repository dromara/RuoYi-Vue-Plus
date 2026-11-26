package org.dromara.workflow.liteflow.operation;

import org.dromara.workflow.domain.bo.TaskOperationBo;
import org.dromara.workflow.domain.context.TaskOperationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TaskOperationNotificationTest {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(org.dromara.workflow.common.enums.TaskOperationEnum.class)
    void notifiesOnlyTheRecipientsOfTheExecutedCommand(org.dromara.workflow.common.enums.TaskOperationEnum operation) {
        var users = mock(org.dromara.system.api.UserService.class);
        var messages = mock(org.dromara.workflow.service.IFlwCommonService.class);
        var request = new TaskOperationBo();
        request.setUserId("8");
        request.setUserIds(List.of("13", "21"));
        request.setMessage("审批变更");
        request.setMessageType(List.of("1"));
        var context = new TaskOperationContext(request, operation.getCode());
        context.setOperation(operation);
        List<Long> expectedIds = switch (operation) {
            case DELEGATE_TASK, TRANSFER_TASK -> List.of(8L);
            case ADD_SIGNATURE, REDUCTION_SIGNATURE -> List.of(13L, 21L);
        };
        var recipients = expectedIds.stream().map(id -> {
            var user = new org.dromara.system.api.domain.UserDTO();
            user.setUserId(id);
            return user;
        }).toList();
        when(users.selectListByIds(expectedIds)).thenReturn(recipients);
        var component = spy(new TaskOpNotifyComponent(messages, users));
        doReturn(context).when(component).getContextBean(TaskOperationContext.class);
        component.process();
        verify(users).selectListByIds(expectedIds);
        verify(messages).sendMessage(eq(request.getMessageType()), eq("审批变更"), contains(operation.getDesc()),
            eq(recipients), eq(org.dromara.workflow.common.constant.FlowConstant.PATH_TASK_WAITING));
        verifyNoMoreInteractions(messages);
    }

    @ParameterizedTest
    @CsvSource({"true,true,true", "true,false,false", "false,true,false", "false,false,false"})
    void onlySuccessfulOperationWithChannelSendsNotification(boolean success, boolean channel, boolean notify) {
        TaskOperationBo request = new TaskOperationBo();
        request.setMessageType(channel ? List.of("1") : List.of());
        TaskOperationContext context = new TaskOperationContext(request, "transfer");
        context.setResult(success);
        var component = spy(new TaskOpNeedNotifyComponent());
        doReturn(context).when(component).getContextBean(TaskOperationContext.class);
        assertEquals(notify, component.processBoolean());
    }
}

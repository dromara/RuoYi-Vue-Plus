package org.dromara.workflow.service.impl;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.ThreadUtils;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.mail.core.MailBuilder;
import org.dromara.system.api.MessageService;
import org.dromara.system.api.domain.UserDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class ResultChannelTest {
    @Test
    void parallelTasksNotifySharedAssigneeOnceEvenWithRepeatedChannelSelection() {
        var messages = mock(MessageService.class);
        var service = new FlwCommonServiceImpl(messages);
        var tasks = mock(IFlwTaskService.class);
        var first = new FlowTask();
        first.setId(11L);
        var second = new FlowTask();
        second.setId(12L);
        var assignee = new UserDTO();
        assignee.setUserId(7L);
        when(tasks.selectByInstId(31L)).thenReturn(List.of(first, second));
        when(tasks.currentTaskAllUser(List.of(11L, 12L))).thenReturn(List.of(assignee, assignee));
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(IFlwTaskService.class)).thenReturn(tasks);
            service.sendMessage("请假", 31L, List.of("1", "1"), "请复核");
        }
        verify(messages).publishMessage(eq(List.of(7L)), argThat(payload ->
            "/task/taskWaiting".equals(payload.getPath()) && "请复核".equals(payload.getMessage())));
        verifyNoMoreInteractions(messages);
    }

    @Test
    void delayedNotificationForInstanceWithNoRemainingTasksDoesNotReachFormerAssignees() {
        var messages = mock(MessageService.class);
        var service = new FlwCommonServiceImpl(messages);
        var tasks = mock(IFlwTaskService.class);
        when(tasks.selectByInstId(31L)).thenReturn(List.of());
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(IFlwTaskService.class)).thenReturn(tasks);
            service.sendMessage("请假", 31L, List.of("1"), "请复核");
        }
        verifyNoInteractions(messages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reviewer@example.org", " "})
    void resultUsesDocumentRouteAndOnlySendsEmailToAvailableAddresses(String address) {
        var messages = mock(MessageService.class);
        var service = new FlwCommonServiceImpl(messages);
        var user = new UserDTO();
        user.setUserId(7L);
        user.setEmail(address);
        MailBuilder builder = mock(RETURNS_SELF);
        try (var mail = mockStatic(MailBuilder.class); var threads = mockStatic(ThreadUtils.class)) {
            mail.when(MailBuilder::of).thenReturn(builder);
            threads.when(() -> ThreadUtils.virtualInvokeAll(any(Runnable[].class))).thenAnswer(call -> {
                for (Runnable task : (Runnable[]) call.getRawArguments()[0]) {
                    task.run();
                }
                return null;
            });
            service.sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of(), List.of(user));
            service.sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of("1"), List.of());
            threads.verifyNoInteractions();
            service.sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of("1", "2"), List.of(user, user));
            verify(messages).publishMessage(eq(List.of(7L)), argThat(payload ->
                "/task/myDocument".equals(payload.getPath()) && payload.getMessage().contains("请假")
                    && payload.getMessage().contains("已完成")));
            if (address.isBlank()) {
                mail.verifyNoInteractions();
            } else {
                verify(builder).to(Set.of(address));
                verify(builder).subject("单据审批提醒");
                verify(builder).text(contains("请假"));
                verify(builder).send();
            }
        }
    }
}

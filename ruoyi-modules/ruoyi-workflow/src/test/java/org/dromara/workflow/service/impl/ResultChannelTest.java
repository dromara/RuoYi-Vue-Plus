package org.dromara.workflow.service.impl;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.ThreadUtils;
import org.dromara.common.mail.core.MailBuilder;
import org.dromara.system.api.MessageService;
import org.dromara.system.api.domain.UserDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class ResultChannelTest {
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

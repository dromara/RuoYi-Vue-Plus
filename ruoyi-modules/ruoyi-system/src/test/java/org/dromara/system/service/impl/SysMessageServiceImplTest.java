package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.push.helper.PushHelper;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.dromara.system.mapper.SysMessageMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SysMessageServiceImplTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void messageBoxInsertFailureStopsDelivery(boolean broadcast) {
        var mapper = mock(SysMessageMapper.class);
        var service = new SysMessageServiceImpl(mapper);
        var payload = PushPayloadDTO.of("message", "backend", "提醒", null);
        payload.setMessageId(7L);
        try (var spring = mockStatic(SpringUtil.class); var push = mockStatic(PushHelper.class)) {
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            assertThrows(ServiceException.class, () -> {
                if (broadcast) {
                    service.publishAll(payload);
                } else {
                    service.publishMessage(List.of(42L), payload);
                }
            });
            push.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void targetedMessagesRequireRecipients(List<Long> recipients) {
        var mapper = mock(SysMessageMapper.class);
        var service = new SysMessageServiceImpl(mapper);
        var payload = PushPayloadDTO.of("message", "backend", "提醒", null);
        payload.setMessageId(7L);
        try (var spring = mockStatic(SpringUtil.class); var push = mockStatic(PushHelper.class)) {
            assertSame(payload, service.storeUsers(recipients, payload));
            service.publishMessage(recipients, payload);
            verifyNoInteractions(mapper);
            push.verifyNoInteractions();
        }
    }
}

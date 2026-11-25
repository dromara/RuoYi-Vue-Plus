package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.service.DictService;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.MessageService;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import org.dromara.system.domain.bo.SysNoticeBo;
import org.dromara.system.domain.vo.SysMessageBoxVo;
import org.dromara.system.service.ISysMessageService;
import org.dromara.system.service.ISysNoticeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemMessageControllerTest {

    @Test
    void broadcastsPersistedNoticeAndItsDetailLink() {
        var notices = mock(ISysNoticeService.class);
        var dictionary = mock(DictService.class);
        var broadcast = mock(MessageService.class);
        var notice = new SysNoticeBo();
        notice.setNoticeTitle("维护");
        notice.setNoticeType("2");
        notice.setNoticeContent("周末升级");
        notice.setStatus("0");
        when(notices.insertNotice(notice)).thenAnswer(invocation -> {
            notice.setNoticeId(31L);
            return 1;
        });
        when(dictionary.getDictLabel("sys_notice_type", "2")).thenReturn("公告");

        var response = new SysNoticeController(notices, dictionary, broadcast).add(notice);

        assertTrue(R.isSuccess(response));
        var payload = ArgumentCaptor.forClass(PushPayloadDTO.class);
        verify(broadcast).publishAll(payload.capture());
        assertEquals("[公告] 维护", payload.getValue().getMessage());
        assertEquals("/system/notice?noticeId=31", payload.getValue().getPath());
        assertEquals(Map.of("noticeId", 31L, "noticeType", "2", "noticeTypeLabel", "公告",
            "noticeTitle", "维护", "noticeContent", "周末升级", "status", "0"), payload.getValue().getData());
    }

    @Test
    void failedNoticeInsertSuppressesBroadcast() {
        var notices = mock(ISysNoticeService.class);
        var dictionary = mock(DictService.class);
        var broadcast = mock(MessageService.class);
        var notice = new SysNoticeBo();
        when(notices.insertNotice(notice)).thenReturn(0);

        var response = new SysNoticeController(notices, dictionary, broadcast).add(notice);

        assertTrue(R.isError(response));
        verify(notices).insertNotice(notice);
        verifyNoInteractions(broadcast);
    }

    @Test
    void returnsTheSignedInUsersMessageBox() {
        var messages = mock(ISysMessageService.class);
        var inbox = new SysMessageBoxVo();
        when(messages.queryMessageBox(21L)).thenReturn(inbox);

        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(21L);
            assertSame(inbox, new SysMessageController(messages).getBox().getData());
        }
    }
}

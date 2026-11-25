package org.dromara.system.controller.system;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAdministrationControllerTest {
    private final ISysUserService users = mock();
    private final SysUserController controller = new SysUserController(users, mock(), mock(), mock());

    @Test
    void missingAccountCannotBeUnlocked() {
        try (var beans = mockStatic(SpringUtil.class); var cache = mockStatic(RedisUtils.class)) {
            assertTrue(R.isError(controller.unlock(32L)));
            cache.verifyNoInteractions();
        }
    }

    @Test
    void unlockingTwiceOnlyRemovesTheSelectedAccountsCounterOnce() {
        var account = new SysUserVo();
        account.setUserName("alice");
        when(users.selectUserById(32L)).thenReturn(account);
        String key = CacheNames.PWD_ERR_CNT_KEY + "alice";
        try (var beans = mockStatic(SpringUtil.class); var cache = mockStatic(RedisUtils.class)) {
            cache.when(() -> RedisUtils.hasKey(key)).thenReturn(true, false);
            assertTrue(R.isSuccess(controller.unlock(32L)));
            assertTrue(R.isSuccess(controller.unlock(32L)));
            cache.verify(() -> RedisUtils.hasKey(key), times(2));
            cache.verify(() -> RedisUtils.deleteObject(key));
            cache.verifyNoMoreInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void resetsPasswordAfterBothAccountChecks(int rows) {
        var request = new SysUserBo();
        request.setUserId(32L);
        request.setPassword("new-password");
        when(users.resetUserPwd(eq(32L), anyString())).thenReturn(rows);

        assertEquals(rows > 0, R.isSuccess(controller.resetPwd(request)));
        var hash = ArgumentCaptor.forClass(String.class);
        var order = inOrder(users);
        order.verify(users).checkUserAllowed(32L);
        order.verify(users).checkUserDataScope(32L);
        order.verify(users).resetUserPwd(eq(32L), hash.capture());
        assertTrue(BCrypt.checkpw("new-password", hash.getValue()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void updatesRequestedAccountStatusAfterBothChecks(int rows) {
        var request = new SysUserBo();
        request.setUserId(32L);
        request.setStatus("1");
        when(users.updateUserStatus(32L, "1")).thenReturn(rows);
        assertEquals(rows > 0, R.isSuccess(controller.changeStatus(request)));
        var order = inOrder(users);
        order.verify(users).checkUserAllowed(32L);
        order.verify(users).checkUserDataScope(32L);
        order.verify(users).updateUserStatus(32L, "1");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectedAccountCheckPreventsPasswordAndStatusWrites(boolean scopeDenied) {
        var request = new SysUserBo();
        request.setUserId(32L);
        request.setPassword("new-password");
        request.setStatus("1");
        var denied = new ServiceException("禁止修改此用户");
        if (scopeDenied) {
            doThrow(denied).when(users).checkUserDataScope(32L);
        } else {
            doThrow(denied).when(users).checkUserAllowed(32L);
        }
        assertSame(denied, assertThrows(ServiceException.class, () -> controller.resetPwd(request)));
        assertSame(denied, assertThrows(ServiceException.class, () -> controller.changeStatus(request)));
        verify(users, never()).resetUserPwd(anyLong(), anyString());
        verify(users, never()).updateUserStatus(anyLong(), anyString());
    }
}

package org.dromara.web.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.LoginType;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.web.service.SysLoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordAuthStrategyTest {

    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-password";
    private static final String HASH = BCrypt.hashpw(PASSWORD);

    private final SysLoginService loginService = mock(SysLoginService.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class, RETURNS_DEEP_STUBS);
    private final PasswordAuthStrategy strategy = new PasswordAuthStrategy(null, loginService, userMapper);

    private enum Account { MISSING, DISABLED, ACTIVE }

    private SysUserVo givenAccount(Account account) {
        SysUserVo user = null;
        if (account != Account.MISSING) {
            user = new SysUserVo();
            user.setStatus(account == Account.DISABLED ? SystemConstants.DISABLE : SystemConstants.NORMAL);
            user.setPassword(HASH);
        }
        LambdaCrudChainWrapper<SysUser, SysUserVo> query = userMapper.lambda();
        doReturn(query).when(query).eq(any(), any());
        when(query.voOne()).thenReturn(user);
        return user;
    }

    private void checkWithRetryPolicy() {
        doAnswer(call -> {
            Supplier<Boolean> failed = call.getArgument(2);
            if (failed.get()) {
                throw new UserException("user.password.retry.limit.count", 1);
            }
            return null;
        }).when(loginService).checkLogin(eq(LoginType.PASSWORD), eq(USERNAME), any());
    }

    @ParameterizedTest
    @EnumSource(Account.class)
    void failedCredentialsPerformOnePasswordCheck(Account account) {
        givenAccount(account);
        checkWithRetryPolicy();
        String input = account == Account.ACTIVE ? "wrong-password" : PASSWORD;
        try (var bcrypt = mockStatic(BCrypt.class, CALLS_REAL_METHODS)) {
            var error = assertThrows(UserException.class, () -> strategy.verifyPassword(USERNAME, input));
            assertEquals("user.password.not.match", error.getCode());
            bcrypt.verify(() -> BCrypt.checkpw(eq(input), anyString()));
        }
    }

    @Test
    void missingUserCannotAuthenticateEvenIfTheDummyHashMatches() {
        givenAccount(Account.MISSING);
        checkWithRetryPolicy();
        try (var bcrypt = mockStatic(BCrypt.class)) {
            bcrypt.when(() -> BCrypt.checkpw(eq(PASSWORD), anyString())).thenReturn(true);
            var error = assertThrows(UserException.class, () -> strategy.verifyPassword(USERNAME, PASSWORD));
            assertEquals("user.password.not.match", error.getCode());
        }
    }

    @Test
    void activeUserWithCorrectPasswordPasses() {
        var user = givenAccount(Account.ACTIVE);
        checkWithRetryPolicy();
        assertSame(user, strategy.verifyPassword(USERNAME, PASSWORD));
    }

    @Test
    void lockedAccountKeepsTheRetryPolicyFastPath() {
        givenAccount(Account.ACTIVE);
        doThrow(new UserException("user.password.retry.limit.exceed", 5, 10))
            .when(loginService).checkLogin(eq(LoginType.PASSWORD), eq(USERNAME), any());
        try (var bcrypt = mockStatic(BCrypt.class)) {
            var error = assertThrows(UserException.class, () -> strategy.verifyPassword(USERNAME, PASSWORD));
            assertEquals("user.password.not.match", error.getCode());
            bcrypt.verifyNoInteractions();
        }
    }
}

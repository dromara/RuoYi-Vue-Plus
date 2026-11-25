package org.dromara.system.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.domain.RoleDTO;
import org.dromara.system.mapper.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionCleanupRulesTest {

    @Test
    void noOnlineTokensMeansNoSessionsAreTouched() {
        var service = new SysRoleServiceImpl(mock(), mock(), mock(), mock());
        try (var sessions = mockStatic(StpUtil.class); var login = mockStatic(LoginHelper.class)) {
            sessions.when(() -> StpUtil.searchTokenValue("", 0, -1, false)).thenReturn(List.of());
            service.cleanOnlineUser(List.of(18L));
            sessions.verify(() -> StpUtil.logoutByTokenValue(anyString()), never());
            login.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource({"user,match,true","user,other,false","user,expired,false","user,missing,false",
        "role,match,true","role,other,false","role,expired,false","role,missing,false","role,noRoles,false"})
    void logsOutOnlyMatchingActiveSessions(String scope, String state, boolean logout) {
        var relations = mock(SysUserRoleMapper.class, RETURNS_DEEP_STUBS);
        var query = relations.lambda();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        var service = new SysRoleServiceImpl(mock(), mock(), relations, mock());
        var user = new LoginUser();
        user.setUserId(state.equals("other") ? 10L : 8L);
        var role = new RoleDTO();
        role.setRoleId(state.equals("other") ? 99L : 3L);
        user.setRoles(state.equals("noRoles") ? List.of() : List.of(role));
        var logic = mock(StpLogic.class);
        when(logic.getTokenActiveTimeoutByToken("phone")).thenReturn(state.equals("expired") ? -2L : -1L);
        var original = StpUtil.stpLogic;
        try (var sessions = mockStatic(StpUtil.class); var login = mockStatic(LoginHelper.class)) {
            StpUtil.stpLogic = logic;
            sessions.when(() -> StpUtil.searchTokenValue("", 0, -1, false)).thenReturn(List.of("token:phone"));
            login.when(() -> LoginHelper.getLoginUser("phone")).thenReturn(state.equals("missing") ? null : user);
            if (scope.equals("role")) {
                service.cleanOnlineUserByRole(3L);
            } else {
                service.cleanOnlineUser(List.of(8L));
            }
            sessions.verify(() -> StpUtil.logoutByTokenValue("phone"), times(logout ? 1 : 0));
        } finally {
            StpUtil.stpLogic = original;
        }
    }
}

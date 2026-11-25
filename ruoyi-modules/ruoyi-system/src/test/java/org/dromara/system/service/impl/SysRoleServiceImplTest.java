package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.event.OnlineUserCleanEvent;
import org.springframework.context.ApplicationContext;
import java.util.List;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysRoleServiceImplTest {

    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysRoleMenuMapper menus = mock(SysRoleMenuMapper.class, RETURNS_DEEP_STUBS);
    private final SysRoleDeptMapper depts = mock(SysRoleDeptMapper.class, RETURNS_DEEP_STUBS);
    private final SysUserRoleMapper users = mock(SysUserRoleMapper.class);
    private final SysRoleServiceImpl service = new SysRoleServiceImpl(roles, menus, users, depts);
    private final SysRoleBo bo = new SysRoleBo();
    private final SysRole role = new SysRole();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void emptyUserSelectionChangesNoAssignments(boolean grant) {
        var context = mock(ApplicationContext.class);
        try (var login = mockStatic(LoginHelper.class); var spring = mockStatic(SpringUtils.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            spring.when(SpringUtils::context).thenReturn(context);
            var ids = List.<Long>of();
            int rows = grant ? service.insertAuthUsers(9L, ids) : service.deleteAuthUsers(9L, ids);
            assertEquals(0, rows);
            verifyNoInteractions(users, context);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void grantsNotifySelectedUsersOnlyAfterSuccessfulWrite(boolean saved) {
        var context = mock(ApplicationContext.class);
        var ids = List.of(12L, 13L);
        when(users.insertBatch(anyList())).thenReturn(saved);
        try (var login = mockStatic(LoginHelper.class); var spring = mockStatic(SpringUtils.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            spring.when(SpringUtils::context).thenReturn(context);
            assertEquals(saved ? 2 : 0, service.insertAuthUsers(9L, ids));
            if (saved) {
                verify(context).publishEvent(OnlineUserCleanEvent.byUsers(ids));
            } else {
                verifyNoInteractions(context);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void selectionContainingCurrentAccountRejectsEntireChange(boolean grant) {
        var ids = List.of(12L, 13L);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(13L);
            assertThrows(ServiceException.class, () -> {
                if (grant) {
                    service.insertAuthUsers(9L, ids);
                } else {
                    service.deleteAuthUsers(9L, ids);
                }
            });
            verifyNoInteractions(users);
        }
    }

    @Test
    void zeroInsertedRolesCannotCreateMenuLinks() {
        bo.setMenuIds(new Long[0]);
        assertEquals(0, save(true));
        verifyNoInteractions(menus);
    }

    @Test
    void zeroUpdatedRolesCannotReplaceLinks() {
        bo.setMenuIds(new Long[0]);
        bo.setDeptIds(new Long[0]);
        assertEquals(0, save(false));
        verifyNoInteractions(menus, depts);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failedLinkBatchAbortsPermissionUpdate(boolean failMenu) {
        bo.setMenuIds(new Long[]{5L});
        bo.setDeptIds(new Long[]{6L});
        when(roles.updateById(role)).thenReturn(1);
        when(menus.insertBatch(anyList())).thenReturn(!failMenu);
        when(depts.insertBatch(anyList())).thenReturn(failMenu);
        assertThrows(ServiceException.class, () -> save(false));
        if (failMenu) {
            verifyNoInteractions(depts);
        }
    }

    private int save(boolean create) {
        var menuQuery = menus.lambda();
        var deptQuery = depts.lambda();
        when(menuQuery.eq(any(), any())).thenReturn(menuQuery);
        when(deptQuery.eq(any(), any())).thenReturn(deptQuery);
        clearInvocations(menus, depts);
        role.setRoleId(2L);
        bo.setRoleId(2L);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysRole.class)).thenReturn(role);
            return create ? service.insertRole(bo) : service.updateRolePermission(bo);
        }
    }
}

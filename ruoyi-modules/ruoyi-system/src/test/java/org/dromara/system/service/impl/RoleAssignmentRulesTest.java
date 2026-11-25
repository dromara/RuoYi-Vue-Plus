package org.dromara.system.service.impl;

import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.mapper.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleAssignmentRulesTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void basicInformationUpdateLeavesPermissionLinksAlone(boolean assigned) {
        var menus = mock(SysRoleMenuMapper.class);
        var depts = mock(SysRoleDeptMapper.class);
        var svc = spy(new SysRoleServiceImpl(roles, menus, mock(), depts));
        var request = new SysRoleBo();
        var role = new org.dromara.system.domain.SysRole();
        role.setRoleId(8L);
        role.setStatus(SystemConstants.DISABLE);
        doReturn(assigned ? 1L : 0L).when(svc).countUserRoleByRoleId(8L);
        when(roles.updateById(role)).thenReturn(1);
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class);
             var mapping = mockStatic(org.dromara.common.core.utils.MapstructUtils.class)) {
            mapping.when(() -> org.dromara.common.core.utils.MapstructUtils.convert(request, org.dromara.system.domain.SysRole.class))
                .thenReturn(role);
            if (assigned) {
                assertThrows(ServiceException.class, () -> svc.updateRoleBaseInfo(request));
                verify(roles, never()).updateById(role);
            } else {
                assertEquals(1, svc.updateRoleBaseInfo(request));
            }
            verifyNoInteractions(menus, depts);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void assignedRolePreventsBatchDeletion(boolean assigned) {
        var rolesService = spy(service);
        var ids = List.of(41L, 42L);
        doNothing().when(rolesService).checkRoleDataScope(ids);
        var role = new org.dromara.system.domain.SysRole();
        role.setRoleId(42L);
        when(roles.selectByIds(ids)).thenReturn(List.of(role));
        doNothing().when(rolesService).checkRoleAllowed(any());
        doReturn(assigned ? 1L : 0L).when(rolesService).countUserRoleByRoleId(42L);
        if (assigned) {
            assertThrows(ServiceException.class, () -> rolesService.deleteRoleByIds(ids));
            verify(roles, never()).deleteByIds(ids);
        } else {
            var roleMenus = mock(SysRoleMenuMapper.class, RETURNS_DEEP_STUBS);
            var roleDepartments = mock(SysRoleDeptMapper.class, RETURNS_DEEP_STUBS);
            var deletable = spy(new SysRoleServiceImpl(roles, roleMenus, mock(), roleDepartments));
            doNothing().when(deletable).checkRoleDataScope(ids);
            doNothing().when(deletable).checkRoleAllowed(any());
            doReturn(0L).when(deletable).countUserRoleByRoleId(42L);
            var menus = roleMenus.lambda();
            var departments = roleDepartments.lambda();
            when(menus.in(any(), eq(ids))).thenReturn(menus);
            when(departments.in(any(), eq(ids))).thenReturn(departments);
            when(roles.deleteByIds(ids)).thenReturn(1);
            assertEquals(1, deletable.deleteRoleByIds(ids));
            verify(menus).delete();
            verify(departments).delete();
        }
    }

    private final SysRoleMapper roles = mock();
    private final SysRoleServiceImpl service = new SysRoleServiceImpl(roles, mock(), mock(), mock());

    @Test
    void splitsAndDeduplicatesRoleKeys() {
        var first = new SysRoleVo();
        first.setRoleKey("finance,review");
        var second = new SysRoleVo();
        second.setRoleKey("review");
        when(roles.selectRolesByUserId(8L)).thenReturn(List.of(first, second));
        assertEquals(Set.of("finance", "review"), service.selectRolePermissionByUserId(8L));
        assertEquals(Set.of(), service.selectRolePermissionByUserId(99L));
    }

    @Test
    void rejectsChangesToBuiltInRoleAndReservedKey() {
        var role = new SysRoleBo();
        role.setRoleId(SystemConstants.SUPER_ADMIN_ROLE_ID);
        assertThrows(ServiceException.class, () -> service.checkRoleAllowed(role));
        role.setRoleId(null);
        role.setRoleKey(SystemConstants.SUPER_ADMIN_ROLE_KEY);
        assertThrows(ServiceException.class, () -> service.checkRoleAllowed(role));
        verifyNoInteractions(roles);
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2})
    void requiresEveryRequestedRoleToBeVisible(int visible) {
        var ids = List.of(9L, 19L);
        when(roles.selectRoleCount(ids)).thenReturn((long) visible);
        try (var login = mockStatic(LoginHelper.class)) {
            if (visible == 2) {
                assertDoesNotThrow(() -> service.checkRoleDataScope(ids));
            } else {
                assertThrows(ServiceException.class, () -> service.checkRoleDataScope(ids));
            }
        }
    }
}

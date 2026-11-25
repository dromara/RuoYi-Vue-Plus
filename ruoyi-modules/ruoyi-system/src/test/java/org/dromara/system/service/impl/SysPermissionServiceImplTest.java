package org.dromara.system.service.impl;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.api.domain.RoleDTO;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysMenuService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysPermissionServiceImplTest {

    @org.junit.jupiter.api.Test
    void emptyRoleSelectionHasNoDataScopePermissions() {
        var menus = mock(org.dromara.system.service.ISysMenuService.class);
        var permissions = new SysPermissionServiceImpl(mock(), menus);
        org.junit.jupiter.api.Assertions.assertTrue(permissions.getDataScopeRoleMap(java.util.List.of()).isEmpty());
        verifyNoInteractions(menus);
    }
    private final ISysRoleService roles = mock();
    private final ISysMenuService menus = mock();
    private final SysPermissionServiceImpl service = new SysPermissionServiceImpl(roles, menus);

    @Test
    void adminReceivesBuiltInPermissions() {
        assertEquals(Set.of(SystemConstants.SUPER_ADMIN_ROLE_KEY), service.getRolePermission(SystemConstants.SUPER_ADMIN_USER_ID));
        assertEquals(Set.of("*:*:*"), service.getMenuPermission(SystemConstants.SUPER_ADMIN_USER_ID));
        verifyNoInteractions(roles, menus);
    }

    @Test
    void ordinaryUserReceivesAssignedRolesAndMenus() {
        when(roles.selectRolePermissionByUserId(21L)).thenReturn(Set.of("reviewer"));
        when(menus.selectMenuPermsByUserId(21L)).thenReturn(Set.of("record:read"));
        assertEquals(Set.of("reviewer"), service.getRolePermission(21L));
        assertEquals(Set.of("record:read"), service.getMenuPermission(21L));
    }

    @Test
    void sharedPermissionIncludesBothRoles() {
        var first = new RoleDTO();
        first.setRoleId(2L);
        var second = new RoleDTO();
        second.setRoleId(3L);
        when(menus.selectMenuPermsByRoleIds(List.of(2L, 3L))).thenReturn(Map.of(
            2L, Set.of("read"), 3L, Set.of("read", "edit")));
        var result = service.getDataScopeRoleMap(List.of(first, second));
        assertEquals(Set.of(2L, 3L), Set.copyOf(result.get("read")));
        assertEquals(List.of(3L), result.get("edit"));
    }
}

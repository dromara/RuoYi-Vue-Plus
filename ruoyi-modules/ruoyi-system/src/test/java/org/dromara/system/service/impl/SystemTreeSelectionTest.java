package org.dromara.system.service.impl;

import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.SysRole;
import org.dromara.system.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemTreeSelectionTest {

    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysMenuMapper menus = mock(SysMenuMapper.class);
    private final SysDeptMapper departments = mock(SysDeptMapper.class);
    private final SysMenuServiceImpl menuService = new SysMenuServiceImpl(menus, roles, mock(SysRoleMenuMapper.class));
    private final SysDeptServiceImpl deptService = new SysDeptServiceImpl(departments, roles, mock(SysUserMapper.class));

    @Test
    void deletedRoleHasNoCheckedNodes() {
        assertAll(
            () -> assertEquals(List.of(), menuService.selectMenuListByRoleId(42L)),
            () -> assertEquals(List.of(), deptService.selectDeptListByRoleId(42L))
        );
        verifyNoInteractions(menus, departments);
    }

    @ParameterizedTest
    @CsvSource({" , true", "true, true", "false, false"})
    void roleTreeSelectionUsesConfiguredLinkageOrDatabaseDefault(Boolean configured, boolean expected) {
        var role = new SysRole();
        role.setMenuCheckStrictly(configured);
        role.setDeptCheckStrictly(configured);
        when(roles.selectById(42L)).thenReturn(role);
        when(menus.selectMenuListByRoleId(42L, expected)).thenReturn(List.of(1L));
        when(departments.selectDeptListByRoleId(42L, expected)).thenReturn(List.of(2L));
        assertAll(
            () -> assertEquals(List.of(1L), menuService.selectMenuListByRoleId(42L)),
            () -> assertEquals(List.of(2L), deptService.selectDeptListByRoleId(42L))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"C", "M"})
    void topLevelEmbeddedLinkBuildsNavigableChild(String type) {
        var menu = new SysMenu();
        menu.setParentId(0L);
        menu.setMenuId(7L);
        menu.setMenuType(type);
        menu.setPath("https://docs.example.com");
        menu.setIsFrame("N");
        menu.setIsCache("N");
        menu.setActiveMenu("/help");
        menu.setQueryParam("{\"tab\":1}");

        var parent = menuService.buildMenus(List.of(menu)).getFirst();
        var child = parent.getChildren().getFirst();
        assertAll(
            () -> assertEquals("/", parent.getPath()),
            () -> assertEquals("docs/example/com", child.getPath()),
            () -> assertEquals("InnerLink", child.getComponent()),
            () -> assertEquals(menu.getPath(), child.getMeta().getLink()),
            () -> assertEquals(Boolean.TRUE, child.getMeta().getNoCache()),
            () -> assertEquals("/help", child.getMeta().getActiveMenu()),
            () -> assertEquals(menu.getQueryParam(), child.getQuery())
        );
    }
}

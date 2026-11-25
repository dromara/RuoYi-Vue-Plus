package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.bo.SysMenuBo;
import org.dromara.system.mapper.SysMenuMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysMenuHierarchyTest {

    private final SysMenuMapper menus = mock();
    private final SysMenuServiceImpl service = new SysMenuServiceImpl(menus, mock(SysRoleMapper.class), mock(SysRoleMenuMapper.class));

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingParentRejectsWrite(boolean create) {
        assertThrows(ServiceException.class, () -> save(99L, create));
        verify(menus, never()).insert(any(SysMenu.class));
        verify(menus, never()).updateById(any(SysMenu.class));
    }

    @Test
    void cannotMoveMenuUnderItsGrandchild() {
        parent(32L, 31L);
        parent(31L, 20L);
        parent(20L, 0L);
        assertThrows(ServiceException.class, () -> save(32L, false));
        verify(menus, never()).updateById(any(SysMenu.class));
    }

    @Test
    void cannotAttachToExistingCycle() {
        parent(31L, 32L);
        parent(32L, 31L);
        assertThrows(ServiceException.class, () -> save(32L, false));
        verify(menus, never()).updateById(any(SysMenu.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 32L})
    void canMoveToRootOrIndependentBranch(long parentId) {
        parent(32L, 31L);
        parent(31L, 0L);
        when(menus.updateById(any(SysMenu.class))).thenReturn(1);
        assertEquals(1, save(parentId, false));
    }

    private void parent(long id, long parentId) {
        var menu = new SysMenu();
        menu.setMenuId(id);
        menu.setParentId(parentId);
        when(menus.selectById(id)).thenReturn(menu);
    }

    private int save(long parentId, boolean create) {
        var bo = new SysMenuBo();
        bo.setMenuId(create ? null : 20L);
        bo.setParentId(parentId);
        var entity = new SysMenu();
        entity.setMenuId(bo.getMenuId());
        entity.setParentId(parentId);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysMenu.class)).thenReturn(entity);
            return create ? service.insertMenu(bo) : service.updateMenu(bo);
        }
    }
}

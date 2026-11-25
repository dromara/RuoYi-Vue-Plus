package org.dromara.system.controller.system;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.system.domain.bo.SysMenuBo;
import org.dromara.system.service.ISysMenuService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuMaintenanceControllerTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void savesValidExternalMenu(boolean create) {
        var menu = new SysMenuBo();
        menu.setMenuId(11L);
        menu.setParentId(0L);
        menu.setPath("https://example.org");
        menu.setIsFrame(SystemConstants.YES);
        when(menus.checkMenuNameUnique(menu)).thenReturn(true);
        when(menus.checkRouteConfigUnique(menu)).thenReturn(true);
        when(menus.insertMenu(menu)).thenReturn(1);
        when(menus.updateMenu(menu)).thenReturn(1);
        assertTrue(R.isSuccess(create ? controller.add(menu) : controller.edit(menu)));
        verify(menus, times(create ? 1 : 0)).insertMenu(menu);
        verify(menus, times(create ? 0 : 1)).updateMenu(menu);
    }

    private final ISysMenuService menus = mock();
    private final SysMenuController controller = new SysMenuController(menus);

    @ParameterizedTest
    @CsvSource({"true,name,菜单名称已存在","false,name,菜单名称已存在",
        "true,path,地址必须以http(s)://开头","false,path,地址必须以http(s)://开头",
        "true,route,路由名称或地址已存在","false,route,路由名称或地址已存在","false,self,上级菜单不能选择自己"})
    void invalidMenuCannotBeSaved(boolean create, String scenario, String reason) {
        var menu = new SysMenuBo();
        menu.setMenuId(23L);
        menu.setParentId(scenario.equals("self") ? 23L : 1L);
        menu.setMenuName("采购");
        menu.setIsFrame(scenario.equals("path") ? SystemConstants.YES : SystemConstants.NO);
        menu.setPath("purchase");
        when(menus.checkMenuNameUnique(menu)).thenReturn(!scenario.equals("name"));
        when(menus.checkRouteConfigUnique(menu)).thenReturn(!scenario.equals("route"));
        var result = create ? controller.add(menu) : controller.edit(menu);
        assertTrue(R.isError(result));
        assertTrue(result.getMsg().endsWith(reason));
        verify(menus, never()).insertMenu(any());
        verify(menus, never()).updateMenu(any());
    }

    @Test
    void assignedMenuCannotBeDeletedIndividually() {
        when(menus.checkMenuExistRole(23L)).thenReturn(true);
        assertTrue(R.isError(controller.remove(23L)));
        verify(menus, never()).deleteMenuById(23L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void batchDeletionRespectsRemainingChildren(boolean hasChildren) {
        var ids = List.of(23L, 24L);
        when(menus.hasChildByMenuId(ids)).thenReturn(hasChildren);
        assertEquals(!hasChildren, R.isSuccess(controller.remove(new Long[]{23L, 24L})));
        verify(menus, times(hasChildren ? 0 : 1)).deleteMenuById(ids);
    }
}

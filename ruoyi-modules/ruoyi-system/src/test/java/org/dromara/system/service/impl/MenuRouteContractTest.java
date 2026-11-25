package org.dromara.system.service.impl;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.SysMenu;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuRouteContractTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void treeUsesAccountScopedMenuSource(boolean admin) {
        var mapper = mock(org.dromara.system.mapper.SysMenuMapper.class);
        long id = admin ? SystemConstants.SUPER_ADMIN_USER_ID : 23L;
        var service = new SysMenuServiceImpl(mapper, mock(), mock());
        assertTrue(service.selectMenuTreeByUserId(id).isEmpty());
        if (admin) {
            verify(mapper).selectMenuTreeAll();
        } else {
            verify(mapper).selectMenuTreeByUserId(id);
        }
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void directoryAndChildMenuPreserveNavigationMetadata() {
        var directory = new SysMenu();
        directory.setMenuId(9L);
        directory.setParentId(0L);
        directory.setMenuType(SystemConstants.TYPE_DIR);
        directory.setPath("sales");
        directory.setIsFrame(SystemConstants.NO);
        var child = new SysMenu();
        child.setMenuId(10L);
        child.setParentId(9L);
        child.setPath("orders");
        child.setMenuType(SystemConstants.TYPE_MENU);
        child.setComponent("sales/orders");
        child.setMenuName("订单");
        child.setIsFrame(SystemConstants.NO);
        child.setIsCache(SystemConstants.NO);
        child.setVisible("1");
        child.setQueryParam("stage=open");
        directory.setChildren(List.of(child));
        var router = new SysMenuServiceImpl(mock(), mock(), mock()).buildMenus(List.of(directory)).getFirst();
        assertEquals("/sales", router.getPath());
        assertEquals(SystemConstants.LAYOUT, router.getComponent());
        assertEquals("noRedirect", router.getRedirect());
        var route = router.getChildren().getFirst();
        assertEquals("Orders10", route.getName());
        assertEquals("orders", route.getPath());
        assertEquals("sales/orders", route.getComponent());
        assertEquals("stage=open", route.getQuery());
        assertEquals("订单", route.getMeta().getTitle());
        assertTrue(route.getHidden());
    }

    @Test
    void nestedDirectoryWithoutExplicitComponentUsesParentView() {
        var directory = new SysMenu();
        directory.setParentId(1L);
        directory.setMenuType(SystemConstants.TYPE_DIR);
        directory.setPath("child");
        directory.setIsFrame(SystemConstants.NO);
        assertEquals(SystemConstants.PARENT_VIEW, directory.getComponentInfo());
    }
}

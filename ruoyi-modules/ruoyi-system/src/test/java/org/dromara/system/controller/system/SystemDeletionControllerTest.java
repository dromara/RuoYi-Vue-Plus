package org.dromara.system.controller.system;

import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.system.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemDeletionControllerTest {

    @Test
    void menuWithChildrenCannotBeDeleted() {
        var menus = mock(ISysMenuService.class);
        when(menus.hasChildByMenuId(15L)).thenReturn(true);

        var response = new SysMenuController(menus).remove(15L);

        assertEquals("存在子菜单,不允许删除", response.getMsg());
        verify(menus, never()).deleteMenuById(15L);
    }

    @Test
    void defaultDepartmentCannotBeDeleted() {
        var departments = mock(ISysDeptService.class);
        var response = new SysDeptController(departments, mock()).remove(SystemConstants.DEFAULT_DEPT_ID);

        assertEquals("默认部门,不允许删除", response.getMsg());
        verifyNoInteractions(departments);
    }

    @Test
    void reportsFailureWhenValidatedOssDeletionFails() {
        var service = mock(ISysOssService.class);
        var ids = List.of(11L, 14L);
        when(service.deleteWithValidByIds(ids, true)).thenReturn(false);

        var response = new SysOssController(service).remove(new Long[]{11L, 14L});

        assertTrue(R.isError(response));
        verify(service).deleteWithValidByIds(ids, true);
    }

    @Test
    void reportsFailureWhenValidatedOssConfigDeletionFails() {
        var service = mock(ISysOssConfigService.class);
        var ids = List.of(11L, 14L);
        when(service.deleteWithValidByIds(ids, true)).thenReturn(false);

        var response = new SysOssConfigController(service).remove(new Long[]{11L, 14L});

        assertTrue(R.isError(response));
        verify(service).deleteWithValidByIds(ids, true);
    }
}

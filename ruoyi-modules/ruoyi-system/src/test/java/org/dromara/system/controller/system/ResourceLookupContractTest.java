package org.dromara.system.controller.system;

import org.dromara.system.domain.bo.*;
import org.dromara.system.domain.vo.*;
import org.dromara.system.service.*;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceLookupContractTest {

    @Test
    void clientStatusEndpointUsesPublicClientIdAndAffectedRows() {
        var clients = mock(ISysClientService.class);
        var request = new SysClientBo();
        request.setId(8L);
        request.setClientId("public-id");
        request.setStatus("1");
        when(clients.updateClientStatus("public-id","1")).thenReturn(0,1);
        var controller = new SysClientController(clients);
        assertTrue(org.dromara.common.core.domain.R.isError(controller.changeStatus(request)));
        assertTrue(org.dromara.common.core.domain.R.isSuccess(controller.changeStatus(request)));
    }

    @Test
    void returnsConfigDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysConfigService.class);
        var controller = new SysConfigController(service);
        var row = new SysConfigVo();
        when(service.selectConfigById(42L)).thenReturn(row);
        var criteria = new SysConfigBo();
        var paging = new PageQuery(5, 3);
        when(service.selectPageConfigList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsClientDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysClientService.class);
        var controller = new SysClientController(service);
        var row = new SysClientVo();
        when(service.queryById(42L)).thenReturn(row);
        var criteria = new SysClientBo();
        var paging = new PageQuery(5, 3);
        when(service.queryPageList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsDictDataDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysDictDataService.class);
        var controller = new SysDictDataController(service, mock());
        var row = new SysDictDataVo();
        when(service.selectDictDataById(42L)).thenReturn(row);
        var criteria = new SysDictDataBo();
        var paging = new PageQuery(5, 3);
        when(service.selectPageDictDataList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsDictTypeDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysDictTypeService.class);
        var controller = new SysDictTypeController(service);
        var row = new SysDictTypeVo();
        when(service.selectDictTypeById(42L)).thenReturn(row);
        var criteria = new SysDictTypeBo();
        var paging = new PageQuery(5, 3);
        when(service.selectPageDictTypeList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsNoticeDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysNoticeService.class);
        var controller = new SysNoticeController(service, mock(), mock());
        var row = new SysNoticeVo();
        when(service.selectNoticeById(42L)).thenReturn(row);
        var criteria = new SysNoticeBo();
        var paging = new PageQuery(5, 3);
        when(service.selectPageNoticeList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsRoleDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysRoleService.class);
        var controller = new SysRoleController(service, mock(), mock());
        var row = new SysRoleVo();
        when(service.selectRoleById(42L)).thenReturn(row);
        var criteria = new SysRoleBo();
        var paging = new PageQuery(5, 3);
        when(service.selectPageRoleList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsPostDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysPostService.class);
        var controller = new SysPostController(service, mock());
        var row = new SysPostVo();
        when(service.selectPostById(42L)).thenReturn(row);
        var criteria = new SysPostBo();
        var paging = new PageQuery(5, 3);
        when(service.selectPagePostList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }

    @Test
    void returnsOssConfigDetailsAndPreservesFilteredPageTotal() {
        var service = mock(ISysOssConfigService.class);
        var controller = new SysOssConfigController(service);
        var row = new SysOssConfigVo();
        when(service.queryById(42L)).thenReturn(row);
        var criteria = new SysOssConfigBo();
        var paging = new PageQuery(5, 3);
        when(service.queryPageList(criteria, paging)).thenReturn(PageResult.build(List.of(row), 18L));
        assertSame(row, controller.getInfo(42L).getData());
        var page = controller.list(criteria, paging).getData();
        assertEquals(List.of(row), page.getRows());
        assertEquals(18L, page.getTotal());
    }
}

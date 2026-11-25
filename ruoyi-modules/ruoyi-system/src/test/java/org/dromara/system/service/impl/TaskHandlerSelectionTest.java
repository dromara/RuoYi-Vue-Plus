package org.dromara.system.service.impl;

import org.dromara.system.api.model.TaskAssigneeBody;
import org.dromara.system.domain.bo.*;
import org.dromara.system.domain.vo.*;
import org.dromara.system.service.*;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskHandlerSelectionTest {

    @Test
    void selectsRoleHandlersWithinRequestedPageAndFilters() {
        var source = mock(ISysRoleService.class);
        var service = new SysTaskAssigneeServiceImpl(mock(), mock(), mock(), source);
        var request = new TaskAssigneeBody();
        request.setPageSize(5);
        request.setPageNum(3);
        request.setHandlerCode("operations");
        request.setHandlerName("采购");
        request.setGroupId("8");
        request.setBeginTime("2026-02-01");
        request.setEndTime("2026-02-28");
        var item = new SysRoleVo();
        item.setRoleId(45L);
        item.setRoleKey("operations");
        item.setRoleName("采购");
        when(source.selectPageRoleList(any(), any())).thenReturn(PageResult.build(List.of(item), 17L));

        var result = service.selectRolesByTaskAssigneeList(request);

        var filter = ArgumentCaptor.forClass(SysRoleBo.class);
        var page = ArgumentCaptor.forClass(PageQuery.class);
        verify(source).selectPageRoleList(filter.capture(), page.capture());
        assertEquals(3, page.getValue().getPageNum());
        assertEquals(5, page.getValue().getPageSize());
        var query = filter.getValue();
        assertEquals("operations", query.getRoleKey());
        assertEquals("采购", query.getRoleName());
        assertEquals("0", query.getStatus());
        assertEquals("2026-02-01", query.getParams().get("beginTime"));
        assertEquals("2026-02-28", query.getParams().get("endTime"));
        assertEquals(17L, result.getTotal());
        assertEquals(1, result.getList().size());
        var handler = result.getList().getFirst();
        assertEquals("45", handler.getStorageId());
        assertEquals("operations", handler.getHandlerCode());
        assertEquals("采购", handler.getHandlerName());
        assertEquals("", handler.getGroupName());
    }

    @Test
    void selectsDeptHandlersWithinRequestedPageAndFilters() {
        var source = mock(ISysDeptService.class);
        var service = new SysTaskAssigneeServiceImpl(mock(), source, mock(), mock());
        var request = new TaskAssigneeBody();
        request.setPageSize(5);
        request.setPageNum(3);
        request.setHandlerCode("operations");
        request.setHandlerName("采购");
        request.setGroupId("8");
        request.setBeginTime("2026-02-01");
        request.setEndTime("2026-02-28");
        var item = new SysDeptVo();
        item.setDeptId(45L);
        item.setDeptCategory("operations");
        item.setDeptName("采购");
        item.setParentId(8L);
        when(source.selectPageDeptList(any(), any())).thenReturn(PageResult.build(List.of(item), 17L));

        var result = service.selectDeptsByTaskAssigneeList(request);

        var filter = ArgumentCaptor.forClass(SysDeptBo.class);
        var page = ArgumentCaptor.forClass(PageQuery.class);
        verify(source).selectPageDeptList(filter.capture(), page.capture());
        assertEquals(3, page.getValue().getPageNum());
        assertEquals(5, page.getValue().getPageSize());
        var query = filter.getValue();
        assertEquals("operations", query.getDeptCategory());
        assertEquals("采购", query.getDeptName());
        assertEquals("0", query.getStatus());
        assertEquals("2026-02-01", query.getParams().get("beginTime"));
        assertEquals("2026-02-28", query.getParams().get("endTime"));
        assertEquals(8L, query.getBelongDeptId());
        assertEquals(17L, result.getTotal());
        assertEquals(1, result.getList().size());
        var handler = result.getList().getFirst();
        assertEquals("45", handler.getStorageId());
        assertEquals("operations", handler.getHandlerCode());
        assertEquals("采购", handler.getHandlerName());
        assertEquals("8", handler.getGroupName());
    }

    @Test
    void selectsUserHandlersWithinRequestedPageAndFilters() {
        var source = mock(ISysUserService.class);
        var service = new SysTaskAssigneeServiceImpl(mock(), mock(), source, mock());
        var request = new TaskAssigneeBody();
        request.setPageSize(5);
        request.setPageNum(3);
        request.setHandlerCode("operations");
        request.setHandlerName("采购");
        request.setGroupId("8");
        request.setBeginTime("2026-02-01");
        request.setEndTime("2026-02-28");
        var item = new SysUserVo();
        item.setUserId(45L);
        item.setUserName("operations");
        item.setNickName("采购");
        item.setDeptId(8L);
        when(source.selectPageUserList(any(), any())).thenReturn(PageResult.build(List.of(item), 17L));

        var result = service.selectUsersByTaskAssigneeList(request);

        var filter = ArgumentCaptor.forClass(SysUserBo.class);
        var page = ArgumentCaptor.forClass(PageQuery.class);
        verify(source).selectPageUserList(filter.capture(), page.capture());
        assertEquals(3, page.getValue().getPageNum());
        assertEquals(5, page.getValue().getPageSize());
        var query = filter.getValue();
        assertEquals("operations", query.getUserName());
        assertEquals("采购", query.getNickName());
        assertEquals("0", query.getStatus());
        assertEquals("2026-02-01", query.getParams().get("beginTime"));
        assertEquals("2026-02-28", query.getParams().get("endTime"));
        assertEquals(8L, query.getDeptId());
        assertEquals(17L, result.getTotal());
        assertEquals(1, result.getList().size());
        var handler = result.getList().getFirst();
        assertEquals("45", handler.getStorageId());
        assertEquals("operations", handler.getHandlerCode());
        assertEquals("采购", handler.getHandlerName());
        assertEquals("8", handler.getGroupName());
    }

    @Test
    void selectsPostHandlersWithinRequestedPageAndFilters() {
        var source = mock(ISysPostService.class);
        var service = new SysTaskAssigneeServiceImpl(source, mock(), mock(), mock());
        var request = new TaskAssigneeBody();
        request.setPageSize(5);
        request.setPageNum(3);
        request.setHandlerCode("operations");
        request.setHandlerName("采购");
        request.setGroupId("8");
        request.setBeginTime("2026-02-01");
        request.setEndTime("2026-02-28");
        var item = new SysPostVo();
        item.setPostId(45L);
        item.setPostCategory("operations");
        item.setPostName("采购");
        item.setDeptId(8L);
        when(source.selectPagePostList(any(), any())).thenReturn(PageResult.build(List.of(item), 17L));

        var result = service.selectPostsByTaskAssigneeList(request);

        var filter = ArgumentCaptor.forClass(SysPostBo.class);
        var page = ArgumentCaptor.forClass(PageQuery.class);
        verify(source).selectPagePostList(filter.capture(), page.capture());
        assertEquals(3, page.getValue().getPageNum());
        assertEquals(5, page.getValue().getPageSize());
        var query = filter.getValue();
        assertEquals("operations", query.getPostCategory());
        assertEquals("采购", query.getPostName());
        assertEquals("0", query.getStatus());
        assertEquals("2026-02-01", query.getParams().get("beginTime"));
        assertEquals("2026-02-28", query.getParams().get("endTime"));
        assertEquals(8L, query.getBelongDeptId());
        assertEquals(17L, result.getTotal());
        assertEquals(1, result.getList().size());
        var handler = result.getList().getFirst();
        assertEquals("45", handler.getStorageId());
        assertEquals("operations", handler.getHandlerCode());
        assertEquals("采购", handler.getHandlerName());
        assertEquals("8", handler.getGroupName());
    }
}

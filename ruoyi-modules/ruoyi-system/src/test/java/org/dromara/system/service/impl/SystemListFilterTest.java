package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.dromara.system.domain.SysClient;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.domain.bo.SysClientBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.mapper.SysClientMapper;
import org.dromara.system.domain.SysDictData;
import org.dromara.system.domain.SysNotice;
import org.dromara.system.domain.SysOperLog;
import org.dromara.system.domain.bo.SysDictDataBo;
import org.dromara.system.domain.bo.SysNoticeBo;
import org.dromara.system.domain.bo.SysOperLogBo;
import org.dromara.system.mapper.SysDictDataMapper;
import org.dromara.system.mapper.SysNoticeMapper;
import org.dromara.system.mapper.SysOperLogMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemListFilterTest {

    @Test
    void filteringBySpecificOperationAndSelectionAppliesBothConstraints() {
        var mapper = mock(SysOperLogMapper.class);
        var filter = new SysOperLogBo();
        filter.setBusinessType(2);
        filter.setBusinessTypes(new Integer[]{2, 3});
        new SysOperLogServiceImpl(mapper).selectOperLogList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysOperLog>> captured = ArgumentCaptor.captor();
        verify(mapper).selectVoList(captured.capture());
        String sql = captured.getValue().getSqlSegment();
        assertTrue(sql.contains("business_type =") && sql.contains("business_type IN"), sql);
    }

    @Test
    void batchDeletingMenusDeletesOnlySelectedMenuLinks() {
        var mapper = mock(org.dromara.system.mapper.SysMenuMapper.class);
        var links = mock(org.dromara.system.mapper.SysRoleMenuMapper.class);
        var service = new SysMenuServiceImpl(mapper, mock(), links);
        var ids = List.of(5L,6L);
        service.deleteMenuById(ids);
        verify(mapper).deleteByIds(ids);
        verify(links).deleteByMenuIds(ids);
    }

    @Test
    void filteredConfigSearchUsesKeyNameAndType() {
        var mapper = mock(org.dromara.system.mapper.SysConfigMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "config"),
            org.dromara.system.domain.SysConfig.class);
        var bo = new org.dromara.system.domain.bo.SysConfigBo();
        bo.setConfigKey("theme");
        bo.setConfigName("颜色");
        bo.setConfigType("N");
        new SysConfigServiceImpl(mapper).selectConfigList(bo);
        ArgumentCaptor<LambdaQueryWrapper<org.dromara.system.domain.SysConfig>> wrapper = ArgumentCaptor.captor();
        verify(mapper).selectVoList(wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment();
        assertTrue(sql.contains("config_key LIKE") && sql.contains("config_name LIKE") && sql.contains("config_type ="), sql);
        assertTrue(wrapper.getValue().getParamNameValuePairs().values().containsAll(List.of("%theme%", "%颜色%", "N")));
    }

    @Test
    void postMapperFiltersUserMembershipAtTheJoin() {
        var mapper = mock(org.dromara.system.mapper.SysPostMapper.class, CALLS_REAL_METHODS);
        for (var type : List.of(org.dromara.system.domain.SysPost.class, org.dromara.system.domain.SysUserPost.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
        var rows = List.of(new org.dromara.system.domain.vo.SysPostVo());
        doReturn(rows).when(mapper).selectJoinList(eq(org.dromara.system.domain.vo.SysPostVo.class), any());
        assertSame(rows, mapper.selectPostsByUserId(18L));
        ArgumentCaptor<MPJLambdaWrapper<org.dromara.system.domain.SysPost>> query = ArgumentCaptor.captor();
        verify(mapper).selectJoinList(eq(org.dromara.system.domain.vo.SysPostVo.class), query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("sup.user_id ="));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue(18L));
    }

    @Test
    void menuBatchChecksOnlyChildrenOutsideRequestedIds() {
        var mapper = mock(org.dromara.system.mapper.SysMenuMapper.class);
        var type = org.dromara.system.domain.SysMenu.class;
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "menu"), type);
        when(mapper.currentModelClass()).thenReturn(type);
        var query = new org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper<
            org.dromara.system.domain.SysMenu, org.dromara.system.domain.vo.SysMenuVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        when(mapper.exists(query)).thenReturn(false);
        var ids = List.of(5L, 6L);
        assertFalse(new SysMenuServiceImpl(mapper, mock(), mock()).hasChildByMenuId(ids));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("parent_id IN") && sql.contains("menu_id NOT IN"), sql);
        var values = new java.util.ArrayList<>(query.getParamNameValuePairs().values());
        assertEquals(2, java.util.Collections.frequency(values, 5L));
        assertEquals(2, java.util.Collections.frequency(values, 6L));
    }

    @Test
    void departmentPostDirectoryFiltersOnRequestedDepartment() {
        var mapper = mock(org.dromara.system.mapper.SysPostMapper.class);
        var type = org.dromara.system.domain.SysPost.class;
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "posts"), type);
        when(mapper.currentModelClass()).thenReturn(type);
        var query = new org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper<
            org.dromara.system.domain.SysPost, org.dromara.system.domain.vo.SysPostVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var row = new org.dromara.system.domain.SysPost();
        row.setPostId(91L);
        when(mapper.selectList(query)).thenReturn(List.of(row));
        var service = new SysPostServiceImpl(mapper, mock(), mock());
        assertEquals(91L, service.selectListByDeptId(8L).getFirst().getPostId());
        assertTrue(query.getSqlSegment().contains("dept_id ="));
        assertTrue(query.getParamNameValuePairs().containsValue(8L));
    }

    @Test
    void menuPermissionsDeduplicateAndIgnoreBlankValuesWithinEachRole() {
        var mapper = mock(org.dromara.system.mapper.SysMenuMapper.class, CALLS_REAL_METHODS);
        var first = new org.dromara.system.domain.vo.SysRoleMenuPermVo();
        first.setRoleId(4L);
        first.setPerms("order:view");
        var blank = new org.dromara.system.domain.vo.SysRoleMenuPermVo();
        blank.setRoleId(5L);
        blank.setPerms("  ");
        doReturn(List.of(first, blank, first)).when(mapper)
            .selectJoinList(eq(org.dromara.system.domain.vo.SysRoleMenuPermVo.class), any());
        for (var type : List.of(org.dromara.system.domain.SysMenu.class, SysRole.class,
                org.dromara.system.domain.SysRoleMenu.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
        assertEquals(java.util.Map.of(4L, java.util.Set.of("order:view")), mapper.selectMenuPermsByRoleIds(List.of(4L,5L)));
        assertTrue(mapper.selectMenuPermsByRoleIds(List.of()).isEmpty());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true,false})
    void roleMenuTreeOmitsParentIdsOnlyForLinkedSelection(boolean linked) {
        var mapper = mock(org.dromara.system.mapper.SysMenuMapper.class, CALLS_REAL_METHODS);
        for (var type : List.of(org.dromara.system.domain.SysMenu.class, SysRole.class,
                org.dromara.system.domain.SysRoleMenu.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
        var root = new org.dromara.system.domain.SysMenu();
        root.setMenuId(11L);
        root.setParentId(0L);
        var child = new org.dromara.system.domain.SysMenu();
        child.setMenuId(12L);
        child.setParentId(11L);
        doReturn(List.of(root, child)).when(mapper).selectJoinList(eq(org.dromara.system.domain.SysMenu.class), any());
        var ids = mapper.selectMenuListByRoleId(41L, linked);
        assertEquals(linked ? List.of(12L) : List.of(11L, 12L), ids);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true,false})
    void roleDeptTreeOmitsParentIdsOnlyForLinkedSelection(boolean linked) {
        var mapper = mock(org.dromara.system.mapper.SysDeptMapper.class, CALLS_REAL_METHODS);
        for (var type : List.of(org.dromara.system.domain.SysDept.class, SysRole.class,
                org.dromara.system.domain.SysRoleDept.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
        var root = new org.dromara.system.domain.SysDept();
        root.setDeptId(11L);
        root.setParentId(0L);
        var child = new org.dromara.system.domain.SysDept();
        child.setDeptId(12L);
        child.setParentId(11L);
        doReturn(List.of(root, child)).when(mapper).selectJoinList(eq(org.dromara.system.domain.SysDept.class), any());
        var ids = mapper.selectDeptListByRoleId(41L, linked);
        assertEquals(linked ? List.of(12L) : List.of(11L, 12L), ids);
    }

    @Test
    void noticeSearchCombinesCreatorTitleTypeAndStatus() {
        var notices = mock(SysNoticeMapper.class);
        var authors = mock(SysUserMapper.class, RETURNS_DEEP_STUBS);
        var query = authors.lambda();
        when(query.eq(any(), eq("alice"))).thenReturn(query);
        var author = new SysUserVo();
        author.setUserId(51L);
        when(query.voOne()).thenReturn(author);
        var filter = new SysNoticeBo();
        filter.setCreateByName("alice");
        filter.setNoticeTitle("维护");
        filter.setNoticeType("2");
        filter.setStatus("0");
        new SysNoticeServiceImpl(notices, authors).selectNoticeList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysNotice>> wrapper = ArgumentCaptor.captor();
        verify(notices).selectVoList(wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment();
        for (String predicate : List.of("create_by =", "notice_title LIKE", "notice_type =", "status =")) {
            assertTrue(sql.contains(predicate), sql);
        }
        assertTrue(wrapper.getValue().getParamNameValuePairs().values().containsAll(List.of(51L, "%维护%", "2", "0")));
    }

    @Test
    void operationExportFiltersTypesAndTimeWindow() {
        var mapper = mock(SysOperLogMapper.class);
        var filter = new SysOperLogBo();
        filter.setBusinessTypes(new Integer[]{1,2});
        filter.getParams().put("beginTime", "2026-01-01");
        filter.getParams().put("endTime", "2026-02-01");
        new SysOperLogServiceImpl(mapper).selectOperLogList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysOperLog>> captured = ArgumentCaptor.captor();
        verify(mapper).selectVoList(captured.capture());
        String sql = captured.getValue().getSqlSegment();
        assertTrue(sql.contains("business_type IN"), sql);
        assertTrue(sql.contains("oper_time BETWEEN"), sql);
        assertTrue(sql.contains("ORDER BY oper_id DESC"), sql);
        assertTrue(captured.getValue().getParamNameValuePairs().values().containsAll(List.of(1, 2)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void roleAssignmentPagesRestrictTheCorrectSideOfTheJoin(boolean allocated) {
        for (var type : List.of(SysUserRole.class, SysRole.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
        var mapper = mock(SysUserMapper.class, CALLS_REAL_METHODS);
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUserVo>(2, 7);
        var filter = new SysUserBo();
        filter.setRoleId(41L);
        doReturn(page).when(mapper).selectJoinPage(eq(page), eq(SysUserVo.class), any());
        if (allocated) {
            mapper.selectAllocatedList(page, filter);
        } else {
            mapper.selectUnallocatedList(page, filter, List.of(15L, 19L));
        }
        ArgumentCaptor<MPJLambdaWrapper<SysUser>> captured = ArgumentCaptor.captor();
        verify(mapper).selectJoinPage(eq(page), eq(SysUserVo.class), captured.capture());
        String sql = captured.getValue().getSqlSegment();
        assertTrue(sql.contains(allocated ? "r.role_id =" : "u.user_id NOT IN"), sql);
        assertTrue(captured.getValue().getParamNameValuePairs().values()
            .containsAll(allocated ? List.of(41L) : List.of(15L, 19L)));
    }

    @BeforeAll
    static void initializeMetadata() {
        for (var type : List.of(SysDictData.class, SysNotice.class, SysOperLog.class, SysClient.class, SysUser.class, SysDept.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
    }

    @Test
    void otherOperationsAreFilteredByZeroBusinessType() {
        var mapper = mock(SysOperLogMapper.class);
        var filter = new SysOperLogBo();
        filter.setBusinessType(0);
        new SysOperLogServiceImpl(mapper).selectOperLogList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysOperLog>> query = ArgumentCaptor.captor();
        verify(mapper).selectVoList(query.capture());
        assertFilter(query.getValue(), "business_type", 0);
    }

    @Test
    void closedNoticeFilterRestrictsListToClosedNotices() {
        var mapper = mock(SysNoticeMapper.class);
        var filter = new SysNoticeBo();
        filter.setStatus("1");
        new SysNoticeServiceImpl(mapper, mock(SysUserMapper.class)).selectNoticeList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysNotice>> query = ArgumentCaptor.captor();
        verify(mapper).selectVoList(query.capture());
        assertFilter(query.getValue(), "status", "1");
    }

    @Test
    void dictionaryValueFilterUsesExactKey() {
        var mapper = mock(SysDictDataMapper.class);
        var filter = new SysDictDataBo();
        filter.setDictValue("pending");
        new SysDictDataServiceImpl(mapper).selectDictDataList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysDictData>> query = ArgumentCaptor.captor();
        verify(mapper).selectVoList(query.capture());
        assertFilter(query.getValue(), "dict_value", "pending");
    }

    @Test
    void clientDeviceSelectionRestrictsExport() {
        var mapper = mock(SysClientMapper.class);
        var filter = new SysClientBo();
        filter.setDeviceType("android");
        new SysClientServiceImpl(mapper).queryList(filter);
        ArgumentCaptor<LambdaQueryWrapper<SysClient>> query = ArgumentCaptor.captor();
        verify(mapper).selectVoList(query.capture());
        assertFilter(query.getValue(), "device_type", "android");
    }

    @Test
    void userExportHonorsIncludedAndExcludedAccounts() {
        var mapper = mock(SysUserMapper.class, CALLS_REAL_METHODS);
        var filter = new SysUserBo();
        filter.setUserId(7L);
        filter.setUserIds("7,8");
        filter.setExcludeUserIds("9,10");
        doReturn(List.of()).when(mapper).selectJoinList(eq(SysUserExportVo.class), any());
        mapper.selectUserExportList(filter, null);
        ArgumentCaptor<MPJLambdaWrapper<SysUser>> query = ArgumentCaptor.captor();
        verify(mapper).selectJoinList(eq(SysUserExportVo.class), query.capture());
        var wrapper = query.getValue();
        String sql = wrapper.getSqlSegment();
        assertAll(
            () -> assertTrue(sql.contains("u.user_id ="), sql),
            () -> assertTrue(sql.contains("u.user_id IN"), sql),
            () -> assertTrue(sql.contains("u.user_id NOT IN"), sql),
            () -> assertTrue(wrapper.getParamNameValuePairs().values().containsAll(List.of(7L, 8L, 9L, 10L)))
        );
    }

    private static void assertFilter(LambdaQueryWrapper<?> query, String column, Object value) {
        String sql = query.getSqlSegment();
        assertAll(
            () -> assertTrue(sql.contains(column + " ="), sql),
            () -> assertTrue(query.getParamNameValuePairs().containsValue(value))
        );
    }
}

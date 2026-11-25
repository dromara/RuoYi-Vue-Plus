package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.system.domain.*;
import org.dromara.system.domain.vo.*;
import org.dromara.system.domain.bo.*;
import org.dromara.system.mapper.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectoryQueryTest {

    @Test
    void dictionaryMapperSelectsOnlyRequestedTypeInDisplayOrder() {
        var mapper = mock(SysDictDataMapper.class, CALLS_REAL_METHODS);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "dictionary"), SysDictData.class);
        doReturn(SysDictData.class).when(mapper).currentModelClass();
        var query = new LambdaCrudChainWrapper<SysDictData, SysDictDataVo>(mapper);
        doReturn(query).when(mapper).lambda();
        var rows = List.of(new SysDictDataVo());
        doReturn(rows).when(mapper).selectVoList(query);
        assertEquals(rows, mapper.selectDictDataByType("status"));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("dict_type =") && sql.contains("ORDER BY dict_sort ASC"), sql);
        assertTrue(query.getParamNameValuePairs().containsValue("status"));
    }

    @Test
    void deletingRoleMenuLinksRestrictsDeletionToSelectedMenus() {
        var mapper = mock(SysRoleMenuMapper.class, CALLS_REAL_METHODS);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "links"), SysRoleMenu.class);
        doReturn(SysRoleMenu.class).when(mapper).currentModelClass();
        var query = new LambdaCrudChainWrapper<SysRoleMenu, SysRoleMenu>(mapper);
        doReturn(query).when(mapper).lambda();
        doReturn(2).when(mapper).delete(query);
        assertEquals(2, mapper.deleteByMenuIds(List.of(7L, 9L)));
        assertTrue(query.getSqlSegment().contains("menu_id IN"));
        assertEquals(Set.of(7L, 9L), Set.copyOf(query.getParamNameValuePairs().values()));
    }

    @Test
    void departmentUserLookupSelectsOnlyNormalAccountsInRequestedDepartments() {
        var mapper = mock(SysUserMapper.class);
        var service = new SysUserServiceImpl(mapper, mock(), mock(), mock(), mock(), mock());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "users"), SysUser.class);
        when(mapper.currentModelClass()).thenReturn(SysUser.class);
        var query = new LambdaCrudChainWrapper<SysUser, SysUserVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var user = new SysUser(42L);
        when(mapper.selectList(query)).thenReturn(List.of(user));
        assertEquals(List.of(42L), service.selectUserIdsByDeptIds(List.of(7L, 8L)));
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("dept_id IN") && sql.contains("status ="), sql);
        assertEquals(Set.of(7L,8L,"0"), Set.copyOf(query.getParamNameValuePairs().values()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void departmentHierarchyIncludesParentOnlyWhenRequested(boolean includeParent) {
        var mapper = mock(SysDeptMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "dept"), SysDept.class);
        when(mapper.currentModelClass()).thenReturn(SysDept.class);
        var query = new LambdaCrudChainWrapper<SysDept, SysDeptVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var child = new SysDeptVo();
        child.setDeptId(22L);
        var parent = new SysDeptVo();
        parent.setDeptId(21L);
        when(mapper.selectDeptList(any())).thenReturn(includeParent ? List.of(parent, child) : List.of(child));
        var service = new SysDeptServiceImpl(mapper, mock(), mock());
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class);
             var db = mockStatic(org.dromara.common.mybatis.helper.DataBaseHelper.class)) {
            db.when(() -> org.dromara.common.mybatis.helper.DataBaseHelper.findInSet(21L, "ancestors"))
                .thenReturn("find_in_set('21', ancestors) <> 0");
            var ids = includeParent ? service.selectDeptAndChildIds(21L) : service.selectChildDeptIds(21L);
            assertEquals(includeParent ? List.of(21L,22L) : List.of(22L), ids);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("status ="), sql);
            assertTrue(sql.contains("find_in_set('21', ancestors)"), sql);
            assertEquals(includeParent, sql.contains("dept_id ="));
        }
    }

    @Test
    void postMembershipsReturnUniqueUserIds() {
        var mapper = mock(SysUserPostMapper.class);
        var service = new SysUserServiceImpl(mock(), mock(), mock(), mock(), mock(), mapper);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "members"), SysUserPost.class);
        when(mapper.currentModelClass()).thenReturn(SysUserPost.class);
        var query = new LambdaCrudChainWrapper<SysUserPost, SysUserPost>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var membership = new SysUserPost();
        membership.setUserId(8L);
        when(mapper.selectList(query)).thenReturn(List.of(membership, membership));
        assertEquals(List.of(8L), service.selectUserIdsByPostIds(List.of(23L, 24L)));
        assertTrue(query.getSqlSegment().contains("post_id IN"));
        assertEquals(Set.of(23L, 24L), Set.copyOf(query.getParamNameValuePairs().values()));
    }

    @Test
    void roleMembershipsReturnUniqueUserIds() {
        var mapper = mock(SysUserRoleMapper.class);
        var service = new SysUserServiceImpl(mock(), mock(), mock(), mock(), mapper, mock());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "members"), SysUserRole.class);
        when(mapper.currentModelClass()).thenReturn(SysUserRole.class);
        var query = new LambdaCrudChainWrapper<SysUserRole, SysUserRole>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var membership = new SysUserRole();
        membership.setUserId(8L);
        when(mapper.selectList(query)).thenReturn(List.of(membership, membership));
        assertEquals(List.of(8L), service.selectUserIdsByRoleIds(List.of(23L, 24L)));
        assertTrue(query.getSqlSegment().contains("role_id IN"));
        assertEquals(Set.of(23L, 24L), Set.copyOf(query.getParamNameValuePairs().values()));
    }

    @Test
    void resolvesAccountByUserName() {
        var mapper = mock(SysUserMapper.class);
        var service = new SysUserServiceImpl(mapper, mock(), mock(), mock(), mock(), mock());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "account"), SysUser.class);
        when(mapper.currentModelClass()).thenReturn(SysUser.class);
        var query = new LambdaCrudChainWrapper<SysUser, SysUserVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var user = new SysUserVo();
        when(mapper.selectVoOne(query)).thenReturn(user);
        assertSame(user, service.selectUserByUserName("alice"));
        assertTrue(query.getSqlSegment().contains("user_name ="));
        assertTrue(query.getParamNameValuePairs().containsValue("alice"));
    }

    @Test
    void resolvesAccountByPhoneNumber() {
        var mapper = mock(SysUserMapper.class);
        var service = new SysUserServiceImpl(mapper, mock(), mock(), mock(), mock(), mock());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "account"), SysUser.class);
        when(mapper.currentModelClass()).thenReturn(SysUser.class);
        var query = new LambdaCrudChainWrapper<SysUser, SysUserVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var user = new SysUserVo();
        when(mapper.selectVoOne(query)).thenReturn(user);
        assertSame(user, service.selectUserByPhoneNumber("13800000001"));
        assertTrue(query.getSqlSegment().contains("phone_number ="));
        assertTrue(query.getParamNameValuePairs().containsValue("13800000001"));
    }

    @Test
    void returnsOnlyExistingDeptNamesForRequestedIds() {
        var mapper = mock(SysDeptMapper.class);
        var service = new SysDeptServiceImpl(mapper, mock(), mock());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "names"), SysDept.class);
        when(mapper.currentModelClass()).thenReturn(SysDept.class);
        var query = new LambdaCrudChainWrapper<SysDept, SysDeptVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var entry = new SysDept();
        entry.setDeptId(13L);
        entry.setDeptName("采购");
        when(mapper.selectList(query)).thenReturn(List.of(entry));
        assertEquals(Map.of(13L, "采购"), service.selectDeptNamesByIds(List.of(13L, 14L)));
        assertTrue(query.getSqlSegment().contains("dept_id IN"));
        assertEquals(Set.of(13L, 14L), Set.copyOf(query.getParamNameValuePairs().values()));
    }

    @Test
    void returnsOnlyExistingPostNamesForRequestedIds() {
        var mapper = mock(SysPostMapper.class);
        var service = new SysPostServiceImpl(mapper, mock(), mock());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "names"), SysPost.class);
        when(mapper.currentModelClass()).thenReturn(SysPost.class);
        var query = new LambdaCrudChainWrapper<SysPost, SysPostVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var entry = new SysPost();
        entry.setPostId(13L);
        entry.setPostName("采购");
        when(mapper.selectList(query)).thenReturn(List.of(entry));
        assertEquals(Map.of(13L, "采购"), service.selectPostNamesByIds(List.of(13L, 14L)));
        assertTrue(query.getSqlSegment().contains("post_id IN"));
        assertEquals(Set.of(13L, 14L), Set.copyOf(query.getParamNameValuePairs().values()));
    }
}

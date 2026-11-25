package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysUserServiceImplTest {

    @org.junit.jupiter.api.Test
    void editingVanishedAccountRaisesBusinessFailure() {
        var mapper = mock(org.dromara.system.mapper.SysUserMapper.class);
        var service = new SysUserServiceImpl(mapper, mock(), mock(), mock(), mock(), mock());
        var bo = new org.dromara.system.domain.bo.SysUserBo();
        bo.setUserId(11L);
        var entity = new org.dromara.system.domain.SysUser();
        entity.setUserId(11L);
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class);
             var mapping = mockStatic(org.dromara.common.core.utils.MapstructUtils.class)) {
            mapping.when(() -> org.dromara.common.core.utils.MapstructUtils.convert(bo, org.dromara.system.domain.SysUser.class))
                .thenReturn(entity);
            org.junit.jupiter.api.Assertions.assertThrows(org.dromara.common.core.exception.ServiceException.class,
                () -> service.updateUser(bo));
            verify(mapper).updateById(entity);
        }
    }

    private final SysUserMapper users = mock();
    private final SysRoleMapper roles = mock();
    private final SysPostMapper posts = mock();
    private final SysUserRoleMapper userRoles = mock();
    private final SysUserPostMapper userPosts = mock(SysUserPostMapper.class, RETURNS_DEEP_STUBS);
    private final SysUserServiceImpl service = new SysUserServiceImpl(users, mock(SysDeptMapper.class), roles, posts, userRoles, userPosts);
    private final SysUserBo form = new SysUserBo();
    private final SysUser entity = new SysUser();

    @Test
    void clearingPostSelectionRemovesPreviousAssignments() {
        var query = userPosts.lambda();
        when(query.eq(any(), eq(42L))).thenReturn(query);
        form.setUserId(42L);
        form.setPostIds(new Long[0]);
        when(users.updateById(entity)).thenReturn(1);
        assertEquals(1, save(false));
        verify(query).delete();
        verify(userPosts, never()).insertBatch(anyList());
    }

    @Test
    void omittedPostsPreserveAssignments() {
        form.setUserId(42L);
        when(users.updateById(entity)).thenReturn(1);
        assertEquals(1, save(false));
        verifyNoInteractions(userPosts);
    }

    @Test
    void failedUserInsertSkipsAllAssignments() {
        form.setPostIds(new Long[]{7L});
        when(posts.selectPostCount(List.of(7L))).thenReturn(1L);
        assertEquals(0, save(true));
        verifyNoInteractions(userPosts, userRoles);
    }

    @Test
    void failedPostAssignmentAbortsCreation() {
        when(users.insert(entity)).thenReturn(1);
        form.setPostIds(new Long[]{7L});
        when(posts.selectPostCount(List.of(7L))).thenReturn(1L);
        when(userPosts.insertBatch(anyList())).thenReturn(false);
        assertThrows(ServiceException.class, () -> save(true));
    }

    @Test
    void failedRoleAssignmentAbortsCreation() {
        when(users.insert(entity)).thenReturn(1);
        form.setRoleIds(new Long[]{8L});
        when(roles.selectRoleCount(List.of(8L))).thenReturn(1L);
        when(userRoles.insertBatch(anyList())).thenReturn(false);
        assertThrows(ServiceException.class, () -> save(true));
    }

    private int save(boolean create) {
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(form, SysUser.class)).thenReturn(entity);
            return create ? service.insertUser(form) : service.updateUser(form);
        }
    }
}

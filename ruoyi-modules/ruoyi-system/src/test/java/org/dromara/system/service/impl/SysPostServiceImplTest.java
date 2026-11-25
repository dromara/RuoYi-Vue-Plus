package org.dromara.system.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.domain.SysPost;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysPostMapper;
import org.dromara.system.mapper.SysUserPostMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SysPostServiceImplTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0,1})
    void postWritesReturnActualAffectedRows(int rows) {
        var mapper = mock(org.dromara.system.mapper.SysPostMapper.class);
        var post = new org.dromara.system.domain.SysPost();
        var request = new org.dromara.system.domain.bo.SysPostBo();
        var service = new SysPostServiceImpl(mapper, mock(), mock());
        when(mapper.insert(post)).thenReturn(rows);
        when(mapper.updateById(post)).thenReturn(rows);
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class);
             var mapping = mockStatic(org.dromara.common.core.utils.MapstructUtils.class)) {
            mapping.when(() -> org.dromara.common.core.utils.MapstructUtils.convert(request, org.dromara.system.domain.SysPost.class)).thenReturn(post);
            org.junit.jupiter.api.Assertions.assertEquals(rows, service.insertPost(request));
            org.junit.jupiter.api.Assertions.assertEquals(rows, service.updatePost(request));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true,false})
    void batchDeletionValidatesTheWholeSelection(boolean assigned) {
        var mapper = mock(org.dromara.system.mapper.SysPostMapper.class);
        var service = spy(new SysPostServiceImpl(mapper, mock(), mock()));
        var post = new org.dromara.system.domain.SysPost();
        post.setPostId(7L);
        var ids = java.util.List.of(6L, 7L);
        when(mapper.selectByIds(ids)).thenReturn(java.util.List.of(post));
        doReturn(assigned ? 1L : 0L).when(service).countUserPostById(7L);
        when(mapper.deleteByIds(ids)).thenReturn(1);
        if (assigned) {
            org.junit.jupiter.api.Assertions.assertThrows(org.dromara.common.core.exception.ServiceException.class,
                () -> service.deletePostByIds(ids));
            verify(mapper, never()).deleteByIds(ids);
        } else {
            org.junit.jupiter.api.Assertions.assertEquals(1, service.deletePostByIds(ids));
        }
    }

    @org.junit.jupiter.api.Test
    void assignedPostsAreReturnedForTheRequestedUser() {
        var mapper = mock(org.dromara.system.mapper.SysPostMapper.class);
        var post = new org.dromara.system.domain.vo.SysPostVo();
        post.setPostId(29L);
        when(mapper.selectPostsByUserId(5L)).thenReturn(java.util.List.of(post));
        var service = new SysPostServiceImpl(mapper, mock(), mock());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(29L), service.selectPostListByUserId(5L));
        org.junit.jupiter.api.Assertions.assertEquals(29L, service.selectListByUserId(5L).getFirst().getPostId());
    }

    @Test
    void singlePostDeletionRejectsAssignedPost() {
        var mapper = mock(SysPostMapper.class);
        var service = spy(new SysPostServiceImpl(mapper, mock(SysDeptMapper.class), mock(SysUserPostMapper.class)));
        var post = new SysPost();
        post.setPostId(7L);
        post.setPostName("仓管");
        when(mapper.selectByIds(List.of(7L))).thenReturn(List.of(post));
        doReturn(1L).when(service).countUserPostById(7L);
        assertThrows(ServiceException.class, () -> service.deletePostById(7L));
        verify(mapper, never()).deleteById(7L);
        verify(mapper, never()).deleteByIds(anyCollection());
    }
}

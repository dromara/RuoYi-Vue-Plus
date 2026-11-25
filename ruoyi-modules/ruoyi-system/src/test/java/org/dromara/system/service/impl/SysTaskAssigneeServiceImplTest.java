package org.dromara.system.service.impl;

import org.dromara.common.core.domain.PageResult;
import org.dromara.system.api.model.TaskAssigneeBody;
import org.dromara.system.domain.bo.SysPostBo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysTaskAssigneeServiceImplTest {

    @Test
    void postPickerFiltersByCategoryAndStoresThePostId() {
        var posts = mock(ISysPostService.class);
        var service = new SysTaskAssigneeServiceImpl(posts, mock(ISysDeptService.class), mock(ISysUserService.class), mock(ISysRoleService.class));
        var request = new TaskAssigneeBody();
        request.setHandlerCode("operations");
        var post = new SysPostVo();
        post.setPostId(23L);
        post.setPostCode("buyer");
        post.setPostCategory("operations");
        when(posts.selectPagePostList(any(), any())).thenReturn(PageResult.build(List.of(post), 1L));
        var result = service.selectPostsByTaskAssigneeList(request);
        var filter = ArgumentCaptor.forClass(SysPostBo.class);
        verify(posts).selectPagePostList(filter.capture(), any());
        assertAll(
            () -> assertEquals("operations", filter.getValue().getPostCategory()),
            () -> assertEquals("operations", result.getList().getFirst().getHandlerCode()),
            () -> assertEquals("23", result.getList().getFirst().getStorageId())
        );
    }
}

package org.dromara.workflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.domain.bo.FlowTaskBo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.mapper.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowTaskListTest {
    @Test
    void personalAndGlobalListsUseSameCategoryTreeAndDifferentUserScopes() {
        FlwTaskMapper tasks = mock(FlwTaskMapper.class);
        FlwHisTaskMapper history = mock(FlwHisTaskMapper.class);
        FlwCategoryMapper categories = mock(FlwCategoryMapper.class);
        var service = new FlwTaskServiceImpl(null, null, null, null, null, null, null, null,
            tasks, history, categories, null, null, null);
        var request = new FlowTaskBo();
        request.setCategory("3");
        var page = new PageQuery(2, 4);
        when(categories.selectCategoryIdsByParentId(3L)).thenReturn(List.of(3L, 4L));
        var tree = List.of("3", "4");
        when(tasks.getListRunTask(any(), eq(request), eq(tree), eq("7"))).thenReturn(new Page<FlowTaskVo>(4, 2, 5));
        when(tasks.getListRunTask(any(), eq(request), eq(tree), isNull())).thenReturn(new Page<FlowTaskVo>(4, 2, 11));
        when(history.getListFinishTask(any(), eq(request), eq(tree), eq("7"))).thenReturn(new Page<FlowHisTaskVo>(4, 2, 2));
        when(history.getListFinishTask(any(), eq(request), eq(tree), isNull())).thenReturn(new Page<FlowHisTaskVo>(4, 2, 9));
        when(tasks.getTaskCopyByPage(any(), eq(request), eq(tree), eq("7"))).thenReturn(new Page<FlowTaskVo>(4, 2, 3));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("7");
            assertEquals(5, service.pageByTaskWait(request, page).getTotal());
            assertEquals(11, service.pageByAllTaskWait(request, page).getTotal());
            assertEquals(2, service.pageByTaskFinish(request, page).getTotal());
            assertEquals(9, service.pageByAllTaskFinish(request, page).getTotal());
            assertEquals(3, service.pageByTaskCopy(request, page).getTotal());
        }
    }
}

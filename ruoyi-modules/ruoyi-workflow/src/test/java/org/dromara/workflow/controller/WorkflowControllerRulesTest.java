package org.dromara.workflow.controller;

import org.dromara.common.core.domain.R;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.service.IFlwCategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowControllerRulesTest {
    private final IFlwCategoryService categories = mock(IFlwCategoryService.class);
    private final FlwCategoryController controller = new FlwCategoryController(categories);

    @Test
    void protectsDefaultCategoryWithoutDatabaseAccess() {
        assertTrue(controller.remove(FlowConstant.FLOW_CATEGORY_ID).getMsg().contains("默认流程分类"));
        verifyNoInteractions(categories);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void protectsCategoryWithChildrenOrDefinitions(boolean hasChildren) {
        when(categories.hasChildByCategoryId(123L)).thenReturn(hasChildren);
        when(categories.checkCategoryExistDefinition(123L)).thenReturn(!hasChildren);
        assertTrue(controller.remove(123L).getMsg().contains(hasChildren ? "下级" : "流程定义"));
        verify(categories, never()).deleteWithValidById(anyLong());
    }

    @Test
    void rejectsDuplicateAndSelfParentBeforeWrite() {
        FlowCategoryBo category = new FlowCategoryBo();
        category.setCategoryId(12L);
        category.setParentId(12L);
        category.setCategoryName("报销");
        assertFalse(R.isSuccess(controller.add(category)));
        assertTrue(controller.edit(category).getMsg().contains("已存在"));
        when(categories.checkCategoryNameUnique(category)).thenReturn(true);
        assertTrue(controller.edit(category).getMsg().contains("不能是自己"));
        verify(categories, never()).insertByBo(any());
        verify(categories, never()).updateByBo(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void switchesOnlyRequestedInstance(boolean active) {
        InsService instances = mock(InsService.class);
        FlwInstanceController controller = new FlwInstanceController(instances, null);
        when(instances.active(8L)).thenReturn(true);
        when(instances.unActive(8L)).thenReturn(false);
        assertEquals(active, controller.active(8L, active).getData());
        verify(instances, times(active ? 1 : 0)).active(8L);
        verify(instances, times(active ? 0 : 1)).unActive(8L);
    }
}

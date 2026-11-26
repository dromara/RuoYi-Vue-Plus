package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.workflow.domain.FlowCategory;
import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.domain.vo.FlowCategoryVo;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryMoveTest {
    private final FlwCategoryMapper mapper = mock(FlwCategoryMapper.class);
    private final FlwCategoryServiceImpl service = new FlwCategoryServiceImpl(null, mapper);
    private final LambdaCrudChainWrapper<FlowCategory, FlowCategoryVo> children = mock(RETURNS_SELF);

    @BeforeEach
    void prepareChildrenQuery() {
        when(mapper.lambda()).thenReturn(children);
    }

    @ParameterizedTest
    @CsvSource({"12,1,'0,1'", "13,12,'0,1,12'", "14,13,'0,1,12,13'"})
    void rejectsSelfAndDescendantsBeforeWriting(long target, long parent, String ancestors) {
        when(mapper.selectById(12L)).thenReturn(category(12, 1, "0,1"));
        when(mapper.selectById(target)).thenReturn(category(target, parent, ancestors));
        assertThrows(ServiceException.class, () -> move(12, target));
        verify(mapper, never()).updateById(any(FlowCategory.class));
        verify(mapper, never()).updateBatchById(anyCollection());
    }

    @Test
    void rejectsMissingParentForInsertAndMove() {
        var request = new FlowCategoryBo();
        request.setParentId(99L);
        when(mapper.selectById(12L)).thenReturn(category(12, 1, "0,1"));
        assertThrows(ServiceException.class, () -> service.insertByBo(request));
        assertThrows(ServiceException.class, () -> move(12, 99));
        verify(mapper, never()).insert(any(FlowCategory.class));
        verify(mapper, never()).updateById(any(FlowCategory.class));
    }

    @Test
    void rootCannotChangeParent() {
        when(mapper.selectById(1L)).thenReturn(category(1, 0, "0"));
        assertThrows(ServiceException.class, () -> move(1, 12));
        verify(mapper, never()).updateById(any(FlowCategory.class));
    }

    @Test
    void moveRewritesPathsForChildrenAndGrandchildren() {
        when(mapper.selectById(12L)).thenReturn(category(12, 1, "0,1"));
        when(mapper.selectById(4L)).thenReturn(category(4, 112, "0,112"));
        when(children.list()).thenReturn(List.of(category(13, 12, "0,1,12"), category(14, 13, "0,1,12,13")));
        move(12, 4);
        verify(mapper).updateById(argThat((FlowCategory row) ->
            row.getParentId() == 4 && "0,112,4".equals(row.getAncestors())));
        verify(mapper).updateBatchById(argThat(rows -> rows.size() == 2
            && rows.stream().anyMatch(row -> row.getCategoryId() == 13 && "0,112,4,12".equals(row.getAncestors()))
            && rows.stream().anyMatch(row -> row.getCategoryId() == 14 && "0,112,4,12,13".equals(row.getAncestors()))));
    }

    private void move(long id, long parent) {
        var request = new FlowCategoryBo();
        request.setCategoryId(id);
        request.setParentId(parent);
        try (var converter = mockStatic(MapstructUtils.class)) {
            converter.when(() -> MapstructUtils.convert(request, FlowCategory.class)).thenReturn(category(id, parent, null));
            service.updateByBo(request);
        }
    }

    private static FlowCategory category(long id, long parent, String ancestors) {
        var category = new FlowCategory();
        category.setCategoryId(id);
        category.setParentId(parent);
        category.setAncestors(ancestors);
        return category;
    }
}

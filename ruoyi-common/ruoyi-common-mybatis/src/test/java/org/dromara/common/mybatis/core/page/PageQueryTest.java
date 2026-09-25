package org.dromara.common.mybatis.core.page;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageQuery 单元测试")
class PageQueryTest {

    /**
     * 验证缺省分页参数会使用框架定义的页码和页大小。
     */
    @Test
    @DisplayName("空分页参数使用默认值")
    void shouldUseDefaultPagination() {
        Page<Object> page = new PageQuery().build();

        assertEquals(PageQuery.DEFAULT_PAGE_NUM, page.getCurrent());
        assertEquals(PageQuery.DEFAULT_PAGE_SIZE, page.getSize());
        assertTrue(page.orders().isEmpty());
    }

    /**
     * 验证非法页码回退到第一页，并正确计算起始行。
     */
    @Test
    @DisplayName("页码小于等于零时回退到第一页")
    void shouldNormalizeInvalidPageNumber() {
        PageQuery query = new PageQuery(20, 0);

        assertEquals(1L, query.build().getCurrent());
        assertEquals(0, query.getFirstNum());
    }

    /**
     * 验证多字段排序支持驼峰转换及每列独立排序方向。
     */
    @Test
    @DisplayName("将驼峰排序字段转换为下划线并支持独立方向")
    void shouldBuildMultipleOrderItems() {
        PageQuery query = new PageQuery(10, 2);
        query.setOrderByColumn("userName,createTime");
        query.setIsAsc("ascending,descending");

        List<OrderItem> orders = query.build().orders();

        assertEquals(2, orders.size());
        assertEquals("user_name", orders.get(0).getColumn());
        assertTrue(orders.get(0).isAsc());
        assertEquals("create_time", orders.get(1).getColumn());
        assertFalse(orders.get(1).isAsc());
        assertEquals(10, query.getFirstNum());
    }

    /**
     * 验证排序字段数与方向数不匹配时抛出业务异常。
     */
    @Test
    @DisplayName("拒绝排序字段和方向数量不一致")
    void shouldRejectMismatchedDirections() {
        PageQuery query = new PageQuery();
        query.setOrderByColumn("id,createTime");
        query.setIsAsc("asc,desc,asc");

        assertThrows(ServiceException.class, query::build);
    }

    /**
     * 验证非法排序字段和未知排序方向都会被拒绝。
     */
    @Test
    @DisplayName("拒绝非法排序字段和排序方向")
    void shouldRejectInvalidOrderInput() {
        PageQuery unsafeColumn = new PageQuery();
        unsafeColumn.setOrderByColumn("id;drop table sys_user");
        unsafeColumn.setIsAsc("asc");
        assertThrows(IllegalArgumentException.class, unsafeColumn::build);

        PageQuery invalidDirection = new PageQuery();
        invalidDirection.setOrderByColumn("id");
        invalidDirection.setIsAsc("random");
        assertThrows(ServiceException.class, invalidDirection::build);
    }

}

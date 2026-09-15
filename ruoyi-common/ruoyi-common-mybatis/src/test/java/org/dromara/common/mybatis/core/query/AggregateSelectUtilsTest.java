package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("AggregateSelectUtils 单元测试")
class AggregateSelectUtilsTest {

    /**
     * 验证聚合字段、子查询字段和已有查询字段的 SQL 拼接格式。
     */
    @Test
    @DisplayName("拼接聚合字段和子查询字段")
    void shouldBuildSelectFragments() {
        assertEquals("COUNT(user_id) AS userCount",
            AggregateSelectUtils.aggregateSelect(SqlAggregateFunction.COUNT, "user_id", "userCount"));
        assertEquals("(SELECT MAX(id) FROM sys_user) AS maxId",
            AggregateSelectUtils.subquerySelect("SELECT MAX(id) FROM sys_user", "maxId"));
        assertEquals("id,COUNT(*) AS total",
            AggregateSelectUtils.appendSelect("id", "COUNT(*) AS total"));
        assertEquals("id", AggregateSelectUtils.appendSelect(null, "id"));
    }

    /**
     * 验证 Lambda getter 可以解析为对应的 Java 属性名。
     */
    @Test
    @DisplayName("从 Lambda getter 提取字段别名")
    void shouldResolveAliasFromGetter() {
        SFunction<TestEntity, Long> getter = TestEntity::getTotalValue;

        assertEquals("totalValue", AggregateSelectUtils.aliasName(getter));
    }

    /**
     * 验证非法 SQL 标识符不能作为查询别名。
     */
    @Test
    @DisplayName("拒绝非法 SQL 别名")
    void shouldRejectInvalidAlias() {
        assertThrows(RuntimeException.class, () -> AggregateSelectUtils.checkAlias("1total"));
        assertThrows(RuntimeException.class, () -> AggregateSelectUtils.checkAlias("total-value"));
        assertThrows(RuntimeException.class, () -> AggregateSelectUtils.checkAlias("total value"));
    }

    private static class TestEntity {

        private Long totalValue;

        /**
         * 提供 Lambda 属性解析使用的测试 getter。
         *
         * @return 测试聚合值
         */
        public Long getTotalValue() {
            return totalValue;
        }

    }

}

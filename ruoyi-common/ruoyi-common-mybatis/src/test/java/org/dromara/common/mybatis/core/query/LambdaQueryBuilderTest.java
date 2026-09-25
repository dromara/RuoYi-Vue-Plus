package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LambdaQueryBuilder 单元测试")
class LambdaQueryBuilderTest {

    /**
     * 初始化测试实体的 MyBatis-Plus 表元数据，供 Lambda 字段解析和逻辑删除 SQL 使用。
     */
    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), TestEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "testRelation"), TestRelation.class);
    }

    /**
     * 验证常用比较、集合、空值、分组和排序条件能够通过查询 DSL 生成 SQL 片段。
     */
    @Test
    @DisplayName("构造常用 Lambda 查询条件")
    void shouldBuildCommonLambdaQueryConditions() {
        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .eq(TestEntity::getName, "alice")
            .ne(false, TestEntity::getName, "ignored")
            .gt(TestEntity::getScore, 60)
            .ge(TestEntity::getScore, 61)
            .lt(TestEntity::getScore, 100)
            .le(TestEntity::getScore, 99)
            .like(TestEntity::getName, "ali")
            .notLike(TestEntity::getName, "bob")
            .between(TestEntity::getScore, 60, 100)
            .notBetween(false, TestEntity::getScore, 0, 10)
            .isNull(TestEntity::getRemark)
            .isNotNull(false, TestEntity::getRemark)
            .in(TestEntity::getId, List.of(1L, 2L))
            .notIn(TestEntity::getId, 3L, 4L)
            .groupBy(TestEntity::getName)
            .having("COUNT(*) > {0}", 1)
            .orderByDesc(TestEntity::getScore)
            .last("LIMIT 10")
            .build();

        String sql = wrapper.getSqlSegment();

        assertTrue(sql.contains("name"));
        assertTrue(sql.contains("score"));
        assertTrue(sql.contains("remark IS NULL"));
        assertTrue(sql.contains("GROUP BY name"));
        assertTrue(sql.contains("ORDER BY score DESC"));
        assertTrue(wrapper.getCustomSqlSegment().contains("LIMIT 10"));
    }

    /**
     * 验证普通字段、聚合字段和 COUNT(*) 可以组合为稳定的 SELECT 列表。
     */
    @Test
    @DisplayName("构造聚合查询字段")
    void shouldBuildAggregateSelectColumns() {
        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .select(TestEntity::getName)
            .selectSum(TestEntity::getScore, "totalScore")
            .selectMax(TestEntity::getScore, "maxScore")
            .selectMin(TestEntity::getScore, "minScore")
            .selectAvg(TestEntity::getScore, "avgScore")
            .selectCount(TestEntity::getId, "idCount")
            .selectCountDistinct(TestEntity::getName, "nameCount")
            .selectCountAll("total")
            .build();

        String select = wrapper.getSqlSelect();

        assertTrue(select.contains("name"));
        assertTrue(select.contains("SUM(score) AS totalScore"));
        assertTrue(select.contains("MAX(score) AS maxScore"));
        assertTrue(select.contains("MIN(score) AS minScore"));
        assertTrue(select.contains("AVG(score) AS avgScore"));
        assertTrue(select.contains("COUNT(id) AS idCount"));
        assertTrue(select.contains("COUNT(DISTINCT name) AS nameCount"));
        assertTrue(select.contains("COUNT(*) AS total"));
    }

    /**
     * 验证子查询自动加入逻辑删除条件、收集占位参数，并支持显式关闭逻辑删除。
     */
    @Test
    @DisplayName("构造带参数的子查询")
    void shouldBuildSubQueryWithLogicDeleteAndParameters() {
        SubQuery<TestEntity> subQuery = SubQuery.ofPlaceholders(TestEntity.class)
            .select(TestEntity::getId)
            .eq(TestEntity::getName, "alice")
            .gt(TestEntity::getScore, 60)
            .in(TestEntity::getId, 1L, 2L)
            .between(TestEntity::getScore, 60, 100)
            .when(false, query -> query.eq(TestEntity::getName, "ignored"));

        String sql = subQuery.build();

        assertTrue(sql.startsWith("SELECT id FROM test_entity WHERE"));
        assertTrue(sql.contains("deleted=0"));
        assertTrue(sql.contains("name = {0}"));
        assertArrayEquals(new Object[]{"alice", 60, 1L, 2L, 60, 100}, subQuery.params());

        String withoutLogicDelete = SubQuery.ofPlaceholders(TestEntity.class)
            .selectCountAll()
            .disableLogicDelete()
            .build();
        assertFalse(withoutLogicDelete.contains("deleted"));
    }

    /**
     * 验证项目查询辅助方法只为有效输入生成条件，避免空筛选值污染业务 SQL。
     */
    @Test
    @DisplayName("按输入有效性追加查询条件")
    void shouldAddOnlyMeaningfulOptionalConditions() {
        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .eqIfPresent(TestEntity::getName, null)
            .eqIfText(TestEntity::getName, " ")
            .eqIfText(TestEntity::getName, "alice")
            .neIfText(TestEntity::getRemark, "")
            .gtIfPresent(TestEntity::getScore, null)
            .geIfPresent(TestEntity::getScore, 60)
            .likeIfText(TestEntity::getRemark, "memo")
            .betweenParams(TestEntity::getScore, null, "begin", "end")
            .betweenParams(TestEntity::getScore, Map.of("begin", 10), "begin", "end")
            .betweenParams(TestEntity::getScore, Map.of("begin", 10, "end", 20), "begin", "end")
            .inIfNotEmpty(TestEntity::getId, List.of())
            .inIfNotEmpty(TestEntity::getId, List.of(1L, 2L))
            .notInIfNotEmpty(TestEntity::getId, new Object[0])
            .notInIfNotEmpty(TestEntity::getId, 3L, 4L)
            .build();

        String sql = wrapper.getSqlSegment();

        assertTrue(sql.contains("name ="));
        assertTrue(sql.contains("score >="));
        assertTrue(sql.contains("remark LIKE"));
        assertEquals(sql.indexOf("score BETWEEN"), sql.lastIndexOf("score BETWEEN"));
        assertTrue(sql.contains("id IN"));
        assertTrue(sql.contains("id NOT IN"));
        assertEquals(new HashSet<>(List.of("alice", 60, "%memo%", 10, 20, 1L, 2L, 3L, 4L)),
            new HashSet<>(wrapper.getParamNameValuePairs().values()));
    }

    /**
     * 验证查询字段和各类子查询条件能关联外层字段、保留逻辑删除并绑定独立参数。
     */
    @Test
    @DisplayName("组合关联子查询")
    void shouldComposeCorrelatedSubQueries() {
        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .select(TestEntity::getId, TestEntity::getName)
            .selectSub(TestRelation.class, sub -> sub
                .selectCountAll()
                .eqColumn(TestRelation::getOwnerId, TestEntity::getId)
                .eq(TestRelation::getState, "selected"), "relationCount")
            .eqSub(TestEntity::getScore, TestRelation.class, sub -> sub
                .selectMax(TestRelation::getPoints)
                .eqColumn(TestRelation::getOwnerId, TestEntity::getId)
                .eq(TestRelation::getState, "scored"))
            .inSub(TestEntity::getId, TestRelation.class, sub -> sub
                .select(TestRelation::getOwnerId)
                .eq(TestRelation::getState, "included"))
            .notInSub(TestEntity::getId, TestRelation.class, sub -> sub
                .select(TestRelation::getOwnerId)
                .eq(TestRelation::getState, "excluded"))
            .existsSub(TestRelation.class, sub -> sub
                .selectCountAll()
                .eqColumn(TestRelation::getOwnerId, TestEntity::getId)
                .eq(TestRelation::getState, "existing"))
            .notExistsSub(TestRelation.class, sub -> sub
                .selectCountAll()
                .eqColumn(TestRelation::getOwnerId, TestEntity::getId)
                .eq(TestRelation::getState, "missing"))
            .build();

        String select = wrapper.getSqlSelect();
        String sql = wrapper.getSqlSegment();

        assertTrue(select.contains("(SELECT COUNT(*) FROM test_relation"));
        assertTrue(select.contains("deleted=0"));
        assertTrue(select.contains("owner_id=test_entity.id"));
        assertTrue(select.contains("AS relationCount"));
        assertTrue(sql.contains("score = (SELECT MAX(points) FROM test_relation"));
        assertTrue(sql.contains("id IN (SELECT owner_id FROM test_relation"));
        assertTrue(sql.contains("id NOT IN (SELECT owner_id FROM test_relation"));
        assertTrue(sql.contains("EXISTS (SELECT COUNT(*) FROM test_relation"));
        assertTrue(sql.contains("NOT EXISTS (SELECT COUNT(*) FROM test_relation"));
        assertTrue(wrapper.getParamNameValuePairs().values().containsAll(
            List.of("selected", "scored", "included", "excluded", "existing", "missing")));
    }

    /**
     * 验证空集合不会产生非法 IN 子句，显式外层别名仍能正确生成关联条件。
     */
    @Test
    @DisplayName("跳过子查询空集合条件")
    void shouldSkipEmptySubQueryCollections() {
        SubQuery<TestRelation> subQuery = SubQuery.ofPlaceholders(TestRelation.class)
            .select(TestRelation::getOwnerId)
            .in(TestRelation::getId, List.of())
            .in(TestRelation::getId, (Object[]) null)
            .eqColumn(TestRelation::getOwnerId, "u", TestEntity::getId)
            .when(true, query -> query.eq(TestRelation::getState, "enabled"));

        String sql = subQuery.build();

        assertFalse(sql.contains(" IN ("));
        assertTrue(sql.contains("owner_id=u.id"));
        assertArrayEquals(new Object[]{"enabled"}, subQuery.params());
    }

    /**
     * 验证缺少查询字段的子查询会在构造阶段失败，防止生成无法执行的 SQL。
     */
    @Test
    @DisplayName("拒绝没有查询字段的子查询")
    void shouldRejectSubQueryWithoutSelectColumn() {
        assertThrows(MybatisPlusException.class,
            () -> SubQuery.ofPlaceholders(TestRelation.class).build());
    }

    /**
     * 验证项目构造器透传的 MyBatis-Plus 嵌套逻辑、批量等值和函数式扩展保持原生语义。
     */
    @Test
    @DisplayName("兼容 MyBatis-Plus 组合条件")
    void shouldPreserveNativeMybatisPlusConditionSemantics() {
        Map<SFunction<TestEntity, ?>, Object> values = new LinkedHashMap<>();
        values.put(TestEntity::getName, "alice");
        values.put(TestEntity::getRemark, null);
        values.put(TestEntity::getScore, 80);

        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .allEq((column, value) -> !Integer.valueOf(80).equals(value), values, true)
            .and(nested -> nested.gt(TestEntity::getScore, 60).lt(TestEntity::getScore, 100))
            .or(nested -> nested.eq(TestEntity::getName, "backup").isNull(TestEntity::getRemark))
            .nested(nested -> nested.likeLeft(TestEntity::getName, "ice")
                .or().likeRight(TestEntity::getName, "ali"))
            .not(nested -> nested.eq(TestEntity::getScore, 0))
            .func(nested -> nested.ge(TestEntity::getScore, 70))
            .apply(nested -> nested.le(TestEntity::getScore, 90))
            .build();

        String sql = wrapper.getSqlSegment();

        assertTrue(sql.contains("name ="));
        assertTrue(sql.contains("remark IS NULL"));
        assertFalse(wrapper.getParamNameValuePairs().values().contains(80));
        assertTrue(sql.contains("AND (score >"));
        assertTrue(sql.contains("OR (name ="));
        assertTrue(sql.contains("NOT (score ="));
        assertTrue(wrapper.getParamNameValuePairs().values().containsAll(
            List.of("alice", 60, 100, "backup", "%ice", "ali%", 0, 70, 90)));
    }

    /**
     * 验证项目暴露的原生 SQL 入口仍由 MyBatis-Plus 完成占位参数绑定和 SQL 片段拼装。
     */
    @Test
    @DisplayName("兼容 MyBatis-Plus 原生 SQL 条件")
    void shouldPreserveNativeSqlFragmentsAndParameterBinding() {
        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .eqSql(TestEntity::getScore, "SELECT MAX(points) FROM test_relation")
            .inSql(TestEntity::getId, "SELECT owner_id FROM test_relation WHERE state = 'enabled'")
            .notInSql(TestEntity::getId, "SELECT owner_id FROM test_relation WHERE state = 'disabled'")
            .exists("SELECT 1 FROM test_relation r WHERE r.owner_id = test_entity.id AND r.state = {0}", "active")
            .notExists("SELECT 1 FROM test_relation r WHERE r.owner_id = test_entity.id AND r.state = {0}", "removed")
            .apply("FIND_IN_SET({0}, name)", "alice")
            .build();

        String sql = wrapper.getSqlSegment();

        assertTrue(sql.contains("score = (SELECT MAX(points) FROM test_relation)"));
        assertTrue(sql.contains("id IN (SELECT owner_id FROM test_relation WHERE state = 'enabled')"));
        assertTrue(sql.contains("id NOT IN (SELECT owner_id FROM test_relation WHERE state = 'disabled')"));
        assertTrue(sql.contains("EXISTS (SELECT 1 FROM test_relation"));
        assertTrue(sql.contains("NOT EXISTS (SELECT 1 FROM test_relation"));
        assertTrue(sql.contains("FIND_IN_SET("));
        assertEquals(new HashSet<>(List.of("active", "removed", "alice")),
            new HashSet<>(wrapper.getParamNameValuePairs().values()));
    }

    /**
     * 验证清空底层 Wrapper 后查询条件、参数、SELECT、注释和尾部 SQL 都不会泄漏到后续查询。
     */
    @Test
    @DisplayName("清空完整查询状态")
    void shouldClearAllQueryState() {
        LambdaQueryWrapper<TestEntity> wrapper = QueryBuilder.lambda(TestEntity.class)
            .select(TestEntity::getName)
            .selectCountAll("total")
            .eq(TestEntity::getName, "alice")
            .comment("query-comment")
            .last("LIMIT 1")
            .build();

        wrapper.clear();

        assertNull(wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSegment().isEmpty());
        assertTrue(wrapper.getCustomSqlSegment().isEmpty());
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    @TableName("test_entity")
    private static class TestEntity {
        @TableId
        private Long id;
        private String name;
        private Integer score;
        private String remark;
        @TableLogic
        private Integer deleted;

        /**
         * 返回测试实体主键，供 Lambda 字段解析。
         *
         * @return 主键
         */
        public Long getId() {
            return id;
        }

        /**
         * 返回测试实体名称，供 Lambda 字段解析。
         *
         * @return 名称
         */
        public String getName() {
            return name;
        }

        /**
         * 返回测试实体分数，供 Lambda 字段解析。
         *
         * @return 分数
         */
        public Integer getScore() {
            return score;
        }

        /**
         * 返回测试实体备注，供 Lambda 字段解析。
         *
         * @return 备注
         */
        public String getRemark() {
            return remark;
        }
    }

    @TableName("test_relation")
    private static class TestRelation {
        @TableId
        private Long id;
        private Long ownerId;
        private String state;
        private Integer points;
        @TableLogic
        private Integer deleted;

        /**
         * 返回关联记录主键，供 Lambda 字段解析。
         *
         * @return 主键
         */
        public Long getId() {
            return id;
        }

        /**
         * 返回关联记录所属主键，供关联子查询字段解析。
         *
         * @return 所属主键
         */
        public Long getOwnerId() {
            return ownerId;
        }

        /**
         * 返回关联记录状态，供子查询参数条件解析。
         *
         * @return 状态
         */
        public String getState() {
            return state;
        }

        /**
         * 返回关联记录分值，供聚合子查询字段解析。
         *
         * @return 分值
         */
        public Integer getPoints() {
            return points;
        }
    }
}

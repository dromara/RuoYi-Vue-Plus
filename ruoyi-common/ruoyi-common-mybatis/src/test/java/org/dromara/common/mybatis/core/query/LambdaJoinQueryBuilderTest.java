package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LambdaJoinQueryBuilder 单元测试")
class LambdaJoinQueryBuilderTest {

    /**
     * 初始化联表测试实体的 MyBatis-Plus 元数据，供 MPJ 字段、表名和逻辑删除解析使用。
     */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "joinUser"), JoinUser.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "joinDept"), JoinDept.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "joinOrder"), JoinOrder.class);
    }

    /**
     * 验证实际列表查询常用的主表别名、多次左联、字段映射、聚合、筛选和排序可以组成完整 SQL。
     */
    @Test
    @DisplayName("构造多表列表查询")
    void shouldBuildAliasedMultiTableQuery() {
        MPJLambdaWrapper<JoinUser> wrapper = QueryBuilder.lambdaJoin("u", JoinUser.class)
            .distinct()
            .select("u", JoinUser::getId, JoinUser::getName)
            .selectAs("d", JoinDept::getName, JoinResult::getDeptName)
            .selectSum("o", JoinOrder::getAmount, "totalAmount")
            .selectCount("o", JoinOrder::getId, "orderCount")
            .leftJoin(JoinDept.class, "d", JoinDept::getId, JoinUser::getDeptId)
            .leftJoin(JoinOrder.class, "o", JoinOrder::getUserId, JoinUser::getId)
            .eqIfPresent("u", JoinUser::getName, null)
            .eqIfText("u", JoinUser::getName, "alice")
            .likeIfText("d", JoinDept::getName, "tech")
            .betweenParams("o", JoinOrder::getCreatedAt, null, "begin", "end")
            .betweenParams("o", JoinOrder::getCreatedAt, Map.of("begin", "2026-01-01"), "begin", "end")
            .betweenParams("o", JoinOrder::getCreatedAt,
                Map.of("begin", "2026-01-01", "end", "2026-01-31"), "begin", "end")
            .inIfNotEmpty("u", JoinUser::getId, List.of())
            .inIfNotEmpty("u", JoinUser::getId, List.of(1L, 2L))
            .notInIfNotEmpty("u", JoinUser::getId, List.of())
            .isNotNull("d", JoinDept::getId)
            .groupBy("u", JoinUser::getId, JoinUser::getName)
            .orderByAsc("u", JoinUser::getName)
            .orderByDesc("o", JoinOrder::getCreatedAt)
            .build();

        String select = wrapper.getSqlSelect();
        String from = wrapper.getFrom();
        String sql = wrapper.getSqlSegment();

        assertEquals("u", wrapper.getAlias());
        assertTrue(wrapper.getSelectDistinct());
        assertTrue(select.contains("u.id"));
        assertTrue(select.contains("u.name"));
        assertTrue(select.contains("d.name AS deptName"));
        assertTrue(select.contains("SUM(o.amount) AS totalAmount"));
        assertTrue(select.contains("COUNT(o.id) AS orderCount"));
        assertTrue(from.contains("LEFT JOIN test_dept d ON"));
        assertTrue(from.contains("d.id = u.dept_id"));
        assertTrue(from.contains("LEFT JOIN test_order o ON"));
        assertTrue(from.contains("o.user_id = u.id"));
        assertTrue(sql.contains("u.name ="));
        assertTrue(sql.contains("d.name LIKE"));
        assertEquals(sql.indexOf("o.created_at BETWEEN"), sql.lastIndexOf("o.created_at BETWEEN"));
        assertTrue(sql.contains("u.id IN"));
        assertTrue(sql.contains("d.id IS NOT NULL"));
        assertTrue(sql.contains("GROUP BY u.id,u.name"));
        assertTrue(sql.contains("ORDER BY u.name ASC,o.created_at DESC"));
        assertEquals(new HashSet<>(List.of("alice", "%tech%", "2026-01-01", "2026-01-31", 1L, 2L)),
            new HashSet<>(wrapper.getParamNameValuePairs().values()));
    }

    /**
     * 验证联表构造器的各类关联子查询使用主表别名、追加逻辑删除条件并完整绑定参数。
     */
    @Test
    @DisplayName("构造联表关联子查询")
    void shouldBuildCorrelatedSubQueriesForJoinQuery() {
        MPJLambdaWrapper<JoinUser> wrapper = QueryBuilder.lambdaJoin("u", JoinUser.class)
            .select("u", JoinUser::getId)
            .selectSub(JoinOrder.class, sub -> sub
                .selectCountAll()
                .eqColumn(JoinOrder::getUserId, "u", JoinUser::getId)
                .eq(JoinOrder::getState, "selected"), "paidCount")
            .eqSub("u", JoinUser::getScore, JoinOrder.class, sub -> sub
                .selectMax(JoinOrder::getAmount)
                .eqColumn(JoinOrder::getUserId, "u", JoinUser::getId)
                .eq(JoinOrder::getState, "scored"))
            .inSub("u", JoinUser::getId, JoinOrder.class, sub -> sub
                .select(JoinOrder::getUserId)
                .eq(JoinOrder::getState, "included"))
            .notInSub("u", JoinUser::getId, JoinOrder.class, sub -> sub
                .select(JoinOrder::getUserId)
                .eq(JoinOrder::getState, "excluded"))
            .existsSub(JoinOrder.class, sub -> sub
                .selectCountAll()
                .eqColumn(JoinOrder::getUserId, "u", JoinUser::getId)
                .eq(JoinOrder::getState, "existing"))
            .notExistsSub(JoinOrder.class, sub -> sub
                .selectCountAll()
                .eqColumn(JoinOrder::getUserId, "u", JoinUser::getId)
                .eq(JoinOrder::getState, "missing"))
            .build();

        String select = wrapper.getSqlSelect();
        String sql = wrapper.getSqlSegment();

        assertTrue(select.contains("(SELECT COUNT(*) FROM test_order"));
        assertTrue(select.contains("deleted=0"));
        assertTrue(select.contains("user_id=u.id"));
        assertTrue(select.contains("AS paidCount"));
        assertTrue(sql.contains("u.score = (SELECT MAX(amount) FROM test_order"));
        assertTrue(sql.contains("u.id IN (SELECT user_id FROM test_order"));
        assertTrue(sql.contains("u.id NOT IN (SELECT user_id FROM test_order"));
        assertTrue(sql.contains("EXISTS (SELECT COUNT(*) FROM test_order"));
        assertTrue(sql.contains("NOT EXISTS (SELECT COUNT(*) FROM test_order"));
        assertTrue(wrapper.getParamNameValuePairs().values().containsAll(
            List.of("selected", "scored", "included", "excluded", "existing", "missing")));
    }

    /**
     * 验证 MPJ 默认别名、无显式别名联表、全字段选择和底层 Wrapper 扩展入口保持兼容。
     */
    @Test
    @DisplayName("兼容 MPJ 默认联表行为")
    void shouldPreserveNativeMpjDefaultsAndEscapeHatch() {
        MPJLambdaWrapper<JoinUser> wrapper = QueryBuilder.lambdaJoin(JoinUser.class)
            .selectAll()
            .selectAll(JoinDept.class)
            .leftJoin(JoinDept.class, JoinDept::getId, JoinUser::getDeptId)
            .neIfText("t", JoinUser::getName, "alice")
            .betweenIfPresent("t", JoinUser::getScore, 60, 100)
            .notInIfNotEmpty("t", JoinUser::getId, List.of(3L, 4L))
            .apply(nativeWrapper -> nativeWrapper.likeRight(JoinDept::getName, "tech"))
            .build();

        String select = wrapper.getSqlSelect();
        String from = wrapper.getFrom();
        String sql = wrapper.getSqlSegment();

        assertEquals("t", wrapper.getAlias());
        assertTrue(select.contains("t.id"));
        assertTrue(select.contains("t.name"));
        assertTrue(select.contains("t1.id"));
        assertTrue(select.contains("t1.name"));
        assertTrue(from.contains("LEFT JOIN test_dept t1 ON"));
        assertTrue(from.contains("t1.id = t.dept_id"));
        assertTrue(sql.contains("t.name <>"));
        assertTrue(sql.contains("t.score BETWEEN"));
        assertTrue(sql.contains("t.id NOT IN"));
        assertTrue(sql.contains("t1.name LIKE"));
        assertEquals(new HashSet<>(List.of("alice", 60, 100, 3L, 4L, "tech%")),
            new HashSet<>(wrapper.getParamNameValuePairs().values()));
    }

    @TableName("test_user")
    private static class JoinUser {
        @TableId
        private Long id;
        private Long deptId;
        private String name;
        private Integer score;

        /**
         * 返回用户主键，供主表与订单表关联。
         *
         * @return 用户主键
         */
        public Long getId() {
            return id;
        }

        /**
         * 返回部门主键，供用户与部门表关联。
         *
         * @return 部门主键
         */
        public Long getDeptId() {
            return deptId;
        }

        /**
         * 返回用户名称，供字段选择、筛选、分组和排序解析。
         *
         * @return 用户名称
         */
        public String getName() {
            return name;
        }

        /**
         * 返回用户分值，供等值子查询条件解析。
         *
         * @return 用户分值
         */
        public Integer getScore() {
            return score;
        }
    }

    @TableName("test_dept")
    private static class JoinDept {
        @TableId
        private Long id;
        private String name;

        /**
         * 返回部门主键，供联表条件和非空筛选解析。
         *
         * @return 部门主键
         */
        public Long getId() {
            return id;
        }

        /**
         * 返回部门名称，供结果字段映射和模糊筛选解析。
         *
         * @return 部门名称
         */
        public String getName() {
            return name;
        }
    }

    @TableName("test_order")
    private static class JoinOrder {
        @TableId
        private Long id;
        private Long userId;
        private Integer amount;
        private String state;
        private String createdAt;
        @TableLogic
        private Integer deleted;

        /**
         * 返回订单主键，供聚合统计字段解析。
         *
         * @return 订单主键
         */
        public Long getId() {
            return id;
        }

        /**
         * 返回订单所属用户，供联表和关联子查询解析。
         *
         * @return 用户主键
         */
        public Long getUserId() {
            return userId;
        }

        /**
         * 返回订单金额，供聚合字段和等值子查询解析。
         *
         * @return 订单金额
         */
        public Integer getAmount() {
            return amount;
        }

        /**
         * 返回订单状态，供子查询参数条件解析。
         *
         * @return 订单状态
         */
        public String getState() {
            return state;
        }

        /**
         * 返回订单创建时间，供区间筛选和排序解析。
         *
         * @return 创建时间
         */
        public String getCreatedAt() {
            return createdAt;
        }
    }

    private static class JoinResult {
        private String deptName;

        /**
         * 返回结果部门名称，供 selectAs 推导字段别名。
         *
         * @return 部门名称
         */
        public String getDeptName() {
            return deptName;
        }
    }
}

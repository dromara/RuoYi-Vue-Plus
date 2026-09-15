package org.dromara.common.mybatis.core.mapper;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

@DisplayName("LambdaCrudChainWrapper 单元测试")
class LambdaCrudChainWrapperTest {

    /**
     * 初始化测试实体表元数据，确保测试覆盖真实的 MyBatis-Plus Lambda 字段解析。
     */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "crudEntity"), CrudEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "crudRelation"), CrudRelation.class);
    }

    /**
     * 验证构造器从 Mapper 获取实体类型，并组合查询字段、聚合字段、嵌套条件和共享参数。
     */
    @Test
    @DisplayName("构造 Mapper 级查询链")
    void shouldBuildMapperBoundQueryState() {
        LambdaCrudChainWrapper<CrudEntity, CrudVo> wrapper = wrapper()
            .select(CrudEntity::getId, CrudEntity::getName)
            .selectSum(CrudEntity::getScore, "totalScore")
            .selectCountAll(CrudVo::getTotal)
            .eq(CrudEntity::getName, "alice")
            .and(nested -> nested.ge(CrudEntity::getScore, 60)
                .or().isNull(CrudEntity::getRemark));

        assertEquals(CrudEntity.class, wrapper.getEntityClass());
        assertTrue(wrapper.getSqlSelect().contains("id,name"));
        assertTrue(wrapper.getSqlSelect().contains("SUM(score) AS totalScore"));
        assertTrue(wrapper.getSqlSelect().contains("COUNT(*) AS total"));
        assertTrue(wrapper.getSqlSegment().contains("AND (score >="));
        assertEquals(new HashSet<>(List.of("alice", 60)),
            new HashSet<>(wrapper.getParamNameValuePairs().values()));
    }

    /**
     * 验证查询条件和更新 SET 片段可以共存，并保持可选赋值、原生 SQL、自增和自减语义。
     */
    @Test
    @DisplayName("组合查询和更新片段")
    void shouldComposeQueryAndUpdateFragments() {
        LambdaCrudChainWrapper<CrudEntity, CrudVo> wrapper = wrapper()
            .eq(CrudEntity::getId, 1L)
            .set(CrudEntity::getName, "updated")
            .setIfPresent(CrudEntity::getRemark, null)
            .setIfText(CrudEntity::getRemark, " ")
            .setIfText(CrudEntity::getRemark, "memo")
            .setSql("score = {0}", 80)
            .setIncrBy(CrudEntity::getScore, new BigDecimal("1.50"))
            .setDecrBy(CrudEntity::getScore, 2);

        String sqlSet = wrapper.getSqlSet();

        assertTrue(wrapper.getSqlSegment().contains("id ="));
        assertTrue(sqlSet.contains("name="));
        assertTrue(sqlSet.contains("remark="));
        assertTrue(sqlSet.contains("score ="));
        assertTrue(sqlSet.contains("score=score + 1.50"));
        assertTrue(sqlSet.contains("score=score - 2"));
        assertFalse(wrapper.getParamNameValuePairs().values().contains(null));
        assertTrue(wrapper.getParamNameValuePairs().values().containsAll(List.of(1L, "updated", "memo", 80)));
    }

    /**
     * 验证 Mapper 链式包装器中的子查询沿用主 Wrapper 参数序列并生成逻辑删除条件。
     */
    @Test
    @DisplayName("构造 Mapper 链式子查询")
    void shouldBuildSubQueriesWithSharedParameters() {
        LambdaCrudChainWrapper<CrudEntity, CrudVo> wrapper = wrapper()
            .select(CrudEntity::getId)
            .selectSub(CrudRelation.class, sub -> sub
                .selectCountAll()
                .eqColumn(CrudRelation::getOwnerId, CrudEntity::getId)
                .eq(CrudRelation::getState, "selected"), CrudVo::getTotal)
            .inSub(CrudEntity::getId, CrudRelation.class, sub -> sub
                .select(CrudRelation::getOwnerId)
                .eq(CrudRelation::getState, "included"))
            .existsSub(CrudRelation.class, sub -> sub
                .selectCountAll()
                .eqColumn(CrudRelation::getOwnerId, CrudEntity::getId)
                .eq(CrudRelation::getState, "existing"));

        assertTrue(wrapper.getSqlSelect().contains("SELECT COUNT(*) FROM crud_relation"));
        assertTrue(wrapper.getSqlSelect().contains("deleted=0"));
        assertTrue(wrapper.getSqlSegment().contains("id IN (SELECT owner_id FROM crud_relation"));
        assertTrue(wrapper.getSqlSegment().contains("EXISTS (SELECT COUNT(*) FROM crud_relation"));
        assertEquals(new HashSet<>(List.of("selected", "included", "existing")),
            new HashSet<>(wrapper.getParamNameValuePairs().values()));
    }

    /**
     * 验证 clear 清除查询、更新、参数和附加 SQL，防止复用 Wrapper 时残留上一次状态。
     */
    @Test
    @DisplayName("清空 Mapper 链式状态")
    void shouldClearQueryAndUpdateState() {
        LambdaCrudChainWrapper<CrudEntity, CrudVo> wrapper = wrapper()
            .select(CrudEntity::getName)
            .selectCountAll("total")
            .set(CrudEntity::getName, "updated")
            .eq(CrudEntity::getId, 1L)
            .first("/*+ INDEX */")
            .comment("update-comment")
            .last("LIMIT 1");

        wrapper.clear();

        assertNull(wrapper.getSqlSelect());
        assertNull(wrapper.getSqlSet());
        assertTrue(wrapper.getSqlSegment().isEmpty());
        assertTrue(wrapper.getCustomSqlSegment().isEmpty());
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    /**
     * 验证 BaseMapperPlus 默认方法可以从具体 Mapper 泛型解析实体和 VO，并创建项目链式包装器。
     */
    @Test
    @DisplayName("通过 BaseMapperPlus 默认入口创建查询链")
    void shouldResolveMapperGenericTypesAndCreateLambdaChain() {
        CrudMapper mapper = mock(CrudMapper.class, CALLS_REAL_METHODS);

        LambdaCrudChainWrapper<CrudEntity, CrudVo> wrapper = mapper.lambda();

        assertEquals(CrudEntity.class, mapper.currentModelClass());
        assertEquals(CrudVo.class, mapper.currentVoClass());
        assertEquals(CrudEntity.class, wrapper.getEntityClass());
    }

    /**
     * 创建绑定测试实体类型的 Mapper 链式包装器。
     *
     * @return Mapper 链式包装器
     */
    @SuppressWarnings("unchecked")
    private static LambdaCrudChainWrapper<CrudEntity, CrudVo> wrapper() {
        BaseMapperPlus<CrudEntity, CrudVo> mapper = mock(BaseMapperPlus.class);
        when(mapper.currentModelClass()).thenReturn(CrudEntity.class);
        return new LambdaCrudChainWrapper<>(mapper);
    }

    private interface CrudMapper extends BaseMapperPlus<CrudEntity, CrudVo> {
    }

    @TableName("crud_entity")
    private static class CrudEntity {
        @TableId
        private Long id;
        private String name;
        private Integer score;
        private String remark;

        /**
         * 返回实体主键，供 Lambda 字段解析。
         *
         * @return 实体主键
         */
        public Long getId() {
            return id;
        }

        /**
         * 返回实体名称，供查询和更新字段解析。
         *
         * @return 实体名称
         */
        public String getName() {
            return name;
        }

        /**
         * 返回实体分值，供聚合和数值更新解析。
         *
         * @return 实体分值
         */
        public Integer getScore() {
            return score;
        }

        /**
         * 返回实体备注，供可选更新和空值查询解析。
         *
         * @return 实体备注
         */
        public String getRemark() {
            return remark;
        }
    }

    private static class CrudVo {
        private Long total;

        /**
         * 返回聚合总数，供 Lambda 推导查询别名。
         *
         * @return 聚合总数
         */
        public Long getTotal() {
            return total;
        }
    }

    @TableName("crud_relation")
    private static class CrudRelation {
        @TableId
        private Long id;
        private Long ownerId;
        private String state;
        @TableLogic
        private Integer deleted;

        /**
         * 返回关联记录所属实体主键，供关联子查询解析。
         *
         * @return 所属实体主键
         */
        public Long getOwnerId() {
            return ownerId;
        }

        /**
         * 返回关联记录状态，供子查询参数绑定。
         *
         * @return 关联状态
         */
        public String getState() {
            return state;
        }
    }
}

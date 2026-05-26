package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * LambdaQueryWrapper 条件构造辅助类。
 *
 * @param <T> 实体类型
 * @author Lion Li
 */
public final class LambdaQueryBuilder<T> implements LambdaQueryCondition<T, LambdaQueryBuilder<T>> {

    /**
     * MyBatis-Plus Lambda 查询包装器。
     */
    private final LambdaQueryWrapper<T> wrapper;

    /**
     * 构造 Lambda 查询构造辅助对象。
     *
     * @param wrapper Lambda 查询包装器
     */
    LambdaQueryBuilder(LambdaQueryWrapper<T> wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * 指定查询字段。
     *
     * @param columns 查询字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> select(SFunction<T, ?>... columns) {
        aggregateWrapper().resetAggregateSelect();
        wrapper.select(columns);
        return this;
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectSum(SFunction<T, ?> column) {
        return selectSum(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectSum(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.SUM, column, alias);
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectSum(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectSum(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectMax(SFunction<T, ?> column) {
        return selectMax(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectMax(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.MAX, column, alias);
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectMax(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectMax(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectMin(SFunction<T, ?> column) {
        return selectMin(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectMin(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.MIN, column, alias);
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectMin(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectMin(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectAvg(SFunction<T, ?> column) {
        return selectAvg(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectAvg(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.AVG, column, alias);
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectAvg(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectAvg(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectCount(SFunction<T, ?> column) {
        return selectCount(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectCount(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.COUNT, column, alias);
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectCount(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectCount(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT(*) 聚合查询字段。
     *
     * @param alias 查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectCountAll(String alias) {
        return selectAggregate(SqlAggregateFunction.COUNT, "*", alias);
    }

    /**
     * 指定 COUNT(*) 聚合查询字段。
     *
     * @param alias 查询别名字段
     * @param <A>   查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectCountAll(SFunction<A, ?> alias) {
        return selectCountAll(AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT(DISTINCT column) 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> selectCountDistinct(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.COUNT, "DISTINCT " + aggregateWrapper().columnName(column), alias);
    }

    /**
     * 指定 COUNT(DISTINCT column) 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <A> LambdaQueryBuilder<T> selectCountDistinct(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectCountDistinct(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定子查询字段。
     * <pre>{@code
     * QueryBuilder.lambda(SysUser.class)
     *     .select(SysUser::getUserId, SysUser::getUserName)
     *     .selectSub(SysUserRole.class, sub -> sub
     *         .selectCountAll()
     *         .eqColumn(SysUserRole::getUserId, SysUser::getUserId),
     *         UserStatVo::getRoleCount)
     *     .build();
     * }</pre>
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param alias       查询别名
     * @param <S>         子查询实体类型
     * @return 当前查询构造辅助对象
     */
    public <S> LambdaQueryBuilder<T> selectSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer, String alias) {
        aggregateWrapper().appendSelectSql(AggregateSelectUtils.subquerySelect(buildSubQuery(entityClass, consumer), alias));
        return this;
    }

    /**
     * 指定子查询字段。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param alias       查询别名字段
     * @param <S>         子查询实体类型
     * @param <A>         查询结果类型
     * @return 当前查询构造辅助对象
     */
    public <S, A> LambdaQueryBuilder<T> selectSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer, SFunction<A, ?> alias) {
        return selectSub(entityClass, consumer, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 添加等于子查询条件。
     * <pre>{@code
     * QueryBuilder.lambda(SysUser.class)
     *     .eqSub(SysUser::getDeptId, SysDept.class, sub -> sub
     *         .select(SysDept::getDeptId)
     *         .eq(SysDept::getDeptName, deptName))
     *     .build();
     * }</pre>
     *
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return 当前查询构造辅助对象
     */
    public <S> LambdaQueryBuilder<T> eqSub(SFunction<T, ?> column, Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        wrapper.eqSql(column, buildSubQuery(entityClass, consumer));
        return this;
    }

    /**
     * 添加 IN 子查询条件。
     * <pre>{@code
     * QueryBuilder.lambda(SysUser.class)
     *     .inSub(SysUser::getUserId, SysUserRole.class, sub -> sub
     *         .select(SysUserRole::getUserId)
     *         .eq(SysUserRole::getRoleId, roleId))
     *     .build();
     * }</pre>
     *
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return 当前查询构造辅助对象
     */
    public <S> LambdaQueryBuilder<T> inSub(SFunction<T, ?> column, Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        wrapper.inSql(column, buildSubQuery(entityClass, consumer));
        return this;
    }

    /**
     * 添加 NOT IN 子查询条件。
     *
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return 当前查询构造辅助对象
     */
    public <S> LambdaQueryBuilder<T> notInSub(SFunction<T, ?> column, Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        wrapper.notInSql(column, buildSubQuery(entityClass, consumer));
        return this;
    }

    /**
     * 添加 EXISTS 子查询条件。
     * <pre>{@code
     * QueryBuilder.lambda(SysUser.class)
     *     .existsSub(SysUserRole.class, sub -> sub
     *         .selectCountAll()
     *         .eqColumn(SysUserRole::getUserId, SysUser::getUserId)
     *         .eq(SysUserRole::getRoleId, roleId))
     *     .build();
     * }</pre>
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return 当前查询构造辅助对象
     */
    public <S> LambdaQueryBuilder<T> existsSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        wrapper.exists(buildSubQuery(entityClass, consumer));
        return this;
    }

    /**
     * 添加 NOT EXISTS 子查询条件。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return 当前查询构造辅助对象
     */
    public <S> LambdaQueryBuilder<T> notExistsSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        wrapper.notExists(buildSubQuery(entityClass, consumer));
        return this;
    }

    /**
     * 添加等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> eq(SFunction<T, ?> column, Object value) {
        wrapper.eq(column, value);
        return this;
    }

    /**
     * 添加等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> eq(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.eq(condition, column, value);
        return this;
    }

    /**
     * 添加不等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> ne(SFunction<T, ?> column, Object value) {
        wrapper.ne(column, value);
        return this;
    }

    /**
     * 添加不等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> ne(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.ne(condition, column, value);
        return this;
    }

    /**
     * 添加大于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> gt(SFunction<T, ?> column, Object value) {
        wrapper.gt(column, value);
        return this;
    }

    /**
     * 添加大于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> gt(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.gt(condition, column, value);
        return this;
    }

    /**
     * 添加大于等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> ge(SFunction<T, ?> column, Object value) {
        wrapper.ge(column, value);
        return this;
    }

    /**
     * 添加大于等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> ge(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.ge(condition, column, value);
        return this;
    }

    /**
     * 添加小于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> lt(SFunction<T, ?> column, Object value) {
        wrapper.lt(column, value);
        return this;
    }

    /**
     * 添加小于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> lt(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.lt(condition, column, value);
        return this;
    }

    /**
     * 添加小于等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> le(SFunction<T, ?> column, Object value) {
        wrapper.le(column, value);
        return this;
    }

    /**
     * 添加小于等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> le(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.le(condition, column, value);
        return this;
    }

    /**
     * 添加模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> like(SFunction<T, ?> column, Object value) {
        wrapper.like(column, value);
        return this;
    }

    /**
     * 添加模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> like(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.like(condition, column, value);
        return this;
    }

    /**
     * 添加非模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notLike(SFunction<T, ?> column, Object value) {
        wrapper.notLike(column, value);
        return this;
    }

    /**
     * 添加非模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notLike(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.notLike(condition, column, value);
        return this;
    }

    /**
     * 添加左模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> likeLeft(SFunction<T, ?> column, Object value) {
        wrapper.likeLeft(column, value);
        return this;
    }

    /**
     * 添加左模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> likeLeft(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.likeLeft(condition, column, value);
        return this;
    }

    /**
     * 添加右模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> likeRight(SFunction<T, ?> column, Object value) {
        wrapper.likeRight(column, value);
        return this;
    }

    /**
     * 添加右模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> likeRight(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.likeRight(condition, column, value);
        return this;
    }

    /**
     * 添加区间条件。
     *
     * @param column 字段
     * @param begin  起始值
     * @param end    结束值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> between(SFunction<T, ?> column, Object begin, Object end) {
        wrapper.between(column, begin, end);
        return this;
    }

    /**
     * 添加区间条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param begin     起始值
     * @param end       结束值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> between(boolean condition, SFunction<T, ?> column, Object begin, Object end) {
        wrapper.between(condition, column, begin, end);
        return this;
    }

    /**
     * 添加非区间条件。
     *
     * @param column 字段
     * @param begin  起始值
     * @param end    结束值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notBetween(SFunction<T, ?> column, Object begin, Object end) {
        wrapper.notBetween(column, begin, end);
        return this;
    }

    /**
     * 添加非区间条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param begin     起始值
     * @param end       结束值
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notBetween(boolean condition, SFunction<T, ?> column, Object begin, Object end) {
        wrapper.notBetween(condition, column, begin, end);
        return this;
    }

    /**
     * 添加全部等于条件。
     *
     * @param params      字段和值映射
     * @param null2IsNull 值为空时是否转为 IS NULL 条件
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> allEq(Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(true, (Map) params, null2IsNull);
        return this;
    }

    /**
     * 添加全部等于条件。
     *
     * @param condition   是否添加该条件
     * @param params      字段和值映射
     * @param null2IsNull 值为空时是否转为 IS NULL 条件
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> allEq(boolean condition, Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(condition, (Map) params, null2IsNull);
        return this;
    }

    /**
     * 添加经过过滤的全部等于条件。
     *
     * @param filter      字段和值过滤器
     * @param params      字段和值映射
     * @param null2IsNull 值为空时是否转为 IS NULL 条件
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> allEq(BiPredicate<SFunction<T, ?>, Object> filter, Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(true, (BiPredicate) filter, (Map) params, null2IsNull);
        return this;
    }

    /**
     * 添加经过过滤的全部等于条件。
     *
     * @param condition   是否添加该条件
     * @param filter      字段和值过滤器
     * @param params      字段和值映射
     * @param null2IsNull 值为空时是否转为 IS NULL 条件
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> allEq(boolean condition, BiPredicate<SFunction<T, ?>, Object> filter, Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(condition, (BiPredicate) filter, (Map) params, null2IsNull);
        return this;
    }

    /**
     * 添加字段为空条件。
     *
     * @param column 字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> isNull(SFunction<T, ?> column) {
        wrapper.isNull(column);
        return this;
    }

    /**
     * 添加字段为空条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> isNull(boolean condition, SFunction<T, ?> column) {
        wrapper.isNull(condition, column);
        return this;
    }

    /**
     * 添加字段非空条件。
     *
     * @param column 字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> isNotNull(SFunction<T, ?> column) {
        wrapper.isNotNull(column);
        return this;
    }

    /**
     * 添加字段非空条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> isNotNull(boolean condition, SFunction<T, ?> column) {
        wrapper.isNotNull(condition, column);
        return this;
    }

    /**
     * 添加包含集合条件。
     *
     * @param column 字段
     * @param values 条件值集合
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> in(SFunction<T, ?> column, Collection<?> values) {
        wrapper.in(column, values);
        return this;
    }

    /**
     * 添加包含集合条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值集合
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> in(boolean condition, SFunction<T, ?> column, Collection<?> values) {
        wrapper.in(condition, column, values);
        return this;
    }

    /**
     * 添加包含数组条件。
     *
     * @param column 字段
     * @param values 条件值数组
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> in(SFunction<T, ?> column, Object... values) {
        wrapper.in(column, values);
        return this;
    }

    /**
     * 添加包含数组条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值数组
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        wrapper.in(condition, column, values);
        return this;
    }

    /**
     * 添加不包含集合条件。
     *
     * @param column 字段
     * @param values 条件值集合
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notIn(SFunction<T, ?> column, Collection<?> values) {
        wrapper.notIn(column, values);
        return this;
    }

    /**
     * 添加不包含集合条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值集合
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> values) {
        wrapper.notIn(condition, column, values);
        return this;
    }

    /**
     * 添加不包含数组条件。
     *
     * @param column 字段
     * @param values 条件值数组
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notIn(SFunction<T, ?> column, Object... values) {
        wrapper.notIn(column, values);
        return this;
    }

    /**
     * 添加不包含数组条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值数组
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        wrapper.notIn(condition, column, values);
        return this;
    }

    /**
     * 添加分组字段。
     *
     * @param columns 分组字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> groupBy(SFunction<T, ?>... columns) {
        wrapper.groupBy(Arrays.asList(columns));
        return this;
    }

    /**
     * 添加分组字段。
     *
     * @param condition 是否添加该分组
     * @param columns   分组字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> groupBy(boolean condition, SFunction<T, ?>... columns) {
        wrapper.groupBy(condition, Arrays.asList(columns));
        return this;
    }

    /**
     * 添加排序条件。
     *
     * @param condition 是否添加该排序
     * @param isAsc     是否升序
     * @param column    排序字段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> orderBy(boolean condition, boolean isAsc, SFunction<T, ?> column) {
        wrapper.orderBy(condition, isAsc, column);
        return this;
    }

    /**
     * 添加升序排序字段。
     *
     * @param columns 排序字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByAsc(SFunction<T, ?>... columns) {
        wrapper.orderByAsc(Arrays.asList(columns));
        return this;
    }

    /**
     * 添加升序排序字段。
     *
     * @param condition 是否添加该排序
     * @param columns   排序字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByAsc(boolean condition, SFunction<T, ?>... columns) {
        wrapper.orderByAsc(condition, Arrays.asList(columns));
        return this;
    }

    /**
     * 添加降序排序字段。
     *
     * @param columns 排序字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByDesc(SFunction<T, ?>... columns) {
        wrapper.orderByDesc(Arrays.asList(columns));
        return this;
    }

    /**
     * 添加降序排序字段。
     *
     * @param condition 是否添加该排序
     * @param columns   排序字段
     * @return 当前查询构造辅助对象
     */
    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByDesc(boolean condition, SFunction<T, ?>... columns) {
        wrapper.orderByDesc(condition, Arrays.asList(columns));
        return this;
    }

    /**
     * 添加 HAVING 条件。
     *
     * @param sqlHaving HAVING SQL 片段
     * @param params    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> having(String sqlHaving, Object... params) {
        wrapper.having(sqlHaving, params);
        return this;
    }

    /**
     * 添加 HAVING 条件。
     *
     * @param condition 是否添加该条件
     * @param sqlHaving HAVING SQL 片段
     * @param params    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> having(boolean condition, String sqlHaving, Object... params) {
        wrapper.having(condition, sqlHaving, params);
        return this;
    }

    /**
     * 添加 AND 嵌套条件。
     *
     * @param consumer 条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> and(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.and(consumer);
        return this;
    }

    /**
     * 添加 AND 嵌套条件。
     *
     * @param condition 是否添加该条件
     * @param consumer  条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> and(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.and(condition, consumer);
        return this;
    }

    /**
     * 添加 OR 拼接。
     *
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> or() {
        wrapper.or();
        return this;
    }

    /**
     * 添加 OR 嵌套条件。
     *
     * @param consumer 条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> or(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.or(consumer);
        return this;
    }

    /**
     * 添加 OR 嵌套条件。
     *
     * @param condition 是否添加该条件
     * @param consumer  条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> or(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.or(condition, consumer);
        return this;
    }

    /**
     * 添加普通嵌套条件。
     *
     * @param consumer 条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> nested(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.nested(consumer);
        return this;
    }

    /**
     * 添加普通嵌套条件。
     *
     * @param condition 是否添加该条件
     * @param consumer  条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> nested(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.nested(condition, consumer);
        return this;
    }

    /**
     * 添加 EXISTS 条件。
     *
     * @param existsSql EXISTS SQL 片段
     * @param values    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> exists(String existsSql, Object... values) {
        wrapper.exists(existsSql, values);
        return this;
    }

    /**
     * 添加 EXISTS 条件。
     *
     * @param condition 是否添加该条件
     * @param existsSql EXISTS SQL 片段
     * @param values    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> exists(boolean condition, String existsSql, Object... values) {
        wrapper.exists(condition, existsSql, values);
        return this;
    }

    /**
     * 添加字段等于 SQL 片段条件。
     *
     * @param column  字段
     * @param inValue SQL 片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> eqSql(SFunction<T, ?> column, String inValue) {
        wrapper.eqSql(true, column, inValue);
        return this;
    }

    /**
     * 添加字段等于 SQL 片段条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param inValue   SQL 片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> eqSql(boolean condition, SFunction<T, ?> column, String inValue) {
        wrapper.eqSql(condition, column, inValue);
        return this;
    }

    /**
     * 添加字段 IN SQL 片段条件。
     *
     * @param column  字段
     * @param inValue SQL 片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> inSql(SFunction<T, ?> column, String inValue) {
        wrapper.inSql(true, column, inValue);
        return this;
    }

    /**
     * 添加字段 IN SQL 片段条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param inValue   SQL 片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> inSql(boolean condition, SFunction<T, ?> column, String inValue) {
        wrapper.inSql(condition, column, inValue);
        return this;
    }

    /**
     * 添加字段 NOT IN SQL 片段条件。
     *
     * @param column  字段
     * @param inValue SQL 片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notInSql(SFunction<T, ?> column, String inValue) {
        wrapper.notInSql(true, column, inValue);
        return this;
    }

    /**
     * 添加字段 NOT IN SQL 片段条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param inValue   SQL 片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notInSql(boolean condition, SFunction<T, ?> column, String inValue) {
        wrapper.notInSql(condition, column, inValue);
        return this;
    }

    /**
     * 添加 NOT EXISTS 条件。
     *
     * @param existsSql EXISTS SQL 片段
     * @param values    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notExists(String existsSql, Object... values) {
        wrapper.notExists(existsSql, values);
        return this;
    }

    /**
     * 添加 NOT EXISTS 条件。
     *
     * @param condition 是否添加该条件
     * @param existsSql EXISTS SQL 片段
     * @param values    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> notExists(boolean condition, String existsSql, Object... values) {
        wrapper.notExists(condition, existsSql, values);
        return this;
    }

    /**
     * 拼接 SQL 片段条件。
     *
     * @param applySql SQL 片段
     * @param values   SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> apply(String applySql, Object... values) {
        wrapper.apply(applySql, values);
        return this;
    }

    /**
     * 拼接 SQL 片段条件。
     *
     * @param condition 是否添加该条件
     * @param applySql  SQL 片段
     * @param values    SQL 片段参数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> apply(boolean condition, String applySql, Object... values) {
        wrapper.apply(condition, applySql, values);
        return this;
    }

    /**
     * 直接对底层查询包装器应用自定义处理。
     *
     * @param consumer 查询包装器处理函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> apply(Consumer<LambdaQueryWrapper<T>> consumer) {
        consumer.accept(wrapper);
        return this;
    }

    /**
     * 使用函数式方式追加条件。
     *
     * @param consumer 条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> func(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.func(true, consumer);
        return this;
    }

    /**
     * 使用函数式方式追加条件。
     *
     * @param condition 是否添加该条件
     * @param consumer  条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> func(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.func(condition, consumer);
        return this;
    }

    /**
     * 添加 NOT 嵌套条件。
     *
     * @param consumer 条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> not(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.not(true, consumer);
        return this;
    }

    /**
     * 添加 NOT 嵌套条件。
     *
     * @param condition 是否添加该条件
     * @param consumer  条件构造函数
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> not(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.not(condition, consumer);
        return this;
    }

    /**
     * 添加 SQL 注释。
     *
     * @param comment SQL 注释内容
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> comment(String comment) {
        wrapper.comment(true, comment);
        return this;
    }

    /**
     * 添加 SQL 注释。
     *
     * @param condition 是否添加该注释
     * @param comment   SQL 注释内容
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> comment(boolean condition, String comment) {
        wrapper.comment(condition, comment);
        return this;
    }

    /**
     * 拼接 SQL 尾部片段。
     *
     * @param lastSql SQL 尾部片段
     * @return 当前查询构造辅助对象
     */
    public LambdaQueryBuilder<T> last(String lastSql) {
        wrapper.last(lastSql);
        return this;
    }

    /**
     * 获取底层 Lambda 查询包装器。
     *
     * @return Lambda 查询包装器
     */
    public LambdaQueryWrapper<T> build() {
        return wrapper;
    }

    /**
     * 指定聚合查询字段。
     *
     * @param function 聚合函数
     * @param column   聚合字段
     * @param alias    查询别名
     * @return 当前查询构造辅助对象
     */
    private LambdaQueryBuilder<T> selectAggregate(SqlAggregateFunction function, SFunction<T, ?> column, String alias) {
        return selectAggregate(function, aggregateWrapper().columnName(column), alias);
    }

    /**
     * 指定聚合查询字段。
     *
     * @param function 聚合函数
     * @param column   聚合字段 SQL
     * @param alias    查询别名
     * @return 当前查询构造辅助对象
     */
    private LambdaQueryBuilder<T> selectAggregate(SqlAggregateFunction function, String column, String alias) {
        aggregateWrapper().appendSelectSql(AggregateSelectUtils.aggregateSelect(function, column, alias));
        return this;
    }

    /**
     * 构建子查询 SQL。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return 子查询 SQL
     */
    private <S> String buildSubQuery(Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        SubQuery<S> subQuery = SubQuery.of(entityClass, aggregateWrapper()::formatSubqueryParam);
        consumer.accept(subQuery);
        return subQuery.build();
    }

    /**
     * 获取支持聚合查询字段的包装器。
     *
     * @return 聚合查询包装器
     */
    @SuppressWarnings("unchecked")
    private AggregateLambdaQueryWrapper<T> aggregateWrapper() {
        return (AggregateLambdaQueryWrapper<T>) wrapper;
    }

}

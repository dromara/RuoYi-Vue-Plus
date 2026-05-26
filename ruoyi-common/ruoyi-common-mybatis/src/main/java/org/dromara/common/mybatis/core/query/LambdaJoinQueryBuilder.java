package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.yulichang.toolkit.LambdaUtils;
import com.github.yulichang.toolkit.support.ColumnCache;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.github.yulichang.wrapper.enums.DefaultFuncEnum;
import com.github.yulichang.wrapper.segments.SelectCache;
import com.github.yulichang.wrapper.segments.SelectNormal;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.helper.DataBaseHelper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MPJ Lambda 联表查询构造辅助类。
 *
 * @param <T> 主表实体类型
 * @author Lion Li
 */
public final class LambdaJoinQueryBuilder<T> {

    /**
     * MyBatis-Plus-Join Lambda 查询包装器。
     */
    private final MPJLambdaWrapper<T> wrapper;

    /**
     * 构造 MPJ Lambda 联表查询构造辅助对象。
     *
     * @param wrapper MPJ Lambda 查询包装器
     */
    LambdaJoinQueryBuilder(MPJLambdaWrapper<T> wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * 添加去重查询。
     *
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> distinct() {
        wrapper.distinct();
        return this;
    }

    /**
     * 指定主表查询字段。
     *
     * @param columns 查询字段
     * @return 当前联表查询构造辅助对象
     */
    @SafeVarargs
    public final <E> LambdaJoinQueryBuilder<T> select(SFunction<E, ?>... columns) {
        wrapper.select(columns);
        return this;
    }

    /**
     * 指定带表别名的同名映射查询字段。
     *
     * @param alias   表别名
     * @param columns 查询字段
     * @return 当前联表查询构造辅助对象
     */
    @SafeVarargs
    public final <E> LambdaJoinQueryBuilder<T> select(String alias, SFunction<E, ?>... columns) {
        if (columns == null || columns.length == 0) {
            return this;
        }
        Class<?> entityClass = LambdaUtils.getEntityClass(columns[0]);
        Map<String, SelectCache> cacheMap = ColumnCache.getMapField(entityClass);
        for (SFunction<E, ?> column : columns) {
            wrapper.getSelectColum().add(new SelectNormal(cacheMap.get(LambdaUtils.getName(column)), wrapper.getIndex(), true, alias));
        }
        return this;
    }

    /**
     * 查询主表全部字段。
     *
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> selectAll() {
        wrapper.selectAll();
        return this;
    }

    /**
     * 查询指定实体全部字段。
     *
     * @param entityClass 实体类型
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> selectAll(Class<?> entityClass) {
        wrapper.selectAll(entityClass);
        return this;
    }

    /**
     * 查询指定别名实体全部字段。
     *
     * @param entityClass 实体类型
     * @param alias       表别名
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> selectAll(Class<?> entityClass, String alias) {
        wrapper.selectAll(entityClass, alias);
        return this;
    }

    /**
     * 指定查询字段并映射到返回对象字段。
     *
     * @param column 查询字段
     * @param alias  返回对象字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectAs(SFunction<S, ?> column, SFunction<X, ?> alias) {
        wrapper.selectAs(column, alias);
        return this;
    }

    /**
     * 指定带表别名的查询字段并映射到返回对象字段。
     *
     * @param tableAlias 表别名
     * @param column     查询字段
     * @param alias      返回对象字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectAs(String tableAlias, SFunction<S, ?> column, SFunction<X, ?> alias) {
        wrapper.selectAs(tableAlias, column, alias);
        return this;
    }

    /**
     * 指定 SQL 查询片段并映射到返回对象字段。
     *
     * @param column SQL 查询片段
     * @param alias  返回对象字段
     * @return 当前联表查询构造辅助对象
     */
    public <X> LambdaJoinQueryBuilder<T> selectAs(String column, SFunction<X, ?> alias) {
        wrapper.selectAs(column, alias);
        return this;
    }

    /**
     * 指定子查询字段。
     * <pre>{@code
     * QueryBuilder.lambdaJoin("u", SysUser.class)
     *     .selectAs("u", SysUser::getUserId, UserStatVo::getUserId)
     *     .selectSub(SysUserRole.class, sub -> sub
     *         .selectCountAll()
     *         .eqColumn(SysUserRole::getUserId, "u", SysUser::getUserId),
     *         UserStatVo::getRoleCount)
     *     .list(UserStatVo.class);
     * }</pre>
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param alias       查询别名
     * @param <S>         子查询实体类型
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer, String alias) {
        SubQuery<S> subQuery = buildPlaceholderSubQuery(entityClass, consumer);
        wrapper.selectFunc("(" + subQuery.build() + ")", func -> func.values(subQuery.params()),
            AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定子查询字段。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param alias       查询别名字段
     * @param <S>         子查询实体类型
     * @param <X>         查询结果类型
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer, SFunction<X, ?> alias) {
        return selectSub(entityClass, consumer, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectSum(SFunction<S, ?> column, String alias) {
        wrapper.selectSum(column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param tableAlias 表别名
     * @param column     聚合字段
     * @param alias      查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectSum(String tableAlias, SFunction<S, ?> column, String alias) {
        wrapper.selectFunc(DefaultFuncEnum.SUM, tableAlias, column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectSum(SFunction<S, ?> column, SFunction<X, ?> alias) {
        return selectSum(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectMax(SFunction<S, ?> column, String alias) {
        wrapper.selectMax(column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param tableAlias 表别名
     * @param column     聚合字段
     * @param alias      查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectMax(String tableAlias, SFunction<S, ?> column, String alias) {
        wrapper.selectFunc(DefaultFuncEnum.MAX, tableAlias, column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectMax(SFunction<S, ?> column, SFunction<X, ?> alias) {
        return selectMax(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectMin(SFunction<S, ?> column, String alias) {
        wrapper.selectMin(column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param tableAlias 表别名
     * @param column     聚合字段
     * @param alias      查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectMin(String tableAlias, SFunction<S, ?> column, String alias) {
        wrapper.selectFunc(DefaultFuncEnum.MIN, tableAlias, column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectMin(SFunction<S, ?> column, SFunction<X, ?> alias) {
        return selectMin(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectAvg(SFunction<S, ?> column, String alias) {
        wrapper.selectAvg(column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param tableAlias 表别名
     * @param column     聚合字段
     * @param alias      查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectAvg(String tableAlias, SFunction<S, ?> column, String alias) {
        wrapper.selectFunc(DefaultFuncEnum.AVG, tableAlias, column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectAvg(SFunction<S, ?> column, SFunction<X, ?> alias) {
        return selectAvg(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectCount(SFunction<S, ?> column, String alias) {
        wrapper.selectCount(column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param tableAlias 表别名
     * @param column     聚合字段
     * @param alias      查询别名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> selectCount(String tableAlias, SFunction<S, ?> column, String alias) {
        wrapper.selectFunc(DefaultFuncEnum.COUNT, tableAlias, column, AggregateSelectUtils.checkAlias(alias));
        return this;
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> selectCount(SFunction<S, ?> column, SFunction<X, ?> alias) {
        return selectCount(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT(*) 聚合查询字段。
     *
     * @param alias 查询别名
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> selectCountAll(String alias) {
        wrapper.select(AggregateSelectUtils.aggregateSelect(SqlAggregateFunction.COUNT, "*", alias));
        return this;
    }

    /**
     * 指定 COUNT(*) 聚合查询字段。
     *
     * @param alias 查询别名字段
     * @return 当前联表查询构造辅助对象
     */
    public <X> LambdaJoinQueryBuilder<T> selectCountAll(SFunction<X, ?> alias) {
        return selectCountAll(AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 添加等于子查询条件。
     *
     * @param alias       表别名
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         字段实体类型
     * @param <Q>         子查询实体类型
     * @return 当前联表查询构造辅助对象
     */
    public <S, Q> LambdaJoinQueryBuilder<T> eqSub(String alias, SFunction<S, ?> column, Class<Q> entityClass,
                                                  Consumer<SubQuery<Q>> consumer) {
        SubQuery<Q> subQuery = buildPlaceholderSubQuery(entityClass, consumer);
        wrapper.apply(true, qualifiedColumn(alias, column) + " = (" + subQuery.build() + ")", subQuery.params());
        return this;
    }

    /**
     * 添加 IN 子查询条件。
     * <pre>{@code
     * QueryBuilder.lambdaJoin("u", SysUser.class)
     *     .inSub("u", SysUser::getUserId, SysUserRole.class, sub -> sub
     *         .select(SysUserRole::getUserId)
     *         .eq(SysUserRole::getRoleId, roleId))
     *     .list(SysUserVo.class);
     * }</pre>
     *
     * @param alias       表别名
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         字段实体类型
     * @param <Q>         子查询实体类型
     * @return 当前联表查询构造辅助对象
     */
    public <S, Q> LambdaJoinQueryBuilder<T> inSub(String alias, SFunction<S, ?> column, Class<Q> entityClass,
                                                  Consumer<SubQuery<Q>> consumer) {
        SubQuery<Q> subQuery = buildPlaceholderSubQuery(entityClass, consumer);
        wrapper.apply(true, qualifiedColumn(alias, column) + " IN (" + subQuery.build() + ")", subQuery.params());
        return this;
    }

    /**
     * 添加 NOT IN 子查询条件。
     *
     * @param alias       表别名
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         字段实体类型
     * @param <Q>         子查询实体类型
     * @return 当前联表查询构造辅助对象
     */
    public <S, Q> LambdaJoinQueryBuilder<T> notInSub(String alias, SFunction<S, ?> column, Class<Q> entityClass,
                                                     Consumer<SubQuery<Q>> consumer) {
        SubQuery<Q> subQuery = buildPlaceholderSubQuery(entityClass, consumer);
        wrapper.apply(true, qualifiedColumn(alias, column) + " NOT IN (" + subQuery.build() + ")", subQuery.params());
        return this;
    }

    /**
     * 添加 EXISTS 子查询条件。
     * <pre>{@code
     * QueryBuilder.lambdaJoin("u", SysUser.class)
     *     .existsSub(SysUserRole.class, sub -> sub
     *         .selectCountAll()
     *         .eqColumn(SysUserRole::getUserId, "u", SysUser::getUserId)
     *         .eq(SysUserRole::getRoleId, roleId))
     *     .list(SysUserVo.class);
     * }</pre>
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <Q>         子查询实体类型
     * @return 当前联表查询构造辅助对象
     */
    public <Q> LambdaJoinQueryBuilder<T> existsSub(Class<Q> entityClass, Consumer<SubQuery<Q>> consumer) {
        SubQuery<Q> subQuery = buildPlaceholderSubQuery(entityClass, consumer);
        wrapper.apply(true, "EXISTS (" + subQuery.build() + ")", subQuery.params());
        return this;
    }

    /**
     * 添加 NOT EXISTS 子查询条件。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <Q>         子查询实体类型
     * @return 当前联表查询构造辅助对象
     */
    public <Q> LambdaJoinQueryBuilder<T> notExistsSub(Class<Q> entityClass, Consumer<SubQuery<Q>> consumer) {
        SubQuery<Q> subQuery = buildPlaceholderSubQuery(entityClass, consumer);
        wrapper.apply(true, "NOT EXISTS (" + subQuery.build() + ")", subQuery.params());
        return this;
    }

    /**
     * 添加左联表。
     *
     * @param entityClass 关联实体类型
     * @param left        关联实体字段
     * @param right       当前查询字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> leftJoin(Class<S> entityClass, SFunction<S, ?> left, SFunction<X, ?> right) {
        wrapper.leftJoin(entityClass, left, right);
        return this;
    }

    /**
     * 添加带别名的左联表。
     *
     * @param entityClass 关联实体类型
     * @param alias       关联表别名
     * @param left        关联实体字段
     * @param right       当前查询字段
     * @return 当前联表查询构造辅助对象
     */
    public <S, X> LambdaJoinQueryBuilder<T> leftJoin(Class<S> entityClass, String alias, SFunction<S, ?> left, SFunction<X, ?> right) {
        wrapper.leftJoin(entityClass, alias, left, right);
        return this;
    }

    /**
     * 添加等于条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> eq(String alias, SFunction<S, ?> column, Object value) {
        return eq(true, alias, column, value);
    }

    /**
     * 添加等于条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param value     条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> eq(boolean condition, String alias, SFunction<S, ?> column, Object value) {
        wrapper.eq(condition, alias, column, value);
        return this;
    }

    /**
     * 值不为空时添加等于条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> eqIfPresent(String alias, SFunction<S, ?> column, Object value) {
        return eq(value != null, alias, column, value);
    }

    /**
     * 文本不为空白时添加等于条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> eqIfText(String alias, SFunction<S, ?> column, String value) {
        return eq(StringUtils.isNotBlank(value), alias, column, value);
    }

    /**
     * 添加不等于条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> ne(String alias, SFunction<S, ?> column, Object value) {
        return ne(true, alias, column, value);
    }

    /**
     * 添加不等于条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param value     条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> ne(boolean condition, String alias, SFunction<S, ?> column, Object value) {
        wrapper.ne(condition, alias, column, value);
        return this;
    }

    /**
     * 文本不为空白时添加不等于条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> neIfText(String alias, SFunction<S, ?> column, String value) {
        return ne(StringUtils.isNotBlank(value), alias, column, value);
    }

    /**
     * 添加模糊匹配条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> like(String alias, SFunction<S, ?> column, Object value) {
        return like(true, alias, column, value);
    }

    /**
     * 添加模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param value     条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> like(boolean condition, String alias, SFunction<S, ?> column, Object value) {
        wrapper.like(condition, alias, column, value);
        return this;
    }

    /**
     * 文本不为空白时添加模糊匹配条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param value  条件值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> likeIfText(String alias, SFunction<S, ?> column, String value) {
        return like(StringUtils.isNotBlank(value), alias, column, value);
    }

    /**
     * 添加区间条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param begin     起始值
     * @param end       结束值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> between(boolean condition, String alias, SFunction<S, ?> column, Object begin, Object end) {
        wrapper.between(condition, alias, column, begin, end);
        return this;
    }

    /**
     * 起止值均不为空时添加区间条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param begin  起始值
     * @param end    结束值
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> betweenIfPresent(String alias, SFunction<S, ?> column, Object begin, Object end) {
        return between(begin != null && end != null, alias, column, begin, end);
    }

    /**
     * 从参数 Map 中读取起止值，均不为空时添加区间条件。
     *
     * @param alias    表别名
     * @param column   字段
     * @param params   参数 Map
     * @param beginKey 起始值参数名
     * @param endKey   结束值参数名
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> betweenParams(String alias, SFunction<S, ?> column, Map<String, Object> params, String beginKey, String endKey) {
        if (params == null) {
            return between(false, alias, column, null, null);
        }
        Object begin = params.get(beginKey);
        Object end = params.get(endKey);
        return between(begin != null && end != null, alias, column, begin, end);
    }

    /**
     * 添加包含集合条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param values    条件值集合
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> in(boolean condition, String alias, SFunction<S, ?> column, Collection<?> values) {
        wrapper.in(condition, alias, column, values);
        return this;
    }

    /**
     * 添加包含数组条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param values    条件值数组
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> in(boolean condition, String alias, SFunction<S, ?> column, Object... values) {
        wrapper.in(condition, alias, column, values);
        return this;
    }

    /**
     * 添加包含集合条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param values 条件值集合
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> in(String alias, SFunction<S, ?> column, Collection<?> values) {
        return in(true, alias, column, values);
    }

    /**
     * 添加包含数组条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param values 条件值数组
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> in(String alias, SFunction<S, ?> column, Object... values) {
        return in(true, alias, column, values);
    }

    /**
     * 集合不为空时添加包含条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param values 条件值集合
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> inIfNotEmpty(String alias, SFunction<S, ?> column, Collection<?> values) {
        return in(values != null && !values.isEmpty(), alias, column, values);
    }

    /**
     * 数组不为空时添加包含条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param values 条件值数组
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> inIfNotEmpty(String alias, SFunction<S, ?> column, Object... values) {
        return in(values != null && values.length > 0, alias, column, values);
    }

    /**
     * 添加不包含集合条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @param values    条件值集合
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> notIn(boolean condition, String alias, SFunction<S, ?> column, Collection<?> values) {
        wrapper.notIn(condition, alias, column, values);
        return this;
    }

    /**
     * 集合不为空时添加不包含条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @param values 条件值集合
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> notInIfNotEmpty(String alias, SFunction<S, ?> column, Collection<?> values) {
        return notIn(values != null && !values.isEmpty(), alias, column, values);
    }

    /**
     * 添加字段非空条件。
     *
     * @param alias  表别名
     * @param column 字段
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> isNotNull(String alias, SFunction<S, ?> column) {
        return isNotNull(true, alias, column);
    }

    /**
     * 添加字段非空条件。
     *
     * @param condition 是否添加该条件
     * @param alias     表别名
     * @param column    字段
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> isNotNull(boolean condition, String alias, SFunction<S, ?> column) {
        wrapper.isNotNull(condition, alias, column);
        return this;
    }

    /**
     * 添加升序排序。
     *
     * @param alias  表别名
     * @param column 字段
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> orderByAsc(String alias, SFunction<S, ?> column) {
        wrapper.orderByAsc(alias, column);
        return this;
    }

    /**
     * 添加降序排序。
     *
     * @param alias  表别名
     * @param column 字段
     * @return 当前联表查询构造辅助对象
     */
    public <S> LambdaJoinQueryBuilder<T> orderByDesc(String alias, SFunction<S, ?> column) {
        wrapper.orderByDesc(alias, column);
        return this;
    }

    /**
     * 拼接 SQL 片段条件。
     *
     * @param condition 是否添加该条件
     * @param applySql  SQL 片段
     * @param values    SQL 片段参数
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> apply(boolean condition, String applySql, Object... values) {
        wrapper.apply(condition, applySql, values);
        return this;
    }

    /**
     * 添加 FIND_IN_SET 条件。
     *
     * @param condition 是否添加该条件
     * @param value     匹配值
     * @param column    字段名
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> findInSet(boolean condition, Object value, String column) {
        return apply(condition, DataBaseHelper.findInSet(value, column));
    }

    /**
     * 使用函数式方式追加 MPJ 原生能力。
     *
     * @param consumer MPJ 查询包装器消费函数
     * @return 当前联表查询构造辅助对象
     */
    public LambdaJoinQueryBuilder<T> apply(Consumer<MPJLambdaWrapper<T>> consumer) {
        consumer.accept(wrapper);
        return this;
    }

    /**
     * 查询列表。
     *
     * @param resultClass 返回对象类型
     * @param <R>         返回对象类型
     * @return 查询结果
     */
    public <R> List<R> list(Class<R> resultClass) {
        return wrapper.list(resultClass);
    }

    /**
     * 分页查询。
     *
     * @param page        分页对象
     * @param resultClass 返回对象类型
     * @param <R>         返回对象类型
     * @param <P>         分页类型
     * @return 分页结果
     */
    public <R, P extends IPage<R>> P page(P page, Class<R> resultClass) {
        return wrapper.page(page, resultClass);
    }

    /**
     * 查询数量。
     *
     * @return 数量
     */
    public Long count() {
        return wrapper.count();
    }

    /**
     * 获取底层 MPJ Lambda 查询包装器。
     *
     * @return MPJ Lambda 查询包装器
     */
    public MPJLambdaWrapper<T> build() {
        return wrapper;
    }

    private <Q> SubQuery<Q> buildPlaceholderSubQuery(Class<Q> entityClass, Consumer<SubQuery<Q>> consumer) {
        SubQuery<Q> subQuery = SubQuery.ofPlaceholders(entityClass);
        consumer.accept(subQuery);
        return subQuery;
    }

    private <S> String qualifiedColumn(String alias, SFunction<S, ?> column) {
        return AggregateSelectUtils.checkAlias(alias) + StringPool.DOT + ColumnCache.getMapField(LambdaUtils.getEntityClass(column))
            .get(LambdaUtils.getName(column)).getColumn();
    }

}

package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Lambda 子查询构造器。
 * <p>
 * 常用于外层查询的 {@code selectSub}、{@code inSub}、{@code existsSub} 等方法中。
 * </p>
 * <p>
 * 注意：子查询 SQL 由本构造器直接生成，默认会根据 MyBatis-Plus 表元数据追加逻辑删除条件，
 * 但不会自动追加项目数据权限条件；如果子查询实体也需要数据权限过滤，请在子查询条件中显式添加。
 * </p>
 * <pre>{@code
 * userMapper.lambda()
 *     .inSub(SysUser::getUserId, SysUserRole.class, sub -> sub
 *         .select(SysUserRole::getUserId)
 *         .eq(SysUserRole::getRoleId, roleId))
 *     .voList();
 *
 * userMapper.lambda()
 *     .select(SysUser::getUserId, SysUser::getUserName)
 *     .selectSub(SysUserRole.class, sub -> sub
 *         .selectCountAll()
 *         .eqColumn(SysUserRole::getUserId, SysUser::getUserId),
 *         UserStatVo::getRoleCount)
 *     .voList();
 * }</pre>
 *
 * @param <T> 子查询实体类型
 * @author Lion Li
 */
public final class SubQuery<T> {

    private final Class<T> entityClass;
    private final SqlParamFormatter paramFormatter;
    private final boolean placeholderParamMode;
    private final List<String> selects = new ArrayList<>();
    private final List<String> conditions = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();
    private boolean withLogicDelete = true;

    private SubQuery(Class<T> entityClass, SqlParamFormatter paramFormatter) {
        this(entityClass, paramFormatter, false);
    }

    private SubQuery(Class<T> entityClass, SqlParamFormatter paramFormatter, boolean placeholderParamMode) {
        this.entityClass = entityClass;
        this.paramFormatter = paramFormatter;
        this.placeholderParamMode = placeholderParamMode;
    }

    /**
     * 创建子查询。
     *
     * @param entityClass 子查询实体类型
     * @param <T>         子查询实体类型
     * @return 子查询构造器
     */
    public static <T> SubQuery<T> of(Class<T> entityClass) {
        return new SubQuery<>(entityClass, null);
    }

    /**
     * 创建子查询。
     *
     * @param entityClass    子查询实体类型
     * @param paramFormatter SQL 参数格式化器
     * @param <T>            子查询实体类型
     * @return 子查询构造器
     */
    public static <T> SubQuery<T> of(Class<T> entityClass, SqlParamFormatter paramFormatter) {
        return new SubQuery<>(entityClass, paramFormatter);
    }

    /**
     * 创建使用 {@code {0}} 参数占位符的子查询。
     *
     * @param entityClass 子查询实体类型
     * @param <T>         子查询实体类型
     * @return 子查询构造器
     */
    static <T> SubQuery<T> ofPlaceholders(Class<T> entityClass) {
        return new SubQuery<>(entityClass, null, true);
    }

    /**
     * 指定子查询字段。
     * <pre>{@code
     * sub.select(SysUserRole::getUserId)
     * }</pre>
     *
     * @param column 查询字段
     * @return 当前子查询构造器
     */
    public SubQuery<T> select(SFunction<T, ?> column) {
        selects.add(columnName(column));
        return this;
    }

    /**
     * 指定 COUNT(*) 查询字段。
     * <pre>{@code
     * sub.selectCountAll()
     * }</pre>
     *
     * @return 当前子查询构造器
     */
    public SubQuery<T> selectCountAll() {
        selects.add(SqlAggregateFunction.COUNT.format("*"));
        return this;
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前子查询构造器
     */
    public SubQuery<T> selectSum(SFunction<T, ?> column) {
        return selectAggregate(SqlAggregateFunction.SUM, column);
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前子查询构造器
     */
    public SubQuery<T> selectMax(SFunction<T, ?> column) {
        return selectAggregate(SqlAggregateFunction.MAX, column);
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前子查询构造器
     */
    public SubQuery<T> selectMin(SFunction<T, ?> column) {
        return selectAggregate(SqlAggregateFunction.MIN, column);
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前子查询构造器
     */
    public SubQuery<T> selectAvg(SFunction<T, ?> column) {
        return selectAggregate(SqlAggregateFunction.AVG, column);
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @return 当前子查询构造器
     */
    public SubQuery<T> selectCount(SFunction<T, ?> column) {
        return selectAggregate(SqlAggregateFunction.COUNT, column);
    }

    /**
     * 禁用子查询逻辑删除条件。
     * <pre>{@code
     * sub.disableLogicDelete()
     * }</pre>
     *
     * @return 当前子查询构造器
     */
    public SubQuery<T> disableLogicDelete() {
        this.withLogicDelete = false;
        return this;
    }

    /**
     * 添加等于条件。
     * <pre>{@code
     * sub.eq(SysUserRole::getRoleId, roleId)
     * }</pre>
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前子查询构造器
     */
    public SubQuery<T> eq(SFunction<T, ?> column, Object value) {
        return condition(column, Constants.EQUALS, value);
    }

    /**
     * 添加大于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前子查询构造器
     */
    public SubQuery<T> gt(SFunction<T, ?> column, Object value) {
        return condition(column, ">", value);
    }

    /**
     * 添加大于等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前子查询构造器
     */
    public SubQuery<T> ge(SFunction<T, ?> column, Object value) {
        return condition(column, ">=", value);
    }

    /**
     * 添加小于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前子查询构造器
     */
    public SubQuery<T> lt(SFunction<T, ?> column, Object value) {
        return condition(column, "<", value);
    }

    /**
     * 添加小于等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前子查询构造器
     */
    public SubQuery<T> le(SFunction<T, ?> column, Object value) {
        return condition(column, "<=", value);
    }

    /**
     * 添加模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 当前子查询构造器
     */
    public SubQuery<T> like(SFunction<T, ?> column, Object value) {
        return condition(column, "LIKE", "%" + value + "%");
    }

    /**
     * 添加 IN 条件。
     * <pre>{@code
     * sub.in(SysUserRole::getRoleId, roleIds)
     * }</pre>
     *
     * @param column 字段
     * @param values 条件值集合
     * @return 当前子查询构造器
     */
    public SubQuery<T> in(SFunction<T, ?> column, Collection<?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return this;
        }
        conditions.add(columnName(column) + " IN (" + values.stream()
            .map(this::formatParam)
            .collect(Collectors.joining(Constants.COMMA)) + ")");
        return this;
    }

    /**
     * 添加 IN 条件。
     *
     * @param column 字段
     * @param values 条件值数组
     * @return 当前子查询构造器
     */
    public SubQuery<T> in(SFunction<T, ?> column, Object... values) {
        if (values == null || values.length == 0) {
            return this;
        }
        return in(column, Arrays.asList(values));
    }

    /**
     * 添加 BETWEEN 条件。
     *
     * @param column 字段
     * @param begin  起始值
     * @param end    结束值
     * @return 当前子查询构造器
     */
    public SubQuery<T> between(SFunction<T, ?> column, Object begin, Object end) {
        conditions.add(columnName(column) + " BETWEEN " + formatParam(begin) + " AND " + formatParam(end));
        return this;
    }

    /**
     * 添加字段相等条件，用于关联外层查询字段。
     * <pre>{@code
     * sub.eqColumn(SysUserRole::getUserId, SysUser::getUserId)
     * }</pre>
     *
     * @param column      子查询字段
     * @param otherColumn 其他表字段
     * @param <O>         其他表实体类型
     * @return 当前子查询构造器
     */
    public <O> SubQuery<T> eqColumn(SFunction<T, ?> column, SFunction<O, ?> otherColumn) {
        conditions.add(columnName(column) + Constants.EQUALS + qualifiedColumnName(otherColumn));
        return this;
    }

    /**
     * 添加字段相等条件，用于关联外层查询字段。
     * <pre>{@code
     * sub.eqColumn(SysUserRole::getUserId, "u", SysUser::getUserId)
     * }</pre>
     *
     * @param column      子查询字段
     * @param tableAlias  其他表别名
     * @param otherColumn 其他表字段
     * @param <O>         其他表实体类型
     * @return 当前子查询构造器
     */
    public <O> SubQuery<T> eqColumn(SFunction<T, ?> column, String tableAlias, SFunction<O, ?> otherColumn) {
        conditions.add(columnName(column) + Constants.EQUALS + AggregateSelectUtils.checkAlias(tableAlias)
            + StringPool.DOT + columnName(otherColumn));
        return this;
    }

    /**
     * 按条件添加子查询条件。
     *
     * @param condition 是否添加
     * @param consumer  子查询条件
     * @return 当前子查询构造器
     */
    public SubQuery<T> when(boolean condition, Consumer<SubQuery<T>> consumer) {
        if (condition) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * 构建子查询 SQL。
     *
     * @return 子查询 SQL
     */
    public String build() {
        Assert.notEmpty(selects, "子查询必须指定查询字段");
        String sql = "SELECT " + String.join(Constants.COMMA, selects) + " FROM " + tableName();
        List<String> whereConditions = buildWhereConditions();
        if (!whereConditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", whereConditions);
        }
        return sql;
    }

    /**
     * 获取子查询参数。
     *
     * @return 子查询参数数组
     */
    Object[] params() {
        return params.toArray();
    }

    private SubQuery<T> selectAggregate(SqlAggregateFunction function, SFunction<T, ?> column) {
        selects.add(function.format(columnName(column)));
        return this;
    }

    private SubQuery<T> condition(SFunction<T, ?> column, String operator, Object value) {
        conditions.add(columnName(column) + StringPool.SPACE + operator + StringPool.SPACE + formatParam(value));
        return this;
    }

    private String formatParam(Object value) {
        if (placeholderParamMode) {
            params.add(value);
            return "{" + (params.size() - 1) + "}";
        }
        Assert.notNull(paramFormatter, "子查询参数需要在外层查询方法中构造");
        return paramFormatter.format(value);
    }

    private String tableName() {
        return tableInfo().getTableName();
    }

    private List<String> buildWhereConditions() {
        List<String> whereConditions = new ArrayList<>();
        String logicDeleteSql = logicDeleteSql();
        if (StringUtils.isNotBlank(logicDeleteSql)) {
            whereConditions.add(logicDeleteSql);
        }
        whereConditions.addAll(conditions);
        return whereConditions;
    }

    private String logicDeleteSql() {
        if (!withLogicDelete) {
            return StringPool.EMPTY;
        }
        return tableInfo().getLogicDeleteSql(false, true);
    }

    private TableInfo tableInfo() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        Assert.notNull(tableInfo, "无法获取实体表信息: %s", entityClass.getName());
        return tableInfo;
    }

    private String qualifiedColumnName(SFunction<?, ?> column) {
        Class<?> columnEntityClass = LambdaUtils.extract(column).getInstantiatedClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(columnEntityClass);
        Assert.notNull(tableInfo, "无法获取实体表信息: %s", columnEntityClass.getName());
        return tableInfo.getTableName() + StringPool.DOT + columnName(column);
    }

    private static String columnName(SFunction<?, ?> column) {
        LambdaMeta meta = LambdaUtils.extract(column);
        String fieldName = PropertyNamer.methodToProperty(meta.getImplMethodName());
        Map<String, ColumnCache> columnMap = LambdaUtils.getColumnMap(meta.getInstantiatedClass());
        Assert.notNull(columnMap, "can not find lambda cache for this entity [%s]", meta.getInstantiatedClass().getName());
        ColumnCache cache = columnMap.get(LambdaUtils.formatKey(fieldName));
        Assert.notNull(cache, "can not find lambda cache for this property [%s]", fieldName);
        return cache.getColumn();
    }

}

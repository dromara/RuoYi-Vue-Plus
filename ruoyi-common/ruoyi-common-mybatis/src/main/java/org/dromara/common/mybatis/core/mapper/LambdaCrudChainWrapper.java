package org.dromara.common.mybatis.core.mapper;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.dromara.common.mybatis.core.query.AggregateSelectUtils;
import org.dromara.common.mybatis.core.query.LambdaQueryCondition;
import org.dromara.common.mybatis.core.query.SqlAggregateFunction;
import org.dromara.common.mybatis.core.query.SubQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Mapper 级 Lambda CRUD 链式包装器。
 *
 * @param <T> table 泛型
 * @param <V> vo 泛型
 * @author Lion Li
 */
public class LambdaCrudChainWrapper<T, V> extends AbstractLambdaWrapper<T, LambdaCrudChainWrapper<T, V>>
    implements Query<LambdaCrudChainWrapper<T, V>, T, SFunction<T, ?>>,
    Update<LambdaCrudChainWrapper<T, V>, SFunction<T, ?>>,
    LambdaQueryCondition<T, LambdaCrudChainWrapper<T, V>> {

    /**
     * 当前链式操作绑定的 Mapper。
     */
    private final BaseMapperPlus<T, V> crudMapper;

    /**
     * 更新 SET 片段集合。
     */
    private final List<String> sqlSet;

    /**
     * 查询字段 SQL 片段。
     */
    private SharedString sqlSelect = new SharedString();

    /**
     * 构造 Mapper 级 Lambda CRUD 链式包装器。
     *
     * @param crudMapper Mapper 对象
     */
    public LambdaCrudChainWrapper(BaseMapperPlus<T, V> crudMapper) {
        this.crudMapper = crudMapper;
        super.setEntityClass(crudMapper.currentModelClass());
        super.initNeed();
        this.sqlSet = new ArrayList<>();
    }

    /**
     * 构造 Mapper 级 Lambda CRUD 链式包装器实例。
     *
     * @param crudMapper          Mapper 对象
     * @param entity              实体对象
     * @param entityClass         实体类型
     * @param sqlSelect           查询字段 SQL 片段
     * @param sqlSet              更新 set SQL 片段集合
     * @param paramNameSeq        参数名称序列
     * @param paramNameValuePairs 参数名称与参数值映射
     * @param mergeSegments       查询条件表达式
     * @param paramAlias          参数别名
     * @param lastSql             SQL 尾部片段
     * @param sqlComment          SQL 注释片段
     * @param sqlFirst            SQL 起始片段
     */
    LambdaCrudChainWrapper(BaseMapperPlus<T, V> crudMapper, T entity, Class<T> entityClass, SharedString sqlSelect,
                           List<String> sqlSet, AtomicInteger paramNameSeq, Map<String, Object> paramNameValuePairs,
                           MergeSegments mergeSegments, SharedString paramAlias, SharedString lastSql,
                           SharedString sqlComment, SharedString sqlFirst) {
        this.crudMapper = crudMapper;
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.sqlSelect = sqlSelect == null ? new SharedString() : sqlSelect;
        this.sqlSet = sqlSet == null ? new ArrayList<>() : sqlSet;
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    /**
     * 按条件选择查询字段。
     *
     * @param condition 是否选择字段
     * @param columns   查询字段集合
     * @return this
     */
    @Override
    public LambdaCrudChainWrapper<T, V> select(boolean condition, List<SFunction<T, ?>> columns) {
        if (condition && CollectionUtils.isNotEmpty(columns)) {
            this.sqlSelect.setStringValue(columnsToString(false, columns));
        }
        return typedThis;
    }

    /**
     * 选择查询字段。
     *
     * @param columns 查询字段
     * @return this
     */
    @SafeVarargs
    public final LambdaCrudChainWrapper<T, V> select(SFunction<T, ?>... columns) {
        return select(true, CollectionUtils.toList(columns));
    }

    /**
     * 按条件选择查询字段。
     *
     * @param condition 是否选择字段
     * @param columns   查询字段
     * @return this
     */
    @SafeVarargs
    public final LambdaCrudChainWrapper<T, V> select(boolean condition, SFunction<T, ?>... columns) {
        return select(condition, CollectionUtils.toList(columns));
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectSum(SFunction<T, ?> column) {
        return selectSum(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectSum(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.SUM, column, alias);
    }

    /**
     * 指定 SUM 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectSum(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectSum(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectMax(SFunction<T, ?> column) {
        return selectMax(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectMax(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.MAX, column, alias);
    }

    /**
     * 指定 MAX 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectMax(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectMax(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectMin(SFunction<T, ?> column) {
        return selectMin(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectMin(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.MIN, column, alias);
    }

    /**
     * 指定 MIN 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectMin(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectMin(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectAvg(SFunction<T, ?> column) {
        return selectAvg(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectAvg(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.AVG, column, alias);
    }

    /**
     * 指定 AVG 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectAvg(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectAvg(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectCount(SFunction<T, ?> column) {
        return selectCount(column, AggregateSelectUtils.aliasName(column));
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectCount(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.COUNT, column, alias);
    }

    /**
     * 指定 COUNT 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectCount(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectCount(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT(*) 聚合查询字段。
     *
     * @param alias 查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectCountAll(String alias) {
        return selectAggregate(SqlAggregateFunction.COUNT, "*", alias);
    }

    /**
     * 指定 COUNT(*) 聚合查询字段。
     *
     * @param alias 查询别名字段
     * @param <A>   查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectCountAll(SFunction<A, ?> alias) {
        return selectCountAll(AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定 COUNT(DISTINCT column) 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> selectCountDistinct(SFunction<T, ?> column, String alias) {
        return selectAggregate(SqlAggregateFunction.COUNT, "DISTINCT " + columnToString(column), alias);
    }

    /**
     * 指定 COUNT(DISTINCT column) 聚合查询字段。
     *
     * @param column 聚合字段
     * @param alias  查询别名字段
     * @param <A>    查询结果类型
     * @return this
     */
    public <A> LambdaCrudChainWrapper<T, V> selectCountDistinct(SFunction<T, ?> column, SFunction<A, ?> alias) {
        return selectCountDistinct(column, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 指定子查询字段。
     * <pre>{@code
     * userMapper.lambda()
     *     .select(SysUser::getUserId, SysUser::getUserName)
     *     .selectSub(SysUserRole.class, sub -> sub
     *         .selectCountAll()
     *         .eqColumn(SysUserRole::getUserId, SysUser::getUserId),
     *         UserStatVo::getRoleCount)
     *     .voList();
     * }</pre>
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param alias       查询别名
     * @param <S>         子查询实体类型
     * @return this
     */
    public <S> LambdaCrudChainWrapper<T, V> selectSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer, String alias) {
        sqlSelect.setStringValue(AggregateSelectUtils.appendSelect(sqlSelect.getStringValue(),
            AggregateSelectUtils.subquerySelect(buildSubQuery(entityClass, consumer), alias)));
        return typedThis;
    }

    /**
     * 指定子查询字段。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param alias       查询别名字段
     * @param <S>         子查询实体类型
     * @param <A>         查询结果类型
     * @return this
     */
    public <S, A> LambdaCrudChainWrapper<T, V> selectSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer, SFunction<A, ?> alias) {
        return selectSub(entityClass, consumer, AggregateSelectUtils.aliasName(alias));
    }

    /**
     * 添加等于子查询条件。
     * <pre>{@code
     * userMapper.lambda()
     *     .eqSub(SysUser::getDeptId, SysDept.class, sub -> sub
     *         .select(SysDept::getDeptId)
     *         .eq(SysDept::getDeptName, deptName))
     *     .voList();
     * }</pre>
     *
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return this
     */
    public <S> LambdaCrudChainWrapper<T, V> eqSub(SFunction<T, ?> column, Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        return super.eqSql(true, column, buildSubQuery(entityClass, consumer));
    }

    /**
     * 添加 IN 子查询条件。
     * <pre>{@code
     * userMapper.lambda()
     *     .inSub(SysUser::getUserId, SysUserRole.class, sub -> sub
     *         .select(SysUserRole::getUserId)
     *         .eq(SysUserRole::getRoleId, roleId))
     *     .voList();
     * }</pre>
     *
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return this
     */
    public <S> LambdaCrudChainWrapper<T, V> inSub(SFunction<T, ?> column, Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        return super.inSql(true, column, buildSubQuery(entityClass, consumer));
    }

    /**
     * 添加 NOT IN 子查询条件。
     *
     * @param column      字段
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return this
     */
    public <S> LambdaCrudChainWrapper<T, V> notInSub(SFunction<T, ?> column, Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        return super.notInSql(true, column, buildSubQuery(entityClass, consumer));
    }

    /**
     * 添加 EXISTS 子查询条件。
     * <pre>{@code
     * userMapper.lambda()
     *     .existsSub(SysUserRole.class, sub -> sub
     *         .selectCountAll()
     *         .eqColumn(SysUserRole::getUserId, SysUser::getUserId)
     *         .eq(SysUserRole::getRoleId, roleId))
     *     .voList();
     * }</pre>
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return this
     */
    public <S> LambdaCrudChainWrapper<T, V> existsSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        return super.exists(true, buildSubQuery(entityClass, consumer));
    }

    /**
     * 添加 NOT EXISTS 子查询条件。
     *
     * @param entityClass 子查询实体类型
     * @param consumer    子查询构造函数
     * @param <S>         子查询实体类型
     * @return this
     */
    public <S> LambdaCrudChainWrapper<T, V> notExistsSub(Class<S> entityClass, Consumer<SubQuery<S>> consumer) {
        return super.notExists(true, buildSubQuery(entityClass, consumer));
    }

    /**
     * 按字段过滤条件选择查询字段。
     *
     * @param entityClass 实体类型
     * @param predicate   字段过滤条件
     * @return this
     */
    @Override
    public LambdaCrudChainWrapper<T, V> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        if (entityClass == null) {
            entityClass = getEntityClass();
        } else {
            setEntityClass(entityClass);
        }
        Assert.notNull(entityClass, "entityClass can not be null");
        this.sqlSelect.setStringValue(TableInfoHelper.getTableInfo(entityClass).chooseSelect(predicate));
        return typedThis;
    }

    /**
     * 获取查询字段 SQL 片段。
     *
     * @return 查询字段 SQL 片段
     */
    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 指定聚合查询字段。
     *
     * @param function 聚合函数
     * @param column   聚合字段
     * @param alias    查询别名
     * @return this
     */
    private LambdaCrudChainWrapper<T, V> selectAggregate(SqlAggregateFunction function, SFunction<T, ?> column, String alias) {
        return selectAggregate(function, columnToString(column), alias);
    }

    /**
     * 指定聚合查询字段。
     *
     * @param function 聚合函数
     * @param column   聚合字段 SQL
     * @param alias    查询别名
     * @return this
     */
    private LambdaCrudChainWrapper<T, V> selectAggregate(SqlAggregateFunction function, String column, String alias) {
        sqlSelect.setStringValue(AggregateSelectUtils.appendSelect(sqlSelect.getStringValue(),
            AggregateSelectUtils.aggregateSelect(function, column, alias)));
        return typedThis;
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
        SubQuery<S> subQuery = SubQuery.of(entityClass, value -> formatParam(null, value));
        consumer.accept(subQuery);
        return subQuery.build();
    }

    /**
     * 按条件设置更新字段。
     *
     * @param condition 是否设置该字段
     * @param column    字段
     * @param val       字段值
     * @param mapping   参数映射
     * @return this
     */
    @Override
    public LambdaCrudChainWrapper<T, V> set(boolean condition, SFunction<T, ?> column, Object val, String mapping) {
        return maybeDo(condition, () -> {
            String sql = formatParam(mapping, val);
            sqlSet.add(columnToString(column) + Constants.EQUALS + sql);
        });
    }

    /**
     * 值不为 null 时设置更新字段。
     *
     * @param column 字段
     * @param value  值
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> setIfPresent(SFunction<T, ?> column, Object value) {
        return set(value != null, column, value);
    }

    /**
     * 文本不为空时设置更新字段。
     *
     * @param column 字段
     * @param value  值
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> setIfText(SFunction<T, ?> column, String value) {
        return set(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 按条件设置自定义 SQL 更新片段。
     *
     * @param condition 是否设置该片段
     * @param setSql    SQL 更新片段
     * @param params    SQL 片段参数
     * @return this
     */
    @Override
    public LambdaCrudChainWrapper<T, V> setSql(boolean condition, String setSql, Object... params) {
        return maybeDo(condition && StringUtils.isNotBlank(setSql), () -> sqlSet.add(formatSqlMaybeWithParam(setSql, params)));
    }

    /**
     * 按条件设置字段自增。
     *
     * @param condition 是否设置该字段
     * @param column    字段
     * @param val       自增值
     * @return this
     */
    @Override
    public LambdaCrudChainWrapper<T, V> setIncrBy(boolean condition, SFunction<T, ?> column, Number val) {
        return maybeDo(condition, () -> {
            String realColumn = columnToString(column);
            String realVal = val instanceof BigDecimal ? ((BigDecimal) val).toPlainString() : String.valueOf(val);
            sqlSet.add(String.format("%s=%s + %s", realColumn, realColumn, realVal));
        });
    }

    /**
     * 按条件设置字段自减。
     *
     * @param condition 是否设置该字段
     * @param column    字段
     * @param val       自减值
     * @return this
     */
    @Override
    public LambdaCrudChainWrapper<T, V> setDecrBy(boolean condition, SFunction<T, ?> column, Number val) {
        return maybeDo(condition, () -> {
            String realColumn = columnToString(column);
            String realVal = val instanceof BigDecimal ? ((BigDecimal) val).toPlainString() : String.valueOf(val);
            sqlSet.add(String.format("%s=%s - %s", realColumn, realColumn, realVal));
        });
    }

    /**
     * 获取更新 set SQL 片段。
     *
     * @return 更新 set SQL 片段
     */
    @Override
    public String getSqlSet() {
        if (CollectionUtils.isEmpty(sqlSet)) {
            return null;
        }
        return String.join(Constants.COMMA, sqlSet);
    }

    /**
     * 获取查询条件 Wrapper。
     *
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> getWrapper() {
        return typedThis;
    }

    /**
     * 添加 FIND_IN_SET 条件。
     *
     * @param value  匹配值
     * @param column 字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> findInSet(Object value, SFunction<T, ?> column) {
        return findInSet(true, value, column);
    }

    /**
     * 添加 FIND_IN_SET 条件。
     *
     * @param condition 是否添加该条件
     * @param value     匹配值
     * @param column    字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> findInSet(boolean condition, Object value, SFunction<T, ?> column) {
        return findInSet(condition, value, columnToString(column));
    }

    /**
     * 值不为空时添加 FIND_IN_SET 条件。
     *
     * @param value  匹配值
     * @param column 字段
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> findInSetIfPresent(Object value, SFunction<T, ?> column) {
        return findInSet(value != null, value, column);
    }

    /**
     * 获取当前 Wrapper。
     *
     * @return this
     */
    public LambdaCrudChainWrapper<T, V> build() {
        return typedThis;
    }

    /**
     * 查询实体列表。
     *
     * @return 实体列表
     */
    public List<T> list() {
        return crudMapper.selectList(typedThis);
    }

    /**
     * 查询实体分页记录。
     *
     * @param page 分页条件
     * @return 实体分页记录
     */
    public List<T> list(IPage<T> page) {
        return crudMapper.selectList(page, typedThis);
    }

    /**
     * 查询 VO 列表。
     *
     * @return VO 列表
     */
    public List<V> voList() {
        return crudMapper.selectVoList(typedThis);
    }

    /**
     * 查询单列对象列表。
     *
     * @return 单列对象列表
     */
    public List<Object> objs() {
        return crudMapper.selectObjs(typedThis);
    }

    /**
     * 查询单列对象列表并转换类型。
     *
     * @param mapper 转换函数
     * @param <C>    转换后的类型
     * @return 单列对象列表
     */
    public <C> List<C> objs(Function<? super Object, C> mapper) {
        return crudMapper.selectObjs(typedThis, mapper);
    }

    /**
     * 查询单个实体。
     *
     * @return 实体
     */
    public T one() {
        return crudMapper.selectOne(typedThis);
    }

    /**
     * 查询单个实体。
     *
     * @param throwEx 查询到多条时是否抛异常
     * @return 实体
     */
    public T one(boolean throwEx) {
        return crudMapper.selectOne(typedThis, throwEx);
    }

    /**
     * 查询单个实体 Optional。
     *
     * @return Optional 实体
     */
    public Optional<T> oneOpt() {
        return Optional.ofNullable(one());
    }

    /**
     * 查询单个 VO。
     *
     * @return VO
     */
    public V voOne() {
        return crudMapper.selectVoOne(typedThis);
    }

    /**
     * 查询单个 VO。
     *
     * @param throwEx 查询到多条时是否抛异常
     * @return VO
     */
    public V voOne(boolean throwEx) {
        return crudMapper.selectVoOne(typedThis, throwEx);
    }

    /**
     * 查询数量。
     *
     * @return 数量
     */
    public Long count() {
        return crudMapper.selectCount(typedThis);
    }

    /**
     * 判断是否存在。
     *
     * @return 是否存在
     */
    public boolean exists() {
        return crudMapper.exists(typedThis);
    }

    /**
     * 查询实体分页。
     *
     * @param page 分页条件
     * @param <P>  分页类型
     * @return 实体分页
     */
    public <P extends IPage<T>> P page(P page) {
        return crudMapper.selectPage(page, typedThis);
    }

    /**
     * 查询 VO 分页。
     *
     * @param page 分页条件
     * @param <P>  分页类型
     * @return VO 分页
     */
    public <P extends IPage<V>> P voPage(IPage<T> page) {
        return crudMapper.selectVoPage(page, typedThis);
    }

    /**
     * 删除数据。
     *
     * @return 是否删除成功
     */
    public boolean delete() {
        return deleteCount() > 0;
    }

    /**
     * 删除数据。
     *
     * @return 影响行数
     */
    public int deleteCount() {
        return crudMapper.delete(typedThis);
    }

    /**
     * 使用 set 片段更新数据。
     *
     * @return 是否更新成功
     */
    public boolean update() {
        return updateCount() > 0;
    }

    /**
     * 使用实体和查询条件更新数据。
     *
     * @param entity 实体
     * @return 是否更新成功
     */
    public boolean update(T entity) {
        return updateCount(entity) > 0;
    }

    /**
     * 使用 set 片段更新数据。
     *
     * @return 影响行数
     */
    public int updateCount() {
        return crudMapper.update(typedThis);
    }

    /**
     * 使用实体和查询条件更新数据。
     *
     * @param entity 实体
     * @return 影响行数
     */
    public int updateCount(T entity) {
        return crudMapper.update(entity, typedThis);
    }

    /**
     * 创建新的链式包装器实例。
     *
     * @return 新的链式包装器实例
     */
    @Override
    protected LambdaCrudChainWrapper<T, V> instance() {
        return new LambdaCrudChainWrapper<>(crudMapper, getEntity(), getEntityClass(), null, null, paramNameSeq,
            paramNameValuePairs, new MergeSegments(), paramAlias, SharedString.emptyString(), SharedString.emptyString(),
            SharedString.emptyString());
    }

    /**
     * 清空当前 Wrapper 状态。
     */
    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
        sqlSet.clear();
    }

}

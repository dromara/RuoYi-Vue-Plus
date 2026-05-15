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

    private final LambdaQueryWrapper<T> wrapper;

    LambdaQueryBuilder(LambdaQueryWrapper<T> wrapper) {
        this.wrapper = wrapper;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> select(SFunction<T, ?>... columns) {
        wrapper.select(columns);
        return this;
    }

    public LambdaQueryBuilder<T> eq(SFunction<T, ?> column, Object value) {
        wrapper.eq(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> eq(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.eq(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> ne(SFunction<T, ?> column, Object value) {
        wrapper.ne(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> ne(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.ne(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> gt(SFunction<T, ?> column, Object value) {
        wrapper.gt(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> gt(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.gt(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> ge(SFunction<T, ?> column, Object value) {
        wrapper.ge(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> ge(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.ge(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> lt(SFunction<T, ?> column, Object value) {
        wrapper.lt(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> lt(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.lt(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> le(SFunction<T, ?> column, Object value) {
        wrapper.le(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> le(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.le(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> like(SFunction<T, ?> column, Object value) {
        wrapper.like(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> like(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.like(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> notLike(SFunction<T, ?> column, Object value) {
        wrapper.notLike(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> notLike(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.notLike(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> likeLeft(SFunction<T, ?> column, Object value) {
        wrapper.likeLeft(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> likeLeft(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.likeLeft(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> likeRight(SFunction<T, ?> column, Object value) {
        wrapper.likeRight(column, value);
        return this;
    }

    public LambdaQueryBuilder<T> likeRight(boolean condition, SFunction<T, ?> column, Object value) {
        wrapper.likeRight(condition, column, value);
        return this;
    }

    public LambdaQueryBuilder<T> between(SFunction<T, ?> column, Object begin, Object end) {
        wrapper.between(column, begin, end);
        return this;
    }

    public LambdaQueryBuilder<T> between(boolean condition, SFunction<T, ?> column, Object begin, Object end) {
        wrapper.between(condition, column, begin, end);
        return this;
    }

    public LambdaQueryBuilder<T> notBetween(SFunction<T, ?> column, Object begin, Object end) {
        wrapper.notBetween(column, begin, end);
        return this;
    }

    public LambdaQueryBuilder<T> notBetween(boolean condition, SFunction<T, ?> column, Object begin, Object end) {
        wrapper.notBetween(condition, column, begin, end);
        return this;
    }

    public LambdaQueryBuilder<T> allEq(Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(true, (Map) params, null2IsNull);
        return this;
    }

    public LambdaQueryBuilder<T> allEq(boolean condition, Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(condition, (Map) params, null2IsNull);
        return this;
    }

    public LambdaQueryBuilder<T> allEq(BiPredicate<SFunction<T, ?>, Object> filter, Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(true, (BiPredicate) filter, (Map) params, null2IsNull);
        return this;
    }

    public LambdaQueryBuilder<T> allEq(boolean condition, BiPredicate<SFunction<T, ?>, Object> filter, Map<?, ?> params, boolean null2IsNull) {
        wrapper.allEq(condition, (BiPredicate) filter, (Map) params, null2IsNull);
        return this;
    }

    public LambdaQueryBuilder<T> isNull(SFunction<T, ?> column) {
        wrapper.isNull(column);
        return this;
    }

    public LambdaQueryBuilder<T> isNull(boolean condition, SFunction<T, ?> column) {
        wrapper.isNull(condition, column);
        return this;
    }

    public LambdaQueryBuilder<T> isNotNull(SFunction<T, ?> column) {
        wrapper.isNotNull(column);
        return this;
    }

    public LambdaQueryBuilder<T> isNotNull(boolean condition, SFunction<T, ?> column) {
        wrapper.isNotNull(condition, column);
        return this;
    }

    public LambdaQueryBuilder<T> in(SFunction<T, ?> column, Collection<?> values) {
        wrapper.in(column, values);
        return this;
    }

    public LambdaQueryBuilder<T> in(boolean condition, SFunction<T, ?> column, Collection<?> values) {
        wrapper.in(condition, column, values);
        return this;
    }

    public LambdaQueryBuilder<T> in(SFunction<T, ?> column, Object... values) {
        wrapper.in(column, values);
        return this;
    }

    public LambdaQueryBuilder<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        wrapper.in(condition, column, values);
        return this;
    }

    public LambdaQueryBuilder<T> notIn(SFunction<T, ?> column, Collection<?> values) {
        wrapper.notIn(column, values);
        return this;
    }

    public LambdaQueryBuilder<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> values) {
        wrapper.notIn(condition, column, values);
        return this;
    }

    public LambdaQueryBuilder<T> notIn(SFunction<T, ?> column, Object... values) {
        wrapper.notIn(column, values);
        return this;
    }

    public LambdaQueryBuilder<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        wrapper.notIn(condition, column, values);
        return this;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> groupBy(SFunction<T, ?>... columns) {
        wrapper.groupBy(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> groupBy(boolean condition, SFunction<T, ?>... columns) {
        wrapper.groupBy(condition, Arrays.asList(columns));
        return this;
    }

    public LambdaQueryBuilder<T> orderBy(boolean condition, boolean isAsc, SFunction<T, ?> column) {
        wrapper.orderBy(condition, isAsc, column);
        return this;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByAsc(SFunction<T, ?>... columns) {
        wrapper.orderByAsc(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByAsc(boolean condition, SFunction<T, ?>... columns) {
        wrapper.orderByAsc(condition, Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByDesc(SFunction<T, ?>... columns) {
        wrapper.orderByDesc(Arrays.asList(columns));
        return this;
    }

    @SafeVarargs
    public final LambdaQueryBuilder<T> orderByDesc(boolean condition, SFunction<T, ?>... columns) {
        wrapper.orderByDesc(condition, Arrays.asList(columns));
        return this;
    }

    public LambdaQueryBuilder<T> having(String sqlHaving, Object... params) {
        wrapper.having(sqlHaving, params);
        return this;
    }

    public LambdaQueryBuilder<T> having(boolean condition, String sqlHaving, Object... params) {
        wrapper.having(condition, sqlHaving, params);
        return this;
    }

    public LambdaQueryBuilder<T> and(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.and(consumer);
        return this;
    }

    public LambdaQueryBuilder<T> and(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.and(condition, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> or() {
        wrapper.or();
        return this;
    }

    public LambdaQueryBuilder<T> or(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.or(consumer);
        return this;
    }

    public LambdaQueryBuilder<T> or(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.or(condition, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> nested(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.nested(consumer);
        return this;
    }

    public LambdaQueryBuilder<T> nested(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.nested(condition, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> exists(String existsSql, Object... values) {
        wrapper.exists(existsSql, values);
        return this;
    }

    public LambdaQueryBuilder<T> exists(boolean condition, String existsSql, Object... values) {
        wrapper.exists(condition, existsSql, values);
        return this;
    }

    public LambdaQueryBuilder<T> eqSql(SFunction<T, ?> column, String inValue) {
        wrapper.eqSql(true, column, inValue);
        return this;
    }

    public LambdaQueryBuilder<T> eqSql(boolean condition, SFunction<T, ?> column, String inValue) {
        wrapper.eqSql(condition, column, inValue);
        return this;
    }

    public LambdaQueryBuilder<T> inSql(SFunction<T, ?> column, String inValue) {
        wrapper.inSql(true, column, inValue);
        return this;
    }

    public LambdaQueryBuilder<T> inSql(boolean condition, SFunction<T, ?> column, String inValue) {
        wrapper.inSql(condition, column, inValue);
        return this;
    }

    public LambdaQueryBuilder<T> notInSql(SFunction<T, ?> column, String inValue) {
        wrapper.notInSql(true, column, inValue);
        return this;
    }

    public LambdaQueryBuilder<T> notInSql(boolean condition, SFunction<T, ?> column, String inValue) {
        wrapper.notInSql(condition, column, inValue);
        return this;
    }

    public LambdaQueryBuilder<T> notExists(String existsSql, Object... values) {
        wrapper.notExists(existsSql, values);
        return this;
    }

    public LambdaQueryBuilder<T> notExists(boolean condition, String existsSql, Object... values) {
        wrapper.notExists(condition, existsSql, values);
        return this;
    }

    public LambdaQueryBuilder<T> apply(String applySql, Object... values) {
        wrapper.apply(applySql, values);
        return this;
    }

    public LambdaQueryBuilder<T> apply(boolean condition, String applySql, Object... values) {
        wrapper.apply(condition, applySql, values);
        return this;
    }

    public LambdaQueryBuilder<T> apply(Consumer<LambdaQueryWrapper<T>> consumer) {
        consumer.accept(wrapper);
        return this;
    }

    public LambdaQueryBuilder<T> func(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.func(true, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> func(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.func(condition, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> not(Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.not(true, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> not(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        wrapper.not(condition, consumer);
        return this;
    }

    public LambdaQueryBuilder<T> comment(String comment) {
        wrapper.comment(true, comment);
        return this;
    }

    public LambdaQueryBuilder<T> comment(boolean condition, String comment) {
        wrapper.comment(condition, comment);
        return this;
    }

    public LambdaQueryBuilder<T> last(String lastSql) {
        wrapper.last(lastSql);
        return this;
    }

    public LambdaQueryWrapper<T> build() {
        return wrapper;
    }

}

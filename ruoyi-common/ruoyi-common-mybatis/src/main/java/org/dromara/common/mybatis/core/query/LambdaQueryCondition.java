package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.helper.DataBaseHelper;

import java.util.Collection;
import java.util.Map;

/**
 * Lambda 查询常用条件扩展。
 *
 * @param <T>        实体类型
 * @param <Children> 链式返回类型
 * @author Lion Li
 */
public interface LambdaQueryCondition<T, Children> {

    Children eq(boolean condition, SFunction<T, ?> column, Object value);

    Children ne(boolean condition, SFunction<T, ?> column, Object value);

    Children gt(boolean condition, SFunction<T, ?> column, Object value);

    Children ge(boolean condition, SFunction<T, ?> column, Object value);

    Children lt(boolean condition, SFunction<T, ?> column, Object value);

    Children le(boolean condition, SFunction<T, ?> column, Object value);

    Children like(boolean condition, SFunction<T, ?> column, Object value);

    Children notLike(boolean condition, SFunction<T, ?> column, Object value);

    Children likeLeft(boolean condition, SFunction<T, ?> column, Object value);

    Children likeRight(boolean condition, SFunction<T, ?> column, Object value);

    Children between(boolean condition, SFunction<T, ?> column, Object begin, Object end);

    Children notBetween(boolean condition, SFunction<T, ?> column, Object begin, Object end);

    Children in(boolean condition, SFunction<T, ?> column, Collection<?> values);

    Children in(boolean condition, SFunction<T, ?> column, Object... values);

    Children notIn(boolean condition, SFunction<T, ?> column, Collection<?> values);

    Children notIn(boolean condition, SFunction<T, ?> column, Object... values);

    Children apply(boolean condition, String applySql, Object... values);

    default Children eqIfPresent(SFunction<T, ?> column, Object value) {
        return eq(value != null, column, value);
    }

    default Children eqIfText(SFunction<T, ?> column, String value) {
        return eq(StringUtils.isNotBlank(value), column, value);
    }

    default Children neIfPresent(SFunction<T, ?> column, Object value) {
        return ne(value != null, column, value);
    }

    default Children neIfText(SFunction<T, ?> column, String value) {
        return ne(StringUtils.isNotBlank(value), column, value);
    }

    default Children gtIfPresent(SFunction<T, ?> column, Object value) {
        return gt(value != null, column, value);
    }

    default Children geIfPresent(SFunction<T, ?> column, Object value) {
        return ge(value != null, column, value);
    }

    default Children ltIfPresent(SFunction<T, ?> column, Object value) {
        return lt(value != null, column, value);
    }

    default Children leIfPresent(SFunction<T, ?> column, Object value) {
        return le(value != null, column, value);
    }

    default Children likeIfText(SFunction<T, ?> column, String value) {
        return like(StringUtils.isNotBlank(value), column, value);
    }

    default Children notLikeIfText(SFunction<T, ?> column, String value) {
        return notLike(StringUtils.isNotBlank(value), column, value);
    }

    default Children likeLeftIfText(SFunction<T, ?> column, String value) {
        return likeLeft(StringUtils.isNotBlank(value), column, value);
    }

    default Children likeRightIfText(SFunction<T, ?> column, String value) {
        return likeRight(StringUtils.isNotBlank(value), column, value);
    }

    default Children betweenIfPresent(SFunction<T, ?> column, Object begin, Object end) {
        return between(begin != null && end != null, column, begin, end);
    }

    default Children betweenParams(SFunction<T, ?> column, Map<String, Object> params, String beginKey, String endKey) {
        if (params == null) {
            return between(false, column, null, null);
        }
        Object begin = params.get(beginKey);
        Object end = params.get(endKey);
        return between(begin != null && end != null, column, begin, end);
    }

    default Children notBetweenIfPresent(SFunction<T, ?> column, Object begin, Object end) {
        return notBetween(begin != null && end != null, column, begin, end);
    }

    default Children inIfNotEmpty(SFunction<T, ?> column, Collection<?> values) {
        return in(values != null && !values.isEmpty(), column, values);
    }

    default Children inIfNotEmpty(SFunction<T, ?> column, Object... values) {
        return in(values != null && values.length > 0, column, values);
    }

    default Children notInIfNotEmpty(SFunction<T, ?> column, Collection<?> values) {
        return notIn(values != null && !values.isEmpty(), column, values);
    }

    default Children notInIfNotEmpty(SFunction<T, ?> column, Object... values) {
        return notIn(values != null && values.length > 0, column, values);
    }

    default Children findInSet(Object value, String column) {
        return findInSet(true, value, column);
    }

    default Children findInSet(boolean condition, Object value, String column) {
        return apply(condition, DataBaseHelper.findInSet(value, column));
    }

    default Children findInSetIfPresent(Object value, String column) {
        return findInSet(value != null, value, column);
    }

}

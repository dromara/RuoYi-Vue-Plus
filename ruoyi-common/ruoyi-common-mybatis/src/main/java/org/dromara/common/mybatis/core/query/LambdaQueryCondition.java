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

    /**
     * 等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children eq(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 不等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children ne(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 大于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children gt(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 大于等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children ge(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 小于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children lt(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 小于等于条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children le(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children like(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 非模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children notLike(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 左模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children likeLeft(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 右模糊匹配条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param value     条件值
     * @return 链式返回对象
     */
    Children likeRight(boolean condition, SFunction<T, ?> column, Object value);

    /**
     * 区间条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param begin     起始值
     * @param end       结束值
     * @return 链式返回对象
     */
    Children between(boolean condition, SFunction<T, ?> column, Object begin, Object end);

    /**
     * 非区间条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param begin     起始值
     * @param end       结束值
     * @return 链式返回对象
     */
    Children notBetween(boolean condition, SFunction<T, ?> column, Object begin, Object end);

    /**
     * 包含集合条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值集合
     * @return 链式返回对象
     */
    Children in(boolean condition, SFunction<T, ?> column, Collection<?> values);

    /**
     * 包含数组条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值数组
     * @return 链式返回对象
     */
    Children in(boolean condition, SFunction<T, ?> column, Object... values);

    /**
     * 不包含集合条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值集合
     * @return 链式返回对象
     */
    Children notIn(boolean condition, SFunction<T, ?> column, Collection<?> values);

    /**
     * 不包含数组条件。
     *
     * @param condition 是否添加该条件
     * @param column    字段
     * @param values    条件值数组
     * @return 链式返回对象
     */
    Children notIn(boolean condition, SFunction<T, ?> column, Object... values);

    /**
     * 拼接 SQL 片段条件。
     *
     * @param condition 是否添加该条件
     * @param applySql  SQL 片段
     * @param values    SQL 片段参数
     * @return 链式返回对象
     */
    Children apply(boolean condition, String applySql, Object... values);

    /**
     * 值不为空时添加等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children eqIfPresent(SFunction<T, ?> column, Object value) {
        return eq(value != null, column, value);
    }

    /**
     * 文本不为空白时添加等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children eqIfText(SFunction<T, ?> column, String value) {
        return eq(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 值不为空时添加不等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children neIfPresent(SFunction<T, ?> column, Object value) {
        return ne(value != null, column, value);
    }

    /**
     * 文本不为空白时添加不等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children neIfText(SFunction<T, ?> column, String value) {
        return ne(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 值不为空时添加大于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children gtIfPresent(SFunction<T, ?> column, Object value) {
        return gt(value != null, column, value);
    }

    /**
     * 值不为空时添加大于等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children geIfPresent(SFunction<T, ?> column, Object value) {
        return ge(value != null, column, value);
    }

    /**
     * 值不为空时添加小于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children ltIfPresent(SFunction<T, ?> column, Object value) {
        return lt(value != null, column, value);
    }

    /**
     * 值不为空时添加小于等于条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children leIfPresent(SFunction<T, ?> column, Object value) {
        return le(value != null, column, value);
    }

    /**
     * 文本不为空白时添加模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children likeIfText(SFunction<T, ?> column, String value) {
        return like(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 文本不为空白时添加非模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children notLikeIfText(SFunction<T, ?> column, String value) {
        return notLike(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 文本不为空白时添加左模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children likeLeftIfText(SFunction<T, ?> column, String value) {
        return likeLeft(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 文本不为空白时添加右模糊匹配条件。
     *
     * @param column 字段
     * @param value  条件值
     * @return 链式返回对象
     */
    default Children likeRightIfText(SFunction<T, ?> column, String value) {
        return likeRight(StringUtils.isNotBlank(value), column, value);
    }

    /**
     * 起止值均不为空时添加区间条件。
     *
     * @param column 字段
     * @param begin  起始值
     * @param end    结束值
     * @return 链式返回对象
     */
    default Children betweenIfPresent(SFunction<T, ?> column, Object begin, Object end) {
        return between(begin != null && end != null, column, begin, end);
    }

    /**
     * 从参数 Map 中读取起止值，均不为空时添加区间条件。
     *
     * @param column   字段
     * @param params   参数 Map
     * @param beginKey 起始值参数名
     * @param endKey   结束值参数名
     * @return 链式返回对象
     */
    default Children betweenParams(SFunction<T, ?> column, Map<String, Object> params, String beginKey, String endKey) {
        if (params == null) {
            return between(false, column, null, null);
        }
        Object begin = params.get(beginKey);
        Object end = params.get(endKey);
        return between(begin != null && end != null, column, begin, end);
    }

    /**
     * 起止值均不为空时添加非区间条件。
     *
     * @param column 字段
     * @param begin  起始值
     * @param end    结束值
     * @return 链式返回对象
     */
    default Children notBetweenIfPresent(SFunction<T, ?> column, Object begin, Object end) {
        return notBetween(begin != null && end != null, column, begin, end);
    }

    /**
     * 集合不为空时添加包含条件。
     *
     * @param column 字段
     * @param values 条件值集合
     * @return 链式返回对象
     */
    default Children inIfNotEmpty(SFunction<T, ?> column, Collection<?> values) {
        return in(values != null && !values.isEmpty(), column, values);
    }

    /**
     * 数组不为空时添加包含条件。
     *
     * @param column 字段
     * @param values 条件值数组
     * @return 链式返回对象
     */
    default Children inIfNotEmpty(SFunction<T, ?> column, Object... values) {
        return in(values != null && values.length > 0, column, values);
    }

    /**
     * 集合不为空时添加不包含条件。
     *
     * @param column 字段
     * @param values 条件值集合
     * @return 链式返回对象
     */
    default Children notInIfNotEmpty(SFunction<T, ?> column, Collection<?> values) {
        return notIn(values != null && !values.isEmpty(), column, values);
    }

    /**
     * 数组不为空时添加不包含条件。
     *
     * @param column 字段
     * @param values 条件值数组
     * @return 链式返回对象
     */
    default Children notInIfNotEmpty(SFunction<T, ?> column, Object... values) {
        return notIn(values != null && values.length > 0, column, values);
    }

    /**
     * 添加 FIND_IN_SET 条件。
     *
     * @param value  匹配值
     * @param column 字段名
     * @return 链式返回对象
     */
    default Children findInSet(Object value, String column) {
        return findInSet(true, value, column);
    }

    /**
     * 添加 FIND_IN_SET 条件。
     *
     * @param condition 是否添加该条件
     * @param value     匹配值
     * @param column    字段名
     * @return 链式返回对象
     */
    default Children findInSet(boolean condition, Object value, String column) {
        return apply(condition, DataBaseHelper.findInSet(value, column));
    }

    /**
     * 值不为空时添加 FIND_IN_SET 条件。
     *
     * @param value  匹配值
     * @param column 字段名
     * @return 链式返回对象
     */
    default Children findInSetIfPresent(Object value, String column) {
        return findInSet(value != null, value, column);
    }

}

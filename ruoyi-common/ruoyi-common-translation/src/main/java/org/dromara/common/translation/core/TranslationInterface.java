package org.dromara.common.translation.core;

import cn.hutool.core.convert.Convert;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.translation.annotation.TranslationType;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 翻译接口 (实现类需标注 {@link TranslationType} 注解标明翻译类型)
 *
 * @author Lion Li
 */
public interface TranslationInterface<T> {

    /**
     * 按翻译键执行转换。
     *
     * @param key   需要被翻译的键(不为空)
     * @param other 其他参数
     * @return 返回键对应的翻译值
     */
    T translation(Object key, String other);

    /**
     * 批量翻译。
     *
     * @param keys 需要被翻译的键集合
     * @param other 其他参数
     * @return 翻译结果映射
     */
    default Map<Object, T> translationBatch(Set<Object> keys, String other) {
        Map<Object, T> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, translation(key, other));
        }
        return result;
    }

    /**
     * 收集 Long 类型键集合。
     *
     * @param keys 原始键集合
     * @return Long 键集合
     */
    default Set<Long> collectLongIds(Collection<Object> keys) {
        Set<Long> result = new LinkedHashSet<>();
        for (Object key : keys) {
            if (key instanceof String ids) {
                result.addAll(parseLongIds(ids));
            } else if (key != null) {
                result.add(Convert.toLong(key));
            }
        }
        return result;
    }

    /**
     * 解析逗号分隔的 Long ID 列表。
     *
     * @param ids 逗号分隔字符串
     * @return Long 列表
     */
    default List<Long> parseLongIds(String ids) {
        return StreamUtils.toList(
            StreamUtils.filter(Arrays.asList(ids.split(StringUtils.SEPARATOR)), StringUtils::isNotBlank),
            value -> Convert.toLong(value.trim())
        );
    }

    /**
     * 按原始 ID 顺序拼接映射值。
     *
     * @param ids    原始 ID 字符串
     * @param mapper ID 到值的映射函数
     * @return 拼接后的结果字符串
     * @param <E> 值类型
     */
    default <E> String joinMappedValues(String ids, Function<Long, E> mapper) {
        return StreamUtils.join(parseLongIds(ids), id -> {
            E value = mapper.apply(id);
            return value == null ? null : String.valueOf(value);
        });
    }
}

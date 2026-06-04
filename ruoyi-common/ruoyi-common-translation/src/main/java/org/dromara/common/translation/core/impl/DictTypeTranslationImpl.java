package org.dromara.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 字典翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.DICT_TYPE_TO_LABEL)
public class DictTypeTranslationImpl implements TranslationInterface<String> {

    private final DictService dictService;

    /**
     * 根据字典类型和字典值翻译显示标签。
     *
     * @param key   字典值
     * @param other 字典类型
     * @return 字典标签
     */
    @Override
    public String translation(Object key, String other) {
        if (key instanceof String dictValue && StringUtils.isNotBlank(other)) {
            return dictService.getDictLabel(other, dictValue);
        }
        return null;
    }

    /**
     * 批量根据字典类型和字典值翻译显示标签。
     *
     * @param keys  字典值集合
     * @param other 字典类型
     * @return 字典值与字典标签映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        if (keys.isEmpty() || StringUtils.isBlank(other)) {
            return Map.of();
        }
        Map<String, String> dictMap = dictService.getAllDictByDictType(other);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            if (key instanceof String dictValue) {
                result.put(key, StreamUtils.join(
                    StreamUtils.filter(Arrays.asList(dictValue.split(",")), StringUtils::isNotBlank),
                    value -> dictMap.get(value.trim())
                ));
            }
        }
        return result;
    }
}

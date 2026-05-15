package org.dromara.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.service.IFlwCategoryService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 流程分类名称翻译实现
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
@TranslationType(type = FlowConstant.CATEGORY_ID_TO_NAME)
public class CategoryNameTranslationImpl implements TranslationInterface<String> {

    private final IFlwCategoryService flwCategoryService;

    /**
     * 将流程分类 ID 翻译为分类名称。
     *
     * @param key 流程分类 ID
     * @param other 额外参数
     * @return 流程分类名称
     */
    @Override
    public String translation(Object key, String other) {
        return flwCategoryService.selectCategoryNameById(Convert.toLong(key));
    }

    /**
     * 批量将流程分类 ID 翻译为分类名称。
     *
     * @param keys 流程分类 ID 集合
     * @param other 额外参数
     * @return 流程分类 ID 与分类名称映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> categoryIds = collectLongIds(keys);
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> categoryNames = flwCategoryService.selectCategoryNameByIds(categoryIds);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, categoryNames));
        }
        return result;
    }

    /**
     * 根据原始键构建流程分类名称翻译值。
     *
     * @param source 原始键
     * @param categoryNames 流程分类 ID 与分类名称映射
     * @return 流程分类名称
     */
    private String buildValue(Object source, Map<Long, String> categoryNames) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, categoryNames::get);
        }
        return source == null ? null : categoryNames.get(Convert.toLong(source));
    }

}

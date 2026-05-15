package org.dromara.common.translation.core.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.json.enhance.JsonEnhancementContext;
import org.dromara.common.json.enhance.JsonFieldContext;
import org.dromara.common.json.enhance.JsonFieldProcessor;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.core.TranslationInterface;
import org.springframework.core.annotation.Order;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 翻译响应处理器。
 */
@Slf4j
@Order(0)
public class TranslationJsonFieldProcessor implements JsonFieldProcessor {

    /**
     * 响应增强上下文中保存待批量翻译数据的属性键。
     */
    private static final String ATTR_BATCHES = TranslationJsonFieldProcessor.class.getName() + ".batches";

    /**
     * 响应增强上下文中保存批量翻译结果的属性键。
     */
    private static final String ATTR_RESULTS = TranslationJsonFieldProcessor.class.getName() + ".results";

    /**
     * 翻译类型与翻译实现映射。
     */
    private final Map<String, TranslationInterface<?>> translationMap;

    /**
     * 构造翻译响应处理器。
     *
     * @param translations 翻译实现列表
     */
    public TranslationJsonFieldProcessor(List<TranslationInterface<?>> translations) {
        Map<String, TranslationInterface<?>> map = new LinkedHashMap<>(translations.size());
        for (TranslationInterface<?> t : translations) {
            TranslationType annotation = t.getClass().getAnnotation(TranslationType.class);
            if (annotation != null) {
                map.put(annotation.type(), t);
            }
        }
        this.translationMap = Collections.unmodifiableMap(map);
    }

    /**
     * 收集字段上的翻译注解和原始值，供后续批量翻译使用。
     *
     * @param fieldContext 字段上下文
     * @param context      响应增强上下文
     */
    @Override
    public void collect(JsonFieldContext fieldContext, JsonEnhancementContext context) {
        Translation translation = fieldContext.getAnnotation(Translation.class);
        if (translation == null) {
            return;
        }
        Object sourceValue = resolveSourceValue(fieldContext, translation);
        if (sourceValue == null) {
            return;
        }
        Map<TranslationBatchKey, Set<Object>> batches = getOrCreateBatches(context);
        batches.computeIfAbsent(new TranslationBatchKey(translation.type(), translation.other()), key -> new LinkedHashSet<>())
            .add(sourceValue);
    }

    /**
     * 根据收集到的原始值执行批量翻译，并将翻译结果写入响应增强上下文。
     *
     * @param context 响应增强上下文
     */
    @Override
    public void prepare(JsonEnhancementContext context) {
        Map<TranslationBatchKey, Set<Object>> batches = context.getAttribute(ATTR_BATCHES);
        if (batches == null || batches.isEmpty()) {
            return;
        }
        Map<TranslationBatchKey, Map<Object, Object>> results = new LinkedHashMap<>(batches.size());
        for (Map.Entry<TranslationBatchKey, Set<Object>> entry : batches.entrySet()) {
            TranslationInterface<?> translation = getTranslation(entry.getKey().type());
            if (translation == null) {
                continue;
            }
            try {
                Map<Object, ?> translated = translation.translationBatch(entry.getValue(), entry.getKey().other());
                results.put(entry.getKey(), new LinkedHashMap<>(translated));
            } catch (Exception e) {
                log.error("批量翻译处理异常，type: {}, other: {}", entry.getKey().type(), entry.getKey().other(), e);
            }
        }
        context.setAttribute(ATTR_RESULTS, results);
    }

    /**
     * 处理单个字段的翻译值，优先使用批量翻译结果，缺失时回退到单值翻译。
     *
     * @param fieldContext 字段上下文
     * @param value        当前字段值
     * @param context      响应增强上下文
     * @return 翻译后的字段值
     */
    @Override
    public Object process(JsonFieldContext fieldContext, Object value, JsonEnhancementContext context) {
        Translation translation = fieldContext.getAnnotation(Translation.class);
        if (translation == null) {
            return value;
        }
        Object sourceValue = resolveSourceValue(fieldContext, translation);
        if (sourceValue == null) {
            return value;
        }
        TranslationBatchKey batchKey = new TranslationBatchKey(translation.type(), translation.other());
        Map<TranslationBatchKey, Map<Object, Object>> results = context.getAttribute(ATTR_RESULTS);
        if (results != null) {
            Map<Object, Object> translatedMap = results.get(batchKey);
            if (translatedMap != null && translatedMap.containsKey(sourceValue)) {
                return translatedMap.get(sourceValue);
            }
        }
        TranslationInterface<?> trans = getTranslation(translation.type());
        if (ObjectUtil.isNull(trans)) {
            return value;
        }
        try {
            return trans.translation(sourceValue, translation.other());
        } catch (Exception e) {
            log.error("翻译处理异常，type: {}, value: {}", translation.type(), sourceValue, e);
            return value;
        }
    }

    /**
     * 获取或创建待批量翻译数据映射。
     *
     * @param context 响应增强上下文
     * @return 待批量翻译数据映射
     */
    private Map<TranslationBatchKey, Set<Object>> getOrCreateBatches(JsonEnhancementContext context) {
        Map<TranslationBatchKey, Set<Object>> batches = context.getAttribute(ATTR_BATCHES);
        if (batches == null) {
            batches = new LinkedHashMap<>();
            context.setAttribute(ATTR_BATCHES, batches);
        }
        return batches;
    }

    /**
     * 解析翻译原始值。
     *
     * @param fieldContext 字段上下文
     * @param translation  翻译注解
     * @return 翻译原始值
     */
    private Object resolveSourceValue(JsonFieldContext fieldContext, Translation translation) {
        if (StringUtils.isNotBlank(translation.mapper())) {
            return ReflectUtils.invokeGetter(fieldContext.owner(), translation.mapper());
        }
        return fieldContext.value();
    }

    /**
     * 根据翻译类型获取翻译实现。
     *
     * @param type 翻译类型
     * @return 翻译实现
     */
    private TranslationInterface<?> getTranslation(String type) {
        return translationMap.get(type);
    }

    /**
     * 批量翻译分组键。
     *
     * @param type  翻译类型
     * @param other 额外参数
     */
    private record TranslationBatchKey(String type, String other) {
    }

}

package org.dromara.common.translation.core.handler;

import org.dromara.common.json.enhance.JsonEnhancementContext;
import org.dromara.common.json.enhance.JsonFieldContext;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.core.TranslationInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TranslationJsonFieldProcessor 单元测试")
class TranslationJsonFieldProcessorTest {

    /**
     * 验证重复翻译键被去重，并优先使用一次批量查询的结果。
     */
    @Test
    @DisplayName("收集重复键后只执行一次批量翻译")
    void shouldCollectDistinctKeysAndUseBatchResult() {
        RecordingTranslation translation = new RecordingTranslation();
        TranslationJsonFieldProcessor processor = new TranslationJsonFieldProcessor(List.of(translation));
        JsonEnhancementContext context = new JsonEnhancementContext(null);
        JsonFieldContext first = fieldContext(1L, "test", "", "dict");
        JsonFieldContext duplicate = fieldContext(1L, "test", "", "dict");
        JsonFieldContext second = fieldContext(2L, "test", "", "dict");

        processor.collect(first, context);
        processor.collect(duplicate, context);
        processor.collect(second, context);
        processor.prepare(context);

        assertEquals(Set.of(1L, 2L), translation.lastBatchKeys);
        assertEquals(1, translation.batchCalls);
        assertEquals("dict-batch-1", processor.process(first, null, context));
        assertEquals("dict-batch-2", processor.process(second, null, context));
        assertEquals(0, translation.singleCalls);
    }

    /**
     * 验证批量结果缺少当前键时会回退到单值翻译。
     */
    @Test
    @DisplayName("批量结果缺失时回退到单值翻译")
    void shouldFallbackToSingleTranslationWhenBatchMissesKey() {
        RecordingTranslation translation = new RecordingTranslation();
        translation.omitBatchValue = true;
        TranslationJsonFieldProcessor processor = new TranslationJsonFieldProcessor(List.of(translation));
        JsonEnhancementContext context = new JsonEnhancementContext(null);
        JsonFieldContext fieldContext = fieldContext(3L, "test", "", "dict");

        processor.collect(fieldContext, context);
        processor.prepare(context);
        Object result = processor.process(fieldContext, null, context);

        assertEquals("dict-single-3", result);
        assertEquals(1, translation.singleCalls);
    }

    /**
     * 验证翻译异常和未注册类型不会中断响应，而是保留原值。
     */
    @Test
    @DisplayName("翻译异常或类型不存在时保留原值")
    void shouldKeepOriginalValueWhenTranslationFailsOrIsMissing() {
        RecordingTranslation translation = new RecordingTranslation();
        translation.throwOnSingle = true;
        TranslationJsonFieldProcessor processor = new TranslationJsonFieldProcessor(List.of(translation));

        assertEquals("original", processor.process(fieldContext(4L, "test", "", "dict"), "original",
            new JsonEnhancementContext(null)));
        assertEquals("original", processor.process(fieldContext(4L, "missing", "", "dict"), "original",
            new JsonEnhancementContext(null)));
    }

    /**
     * 验证 Translation.mapper 指定的所属对象属性作为翻译源值。
     */
    @Test
    @DisplayName("mapper 属性作为翻译源值")
    void shouldUseMappedOwnerPropertyAsSourceValue() {
        RecordingTranslation translation = new RecordingTranslation();
        TranslationJsonFieldProcessor processor = new TranslationJsonFieldProcessor(List.of(translation));
        SourceOwner owner = new SourceOwner(9L);
        JsonFieldContext fieldContext = fieldContext(owner, "displayName", "ignored", "test", "sourceId", "dict");

        Object result = processor.process(fieldContext, "original", new JsonEnhancementContext(null));

        assertEquals("dict-single-9", result);
    }

    /**
     * 创建直接使用字段值的翻译字段上下文。
     *
     * @param value  字段值
     * @param type   翻译类型
     * @param mapper 映射属性
     * @param other  额外参数
     * @return 翻译字段上下文
     */
    private static JsonFieldContext fieldContext(Object value, String type, String mapper, String other) {
        return fieldContext(new Object(), "value", value, type, mapper, other);
    }

    /**
     * 创建可指定所属对象和属性的翻译字段上下文。
     *
     * @param owner        字段所属对象
     * @param propertyName 字段名
     * @param value        字段原始值
     * @param type         翻译类型
     * @param mapper       映射属性
     * @param other        额外参数
     * @return 翻译字段上下文
     */
    private static JsonFieldContext fieldContext(Object owner, String propertyName, Object value, String type,
                                                 String mapper, String other) {
        Translation annotation = mock(Translation.class);
        when(annotation.type()).thenReturn(type);
        when(annotation.mapper()).thenReturn(mapper);
        when(annotation.other()).thenReturn(other);
        JsonFieldContext fieldContext = mock(JsonFieldContext.class);
        when(fieldContext.getAnnotation(Translation.class)).thenReturn(annotation);
        when(fieldContext.owner()).thenReturn(owner);
        when(fieldContext.propertyName()).thenReturn(propertyName);
        when(fieldContext.value()).thenReturn(value);
        return fieldContext;
    }

    @TranslationType(type = "test")
    private static class RecordingTranslation implements TranslationInterface<String> {

        private int batchCalls;
        private int singleCalls;
        private Set<Object> lastBatchKeys;
        private boolean omitBatchValue;
        private boolean throwOnSingle;

        /**
         * 记录单值翻译次数并返回可断言的翻译结果。
         *
         * @param key   翻译键
         * @param other 额外参数
         * @return 单值翻译结果
         */
        @Override
        public String translation(Object key, String other) {
            singleCalls++;
            if (throwOnSingle) {
                throw new IllegalStateException("translation failed");
            }
            return other + "-single-" + key;
        }

        /**
         * 记录批量翻译参数，并按测试开关生成或省略结果。
         *
         * @param keys  翻译键集合
         * @param other 额外参数
         * @return 批量翻译结果
         */
        @Override
        public Map<Object, String> translationBatch(Set<Object> keys, String other) {
            batchCalls++;
            lastBatchKeys = Set.copyOf(keys);
            Map<Object, String> result = new LinkedHashMap<>();
            if (!omitBatchValue) {
                keys.forEach(key -> result.put(key, other + "-batch-" + key));
            }
            return result;
        }

    }

    private static class SourceOwner {

        private final Long sourceId;

        /**
         * 创建带翻译源属性的测试对象。
         *
         * @param sourceId 翻译源 ID
         */
        private SourceOwner(Long sourceId) {
            this.sourceId = sourceId;
        }

        /**
         * 返回 Translation.mapper 需要读取的源 ID。
         *
         * @return 翻译源 ID
         */
        public Long getSourceId() {
            return sourceId;
        }

    }

}

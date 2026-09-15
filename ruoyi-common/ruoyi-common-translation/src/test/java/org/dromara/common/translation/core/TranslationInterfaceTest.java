package org.dromara.common.translation.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TranslationInterface 单元测试")
class TranslationInterfaceTest {

    private final TranslationInterface<String> translation = (key, other) -> other + "-" + key;

    /**
     * 验证默认批量实现按输入顺序逐项调用单值翻译。
     */
    @Test
    @DisplayName("默认批量翻译保持输入顺序并逐项翻译")
    void defaultBatchTranslationShouldPreserveOrder() {
        Set<Object> keys = new LinkedHashSet<>(List.of(2L, 1L));

        Map<Object, String> result = translation.translationBatch(keys, "name");

        assertEquals(List.of(2L, 1L), result.keySet().stream().toList());
        assertEquals(Map.of(2L, "name-2", 1L, "name-1"), result);
    }

    /**
     * 验证数字及逗号分隔字符串能够去重收集为 Long ID。
     */
    @Test
    @DisplayName("收集数字和逗号字符串中的 Long ID")
    void shouldCollectLongIds() {
        Set<Long> result = translation.collectLongIds(List.of("1, 2,1", 3L, " "));

        assertEquals(new LinkedHashSet<>(List.of(1L, 2L, 3L)), result);
        assertEquals(List.of(4L, 5L), translation.parseLongIds("4, 5"));
    }

    /**
     * 验证映射值按照原始 ID 顺序拼接，并忽略空映射结果。
     */
    @Test
    @DisplayName("按原始 ID 顺序拼接已映射值并忽略空结果")
    void shouldJoinMappedValuesInSourceOrder() {
        String result = translation.joinMappedValues("2,1,3", id -> id == 1L ? null : "user-" + id);

        assertEquals("user-2,user-3", result);
    }

}

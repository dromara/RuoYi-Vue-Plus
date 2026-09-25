package org.dromara.common.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StreamUtils 单元测试")
class StreamUtilsTest {

    /**
     * 验证过滤结果保持可修改，并对空输入返回空集合。
     */
    @Test
    @DisplayName("过滤结果可修改且空集合返回空列表")
    void filterShouldReturnMutableListAndHandleEmptyInput() {
        List<Integer> result = StreamUtils.filter(List.of(1, 2, 3), value -> value % 2 == 1);

        assertEquals(List.of(1, 3), result);
        assertDoesNotThrow(() -> result.add(5));
        assertTrue(StreamUtils.<Integer>filter(null, value -> true).isEmpty());
    }

    /**
     * 验证集合拼接会跳过映射函数产生的空值。
     */
    @Test
    @DisplayName("拼接时忽略映射结果中的空值")
    void joinShouldIgnoreNullMappedValues() {
        String result = StreamUtils.join(List.of("first", "skip", "last"),
            value -> "skip".equals(value) ? null : value, "|");

        assertEquals("first|last", result);
        assertEquals(StringUtils.EMPTY, StreamUtils.join(List.<String>of(), value -> value));
    }

    /**
     * 验证集合转 Map 时的空元素过滤和重复键处理规则。
     */
    @Test
    @DisplayName("转 Map 时重复键保留第一个值并忽略空元素")
    void toMapShouldKeepFirstDuplicateValue() {
        List<TestItem> items = Arrays.asList(
            new TestItem(1L, "first"),
            new TestItem(1L, "second"),
            null,
            new TestItem(2L, "third")
        );

        Map<Long, String> result = StreamUtils.toMap(items, TestItem::id, TestItem::value);

        assertEquals(Map.of(1L, "first", 2L, "third"), result);
    }

    /**
     * 验证分组结果按照键在输入中的首次出现顺序排列。
     */
    @Test
    @DisplayName("分组结果保持输入键的出现顺序")
    void groupByKeyShouldPreserveKeyOrder() {
        List<TestItem> items = List.of(
            new TestItem(2L, "a"),
            new TestItem(1L, "b"),
            new TestItem(2L, "c")
        );

        Map<Long, List<TestItem>> result = StreamUtils.groupByKey(items, TestItem::id);

        assertEquals(List.of(2L, 1L), result.keySet().stream().toList());
        assertEquals(List.of("a", "c"), result.get(2L).stream().map(TestItem::value).toList());
    }

    /**
     * 验证两个 Map 合并时同时处理独有键和共有键。
     */
    @Test
    @DisplayName("合并 Map 时覆盖两侧独有键和共有键")
    void mergeShouldCoverUnionOfKeys() {
        Map<Long, String> left = new LinkedHashMap<>();
        left.put(1L, "L1");
        left.put(2L, "L2");
        Map<Long, String> right = new LinkedHashMap<>();
        right.put(2L, "R2");
        right.put(3L, "R3");

        Map<Long, String> result = StreamUtils.merge(left, right,
            (leftValue, rightValue) -> String.valueOf(leftValue) + ":" + String.valueOf(rightValue));

        assertEquals("L1:null", result.get(1L));
        assertEquals("L2:R2", result.get(2L));
        assertEquals("null:R3", result.get(3L));
    }

    private record TestItem(Long id, String value) {
    }

}

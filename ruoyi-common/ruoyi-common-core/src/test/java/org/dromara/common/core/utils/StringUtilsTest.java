package org.dromara.common.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StringUtils 补充单元测试")
class StringUtilsTest {

    /**
     * 验证空白默认值、裁剪、截取、格式化和命名转换等基础字符串操作。
     */
    @Test
    @DisplayName("处理基础字符串转换")
    void shouldHandleBasicStringTransformations() {
        assertEquals("fallback", StringUtils.blankToDefault(" ", "fallback"));
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isNotEmpty("value"));
        assertEquals("value", StringUtils.trim(" value "));
        assertEquals("bc", StringUtils.substring("abcd", 1, 3));
        assertEquals("id=12", StringUtils.format("id={}", 12));
        assertEquals("user_name", StringUtils.toUnderScoreCase("userName"));
        assertEquals("HelloWorld", StringUtils.convertToCamelCase("HELLO_WORLD"));
        assertEquals("userName", StringUtils.toCamelCase("user_name"));
    }

    /**
     * 验证分隔字符串时的空白过滤、裁剪、去重和自定义类型转换行为。
     */
    @Test
    @DisplayName("拆分字符串集合")
    void shouldSplitStringsIntoCollections() {
        assertEquals(List.of("a", "b"), StringUtils.str2List(" a, ,b ", ",", true, true));
        assertEquals(Set.of("a", "b"), StringUtils.str2Set("a,b,a", ","));
        assertEquals(List.of("a", "b"), StringUtils.splitList("a,b"));
        assertEquals(List.of(1, 2), StringUtils.splitTo("1|2", "|", value -> Integer.valueOf(value.toString())));
        assertTrue(StringUtils.splitList(" ").isEmpty());
    }

    /**
     * 验证 Ant 风格路径规则可以区分单层通配符、跨层通配符和空输入。
     */
    @Test
    @DisplayName("匹配路径规则")
    void shouldMatchAntStylePaths() {
        assertTrue(StringUtils.isMatch("/system/**", "/system/user/list"));
        assertFalse(StringUtils.isMatch("/system/*", "/system/user/list"));
        assertTrue(StringUtils.matches("/system/user/list", List.of("/login", "/system/**")));
        assertFalse(StringUtils.matches("", List.of("/**")));
        assertFalse(StringUtils.matches("/system", List.of()));
    }

    /**
     * 验证定长左补齐会补零、截取尾部，并正确处理空值。
     */
    @Test
    @DisplayName("定长左补齐字符串")
    void shouldPadOrTruncateFromLeft() {
        assertEquals("0012", StringUtils.padl(12, 4));
        assertEquals("cdef", StringUtils.padl("abcdef", 4, '0'));
        assertEquals("***a", StringUtils.padl("a", 4, '*'));
        assertEquals("000", StringUtils.padl(null, 3, '0'));
    }

    /**
     * 验证大小写敏感和忽略大小写的查找、前后缀与替换方法保持不同语义。
     */
    @Test
    @DisplayName("比较和替换字符串")
    void shouldCompareAndReplaceStrings() {
        assertTrue(StringUtils.containsAnyIgnoreCase("Hello", "WORLD", "he"));
        assertTrue(StringUtils.inStringIgnoreCase("ADMIN", "user", "admin"));
        assertTrue(StringUtils.startWithAnyIgnoreCase("Bearer token", "basic", "bearer"));
        assertTrue(StringUtils.equalsAny("a", "b", "a"));
        assertTrue(StringUtils.equalsAnyIgnoreCase("A", "b", "a"));
        assertTrue(StringUtils.containsIgnoreCase("Hello", "ELL"));
        assertTrue(StringUtils.endsWithIgnoreCase("report.XLSX", ".xlsx"));
        assertEquals(2, StringUtils.indexOf("abcabc", "ca"));
        assertEquals("path", StringUtils.removeStart("/path", "/"));
        assertEquals("a-b_c", StringUtils.replaceOnce("a_b_c", "_", "-"));
    }

    /**
     * 验证字符集转换、URL 判断与逗号拼接的公共便捷方法。
     */
    @Test
    @DisplayName("处理编码和拼接")
    void shouldHandleEncodingUrlAndJoining() {
        assertEquals("中文", StringUtils.convert("中文", StandardCharsets.UTF_8, StandardCharsets.UTF_8));
        assertEquals("", StringUtils.convert("", StandardCharsets.UTF_8, StandardCharsets.UTF_16));
        assertTrue(StringUtils.ishttp("https://example.com/path"));
        assertEquals("a,b", StringUtils.joinComma(List.of("a", "b")));
        assertEquals("1,2", StringUtils.joinComma(new Integer[]{1, 2}));
    }
}

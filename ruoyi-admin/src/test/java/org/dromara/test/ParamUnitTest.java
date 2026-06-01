package org.dromara.test;

import org.dromara.common.core.enums.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 带参数单元测试案例
 *
 * @author Lion Li
 */
@DisplayName("带参数单元测试案例")
public class ParamUnitTest {

    /**
     * 参数化测试共用的字符串样例。
     */
    private static final List<String> TEST_VALUES = List.of("t1", "t2", "t3");

    /**
     * 提供 {@link MethodSource} 参数化测试数据。
     *
     * @return 测试参数流
     */
    public static Stream<String> getParam() {
        return TEST_VALUES.stream();
    }

    /**
     * 验证 {@link ValueSource} 能按固定字符串集合逐个传参。
     *
     * @param str 当前参数值
     */
    @DisplayName("测试 @ValueSource 注解")
    @ParameterizedTest
    @ValueSource(strings = {"t1", "t2", "t3"})
    public void testValueSource(String str) {
        assertTrue(TEST_VALUES.contains(str));
    }

    /**
     * 验证 {@link NullSource} 能传入空值参数。
     *
     * @param str 当前参数值
     */
    @DisplayName("测试 @NullSource 注解")
    @ParameterizedTest
    @NullSource
    public void testNullSource(String str) {
        assertNull(str);
    }

    /**
     * 验证 {@link EnumSource} 能遍历用户类型枚举。
     *
     * @param type 当前用户类型
     */
    @DisplayName("测试 @EnumSource 注解")
    @ParameterizedTest
    @EnumSource(UserType.class)
    public void testEnumSource(UserType type) {
        assertNotNull(type);
        assertFalse(type.getUserType().isBlank());
    }

    /**
     * 验证 {@link MethodSource} 能读取方法提供的参数流。
     *
     * @param str 当前参数值
     */
    @DisplayName("测试 @MethodSource 注解")
    @ParameterizedTest
    @MethodSource("getParam")
    public void testMethodSource(String str) {
        assertTrue(TEST_VALUES.contains(str));
    }

    /**
     * 每个参数化测试执行前的初始化示例。
     */
    @BeforeEach
    public void testBeforeEach() {
        System.out.println("@BeforeEach ==================");
    }

    /**
     * 每个参数化测试执行后的清理示例。
     */
    @AfterEach
    public void testAfterEach() {
        System.out.println("@AfterEach ==================");
    }


}

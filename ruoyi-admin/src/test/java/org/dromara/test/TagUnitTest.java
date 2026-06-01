package org.dromara.test;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 标签单元测试案例
 *
 * @author Lion Li
 */
@DisplayName("标签单元测试案例")
public class TagUnitTest {

    /**
     * 验证 dev 标签测试可以独立筛选执行。
     */
    @Tag("dev")
    @DisplayName("测试 @Tag dev")
    @Test
    public void testTagDev() {
        assertEquals("dev", "dev");
    }

    /**
     * 验证 prod 标签测试可以独立筛选执行。
     */
    @Tag("prod")
    @DisplayName("测试 @Tag prod")
    @Test
    public void testTagProd() {
        assertEquals("prod", "prod");
    }

    /**
     * 验证 local 标签测试可以独立筛选执行。
     */
    @Tag("local")
    @DisplayName("测试 @Tag local")
    @Test
    public void testTagLocal() {
        assertEquals("local", "local");
    }

    /**
     * 验证 exclude 标签测试可以配合构建配置排除。
     */
    @Tag("exclude")
    @DisplayName("测试 @Tag exclude")
    @Test
    public void testTagExclude() {
        assertEquals("exclude", "exclude");
    }

    /**
     * 每个标签测试执行前的初始化示例。
     */
    @BeforeEach
    public void testBeforeEach() {
        System.out.println("@BeforeEach ==================");
    }

    /**
     * 每个标签测试执行后的清理示例。
     */
    @AfterEach
    public void testAfterEach() {
        System.out.println("@AfterEach ==================");
    }


}

package org.dromara.test;

import org.dromara.common.web.config.properties.CaptchaProperties;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试基础案例。
 *
 * @author Lion Li
 */
@DisplayName("单元测试案例")
public class DemoUnitTest {

    /**
     * 所有测试执行前的初始化示例。
     */
    @BeforeAll
    public static void testBeforeAll() {
        System.out.println("@BeforeAll ==================");
    }

    /**
     * 所有测试执行后的清理示例。
     */
    @AfterAll
    public static void testAfterAll() {
        System.out.println("@AfterAll ==================");
    }

    /**
     * 验证普通 {@link Test} 与 {@link DisplayName} 注解的使用方式。
     */
    @DisplayName("测试 @Test @DisplayName 注解")
    @Test
    public void testTest() {
        CaptchaProperties captchaProperties = new CaptchaProperties();
        captchaProperties.setEnable(Boolean.TRUE);
        captchaProperties.setType("math");
        captchaProperties.setNumberLength(1);
        captchaProperties.setCharLength(4);

        assertAll("验证码配置属性",
            () -> assertTrue(captchaProperties.getEnable()),
            () -> assertEquals("math", captchaProperties.getType()),
            () -> assertEquals(1, captchaProperties.getNumberLength()),
            () -> assertEquals(4, captchaProperties.getCharLength())
        );
    }

    /**
     * 演示 {@link Disabled} 注解，保留一个不会被执行的测试占位。
     */
    @Disabled
    @DisplayName("测试 @Disabled 注解")
    @Test
    public void testDisabled() {
        fail("禁用测试不应被执行");
    }

    /**
     * 验证 {@link Timeout} 注解在指定时间内可以正常通过。
     *
     * @throws InterruptedException 线程等待被中断时抛出
     */
    @Timeout(value = 2L, unit = TimeUnit.SECONDS)
    @DisplayName("测试 @Timeout 注解")
    @Test
    public void testTimeout() throws InterruptedException {
        Thread.sleep(100);
        assertTrue(true);
    }

    /**
     * 验证 {@link RepeatedTest} 注解会按指定次数重复执行。
     */
    @DisplayName("测试 @RepeatedTest 注解")
    @RepeatedTest(3)
    public void testRepeatedTest() {
        assertDoesNotThrow(() -> Integer.parseInt("666"));
    }

    /**
     * 每个测试执行前的初始化示例。
     */
    @BeforeEach
    public void testBeforeEach() {
        System.out.println("@BeforeEach ==================");
    }

    /**
     * 每个测试执行后的清理示例。
     */
    @AfterEach
    public void testAfterEach() {
        System.out.println("@AfterEach ==================");
    }

}

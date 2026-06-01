package org.dromara.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 断言单元测试案例
 *
 * @author Lion Li
 */
@DisplayName("断言单元测试案例")
public class AssertUnitTest {

    /**
     * 验证相等与不相等断言，确保值比较语义清晰。
     */
    @DisplayName("测试 assertEquals 方法")
    @Test
    public void testAssertEquals() {
        Assertions.assertEquals("666", new String("666"));
        Assertions.assertNotEquals("666", "777");
    }

    /**
     * 验证同一对象引用与不同对象引用的断言。
     */
    @DisplayName("测试 assertSame 方法")
    @Test
    public void testAssertSame() {
        Object obj = new Object();
        Object obj1 = obj;
        Object obj2 = new Object();
        Assertions.assertSame(obj, obj1);
        Assertions.assertNotSame(obj, obj2);
    }

    /**
     * 验证布尔条件断言，覆盖 true 与 false 两类结果。
     */
    @DisplayName("测试 assertTrue 方法")
    @Test
    public void testAssertTrue() {
        Assertions.assertTrue(true);
        Assertions.assertFalse(false);
    }

    /**
     * 验证空值与非空值断言，避免空指针场景被误判。
     */
    @DisplayName("测试 assertNull 方法")
    @Test
    public void testAssertNull() {
        Assertions.assertNull(null);
        Assertions.assertNotNull("not null");
    }

}

package org.dromara.common.mybatis.helper;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.domain.DataPermissionAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("DataPermissionHelper 单元测试")
class DataPermissionHelperTest {

    @DataPermission({})
    interface ScopedMapper {
        void list();

        @DataPermission(value = {}, joinStr = "AND")
        void update();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void proxyMapperInstallsMethodOrInterfacePermissionAndRestoresOuterScope(boolean update) throws Throwable {
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Runnable.class, ScopedMapper.class}, (obj, method, args) -> null);
        var method = ScopedMapper.class.getMethod(update ? "update" : "list");
        var pointcut = new org.dromara.common.mybatis.aspect.DataPermissionPointcut();
        assertTrue(pointcut.matches(method, proxy.getClass()));
        assertFalse(pointcut.matches(Runnable.class.getMethod("run"), Runnable.class));
        var invocation = mock(org.aopalliance.intercept.MethodInvocation.class);
        org.mockito.Mockito.when(invocation.getThis()).thenReturn(proxy);
        org.mockito.Mockito.when(invocation.getMethod()).thenReturn(method);
        var outer = update ? mock(DataPermission.class) : null;
        DataPermissionHelper.setPermission(outer);
        org.mockito.Mockito.when(invocation.proceed()).thenAnswer(call -> {
            assertEquals(update ? "AND" : "", DataPermissionHelper.getPermission().joinStr());
            return 42;
        });
        assertEquals(42, new org.dromara.common.mybatis.aspect.DataPermissionAdvice().invoke(invocation));
        assertSame(outer, DataPermissionHelper.getPermission());
    }
    /**
     * 清理数据权限测试使用的线程变量和 MyBatis-Plus 忽略策略。
     */
    @AfterEach
    void clearThreadContext() {
        DataPermissionHelper.removePermission();
        InterceptorIgnoreHelper.clearIgnoreStrategy();
    }

    /**
     * 验证 Mapper 数据权限注解可以在线程内设置、读取并清理。
     */
    @Test
    @DisplayName("管理数据权限注解线程变量")
    void shouldManagePermissionThreadLocal() {
        DataPermission permission = mock(DataPermission.class);

        DataPermissionHelper.setPermission(permission);
        assertSame(permission, DataPermissionHelper.getPermission());

        DataPermissionHelper.removePermission();
        assertNull(DataPermissionHelper.getPermission());
    }

    /**
     * 验证数据权限访问上下文根据接口权限或角色集合判断是否存在约束。
     */
    @Test
    @DisplayName("判断数据权限访问约束")
    void shouldDetectDataPermissionConstraints() {
        assertFalse(DataPermissionAccess.EMPTY.constrained());
        assertTrue(new DataPermissionAccess(Set.of("system:user:list"), Set.of()).constrained());
        assertTrue(new DataPermissionAccess(Set.of(), Set.of("admin")).constrained());
    }

    /**
     * 验证嵌套忽略数据权限会在内部保持忽略状态，并在退出后恢复原始线程状态。
     */
    @Test
    @DisplayName("嵌套忽略并恢复数据权限")
    void shouldNestAndRestoreDataPermissionIgnoreState() {
        assertFalse(InterceptorIgnoreHelper.willIgnoreDataPermission("test.select"));

        String result = DataPermissionHelper.ignore(() -> {
            assertTrue(InterceptorIgnoreHelper.willIgnoreDataPermission("test.select"));
            DataPermissionHelper.ignore(() ->
                assertTrue(InterceptorIgnoreHelper.willIgnoreDataPermission("test.select")));
            assertTrue(InterceptorIgnoreHelper.willIgnoreDataPermission("test.select"));
            return "done";
        });

        assertEquals("done", result);
        assertFalse(InterceptorIgnoreHelper.willIgnoreDataPermission("test.select"));
    }

    /**
     * 验证忽略数据权限的业务代码抛出异常时仍会在 finally 中恢复线程状态。
     */
    @Test
    @DisplayName("异常后恢复数据权限忽略状态")
    void shouldRestoreIgnoreStateAfterException() {
        assertThrows(IllegalStateException.class, () -> DataPermissionHelper.ignore(() -> {
            throw new IllegalStateException("failed");
        }));

        assertFalse(InterceptorIgnoreHelper.willIgnoreDataPermission("test.select"));
    }
}

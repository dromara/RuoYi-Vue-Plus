package org.dromara.common.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.hutool.http.HttpStatus;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.core.dao.PlusSaTokenDao;
import org.dromara.common.satoken.core.service.SaPermissionImpl;
import org.dromara.common.satoken.handler.SaTokenExceptionHandler;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.PermissionService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.api.model.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("common-satoken 功能单元测试")
class SaTokenFunctionTest {

    /**
     * 验证权限校验失败统一返回 HTTP 403 业务响应。
     */
    @Test
    @DisplayName("处理权限校验异常")
    void shouldHandlePermissionException() {
        SaTokenExceptionHandler handler = new SaTokenExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user");

        R<Void> result = handler.handleNotAccessException(new NotPermissionException("system:user:list"), request);

        assertEquals(HttpStatus.HTTP_FORBIDDEN, result.getCode());
        assertEquals("没有访问权限，请联系管理员授权", result.getMsg());
    }

    /**
     * 验证不同未登录类型转换为对应的用户提示。
     */
    @Test
    @DisplayName("处理登录超时和被顶下线异常")
    void shouldHandleNotLoginExceptionByType() {
        SaTokenExceptionHandler handler = new SaTokenExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");

        NotLoginException timeout = NotLoginException.newInstance("login", NotLoginException.TOKEN_TIMEOUT, "timeout", "token");
        NotLoginException replaced = NotLoginException.newInstance("login", NotLoginException.BE_REPLACED, "replaced", "token");

        assertEquals("登录已过期，请重新登录", handler.handleNotLoginException(timeout, request).getMsg());
        assertEquals("当前账号已在其他设备登录，您已被强制下线", handler.handleNotLoginException(replaced, request).getMsg());
    }

    /**
     * 验证 Redis 毫秒 TTL 转换为 Sa-Token 秒 TTL 时包含精度补偿，并保留特殊负值。
     */
    @Test
    @DisplayName("转换 Sa-Token 过期时间")
    void shouldConvertRedisTimeoutToSeconds() {
        PlusSaTokenDao dao = new PlusSaTokenDao();

        assertEquals(2L, ((Long) ReflectionTestUtils.invokeMethod(dao, "toTimeoutSeconds", 1000L)).longValue());
        assertEquals(-1L, ((Long) ReflectionTestUtils.invokeMethod(dao, "toTimeoutSeconds", -1L)).longValue());
        assertEquals(-2L, ((Long) ReflectionTestUtils.invokeMethod(dao, "toTimeoutSeconds", -2L)).longValue());
    }

    /**
     * 验证当前登录对象的菜单和角色权限直接来自会话快照，并对空权限集合返回可修改空列表。
     */
    @Test
    @DisplayName("读取当前会话权限快照")
    void shouldReadPermissionsFromCurrentLoginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserType("sys_user");
        loginUser.setUserId(7L);
        loginUser.setMenuPermission(Set.of("system:user:list", "system:user:add"));
        loginUser.setRolePermission(Set.of());

        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(loginUser);
            SaPermissionImpl permission = new SaPermissionImpl();

            List<String> menus = permission.getPermissionList("sys_user:7", "login");
            List<String> roles = permission.getRoleList("sys_user:7", "login");

            assertEquals(Set.of("system:user:list", "system:user:add"), Set.copyOf(menus));
            assertEquals(List.of(), roles);
            roles.add("temporary");
        }
    }

    /**
     * 验证查询其他登录对象时按登录 ID 提取用户 ID 并委托权限服务，格式错误时立即失败。
     */
    @Test
    @DisplayName("通过权限服务查询其他用户权限")
    void shouldLoadRemotePermissionsAndRejectMalformedLoginId() {
        PermissionService service = mock(PermissionService.class);
        when(service.getMenuPermission(9L)).thenReturn(Set.of("system:dept:list"));

        try (var login = mockStatic(LoginHelper.class);
             var spring = mockStatic(SpringUtil.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(null);
            spring.when(() -> SpringUtil.getBean(PermissionService.class)).thenReturn(service);
            SaPermissionImpl permission = new SaPermissionImpl();

            assertEquals(List.of("system:dept:list"), permission.getPermissionList("sys_user:9", "login"));
            assertThrows(ServiceException.class, () -> permission.getPermissionList("invalid", "login"));
            verify(service).getMenuPermission(9L);
        }
    }

    /**
     * 验证系统未提供权限服务时返回明确业务错误，而不是空权限导致静默拒绝。
     */
    @Test
    @DisplayName("缺少权限服务时明确失败")
    void shouldFailClearlyWhenPermissionServiceIsMissing() {
        try (var login = mockStatic(LoginHelper.class);
             var spring = mockStatic(SpringUtil.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(null);
            spring.when(() -> SpringUtil.getBean(PermissionService.class))
                .thenThrow(new IllegalStateException("missing bean"));

            ServiceException exception = assertThrows(ServiceException.class,
                () -> new SaPermissionImpl().getRoleList("sys_user:9", "login"));

            assertEquals("PermissionService 实现类不存在", exception.getMessage());
        }
    }
}

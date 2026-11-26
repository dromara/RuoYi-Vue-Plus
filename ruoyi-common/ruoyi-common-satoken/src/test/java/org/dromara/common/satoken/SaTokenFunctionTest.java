package org.dromara.common.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.satoken.core.dao.PlusSaTokenDao;
import org.dromara.common.satoken.core.service.SaPermissionImpl;
import org.dromara.common.satoken.handler.SaTokenExceptionHandler;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.core.service.PermissionService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.api.model.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.dromara.common.redis.utils.RedisUtils;
import org.redisson.api.RedissonClient;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("common-satoken 功能单元测试")
class SaTokenFunctionTest {

    @Test
    void tokenLookupReturnsSessionSnapshotAndHandlesAbsentOrExpiredTokens() {
        var user = new LoginUser();
        var session = mock(cn.dev33.satoken.session.SaSession.class);
        when(session.get(LoginHelper.LOGIN_USER_KEY)).thenReturn(user);
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getTokenSessionByToken("valid")).thenReturn(session);
            stp.when(() -> StpUtil.getTokenSessionByToken("expired")).thenThrow(
                NotLoginException.newInstance("login", NotLoginException.TOKEN_TIMEOUT, "expired", "expired"));
            assertEquals(user, LoginHelper.getLoginUser("valid"));
            org.junit.jupiter.api.Assertions.assertNull(LoginHelper.getLoginUser(" "));
            org.junit.jupiter.api.Assertions.assertNull(LoginHelper.getLoginUser("expired"));
            org.junit.jupiter.api.Assertions.assertNull(LoginHelper.getLoginUser("unknown"));
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            assertEquals(user, LoginHelper.getLoginUser());
        }
    }

    @Test
    void loginKeepsExistingTerminalInformationAndSavesTheUserSnapshot() {
        var user = new LoginUser();
        user.setUserId(12L);
        user.setUserType("sys_user");
        user.setBrowser("mobile browser");
        user.setOs("mobile OS");
        user.setLoginLocation("office");
        var request = new MockHttpServletRequest();
        var session = mock(cn.dev33.satoken.session.SaSession.class);
        try (var servlet = mockStatic(org.dromara.common.core.utils.ServletUtils.class);
             var stp = mockStatic(StpUtil.class)) {
            servlet.when(org.dromara.common.core.utils.ServletUtils::getRequest).thenReturn(request);
            servlet.when(() -> org.dromara.common.core.utils.ServletUtils.getClientIP(request)).thenReturn("192.0.2.7");
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            var model = new cn.dev33.satoken.stp.parameter.SaLoginParameter().setDeviceType("mobile");
            LoginHelper.login(user, model);
            assertEquals("192.0.2.7", user.getIpaddr());
            assertEquals("mobile", user.getDeviceType());
            assertEquals("office", user.getLoginLocation());
            assertEquals("mobile browser", user.getBrowser());
            assertEquals("mobile OS", user.getOs());
            verify(session).set(LoginHelper.LOGIN_USER_KEY, user);
            stp.verify(() -> StpUtil.login(user.getLoginId(), model));
        }
    }
    @Test
    void tokenValuesUseSecondsAndRefreshTheLocalCacheAfterMutations() {
        var dao = new PlusSaTokenDao();
        String key = "token:login:test";
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            try (var redis = mockStatic(RedisUtils.class)) {
                dao.set(key, "ignored", 0);
                dao.set(key, "expired", -2);
                dao.update(key, "missing");
                dao.updateObject(key, "missing");
                redis.verify(() -> RedisUtils.setCacheObject(key, "missing", true), never());
                redis.verify(() -> RedisUtils.setCacheObject(key, "ignored", Duration.ZERO), never());
                redis.verify(() -> RedisUtils.setCacheObject(key, "expired", Duration.ofSeconds(-2)), never());

                dao.set(key, "initial", -1);
                redis.verify(() -> RedisUtils.setCacheObject(key, "initial"));
                redis.when(() -> RedisUtils.getCacheObject(key)).thenReturn("initial", "updated", 42, 43);
                assertEquals("initial", dao.get(key));
                assertEquals("initial", dao.get(key));
                redis.verify(() -> RedisUtils.getCacheObject(key), times(1));

                redis.when(() -> RedisUtils.hasKey(key)).thenReturn(true);
                dao.update(key, "updated");
                redis.verify(() -> RedisUtils.setCacheObject(key, "updated", true));
                assertEquals("updated", dao.get(key));

                dao.setObject(key, 42, 60);
                redis.verify(() -> RedisUtils.setCacheObject(key, 42, Duration.ofSeconds(60)));
                assertEquals(42, dao.getObject(key, Integer.class));
                dao.updateObject(key, 43);
                redis.verify(() -> RedisUtils.setCacheObject(key, 43, true));
                assertEquals(43, dao.getObject(key));

                redis.when(() -> RedisUtils.deleteObject(key)).thenReturn(false, true);
                dao.delete(key);
                assertEquals(43, dao.getObject(key));
                dao.deleteObject(key);
                redis.when(() -> RedisUtils.getCacheObject(key)).thenReturn(null);
                org.junit.jupiter.api.Assertions.assertNull(dao.getObject(key));

                dao.updateTimeout(key, 10);
                dao.updateObjectTimeout(key, 20);
                redis.verify(() -> RedisUtils.expire(key, Duration.ofSeconds(10)));
                redis.verify(() -> RedisUtils.expire(key, Duration.ofSeconds(20)));
                redis.when(() -> RedisUtils.getTimeToLive(key)).thenReturn(1500L, -2L);
                assertEquals(2, dao.getTimeout(key));
                assertEquals(-2, dao.getObjectTimeout(key));
            }
        }
    }
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

        assertEquals("登录已过期，请重新登录", ((R<?>) handler.handleNotLoginException(timeout, request).getBody()).getMsg());
        assertEquals("当前账号已在其他设备登录，您已被强制下线", ((R<?>) handler.handleNotLoginException(replaced, request).getBody()).getMsg());
    }

    @Test
    @DisplayName("SSE 登录失效返回无正文 401")
    void shouldRejectSseWithoutJsonConversion() {
        SaTokenExceptionHandler handler = new SaTokenExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resource/message");
        request.setServletPath("/resource/message");
        NotLoginException expired = NotLoginException.newInstance("login", NotLoginException.TOKEN_TIMEOUT, "timeout", "token");

        var response = handler.handleNotLoginException(expired, request);

        assertEquals(HttpStatus.HTTP_UNAUTHORIZED, response.getStatusCode().value());
        assertEquals(null, response.getBody());
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
     * 历史冒号格式与当前分隔符格式的登录ID均应命中会话快照。
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

            assertEquals(Set.of("system:user:list", "system:user:add"),
                Set.copyOf(permission.getPermissionList("sys_user-7", "login")));
            assertEquals(List.of(), permission.getRoleList("sys_user-7", "login"));
        }
    }

    @Test
    @DisplayName("精确解析当前登录身份的用户类型")
    void shouldResolveUserTypeFromLoginIdPrefix() {
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsString).thenReturn("sys_user-7", "app_user:9", "prefix_sys_user_suffix-7");

            assertEquals(UserType.SYS_USER, LoginHelper.getUserType());
            assertEquals(UserType.APP_USER, LoginHelper.getUserType());
            assertEquals("用户类型无效", assertThrows(RuntimeException.class,
                LoginHelper::getUserType).getMessage());
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
            assertEquals(List.of("system:dept:list"), permission.getPermissionList("sys_user-9", "login"));
            assertThrows(RuntimeException.class, () -> permission.getPermissionList("invalid", "login"));
            verify(service, times(2)).getMenuPermission(9L);
        }
    }

    /**
     * 验证权限服务获取失败时保留根因，并由通用异常处理器隐藏内部配置。
     */
    @Test
    @DisplayName("缺少权限服务时明确失败")
    void shouldFailClearlyWhenPermissionServiceIsMissing() {
        try (var login = mockStatic(LoginHelper.class);
             var spring = mockStatic(SpringUtil.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(null);
            spring.when(() -> SpringUtil.getBean(PermissionService.class))
                .thenThrow(new IllegalStateException("missing bean"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new SaPermissionImpl().getRoleList("sys_user:9", "login"));

            assertEquals("权限服务获取失败", exception.getMessage());
            assertEquals("missing bean", exception.getCause().getMessage());
        }
    }
}

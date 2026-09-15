package org.dromara.common.security;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.DispatcherType;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.security.config.SecurityConfig;
import org.dromara.common.security.config.properties.SecurityProperties;
import org.dromara.common.security.handler.AllUrlHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("common-security 功能单元测试")
class SecurityConfigTest {

    /**
     * 验证 Sa-Token 上下文过滤器覆盖请求、异步和错误分发，并保持最高优先级。
     */
    @Test
    @DisplayName("注册 Sa-Token 上下文过滤器")
    void shouldRegisterSaTokenContextFilterForAsyncDispatch() {
        SecurityConfig config = new SecurityConfig(new SecurityProperties());
        SaTokenContextFilterForJakartaServlet filter = mock(SaTokenContextFilterForJakartaServlet.class);

        FilterRegistrationBean<SaTokenContextFilterForJakartaServlet> registration =
            config.saTokenContextFilterRegistration(filter);

        assertSame(filter, registration.getFilter());
        assertEquals("saTokenContextFilterForServlet", registration.getFilterName());
        assertEquals(Set.of("/*"), registration.getUrlPatterns());
        assertEquals(Set.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR),
            ReflectionTestUtils.getField(registration, "dispatcherTypes"));
        assertTrue(registration.isAsyncSupported());
        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
    }

    /**
     * 验证安全排除路径配置可以完整保存，供拦截器注册时使用。
     */
    @Test
    @DisplayName("保存安全排除路径")
    void shouldStoreSecurityExcludePaths() {
        SecurityProperties properties = new SecurityProperties();
        properties.setExcludes(new String[]{"/login", "/captcha"});

        assertArrayEquals(new String[]{"/login", "/captcha"}, properties.getExcludes());
    }

    /**
     * 验证控制器路径中的变量会归一化为通配符，并去重保存为统一鉴权 URL 列表。
     */
    @Test
    @DisplayName("收集并归一化全部控制器路径")
    void shouldCollectNormalizedControllerUrls() {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo info = mock(RequestMappingInfo.class);
        PathPatternsRequestCondition condition = mock(PathPatternsRequestCondition.class);
        when(condition.getPatterns()).thenReturn(Set.of(
            PathPatternParser.defaultInstance.parse("/users/{id}"),
            PathPatternParser.defaultInstance.parse("/users/list")));
        when(info.getPathPatternsCondition()).thenReturn(condition);
        when(mapping.getHandlerMethods()).thenReturn(Map.of(info, mock(HandlerMethod.class)));

        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class))
                .thenReturn(mapping);
            AllUrlHandler handler = new AllUrlHandler();

            handler.afterPropertiesSet();

            assertEquals(Set.of("/users/*", "/users/list"), Set.copyOf(handler.getUrls()));
        }
    }

    /**
     * 验证客户端授权路径和 IP 白名单都通过时允许访问，任一规则不匹配时拒绝请求。
     */
    @Test
    @DisplayName("校验客户端路径和 IP 访问规则")
    void shouldValidateClientPathAndIpRules() {
        SecurityConfig config = new SecurityConfig(new SecurityProperties());
        MockHttpServletRequest allowed = new MockHttpServletRequest("GET", "/system/users");
        allowed.setServletPath("/system/users");
        allowed.addHeader("X-Forwarded-For", "10.0.0.8");

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getExtra("clientAccessPath")).thenReturn("/system/**,/profile");
            stp.when(() -> StpUtil.getExtra("clientIpWhitelist")).thenReturn("10.0.0.0/24;127.0.0.1");

            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(config, "validateClientAccessRules", allowed));

            MockHttpServletRequest denied = new MockHttpServletRequest("GET", "/admin/users");
            denied.setServletPath("/admin/users");
            denied.addHeader("X-Forwarded-For", "10.0.0.8");
            assertThrows(NotPermissionException.class,
                () -> ReflectionTestUtils.invokeMethod(config, "validateClientAccessRules", denied));
        }
    }
}

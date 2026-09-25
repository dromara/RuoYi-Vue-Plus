package org.dromara.common.social;

import cn.hutool.extra.spring.SpringUtil;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthMicrosoftRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthStackOverflowRequest;
import me.zhyd.oauth.request.AuthWeChatEnterpriseQrcodeV2Request;
import org.dromara.common.social.config.properties.SocialLoginConfigProperties;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.AuthRedisStateCache;
import org.dromara.common.social.utils.SocialUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("SocialUtils 本地构造契约单元测试")
class SocialUtilsTest {

    /**
     * 注册授权状态缓存 mock，使 SocialUtils 可以完成请求对象构造而不连接 Redis。
     */
    @BeforeAll
    static void initializeSocialContext() {
        AuthRedisStateCache stateCache = mock(AuthRedisStateCache.class);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("authRedisStateCache", stateCache);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 验证 GitHub 配置映射为对应请求类型，并完整保留基础授权配置。
     */
    @Test
    @DisplayName("构造 GitHub 授权请求")
    void shouldBuildGithubRequestWithBaseConfig() throws Exception {
        SocialLoginConfigProperties config = config();

        AuthRequest request = SocialUtils.getAuthRequest("github", properties("github", config));

        assertInstanceOf(AuthGithubRequest.class, request);
        AuthConfig authConfig = requestConfig(request);
        assertEquals("client-id", authConfig.getClientId());
        assertEquals("client-secret", authConfig.getClientSecret());
        assertEquals("https://example.test/callback", authConfig.getRedirectUri());
        assertEquals(List.of("user", "email"), authConfig.getScopes());
    }

    /**
     * 验证 Microsoft、Stack Overflow 和企业微信的专用配置会写入 JustAuth AuthConfig。
     */
    @Test
    @DisplayName("映射平台专用授权配置")
    void shouldMapProviderSpecificConfiguration() throws Exception {
        SocialLoginConfigProperties microsoft = config();
        microsoft.setTenantId("tenant-id");
        SocialLoginConfigProperties stackOverflow = config();
        stackOverflow.setStackOverflowKey("stack-key");
        SocialLoginConfigProperties enterprise = config();
        enterprise.setAgentId("agent-id");
        SocialProperties properties = new SocialProperties();
        properties.setType(Map.of(
            "microsoft", microsoft,
            "stack_overflow", stackOverflow,
            "wechat_enterprise", enterprise));

        assertInstanceOf(AuthMicrosoftRequest.class, SocialUtils.getAuthRequest("microsoft", properties));
        assertEquals("tenant-id", requestConfig(SocialUtils.getAuthRequest("microsoft", properties)).getTenantId());
        assertEquals("stack-key", requestConfig(SocialUtils.getAuthRequest("stack_overflow", properties)).getStackOverflowKey());
        assertInstanceOf(AuthStackOverflowRequest.class, SocialUtils.getAuthRequest("stack_overflow", properties));
        assertEquals("agent-id", requestConfig(SocialUtils.getAuthRequest("wechat_enterprise", properties)).getAgentId());
        assertInstanceOf(AuthWeChatEnterpriseQrcodeV2Request.class,
            SocialUtils.getAuthRequest("wechat_enterprise", properties));
    }

    /**
     * 验证缺少平台配置和 switch 未支持的平台分别返回清晰的授权异常。
     */
    @Test
    @DisplayName("拒绝不支持的平台配置")
    void shouldRejectMissingAndUnknownProviderConfiguration() {
        SocialProperties empty = new SocialProperties();
        empty.setType(Map.of());
        AuthException missing = assertThrows(AuthException.class,
            () -> SocialUtils.getAuthRequest("github", empty));

        SocialProperties unknown = properties("future_provider", config());
        AuthException unsupported = assertThrows(AuthException.class,
            () -> SocialUtils.getAuthRequest("future_provider", unknown));

        assertEquals("不支持的第三方登录类型", missing.getMessage());
        assertEquals("未获取到有效的Auth配置", unsupported.getMessage());
    }

    /**
     * 创建包含公共授权参数的社交平台配置。
     *
     * @return 测试配置
     */
    private static SocialLoginConfigProperties config() {
        SocialLoginConfigProperties config = new SocialLoginConfigProperties();
        config.setClientId("client-id");
        config.setClientSecret("client-secret");
        config.setRedirectUri("https://example.test/callback");
        config.setServerUrl("https://example.test");
        config.setScopes(List.of("user", "email"));
        return config;
    }

    /**
     * 创建只包含一个平台配置的容器。
     *
     * @param source 平台标识
     * @param config 平台配置
     * @return 社交平台属性
     */
    private static SocialProperties properties(String source, SocialLoginConfigProperties config) {
        SocialProperties properties = new SocialProperties();
        properties.setType(Map.of(source, config));
        return properties;
    }

    /**
     * 读取 JustAuth 请求对象继承层级中的 AuthConfig，验证工具到第三方库的映射结果。
     *
     * @param request 授权请求
     * @return JustAuth 配置
     * @throws Exception 反射读取失败
     */
    private static AuthConfig requestConfig(AuthRequest request) throws Exception {
        Class<?> type = request.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("config");
                field.setAccessible(true);
                return (AuthConfig) field.get(request);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        fail("授权请求未找到 AuthConfig 字段");
        return null;
    }
}

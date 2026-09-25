package org.dromara.common.redis;

import cn.hutool.http.HttpStatus;
import com.baomidou.lock.exception.LockFailureException;
import org.dromara.common.core.domain.R;
import org.dromara.common.redis.aspectj.RateLimiterAspect;
import org.dromara.common.redis.aspectj.RepeatSubmitAspect;
import org.dromara.common.redis.config.CacheConfig;
import org.dromara.common.redis.config.IdempotentConfig;
import org.dromara.common.redis.config.RateLimiterConfig;
import org.dromara.common.redis.handler.RedisExceptionHandler;
import org.dromara.common.redis.manager.PlusSpringCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("common-redis 配置与异常单元测试")
class RedisConfigurationTest {

    /**
     * 验证缓存配置创建可用的 Caffeine 实例和项目自定义缓存管理器。
     */
    @Test
    @DisplayName("创建缓存基础组件")
    void shouldCreateCacheInfrastructureBeans() {
        CacheConfig configuration = new CacheConfig();
        com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine = configuration.caffeine();
        CacheManager manager = configuration.cacheManager(caffeine);

        caffeine.put("key", "value");
        assertEquals("value", caffeine.getIfPresent("key"));
        assertInstanceOf(PlusSpringCacheManager.class, manager);
    }

    /**
     * 验证幂等和限流自动配置能够独立创建对应切面 Bean。
     */
    @Test
    @DisplayName("创建幂等与限流切面")
    void shouldCreateIdempotentAndRateLimiterAspects() {
        RepeatSubmitAspect repeatSubmitAspect = new IdempotentConfig().repeatSubmitAspect();
        RateLimiterAspect rateLimiterAspect = new RateLimiterConfig().rateLimiterAspect();

        assertNotNull(repeatSubmitAspect);
        assertNotNull(rateLimiterAspect);
    }

    /**
     * 验证分布式锁获取失败会转换为服务不可用业务响应。
     */
    @Test
    @DisplayName("转换分布式锁异常")
    void shouldConvertLockFailureToUnavailableResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders/submit");

        R<Void> response = new RedisExceptionHandler()
            .handleLockFailureException(mock(LockFailureException.class), request);

        assertEquals(HttpStatus.HTTP_UNAVAILABLE, response.getCode());
        assertEquals("业务处理中，请稍后再试...", response.getMsg());
    }
}

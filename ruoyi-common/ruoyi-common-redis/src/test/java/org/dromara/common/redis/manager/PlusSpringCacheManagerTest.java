package org.dromara.common.redis.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlusSpringCacheManager 单元测试")
class PlusSpringCacheManagerTest {

    /**
     * 验证扩展缓存名称能够解析 TTL、最大空闲时间、容量和本地缓存开关。
     */
    @Test
    @DisplayName("解析扩展缓存名称")
    void shouldResolveExtendedCacheNameOptions() {
        PlusSpringCacheManager manager = new PlusSpringCacheManager();
        CacheConfig template = new CacheConfig();
        manager.setConfig(Map.of("users", template));
        String cacheName = "users#5m#30s#200#0";
        String[] parts = cacheName.split("#");

        CacheConfig resolved = ReflectionTestUtils.invokeMethod(
            manager, "resolveCacheConfig", cacheName, "users", parts);
        Integer local = ReflectionTestUtils.invokeMethod(manager, "resolveLocal", (Object) parts);

        assertNotNull(resolved);
        assertEquals(300_000L, resolved.getTTL());
        assertEquals(30_000L, resolved.getMaxIdleTime());
        assertEquals(200, resolved.getMaxSize());
        assertEquals(0, local);
        assertEquals(0L, template.getTTL());
        assertTrue(manager.getCacheNames().contains(cacheName));
    }

    /**
     * 验证空配置会重置缓存配置集合，默认配置和本地缓存开关使用零值与启用状态。
     */
    @Test
    @DisplayName("创建默认缓存配置")
    void shouldCreateDefaultCacheConfiguration() {
        PlusSpringCacheManager manager = new PlusSpringCacheManager();
        manager.setConfig(null);

        CacheConfig config = ReflectionTestUtils.invokeMethod(manager, "createDefaultConfig");
        Integer local = ReflectionTestUtils.invokeMethod(manager, "resolveLocal", (Object) new String[]{"users"});

        assertNotNull(config);
        assertEquals(0L, config.getTTL());
        assertEquals(1, local);
        assertTrue(manager.getCacheNames().isEmpty());
    }
}

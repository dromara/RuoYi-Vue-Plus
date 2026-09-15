package org.dromara.common.redis.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CaffeineCacheDecorator 单元测试")
class CaffeineCacheDecoratorTest {

    /**
     * 验证首次读取回源到底层缓存，后续读取命中本地一级缓存，并在写入时失效旧值。
     */
    @Test
    @DisplayName("读取并刷新一级缓存")
    void shouldReadThroughAndInvalidateLocalCacheOnPut() {
        ConcurrentMapCache remote = new ConcurrentMapCache("remote");
        com.github.benmanes.caffeine.cache.Cache<Object, Object> local = Caffeine.newBuilder().build();
        CaffeineCacheDecorator decorator = new CaffeineCacheDecorator("users", remote, local);
        remote.put("1", "alice");

        assertEquals("alice", decorator.get("1", String.class));
        remote.put("1", "changed-remotely");
        assertEquals("alice", decorator.get("1", String.class));

        decorator.put("1", "bob");
        assertEquals("bob", decorator.get("1", String.class));
        assertEquals("users:1", decorator.getUniqueKey("1"));
        assertSame(remote.getNativeCache(), decorator.getNativeCache());
    }

    /**
     * 验证 Callable 加载、条件写入与删除会同步维护底层缓存和一级缓存。
     */
    @Test
    @DisplayName("维护缓存写入和删除一致性")
    void shouldKeepLocalAndRemoteCacheConsistent() {
        ConcurrentMapCache remote = new ConcurrentMapCache("remote");
        com.github.benmanes.caffeine.cache.Cache<Object, Object> local = Caffeine.newBuilder().build();
        CaffeineCacheDecorator decorator = new CaffeineCacheDecorator("users", remote, local);

        assertEquals("loaded", decorator.get("1", () -> "loaded"));
        Cache.ValueWrapper existing = decorator.putIfAbsent("1", "other");
        assertEquals("loaded", existing.get());
        assertTrue(decorator.evictIfPresent("1"));
        assertNull(decorator.get("1"));
        assertFalse(decorator.evictIfPresent("missing"));
    }

    /**
     * 验证清空操作只移除当前缓存命名空间的本地键，并支持整体失效。
     */
    @Test
    @DisplayName("按命名空间清理一级缓存")
    void shouldClearOnlyCurrentLocalNamespace() {
        ConcurrentMapCache remote = new ConcurrentMapCache("remote");
        com.github.benmanes.caffeine.cache.Cache<Object, Object> local = Caffeine.newBuilder().build();
        CaffeineCacheDecorator decorator = new CaffeineCacheDecorator("users", remote, local);
        local.put("users:1", "alice");
        local.put("roles:1", "admin");
        remote.put("1", "alice");

        decorator.clear();

        assertNull(local.getIfPresent("users:1"));
        assertEquals("admin", local.getIfPresent("roles:1"));
        assertNull(remote.get("1"));

        remote.put("2", "bob");
        local.put("users:2", "bob");
        assertTrue(decorator.invalidate());
        assertNull(local.getIfPresent("users:2"));
    }
}

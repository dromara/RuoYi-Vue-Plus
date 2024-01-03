package org.dromara.common.redis.manager;

import cn.hutool.core.lang.Console;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Cache 装饰器(用于扩展一级缓存)
 *
 * @author LionLi
 */
public class PlusCacheWrapper implements Cache {

    private static final com.github.benmanes.caffeine.cache.Cache<Object, Object> CAFFEINE = Caffeine.newBuilder()
        // 设置最后一次写入或访问后经过固定时间过期
        .expireAfterWrite(30, TimeUnit.SECONDS)
        // 初始的缓存空间大小
        .initialCapacity(100)
        // 缓存的最大条数
        .maximumSize(1000)
        .build();

    private final Cache cache;

    public PlusCacheWrapper(Cache cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public Object getNativeCache() {
        return cache.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        Object o = CAFFEINE.get(key, k -> cache.get(key));
        Console.log("redisson caffeine -> key: " + key + ",value:" + o);
        return (ValueWrapper) o;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object o = CAFFEINE.get(key, k -> cache.get(key, type));
        Console.log("redisson caffeine -> key: " + key + ",value:" + o);
        return (T) o;
    }

    @Override
    public void put(Object key, Object value) {
        cache.put(key, value);
        CAFFEINE.put(key, value);
    }

    public ValueWrapper putIfAbsent(Object key, Object value) {
        return cache.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        evictIfPresent(key);
    }

    public boolean evictIfPresent(Object key) {
        boolean b = cache.evictIfPresent(key);
        if (b) {
            CAFFEINE.invalidate(key);
        }
        return b;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    public boolean invalidate() {
        return cache.invalidate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object o = CAFFEINE.get(key, k -> cache.get(key, valueLoader));
        Console.log("redisson caffeine -> key: " + key + ",value:" + o);
        return (T) o;
    }

}

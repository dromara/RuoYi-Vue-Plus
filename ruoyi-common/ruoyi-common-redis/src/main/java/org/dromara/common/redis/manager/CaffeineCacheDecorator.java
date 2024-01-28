package org.dromara.common.redis.manager;

import cn.hutool.core.lang.Console;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache 装饰器模式(用于扩展 Caffeine 一级缓存)
 *
 * @author LionLi
 */
public class CaffeineCacheDecorator implements Cache {

    private static final com.github.benmanes.caffeine.cache.Cache<Object, Object>
        CAFFEINE = SpringUtils.getBean("caffeine");

    private final Cache cache;

    public CaffeineCacheDecorator(Cache cache) {
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

    public String getUniqueKey(Object key) {
        return cache.getName() + ":" + key;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object o = CAFFEINE.get(getUniqueKey(key), k -> cache.get(key));
        return (ValueWrapper) o;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object o = CAFFEINE.get(getUniqueKey(key), k -> cache.get(key, type));
        return (T) o;
    }

    @Override
    public void put(Object key, Object value) {
        cache.put(key, value);
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
            CAFFEINE.invalidate(getUniqueKey(key));
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
        Object o = CAFFEINE.get(getUniqueKey(key), k -> cache.get(key, valueLoader));
        return (T) o;
    }

}

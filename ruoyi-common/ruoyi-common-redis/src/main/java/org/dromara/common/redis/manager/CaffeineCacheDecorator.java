package org.dromara.common.redis.manager;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache 装饰器模式(用于扩展 Caffeine 一级缓存)
 *
 * @author LionLi
 */
public class CaffeineCacheDecorator implements Cache {

    private final String name;
    private final Cache cache;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine;

    /**
     * 创建带 Caffeine 一级缓存的缓存装饰器。
     *
     * @param name     缓存名称
     * @param cache    被装饰的缓存实例
     * @param caffeine 本地一级缓存实例
     */
    public CaffeineCacheDecorator(String name, Cache cache, com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine) {
        this.name = name;
        this.cache = cache;
        this.caffeine = caffeine;
    }

    /**
     * 获取缓存名称。
     *
     * @return 缓存名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取底层原生缓存对象。
     *
     * @return 原生缓存对象
     */
    @Override
    public Object getNativeCache() {
        return cache.getNativeCache();
    }

    /**
     * 构造 Caffeine 一级缓存使用的唯一键。
     *
     * @param key 缓存键
     * @return 唯一键
     */
    public String getUniqueKey(Object key) {
        return name + ":" + key;
    }

    /**
     * 获取缓存值。
     *
     * @param key 缓存键
     * @return 缓存值包装对象
     */
    @Override
    public ValueWrapper get(Object key) {
        Object o = caffeine.get(getUniqueKey(key), k -> cache.get(key));
        return (ValueWrapper) o;
    }

    /**
     * 按指定类型获取缓存值。
     *
     * @param key  缓存键
     * @param type 值类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Class<T> type) {
        Object o = caffeine.get(getUniqueKey(key), k -> cache.get(key, type));
        return (T) o;
    }

    /**
     * 写入缓存值，并清理本地一级缓存。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        caffeine.invalidate(getUniqueKey(key));
        cache.put(key, value);
    }

    /**
     * 当键不存在时写入缓存值。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 原缓存值包装对象
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        caffeine.invalidate(getUniqueKey(key));
        return cache.putIfAbsent(key, value);
    }

    /**
     * 删除缓存值。
     *
     * @param key 缓存键
     */
    @Override
    public void evict(Object key) {
        evictIfPresent(key);
    }

    /**
     * 删除缓存值并返回是否删除成功。
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    @Override
    public boolean evictIfPresent(Object key) {
        boolean b = cache.evictIfPresent(key);
        if (b) {
            caffeine.invalidate(getUniqueKey(key));
        }
        return b;
    }

    /**
     * 清空缓存。
     */
    @Override
    public void clear() {
        clearLocalCache();
        cache.clear();
    }

    /**
     * 使缓存整体失效。
     *
     * @return 是否失效成功
     */
    @Override
    public boolean invalidate() {
        boolean invalidated = cache.invalidate();
        if (invalidated) {
            clearLocalCache();
        }
        return invalidated;
    }

    /**
     * 获取缓存值，不存在时通过回调加载。
     *
     * @param key         缓存键
     * @param valueLoader 回调加载器
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object o = caffeine.get(getUniqueKey(key), k -> cache.get(key, valueLoader));
        return (T) o;
    }

    /**
     * 清理当前缓存命名空间下的本地一级缓存。
     */
    private void clearLocalCache() {
        String prefix = name + ":";
        caffeine.asMap().keySet().removeIf(key -> key instanceof String cacheKey && cacheKey.startsWith(prefix));
    }

}

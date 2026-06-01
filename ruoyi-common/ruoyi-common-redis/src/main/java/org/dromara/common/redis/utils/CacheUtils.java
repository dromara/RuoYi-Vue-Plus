package org.dromara.common.redis.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 缓存操作工具类
 *
 * @author Michelle.Chung
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked"})
public class CacheUtils {

    /**
     * 获取缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     * @return 缓存值
     */
    public static <T> T get(String cacheNames, Object key) {
        Cache.ValueWrapper wrapper = getRequiredCache(cacheNames).get(key);
        return wrapper != null ? (T) wrapper.get() : null;
    }

    /**
     * 保存缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     * @param value      缓存值
     */
    public static void put(String cacheNames, Object key, Object value) {
        getRequiredCache(cacheNames).put(key, value);
    }

    /**
     * 删除缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     */
    public static void evict(String cacheNames, Object key) {
        getRequiredCache(cacheNames).evict(key);
    }

    /**
     * 清空缓存值
     *
     * @param cacheNames 缓存组名称
     */
    public static void clear(String cacheNames) {
        getRequiredCache(cacheNames).clear();
    }

    /**
     * 获取必需存在的缓存实例。
     *
     * @param cacheNames 缓存组名称
     * @return 缓存实例
     */
    private static Cache getRequiredCache(String cacheNames) {
        Cache cache = CacheManagerHolder.CACHE_MANAGER.getCache(cacheNames);
        if (cache == null) {
            throw new IllegalArgumentException("Cache '" + cacheNames + "' does not exist.");
        }
        return cache;
    }

    /**
     * 延迟持有 Spring Cache 管理器。
     */
    private static class CacheManagerHolder {
        /**
         * Spring Cache 管理器。
         */
        private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);
    }

}

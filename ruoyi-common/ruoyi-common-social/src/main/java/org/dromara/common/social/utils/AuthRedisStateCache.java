package org.dromara.common.social.utils;

import lombok.AllArgsConstructor;
import me.zhyd.oauth.cache.AuthStateCache;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.social.config.properties.SocialProperties;

import java.time.Duration;

@AllArgsConstructor
public class AuthRedisStateCache implements AuthStateCache {

    private final SocialProperties socialProperties;

    /**
     * 存入缓存
     *
     * @param key   缓存key
     * @param value 缓存内容
     */
    @Override
    public void cache(String key, String value) {
        RedisUtils.setCacheObject(key, value, Duration.ofMillis(socialProperties.getTimeout()));
    }

    /**
     * 存入缓存
     *
     * @param key     缓存key
     * @param value   缓存内容
     * @param timeout 指定缓存过期时间(毫秒)
     */
    @Override
    public void cache(String key, String value, long timeout) {
        RedisUtils.setCacheObject(key, value, Duration.ofMillis(timeout));
    }

    /**
     * 获取缓存内容
     *
     * @param key 缓存key
     * @return 缓存内容
     */
    @Override
    public String get(String key) {
        return RedisUtils.getCacheObject(key);
    }

    /**
     * 是否存在key，如果对应key的value值已过期，也返回false
     *
     * @param key 缓存key
     * @return true：存在key，并且value没过期；false：key不存在或者已过期
     */
    @Override
    public boolean containsKey(String key) {
        return RedisUtils.hasKey(key);
    }
}

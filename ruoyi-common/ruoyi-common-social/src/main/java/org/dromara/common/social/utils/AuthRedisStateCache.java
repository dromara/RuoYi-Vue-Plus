package org.dromara.common.social.utils;

import jakarta.annotation.PostConstruct;
import me.zhyd.oauth.cache.AuthStateCache;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.social.config.properties.SocialProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

public class AuthRedisStateCache implements AuthStateCache {

    private final SocialProperties socialProperties;
    private final RedisTemplate<String, String> redisTemplate;

    private ValueOperations<String, String> valueOperations;

    @PostConstruct
    public void init() {
        valueOperations = redisTemplate.opsForValue();
    }


    public AuthRedisStateCache() {
        this.socialProperties = new SocialProperties();
        redisTemplate = new RedisTemplate<>();
    }

    /**
     * 存入缓存
     *
     * @param key   缓存key
     * @param value 缓存内容
     */
    @Override
    public void cache(String key, String value) {
        // TODO: 自定义存入缓存
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
        // TODO: 自定义存入缓存
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
        // TODO: 自定义获取缓存内容
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
        // TODO: 自定义判断key是否存在
        return RedisUtils.hasKey(key);
    }
}

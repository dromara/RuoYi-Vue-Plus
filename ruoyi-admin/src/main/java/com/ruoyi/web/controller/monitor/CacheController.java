package com.ruoyi.web.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.CacheUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.SysCache;
import lombok.RequiredArgsConstructor;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存监控
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/cache")
public class CacheController {

    private final RedissonConnectionFactory connectionFactory;

    private final static List<SysCache> CACHES = new ArrayList<>();

    static {
        CACHES.add(new SysCache(CacheConstants.ONLINE_TOKEN_KEY, "在线用户"));
        CACHES.add(new SysCache(CacheNames.SYS_CONFIG, "配置信息"));
        CACHES.add(new SysCache(CacheNames.SYS_DICT, "数据字典"));
        CACHES.add(new SysCache(CacheConstants.CAPTCHA_CODE_KEY, "验证码"));
        CACHES.add(new SysCache(CacheConstants.REPEAT_SUBMIT_KEY, "防重提交"));
        CACHES.add(new SysCache(CacheConstants.RATE_LIMIT_KEY, "限流处理"));
        CACHES.add(new SysCache(CacheNames.SYS_OSS_CONFIG, "OSS配置"));
        CACHES.add(new SysCache(CacheConstants.PWD_ERR_CNT_KEY, "密码错误次数"));
    }

    /**
     * 获取缓存监控列表
     */
    @SaCheckPermission("monitor:cache:list")
    @GetMapping()
    public R<Map<String, Object>> getInfo() throws Exception {
        RedisConnection connection = connectionFactory.getConnection();
        Properties info = connection.info();
        Properties commandStats = connection.info("commandstats");
        Long dbSize = connection.dbSize();

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        if (commandStats != null) {
            commandStats.stringPropertyNames().forEach(key -> {
                Map<String, String> data = new HashMap<>(2);
                String property = commandStats.getProperty(key);
                data.put("name", StringUtils.removeStart(key, "cmdstat_"));
                data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
                pieList.add(data);
            });
        }
        result.put("commandStats", pieList);
        return R.ok(result);
    }

    /**
     * 获取缓存监控缓存名列表
     */
    @SaCheckPermission("monitor:cache:list")
    @GetMapping("/getNames")
    public R<List<SysCache>> cache() {
        return R.ok(CACHES);
    }

    /**
     * 获取缓存监控Key列表
     *
     * @param cacheName 缓存名
     */
    @SaCheckPermission("monitor:cache:list")
    @GetMapping("/getKeys/{cacheName}")
    public R<Collection<String>> getCacheKeys(@PathVariable String cacheName) {
        Collection<String> cacheKeys = new HashSet<>(0);
        if (isCacheNames(cacheName)) {
            Set<Object> keys = CacheUtils.keys(cacheName);
            if (CollUtil.isNotEmpty(keys)) {
                cacheKeys = keys.stream().map(Object::toString).collect(Collectors.toList());
            }
        } else {
            cacheKeys = RedisUtils.keys(cacheName + "*");
        }
        return R.ok(cacheKeys);
    }

    /**
     * 获取缓存监控缓存值详情
     *
     * @param cacheName 缓存名
     * @param cacheKey  缓存key
     */
    @SaCheckPermission("monitor:cache:list")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public R<SysCache> getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey) {
        Object cacheValue;
        if (isCacheNames(cacheName)) {
            cacheValue = CacheUtils.get(cacheName, cacheKey);
        } else {
            cacheValue = RedisUtils.getCacheObject(cacheKey);
        }
        SysCache sysCache = new SysCache(cacheName, cacheKey, JsonUtils.toJsonString(cacheValue));
        return R.ok(sysCache);
    }

    /**
     * 清理缓存监控缓存名
     *
     * @param cacheName 缓存名
     */
    @SaCheckPermission("monitor:cache:list")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public R<Void> clearCacheName(@PathVariable String cacheName) {
        if (isCacheNames(cacheName)) {
            CacheUtils.clear(cacheName);
        } else {
            RedisUtils.deleteKeys(cacheName + "*");
        }
        return R.ok();
    }

    /**
     * 清理缓存监控Key
     *
     * @param cacheKey key名
     */
    @SaCheckPermission("monitor:cache:list")
    @DeleteMapping("/clearCacheKey/{cacheName}/{cacheKey}")
    public R<Void> clearCacheKey(@PathVariable String cacheName, @PathVariable String cacheKey) {
        if (isCacheNames(cacheName)) {
            CacheUtils.evict(cacheName, cacheKey);
        } else {
            RedisUtils.deleteObject(cacheKey);
        }
        return R.ok();
    }

    /**
     * 清理全部缓存监控
     */
    @SaCheckPermission("monitor:cache:list")
    @DeleteMapping("/clearCacheAll")
    public R<Void> clearCacheAll() {
        RedisUtils.deleteKeys("*");
        return R.ok();
    }

    private boolean isCacheNames(String cacheName) {
        return !StringUtils.contains(cacheName, ":");
    }
}

package org.dromara.common.redis.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * 发号器工具类
 *
 * @author 秋辞未寒
 * @date 2024-12-10
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SequenceUtils {

    /**
     * 默认初始值
     */
    public static final Long DEFAULT_INIT_VALUE = 1L;
    /**
     * 默认步长
     */
    public static final Long DEFAULT_STEP_VALUE = 1L;
    /**
     * 默认过期时间-天
     */
    public static final Duration DEFAULT_EXPIRE_TIME_DAY = Duration.ofDays(1);
    /**
     * 默认过期时间-分钟
     */
    public static final Duration DEFAULT_EXPIRE_TIME_MINUTE = Duration.ofMinutes(1);

    /**
     * 获取Redisson客户端实例
     */
    private static final RedissonClient REDISSON_CLIENT = SpringUtils.getBean(RedissonClient.class);

    /**
     * 获取ID生成器
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param initValue  ID初始值
     * @param stepValue  ID步长
     * @return ID生成器
     */
    private static RIdGenerator getIdGenerator(String key, Duration expireTime, Long initValue, Long stepValue) {
        if (initValue == null || initValue <= 0) {
            initValue = DEFAULT_INIT_VALUE;
        }
        if (stepValue == null || stepValue <= 0) {
            stepValue = DEFAULT_STEP_VALUE;
        }
        RIdGenerator idGenerator = REDISSON_CLIENT.getIdGenerator(key);
        // 设置初始值和步长
        idGenerator.tryInit(initValue, stepValue);
        // 设置过期时间
        idGenerator.expire(expireTime);
        return idGenerator;
    }

    /**
     * 获取指定业务key的唯一id
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param initValue  ID初始值
     * @param stepValue  ID步长
     * @return 唯一id
     */
    public static long nextId(String key, Duration expireTime, Long initValue, Long stepValue) {
        return getIdGenerator(key, expireTime, initValue, stepValue).nextId();
    }

    /**
     * 获取指定业务key的唯一id字符串
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param initValue  ID初始值
     * @param stepValue  ID步长
     * @return 唯一id
     */
    public static String nextIdStr(String key, Duration expireTime, Long initValue, Long stepValue) {
        return String.valueOf(nextId(key, expireTime, initValue, stepValue));
    }

    /**
     * 获取指定业务key的唯一id (ID初始值=1,ID步长=1)
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @return 唯一id
     */
    public static long nextId(String key, Duration expireTime) {
        return getIdGenerator(key, expireTime, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE).nextId();
    }

    /**
     * 获取指定业务key的唯一id字符串 (ID初始值=1,ID步长=1)
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @return 唯一id
     */
    public static String nextIdStr(String key, Duration expireTime) {
        return String.valueOf(nextId(key, expireTime));
    }

    /**
     * 获取 yyyyMMdd 开头的唯一id
     *
     * @return 唯一id
     */
    public static String nextIdDate() {
        return nextIdDate("");
    }

    /**
     * 获取 prefix + yyyyMMdd 开头的唯一id
     *
     * @param prefix 业务前缀
     * @return 唯一id
     */
    public static String nextIdDate(String prefix) {
        // 前缀+日期 构建 prefixKey
        String prefixKey = StringUtils.format("{}{}", StringUtils.blankToDefault(prefix, ""), DateUtil.format(DateUtil.date(), DatePattern.PURE_DATE_FORMATTER));
        // 获取下一个id
        long nextId = getIdGenerator(prefixKey, DEFAULT_EXPIRE_TIME_DAY, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE).nextId();
        // 返回完整id
        return StringUtils.format("{}{}", prefixKey, nextId);
    }

    /**
     * 获取 yyyyMMddHHmmss 开头的唯一id
     *
     * @return 唯一id
     */
    public static String nextIdDateTime() {
        return nextIdDateTime("");
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 开头的唯一id
     *
     * @param prefix 业务前缀
     * @return 唯一id
     */
    public static String nextIdDateTime(String prefix) {
        // 前缀+日期时间 构建 prefixKey
        String prefixKey = StringUtils.format("{}{}", StringUtils.blankToDefault(prefix, ""), DateUtil.format(DateUtil.date(), DatePattern.PURE_DATETIME_FORMATTER));
        // 获取下一个id
        long nextId = getIdGenerator(prefixKey, DEFAULT_EXPIRE_TIME_MINUTE, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE).nextId();
        // 返回完整id
        return StringUtils.format("{}{}", prefixKey, nextId);
    }

}

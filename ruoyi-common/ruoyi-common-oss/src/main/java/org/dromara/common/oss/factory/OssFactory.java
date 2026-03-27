package org.dromara.common.oss.factory;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.oss.client.DefaultOssClientImpl;
import org.dromara.common.oss.client.OssClient;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.oss.exception.S3StorageException;
import org.dromara.common.oss.properties.OssProperties;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * S3存储客户端工厂
 *
 * @author 秋辞未寒
 */
@Slf4j
public class OssFactory {

    private static final Map<String, OssClient> CLIENT_CACHE = new ConcurrentHashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 获取默认实例
     */
    public static OssClient instance() {
        // 获取redis 默认类型
        String configKey = RedisUtils.getCacheObject(OssConstant.DEFAULT_CONFIG_KEY);
        if (StringUtils.isEmpty(configKey)) {
            throw S3StorageException.form("文件存储服务类型无法找到!");
        }
        return instance(configKey);
    }

    /**
     * 根据类型获取实例
     */
    public static OssClient instance(String configKey) {
        String json = CacheUtils.get(CacheNames.SYS_OSS_CONFIG, configKey);
        if (json == null) {
            throw S3StorageException.form("系统异常, '" + configKey + "'配置信息不存在!");
        }
        OssProperties properties = JsonUtils.parseObject(json, OssProperties.class);
        OssClientConfig config = OssClientConfig.formProperties(properties);
        LOCK.lock();
        try {
            OssClient client = CLIENT_CACHE.get(configKey);
            if (client != null) {
                if (client.verifyConfig(config)) {
                    return client;
                }
                closeClient(configKey, client);
            }
            OssClient newClient = new DefaultOssClientImpl(configKey, config);
            CLIENT_CACHE.put(configKey, newClient);
            return newClient;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * 移除实例
     */
    public static boolean remove(String configKey) {
        OssClient client = CLIENT_CACHE.remove(configKey);
        if (client == null) {
            return false;
        }
        closeClient(configKey, client);
        return true;
    }

    private static void closeClient(String configKey, OssClient client) {
        try {
            client.close();
        } catch (Exception e) {
            log.warn("S3存储客户端 [{}] 关闭异常，错误信息: {}", configKey, e.getMessage(), e);
        }
    }

}

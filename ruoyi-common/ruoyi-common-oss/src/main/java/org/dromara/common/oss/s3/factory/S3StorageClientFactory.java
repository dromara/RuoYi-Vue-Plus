package org.dromara.common.oss.s3.factory;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.oss.s3.builder.DefaultS3StorageClientBuilder;
import org.dromara.common.oss.s3.client.S3StorageClient;
import org.dromara.common.oss.s3.config.S3StorageClientConfig;
import org.dromara.common.oss.s3.exception.S3StorageException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3存储客户端工厂
 *
 * @author 秋辞未寒
 */
@Slf4j
public class S3StorageClientFactory {

    private static final Map<String, S3StorageClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取默认实例
     */
    public static S3StorageClient instance() {
        // 获取redis 默认类型
        String configKey = RedisUtils.getCacheObject(OssConstant.DEFAULT_CONFIG_KEY);
        if (StringUtils.isEmpty(configKey)) {
            throw S3StorageException.of("文件存储服务类型无法找到!");
        }
        return instance(configKey);
    }

    /**
     * 根据类型获取实例
     */
    public static S3StorageClient instance(String configKey) {
        // 使用租户标识避免多个租户相同key实例覆盖
        return CLIENT_CACHE.computeIfAbsent(configKey, S3StorageClientFactory::instanceCache);
    }

    /**
     * 使用缓存实例化
     */
    private static S3StorageClient instanceCache(String configKey) {
        String json = CacheUtils.get(CacheNames.SYS_OSS_CONFIG, configKey);
        if (json == null) {
            throw S3StorageException.of("系统异常, '" + configKey + "'配置信息不存在!");
        }
        S3StorageClientConfig config = JsonUtils.parseObject(json, S3StorageClientConfig.class);
        return DefaultS3StorageClientBuilder.INSTANCE.build(config);
    }

    /**
     * 移除实例
     */
    public static boolean remove(String configKey) {
        S3StorageClient client = CLIENT_CACHE.remove(configKey);
        if (client == null) {
            return false;
        }
        try {
            client.close();
        } catch (Exception e) {
            log.warn("S3存储客户端关闭异常，错误信息: {}",e.getMessage(),e);
        }
        return true;
    }

}

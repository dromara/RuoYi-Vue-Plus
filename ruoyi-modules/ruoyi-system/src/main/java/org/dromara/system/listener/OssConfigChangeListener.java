package org.dromara.system.listener;

import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.event.OssConfigChangeEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OSS 配置变更监听器。
 *
 * @author Lion Li
 */
@Component
public class OssConfigChangeListener {

    /**
     * 数据提交后同步刷新 OSS 配置缓存与客户端实例。
     *
     * @param event OSS 配置变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void refreshOssConfig(OssConfigChangeEvent event) {
        if (event.defaultConfig()) {
            RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, event.configKey());
            return;
        }
        if (StringUtils.isNotBlank(event.oldConfigKey()) && !StringUtils.equals(event.oldConfigKey(), event.configKey())) {
            CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, event.oldConfigKey());
            OssFactory.remove(event.oldConfigKey());
        }
        if (StringUtils.isBlank(event.configKey())) {
            return;
        }
        if (StringUtils.isBlank(event.configJson())) {
            CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, event.configKey());
        } else {
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, event.configKey(), event.configJson());
        }
        OssFactory.remove(event.configKey());
    }

}

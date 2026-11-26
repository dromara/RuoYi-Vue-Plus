package org.dromara.common.sms;

import org.dromara.common.core.constant.GlobalConstants;
import cn.hutool.extra.spring.SpringUtil;
import java.time.Duration;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.sms.core.dao.PlusSmsDao;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class PlusSmsDaoTest {

    @Test
    void retryEntriesUseSharedNamespaceAndExplicitTtlInSeconds() {
        var dao = new PlusSmsDao();
        String key = GlobalConstants.GLOBAL_REDIS_KEY + "sms:retry";
        try (var spring = mockStatic(SpringUtil.class)) {
            RedissonClient client = mock();
            spring.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(client);
            try (var cache = mockStatic(RedisUtils.class)) {
                cache.when(() -> RedisUtils.getCacheObject(key)).thenReturn("code");
                dao.set("sms:retry", "code", 45);
                dao.set("sms:retry", "updated");
                assertEquals("code", dao.get("sms:retry"));
                dao.clean();
                cache.verify(() -> RedisUtils.setCacheObject(key, "code", Duration.ofSeconds(45)));
                cache.verify(() -> RedisUtils.setCacheObject(key, "updated", true));
                cache.verify(() -> RedisUtils.deleteKeys(GlobalConstants.GLOBAL_REDIS_KEY + "sms:*"));
            }
        }
    }

    @Test
    void removeShouldReturnCachedValueOnlyOnce() {
        RedissonClient redis = mock(RedissonClient.class);
        RBucket<Object> bucket = mock();
        when(redis.getBucket(GlobalConstants.GLOBAL_REDIS_KEY + "sms:retry")).thenReturn(bucket);
        when(bucket.getAndDelete()).thenReturn("retry-id", (Object) null);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(redis);
            try (var cache = mockStatic(RedisUtils.class)) {
                cache.when(RedisUtils::getClient).thenReturn(redis);
                var dao = new PlusSmsDao();
                assertEquals("retry-id", dao.remove("sms:retry"));
                assertNull(dao.remove("sms:retry"));
            }
        }
    }
}

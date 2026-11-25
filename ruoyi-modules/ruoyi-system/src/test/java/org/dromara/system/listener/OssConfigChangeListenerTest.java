package org.dromara.system.listener;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.event.OssConfigChangeEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OssConfigChangeListenerTest {

    @ParameterizedTest
    @CsvSource({"save,archive","blank,'  '","default,archive"})
    void refreshesOnlyTheRequestedConfig(String action, String key) {
        var listener = new OssConfigChangeListener();
        try (var beans = mockStatic(SpringUtil.class); var cache = mockStatic(CacheUtils.class);
             var factory = mockStatic(OssFactory.class); var redis = mockStatic(RedisUtils.class)) {
            if (action.equals("default")) {
                listener.refreshOssConfig(OssConfigChangeEvent.useDefault(key));
                redis.verify(() -> RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, key));
                cache.verifyNoInteractions();
            } else {
                listener.refreshOssConfig(OssConfigChangeEvent.save(key, null, "{}"));
                if (action.equals("blank")) {
                    cache.verifyNoInteractions();
                    factory.verifyNoInteractions();
                } else {
                    cache.verify(() -> CacheUtils.put(org.dromara.common.core.constant.CacheNames.SYS_OSS_CONFIG, key, "{}"));
                    factory.verify(() -> OssFactory.remove(key));
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"old, new, new", "other, new, other", "old, , ", "other, , other"})
    void defaultKeyTracksChangesToReferencedConfig(String current, String renamed, String expected) {
        var pointer = new AtomicReference<>(current);
        var client = mock(RedissonClient.class);
        RBucket<String> bucket = mock();
        when(client.<String>getBucket(OssConstant.DEFAULT_CONFIG_KEY)).thenReturn(bucket);
        when(bucket.compareAndSet(anyString(), nullable(String.class))).thenAnswer(call ->
            java.util.Objects.equals(pointer.get(), call.getArgument(0)) && pointer.compareAndSet(pointer.get(), call.getArgument(1)));
        try (var spring = mockStatic(SpringUtil.class); var redis = mockStatic(RedisUtils.class);
             var cache = mockStatic(CacheUtils.class); var factory = mockStatic(OssFactory.class)) {
            redis.when(RedisUtils::getClient).thenReturn(client);
            var event = renamed == null ? OssConfigChangeEvent.remove("old") : OssConfigChangeEvent.save(renamed, "old", "{}");
            new OssConfigChangeListener().refreshOssConfig(event);
            assertEquals(expected, pointer.get());
        }
    }
}

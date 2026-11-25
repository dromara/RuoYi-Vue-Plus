package org.dromara.common.sms;

import org.dromara.common.core.constant.GlobalConstants;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.sms.core.dao.PlusSmsDao;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class PlusSmsDaoTest {

    @Test
    void removeShouldReturnCachedValueOnlyOnce() {
        RedissonClient redis = mock(RedissonClient.class);
        RBucket<Object> bucket = mock();
        when(redis.getBucket(GlobalConstants.GLOBAL_REDIS_KEY + "sms:retry")).thenReturn(bucket);
        when(bucket.getAndDelete()).thenReturn("retry-id", (Object) null);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(redis);
            var dao = new PlusSmsDao();
            assertEquals("retry-id", dao.remove("sms:retry"));
            assertNull(dao.remove("sms:retry"));
        }
    }
}

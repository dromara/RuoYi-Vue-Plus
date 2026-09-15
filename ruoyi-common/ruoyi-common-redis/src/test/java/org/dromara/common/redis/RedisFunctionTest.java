package org.dromara.common.redis;

import org.dromara.common.redis.handler.KeyPrefixHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("common-redis 功能单元测试")
class RedisFunctionTest {

    /**
     * 验证 Redis Key 在写入时增加前缀、读取时移除前缀，且不会重复添加。
     */
    @Test
    @DisplayName("映射和还原 Redis Key 前缀")
    void shouldMapAndUnmapKeyPrefix() {
        KeyPrefixHandler handler = new KeyPrefixHandler("app");

        assertEquals("app:user:1", handler.map("user:1"));
        assertEquals("app:user:1", handler.map("app:user:1"));
        assertEquals("user:1", handler.unmap("app:user:1"));
        assertEquals("other:user:1", handler.unmap("other:user:1"));
    }

    /**
     * 验证空前缀不会改变有效 Key，空白 Key 按无效输入返回空值。
     */
    @Test
    @DisplayName("处理空 Redis Key 前缀")
    void shouldHandleBlankKeyPrefixAndName() {
        KeyPrefixHandler handler = new KeyPrefixHandler(" ");

        assertEquals("user:1", handler.map("user:1"));
        assertEquals("user:1", handler.unmap("user:1"));
        assertNull(handler.map(" "));
        assertNull(handler.unmap(null));
    }
}

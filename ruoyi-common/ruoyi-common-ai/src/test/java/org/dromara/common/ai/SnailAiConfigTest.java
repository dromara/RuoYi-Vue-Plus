package org.dromara.common.ai;

import org.dromara.common.ai.config.SnailAiConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("common-ai 功能单元测试")
class SnailAiConfigTest {

    /**
     * 验证 AI 自动配置只在显式开启 snail-ai.enabled 时生效，避免默认启动外部 AI 客户端。
     */
    @Test
    @DisplayName("声明 Snail AI 启用条件")
    void shouldDeclareSnailAiEnablementCondition() {
        ConditionalOnProperty condition = SnailAiConfig.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(condition);
        assertEquals("snail-ai", condition.prefix());
        assertArrayEquals(new String[]{"enabled"}, condition.name());
        assertEquals("true", condition.havingValue());
    }
}

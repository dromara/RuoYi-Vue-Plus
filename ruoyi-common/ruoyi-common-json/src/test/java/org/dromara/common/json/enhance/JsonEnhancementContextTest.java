package org.dromara.common.json.enhance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonEnhancementContext 单元测试")
class JsonEnhancementContextTest {

    /**
     * 验证上下文属性按需创建，并可读取、复用和移除。
     */
    @Test
    @DisplayName("上下文属性支持创建、读取和移除")
    void shouldManageAttributes() {
        JsonEnhancementContext context = new JsonEnhancementContext(null);
        List<String> values = context.getOrCreateAttribute("values", ArrayList::new);
        values.add("first");

        List<String> sameValues = context.getOrCreateAttribute("values", ArrayList::new);

        assertSame(values, sameValues);
        assertEquals(List.of("first"), context.<List<String>>getAttribute("values"));
        assertTrue(context.containsAttribute("values"));
        context.removeAttribute("values");
        assertFalse(context.containsAttribute("values"));
    }

    /**
     * 验证响应增强处理标记能够被正确设置。
     */
    @Test
    @DisplayName("可以标记响应需要增强处理")
    void shouldMarkProcessingRequired() {
        JsonEnhancementContext context = new JsonEnhancementContext(null);

        context.markProcessingRequired();

        assertTrue(context.isProcessingRequired());
    }

}

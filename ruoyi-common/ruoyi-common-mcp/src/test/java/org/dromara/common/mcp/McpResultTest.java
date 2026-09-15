package org.dromara.common.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.dromara.common.mcp.core.McpResourceReadResult;
import org.dromara.common.mcp.core.McpToolCallResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("common-mcp 功能单元测试")
class McpResultTest {

    /**
     * 验证 MCP 工具调用结果会保留服务端、内容、结构化数据和错误标记。
     */
    @Test
    @DisplayName("转换 MCP 工具调用结果")
    void shouldConvertMcpToolCallResult() {
        McpSchema.CallToolResult sdkResult = mock(McpSchema.CallToolResult.class);
        McpSchema.Content content = mock(McpSchema.Content.class);
        when(sdkResult.isError()).thenReturn(true);
        when(sdkResult.content()).thenReturn(List.of(content));
        when(sdkResult.structuredContent()).thenReturn(Map.of("id", 1));

        McpToolCallResult result = McpToolCallResult.of("server-a", sdkResult);

        assertEquals("server-a", result.serverName());
        assertTrue(result.error());
        assertEquals(List.of(content), result.content());
        assertEquals(Map.of("id", 1), result.structuredContent());
    }

    /**
     * 验证 MCP SDK 返回空错误标记时按成功处理，兼容未显式设置 isError 的服务端。
     */
    @Test
    @DisplayName("兼容空 MCP 错误标记")
    void shouldTreatNullMcpErrorFlagAsSuccess() {
        McpSchema.CallToolResult sdkResult = mock(McpSchema.CallToolResult.class);

        assertFalse(McpToolCallResult.of("server-a", sdkResult).error());
    }

    /**
     * 验证 MCP 资源读取结果会附加来源服务端并保留资源内容列表。
     */
    @Test
    @DisplayName("转换 MCP 资源读取结果")
    void shouldConvertMcpResourceReadResult() {
        McpSchema.ReadResourceResult sdkResult = mock(McpSchema.ReadResourceResult.class);
        McpSchema.ResourceContents content = mock(McpSchema.ResourceContents.class);
        when(sdkResult.contents()).thenReturn(List.of(content));

        McpResourceReadResult result = McpResourceReadResult.of("server-b", sdkResult);

        assertEquals("server-b", result.serverName());
        assertEquals(List.of(content), result.contents());
    }
}

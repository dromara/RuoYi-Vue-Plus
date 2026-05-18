package org.dromara.common.mcp.core;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * MCP 工具调用结果。
 *
 * @author Lion Li
 */
public record McpToolCallResult(
    String serverName,
    boolean error,
    List<McpSchema.Content> content,
    Object structuredContent
) {

    public static McpToolCallResult of(String serverName, McpSchema.CallToolResult result) {
        return new McpToolCallResult(
            serverName,
            Boolean.TRUE.equals(result.isError()),
            result.content(),
            result.structuredContent()
        );
    }
}

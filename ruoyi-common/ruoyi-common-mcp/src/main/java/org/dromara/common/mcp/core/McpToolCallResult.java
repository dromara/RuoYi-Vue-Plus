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

    /**
     * 将 MCP SDK 工具调用结果转换为带服务端名称的项目结果对象。
     *
     * @param serverName MCP 服务端名称
     * @param result MCP SDK 工具调用结果
     * @return MCP 工具调用结果
     */
    public static McpToolCallResult of(String serverName, McpSchema.CallToolResult result) {
        return new McpToolCallResult(
            serverName,
            Boolean.TRUE.equals(result.isError()),
            result.content(),
            result.structuredContent()
        );
    }
}

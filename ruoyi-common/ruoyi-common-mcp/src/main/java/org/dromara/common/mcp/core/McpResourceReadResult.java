package org.dromara.common.mcp.core;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * MCP 资源读取结果。
 *
 * @author Lion Li
 */
public record McpResourceReadResult(
    String serverName,
    List<McpSchema.ResourceContents> contents
) {

    public static McpResourceReadResult of(String serverName, McpSchema.ReadResourceResult result) {
        return new McpResourceReadResult(serverName, result.contents());
    }
}

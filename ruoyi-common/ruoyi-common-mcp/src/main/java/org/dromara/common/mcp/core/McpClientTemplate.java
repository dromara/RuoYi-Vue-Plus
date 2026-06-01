package org.dromara.common.mcp.core;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP Client 通用操作模板。
 * <p>
 * Spring AI 已经负责 MCP Client 的创建、初始化与连接管理，本模板只做项目内常用调用封装，
 * 避免业务模块直接遍历 `McpSyncClient` 或重复处理返回结构。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
public class McpClientTemplate {

    private final List<McpSyncClient> mcpSyncClients;

    /**
     * 查询所有已连接 MCP Server 的工具列表。
     *
     * @return Server 名称与工具名称列表
     */
    public Map<String, List<String>> listTools() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (McpSyncClient client : mcpSyncClients) {
            List<String> tools = client.listTools().tools().stream()
                .map(McpSchema.Tool::name)
                .toList();
            result.put(getServerName(client), tools);
        }
        return result;
    }

    /**
     * 调用所有 MCP Server 上的同名工具。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 各 Server 的工具调用结果
     */
    public Map<String, McpToolCallResult> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, McpToolCallResult> result = new LinkedHashMap<>();
        for (McpSyncClient client : mcpSyncClients) {
            McpSchema.CallToolResult callResult = client.callTool(new McpSchema.CallToolRequest(toolName, arguments));
            result.put(getServerName(client), McpToolCallResult.of(getServerName(client), callResult));
        }
        return result;
    }

    /**
     * 调用指定 MCP Server 上的工具。
     *
     * @param serverName Server 名称
     * @param toolName   工具名称
     * @param arguments  工具参数
     * @return 工具调用结果
     */
    public Optional<McpToolCallResult> callTool(String serverName, String toolName, Map<String, Object> arguments) {
        return findClient(serverName)
            .map(client -> McpToolCallResult.of(serverName, client.callTool(new McpSchema.CallToolRequest(toolName, arguments))));
    }

    /**
     * 读取所有 MCP Server 上的同名资源。
     *
     * @param uri 资源地址
     * @return 各 Server 的资源内容
     */
    public Map<String, McpResourceReadResult> readResource(String uri) {
        Map<String, McpResourceReadResult> result = new LinkedHashMap<>();
        for (McpSyncClient client : mcpSyncClients) {
            McpSchema.ReadResourceResult readResult = client.readResource(new McpSchema.ReadResourceRequest(uri));
            result.put(getServerName(client), McpResourceReadResult.of(getServerName(client), readResult));
        }
        return result;
    }

    /**
     * 读取指定 MCP Server 上的资源。
     *
     * @param serverName Server 名称
     * @param uri        资源地址
     * @return 资源内容
     */
    public Optional<McpResourceReadResult> readResource(String serverName, String uri) {
        return findClient(serverName)
            .map(client -> McpResourceReadResult.of(serverName, client.readResource(new McpSchema.ReadResourceRequest(uri))));
    }

    /**
     * 按 Server 名称查找 MCP Client。
     *
     * @param serverName Server 名称
     * @return MCP Client
     */
    public Optional<McpSyncClient> findClient(String serverName) {
        return mcpSyncClients.stream()
            .filter(client -> serverName.equals(getServerName(client)))
            .findFirst();
    }

    /**
     * 查询已连接的 MCP Client。
     *
     * @return MCP Client 列表
     */
    public List<McpSyncClient> getClients() {
        return mcpSyncClients;
    }

    /**
     * 解析 MCP Server 名称。
     *
     * @param client MCP 同步客户端
     * @return Server 名称
     */
    private String getServerName(McpSyncClient client) {
        McpSchema.Implementation serverInfo = client.getServerInfo();
        if (serverInfo == null || serverInfo.name() == null) {
            return "unknown";
        }
        return serverInfo.name();
    }
}

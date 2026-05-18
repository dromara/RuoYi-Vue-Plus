package org.dromara.demo.mcp;

import org.dromara.common.mcp.core.McpClientTemplate;
import org.dromara.common.mcp.core.McpResourceReadResult;
import org.dromara.common.mcp.core.McpToolCallResult;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 演示服务，从外部 MCP Server 接收数据后交给业务层处理。
 * <p>
 * 本服务会注入 Spring AI 自动创建的多个 `McpSyncClient`。当
 * `spring.ai.mcp.client.enabled=true` 且配置了多个 connection 时，
 * 这里可以统一遍历这些 MCP Server。
 * <p>
 * 如果要用当前项目自己连自己测试，建议启动两个 `ruoyi-admin` 实例：
 * 一个作为 server，一个作为 client，client 指向 server 的地址。
 *
 * @author Lion Li
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(McpClientTemplate.class)
public class McpDemoClientService {

    private final McpClientTemplate mcpClientTemplate;

    /**
     * 查询已接入的 MCP Server 与工具列表。
     *
     * @return Server 名称与工具名称列表
     */
    public Map<String, List<String>> listRemoteTools() {
        return mcpClientTemplate.listTools();
    }

    /**
     * 调用外部 MCP 工具接收数据。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 工具返回内容
     */
    public Map<String, McpToolCallResult> callRemoteTool(String toolName, Map<String, Object> arguments) {
        return mcpClientTemplate.callTool(toolName, arguments);
    }

    /**
     * 读取外部 MCP 资源。
     *
     * @param uri 资源地址
     * @return 资源内容
     */
    public Map<String, McpResourceReadResult> readRemoteResource(String uri) {
        return mcpClientTemplate.readResource(uri);
    }

    /**
     * 模拟业务处理入口。真实业务可在这里做校验、转换、去重、入库和审计。
     * <p>
     * MCP 返回数据不建议直接入库，应先转换成业务 BO/DTO，再进入业务 Service。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 处理结果
     */
    public McpDemoHandleResult receiveAndHandle(String toolName, Map<String, Object> arguments) {
        return new McpDemoHandleResult("MCP", true, callRemoteTool(toolName, arguments));
    }

    /**
     * MCP 数据处理结果。
     *
     * @param sourceType 数据来源类型
     * @param handled 是否已处理
     * @param data MCP 原始返回数据
     */
    public record McpDemoHandleResult(
        String sourceType,
        boolean handled,
        Map<String, McpToolCallResult> data
    ) {
    }
}

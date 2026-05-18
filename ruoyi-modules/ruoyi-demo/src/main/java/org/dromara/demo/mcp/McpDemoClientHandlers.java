package org.dromara.demo.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.ai.mcp.annotation.McpSampling;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MCP Client 能力回调演示。
 * <p>
 * `@McpSampling` 与 `@McpElicitation` 不是对外提供给 MCP Client 调用的工具，
 * 而是 MCP Server 在处理请求时，反向请求 MCP Client 进行模型采样或补充信息时的回调处理器。
 * <p>
 * 这里提供最小示例，主要用于展示注解写法，并让 Spring AI MCP 注解扫描器能扫描到
 * sampling/elicitation 方法，避免启动时输出 "No sampling methods found" 与
 * "No elicitation methods found" 提示。
 * <p>
 * Spring AI 2.0.0-M6 要求 `clients` 不能为空。这里使用 `ruoyi-demo` 作为示例客户端名称，
 * 实际使用时应与 `spring.ai.mcp.client.*.connections` 中的连接名保持一致。
 *
 * @author Lion Li
 */
@Component
public class McpDemoClientHandlers {

    /**
     * MCP sampling 回调示例。
     *
     * @param request 采样请求
     * @return 采样结果
     */
    @McpSampling(clients = "ruoyi-demo")
    public McpSchema.CreateMessageResult sampling(McpSchema.CreateMessageRequest request) {
        String text = request.systemPrompt() == null ? "demo sampling response" : request.systemPrompt();
        return new McpSchema.CreateMessageResult(
            McpSchema.Role.ASSISTANT,
            new McpSchema.TextContent(text),
            "ruoyi-demo-sampling",
            McpSchema.CreateMessageResult.StopReason.END_TURN
        );
    }

    /**
     * MCP elicitation 回调示例。
     *
     * @param request 补充信息请求
     * @return 补充信息结果
     */
    @McpElicitation(clients = "ruoyi-demo")
    public McpSchema.ElicitResult elicitation(McpSchema.ElicitRequest request) {
        return new McpSchema.ElicitResult(
            McpSchema.ElicitResult.Action.ACCEPT,
            Map.of("message", request.message())
        );
    }
}

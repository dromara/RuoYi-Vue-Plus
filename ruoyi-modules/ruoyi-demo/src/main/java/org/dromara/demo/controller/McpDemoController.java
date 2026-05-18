package org.dromara.demo.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mcp.core.McpResourceReadResult;
import org.dromara.demo.mcp.McpDemoClientService;
import org.dromara.demo.mcp.McpDemoClientService.McpDemoHandleResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 演示案例。
 * <p>
 * 这些接口不是 MCP 协议接口，只是为了在后台系统中手动触发 MCP Client 调用。
 * 真实 MCP Server 入口由 Spring AI 挂载到 `/mcp`。
 * <p>
 * 调用示例：
 * <ul>
 *     <li>`GET /demo/mcp/tools`：查看已连接 MCP Server 的工具列表</li>
 *     <li>`GET /demo/mcp/receive?toolName=demo_get_data&amp;id=1`：调用外部工具并模拟业务处理</li>
 *     <li>`GET /demo/mcp/resource?uri=demo://summary`：读取外部 MCP 资源</li>
 * </ul>
 * 当 `spring.ai.mcp.client.enabled=false` 时，接口会返回提示，不影响应用启动。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mcp")
public class McpDemoController {

    private final McpDemoClientService mcpDemoClientService;

    /**
     * 查询外部 MCP Server 工具列表。
     */
    @GetMapping("/tools")
    public R<Map<String, List<String>>> tools() {
        try {
            return R.ok(mcpDemoClientService.listRemoteTools());
        } catch (IllegalStateException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 调用外部 MCP 工具并模拟业务处理。
     */
    @GetMapping("/receive")
    public R<McpDemoHandleResult> receive(@RequestParam(defaultValue = "demo_get_data") String toolName,
                                          @RequestParam(defaultValue = "1") String id) {
        try {
            return R.ok(mcpDemoClientService.receiveAndHandle(toolName, Map.of("id", id)));
        } catch (IllegalStateException e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 读取外部 MCP 资源。
     */
    @GetMapping("/resource")
    public R<Map<String, McpResourceReadResult>> resource(@RequestParam(defaultValue = "demo://summary") String uri) {
        try {
            return R.ok(mcpDemoClientService.readRemoteResource(uri));
        } catch (IllegalStateException e) {
            return R.fail(e.getMessage());
        }
    }
}

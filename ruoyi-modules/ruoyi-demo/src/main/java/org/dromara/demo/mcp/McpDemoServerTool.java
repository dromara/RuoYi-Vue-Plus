package org.dromara.demo.mcp;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MCP Server 演示工具，对外提供业务数据。
 * <p>
 * 启动 `ruoyi-admin` 后，本类中的 `@McpTool` 与 `@McpResource` 会被 Spring AI
 * MCP 注解扫描器注册到当前应用的 MCP Server，默认通过同端口 `/mcp` 暴露。
 * <p>
 * 可被外部 MCP Client 调用的示例能力：
 * <ul>
 *     <li>`demo_get_data`：根据 id 查询一条演示数据</li>
 *     <li>`demo_list_data`：按 limit 查询演示数据列表</li>
 *     <li>`demo://summary`：读取演示模块摘要资源</li>
 * </ul>
 * 真实业务中不要在 MCP 工具里直接访问 Mapper，应继续调用业务 Service，
 * 并保留权限、租户、脱敏、审计和幂等规则。
 *
 * @author Lion Li
 */
@Component
public class McpDemoServerTool {

    /**
     * 查询一条演示数据。
     *
     * @param id 业务主键
     * @return 演示数据
     */
    @McpTool(name = "demo_get_data", description = "根据业务主键查询演示数据")
    public McpDemoData getData(@McpToolParam(description = "业务主键") String id) {
        return new McpDemoData(id, "演示数据-" + id, "ruoyi-demo", 100, LocalDateTime.now());
    }

    /**
     * 查询演示数据列表。
     *
     * @param limit 返回条数
     * @return 演示数据列表
     */
    @McpTool(name = "demo_list_data", description = "查询演示数据列表")
    public List<McpDemoData> listData(@McpToolParam(description = "返回条数") Integer limit) {
        int size = limit == null || limit < 1 ? 3 : Math.min(limit, 20);
        return java.util.stream.IntStream.rangeClosed(1, size)
            .mapToObj(index -> new McpDemoData(String.valueOf(index), "演示数据-" + index, "ruoyi-demo", index * 10, LocalDateTime.now()))
            .toList();
    }

    /**
     * 读取演示资源。
     *
     * @return 演示资源内容
     */
    @McpResource(
        name = "demo_summary",
        uri = "demo://summary",
        description = "演示模块摘要资源",
        mimeType = "text/plain"
    )
    public String summary() {
        return "ruoyi-demo MCP resource summary";
    }

    /**
     * MCP 演示数据。
     *
     * @author Lion Li
     */
    public record McpDemoData(
        String id,
        String name,
        String source,
        Integer amount,
        LocalDateTime syncTime
    ) {
    }

}

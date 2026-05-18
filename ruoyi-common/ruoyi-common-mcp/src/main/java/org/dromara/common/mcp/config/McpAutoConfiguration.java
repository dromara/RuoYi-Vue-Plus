package org.dromara.common.mcp.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.dromara.common.mcp.core.McpClientTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * MCP 公共模块自动配置。
 *
 * @author Lion Li
 */
@AutoConfiguration
public class McpAutoConfiguration {

    /**
     * MCP Client 通用操作模板。
     */
    @Bean
    @ConditionalOnBean(McpSyncClient.class)
    @ConditionalOnMissingBean
    public McpClientTemplate mcpClientTemplate(List<McpSyncClient> mcpSyncClients) {
        return new McpClientTemplate(mcpSyncClients);
    }
}

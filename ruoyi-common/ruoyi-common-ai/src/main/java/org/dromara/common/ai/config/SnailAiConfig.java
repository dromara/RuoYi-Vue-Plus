package org.dromara.common.ai.config;

import com.aizuda.snail.ai.agent.starter.EnableSnailAiAgent;
import com.aizuda.snail.ai.openapi.client.starter.EnableSnailAiOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Snail AI 自动配置
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "snail-ai", name = "enabled", havingValue = "true")
@EnableSnailAiAgent
@EnableSnailAiOpenApi
public class SnailAiConfig {
}

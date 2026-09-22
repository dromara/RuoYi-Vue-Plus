package org.dromara.de.conversation.config;

import lombok.RequiredArgsConstructor;
import org.dromara.de.conversation.security.LlmIngestAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * LLM-ingest 鉴权拦截器注册（占位实现）
 *
 * ★ 2026-09-22 落地（owner 解封 R1 后必做）：
 * - 在 application.yml 加 de-conversation.llm-service.token
 * - 本类在 Spring 容器初始化时自动注册拦截器
 *
 * ★ 拦截范围：
 * - /de/conversation/{id}/llm-message 路径（单段通配，Fix #3 路径精确匹配）
 * - caller 端 /de/conversation/{id} 不拦截（Sa-Token 处理）
 *
 * @author AIP
 * @since 2026-09-22
 */
@Configuration
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "de-conversation.llm-service", name = "token")
public class LlmIngestAuthConfig implements WebMvcConfigurer {

    private final LlmIngestAuthInterceptor llmIngestAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ★ Fix #3 (MEDIUM): 路径精确匹配 — 单段通配 = 与 Controller 路径一一对应
        // 未来如加多段路径（/v2/.../llm-message）需显式注册
        registry.addInterceptor(llmIngestAuthInterceptor)
                .addPathPatterns("/de/conversation/*/llm-message");
    }
}
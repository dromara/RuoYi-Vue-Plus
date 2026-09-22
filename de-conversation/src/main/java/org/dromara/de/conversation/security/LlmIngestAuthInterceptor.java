package org.dromara.de.conversation.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * LLM-ingest 端点鉴权拦截器（fail-closed）
 *
 * ★ 2026-09-22 落地 + 第二次安全审查修补（fail-open → fail-closed）：
 * - 拦截器仅在 de-conversation.llm-service.token 配置存在时创建（@ConditionalOnProperty）
 * - 配置存在但空白 → @PostConstruct 抛 IllegalStateException（启动失败而非静默放行）
 * - 配置存在且非空 → 校验 X-LLM-Service-Token header（常量时间比对）
 * - 失败 = 403
 *
 * ★ 设计原则（fail-closed by default）：
 * - 任何未配置 / 配置错误 / 鉴权逻辑降级 → 拒绝而非放行
 * - 静默放行 = 配置漂移 = 严重安全事件
 *
 * @author AIP
 * @since 2026-09-22
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "de-conversation.llm-service", name = "token")
public class LlmIngestAuthInterceptor implements HandlerInterceptor {

    /**
     * 共享密钥（须 owner 解封 R1 后在 application.yml 配置 32+ 字节随机）
     */
    @Value("${de-conversation.llm-service.token:}")
    private String expectedToken;

    /**
     * 启动期 fail-closed 校验：配置存在但空白 = 抛 IllegalStateException
     * （不要静默放行 — 配置漂移 = 安全事件）
     */
    @PostConstruct
    public void validateConfig() {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new IllegalStateException(
                "de-conversation.llm-service.token 已声明（@ConditionalOnProperty）但值为空白。"
                + "请在 application.yml 或 ~/.aip/aip-local.yml 配置 32+ 字节随机 token。"
            );
        }
        if (expectedToken.length() < 32) {
            throw new IllegalStateException(
                "de-conversation.llm-service.token 长度 < 32 字节（不足防暴力枚举）。"
                + "请生成 32+ 字节随机 token。"
            );
        }
        log.info("[SECURITY] LLM-ingest 鉴权拦截器激活：token.len={}", expectedToken.length());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // ★ fail-closed：未配置 = 拦截器未注册（@ConditionalOnProperty 守护）
        // 此处不再有"放行"分支 — 任何不匹配 = 403

        String providedToken = request.getHeader("X-LLM-Service-Token");
        if (providedToken == null || providedToken.isEmpty()) {
            log.warn("[SECURITY] /llm-message 缺 X-LLM-Service-Token header — 403");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        // 常量时间比对（防 timing attack）
        if (!constantTimeEquals(expectedToken, providedToken)) {
            log.warn("[SECURITY] /llm-message token 不匹配 — 403");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    /**
     * 常量时间字符串比对（防 timing attack）
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
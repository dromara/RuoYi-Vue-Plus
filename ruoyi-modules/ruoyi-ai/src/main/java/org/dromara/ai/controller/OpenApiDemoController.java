package org.dromara.ai.controller;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.PageResult;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.*;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import com.aizuda.snail.ai.openapi.client.core.listener.SseEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 使用示例 Controller
 * 演示如何使用 OpenAPI Client 调用 Snail AI 服务端接口
 *
 * @author opensnail
 * @date 2026-04-25
 */
@Slf4j
@RestController
@RequestMapping("/snail-ai")
@RequiredArgsConstructor
public class OpenApiDemoController {

    private final OpenApiAgentClient agentClient;
    private final OpenApiChatClient chatClient;
    private final OpenApiConversationClient conversationClient;
    private final OpenApiUserClient userClient;
    @Value("${snail-ai.chat-mode:stream}")
    private String chatMode;

    // ==================== User 相关接口 ====================

    /**
     * 注册当前登录用户并返回 OpenAPI 用户信息。
     */
    @PostMapping("/user/register")
    public Result<OpenApiUserVO> registerCurrentUser() {
        OpenApiUserVO user = ensureOpenApiUser();
        return Result.ok(user);
    }

    /**
     * 查询当前登录用户对应的 OpenAPI 用户信息。
     */
    @GetMapping("/user")
    public Result<OpenApiUserVO> getUser() {
        String openId = ensureOpenId();
        OpenApiUserQueryRequest request = new OpenApiUserQueryRequest();
        request.setOpenId(openId);
        return userClient.getUser(request);
    }

    // ==================== Agent 相关接口 ====================

    /**
     * 查询当前用户可访问的智能体列表。
     */
    @GetMapping("/agents")
    public Result<List<OpenApiAgentVO>> listAgents() {
        return agentClient.listAgents();
    }

    /**
     * 根据智能体 ID 查询智能体详情。
     */
    @GetMapping("/agent/{agentId}")
    public Result<OpenApiAgentVO> getAgent(
            @PathVariable Long agentId) {
        OpenApiAgentIdentityRequest request = new OpenApiAgentIdentityRequest();
        request.setAgentId(agentId);
        return agentClient.getAgent(request);
    }

    // ==================== Conversation 相关接口 ====================

    /**
     * 为指定智能体创建新会话。
     */
    @PostMapping("/agent/{agentId}/conversation")
    public Result<OpenApiConversationVO> createConversation(
            @PathVariable Long agentId,
            @RequestBody OpenApiCreateConversationRequest request) {
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        return conversationClient.createConversation(request);
    }

    /**
     * 分页查询指定智能体下的会话列表。
     */
    @GetMapping("/agent/{agentId}/conversations")
    public PageResult<List<OpenApiConversationVO>> listConversations(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        OpenApiConversationQueryRequest request = new OpenApiConversationQueryRequest();
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        request.setPage(page);
        request.setSize(size);
        return conversationClient.listConversations(request);
    }

    /**
     * 查询指定会话的消息历史。
     */
    @GetMapping("/agent/{agentId}/conversation/{conversationId}/messages")
    public Result<List<OpenApiMessageVO>> getMessages(
            @PathVariable Long agentId,
            @PathVariable String conversationId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(ensureOpenId());
        return conversationClient.getMessages(request);
    }

    /**
     * 删除指定会话。
     */
    @DeleteMapping("/agent/{agentId}/conversation/{conversationId}")
    public Result<Void> deleteConversation(
            @PathVariable Long agentId,
            @PathVariable String conversationId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(ensureOpenId());
        return conversationClient.deleteConversation(request);
    }

    // ==================== Chat 相关接口 ====================

    /**
     * 获取当前聊天发送模式。
     */
    @GetMapping("/chat/mode")
    public Result<Map<String, String>> getChatMode() {
        String mode = "sync".equalsIgnoreCase(chatMode) ? "sync" : "stream";
        return Result.ok(Map.of("mode", mode));
    }

    /**
     * 同步对话接口。
     */
    @PostMapping("/agent/{agentId}/chat/sync")
    public Result<OpenApiChatSyncResponse> chatSync(
            @PathVariable Long agentId,
            @RequestBody OpenApiChatRequest request) {
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        log.info("Sync chat request: agentId={}, content={}", agentId, request.getContent());
        return chatClient.chatSync(request);
    }

    /**
     * 流式对话接口，按 SSE 事件返回消息分片。
     */
    @GetMapping("/agent/{agentId}/chat/stream")
    public SseEmitter chatStream(
            @PathVariable Long agentId,
            @RequestParam String content,
            @RequestParam(required = false) String conversationId) {
        SseEmitter emitter = new SseEmitter(300000L);
        emitter.onTimeout(() -> {
            safeSend(emitter, "error", "SSE stream timeout");
            safeSend(emitter, "done", "");
            emitter.complete();
        });
        emitter.onError(error -> log.warn("SSE emitter error: {}", error.getMessage()));

        OpenApiChatRequest request = new OpenApiChatRequest();
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        request.setContent(content);
        request.setConversationId(conversationId);

        log.info("Stream chat request: agentId={}, content={}", agentId, content);
        try {
            chatClient.chatStream(request, new SseEventListener() {
                @Override
                public void onText(String text) {
                    safeSend(emitter, "text", text);
                }

                @Override
                public void onThinking(String thinking) {
                    safeSend(emitter, "thinking", thinking);
                }

                @Override
                public void onComplete(String data) {
                    safeSend(emitter, "done", data);
                    log.info("Stream chat completed");
                    emitter.complete();
                }

                @Override
                public void onError(String errorMessage) {
                    log.error("Stream chat error: {}", errorMessage);
                    safeSend(emitter, "error", errorMessage);
                    safeSend(emitter, "done", "");
                    emitter.complete();
                }
            });
        } catch (Exception e) {
            log.error("Stream chat exception", e);
            safeSend(emitter, "error", "stream exception: " + e.getMessage());
            safeSend(emitter, "done", "");
            emitter.complete();
        }

        return emitter;
    }

    /**
     * 输出一条 SSE 事件。
     */
    private void safeSend(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data == null ? "" : data));
        } catch (IOException e) {
            log.warn("SSE send failed, event={}", event, e);
        }
    }

    /**
     * 获取当前登录用户对应的 openId，不存在时会自动注册。
     */
    private String ensureOpenId() {
        return ensureOpenApiUser().getOpenId();
    }

    /**
     * 确保当前登录用户已注册为 OpenAPI 用户。
     */
    private OpenApiUserVO ensureOpenApiUser() {
        Long userId = LoginHelper.getUserId();
        String username = LoginHelper.getLoginUser().getNickname();

        OpenApiUserRegisterRequest registerRequest = new OpenApiUserRegisterRequest();
        registerRequest.setExternalId(String.valueOf(userId));
        registerRequest.setNickname(username);
        Result<OpenApiUserVO> registerResult = userClient.register(registerRequest);
        if (registerResult == null || registerResult.getData() == null) {
            throw new SnailAiException("注册 OpenAPI 用户失败，返回为空");
        }
        OpenApiUserVO user = registerResult.getData();
        return user;
    }
}

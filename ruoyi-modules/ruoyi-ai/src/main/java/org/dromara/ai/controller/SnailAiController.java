package org.dromara.ai.controller;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.common.openapi.dto.*;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import com.aizuda.snail.ai.openapi.client.core.listener.SseEventListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.api.model.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Snail AI OpenAPI 控制器
 *
 * @author opensnail
 * @date 2026-04-25
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/snail-ai")
@RequiredArgsConstructor
public class SnailAiController extends BaseController {

    /**
     * Snail AI 成功状态码。
     */
    private static final int SNAIL_AI_SUCCESS = 1;
    /**
     * SSE 超时时间。
     */
    private static final long SSE_TIMEOUT = 300000L;

    private final OpenApiAgentClient agentClient;
    private final OpenApiChatClient chatClient;
    private final OpenApiConversationClient conversationClient;
    private final OpenApiUserClient userClient;
    /**
     * 聊天模式。
     */
    @Value("${snail-ai.chat-mode:stream}")
    private String chatMode;

    /**
     * 注册当前登录用户并返回 OpenAPI 用户信息。
     */
    @PostMapping("/user/register")
    public R<OpenApiUserVO> registerCurrentUser() {
        return R.ok(ensureOpenApiUser());
    }

    /**
     * 查询当前登录用户对应的 OpenAPI 用户信息。
     */
    @GetMapping("/user")
    public R<OpenApiUserVO> getUser() {
        OpenApiUserQueryRequest request = new OpenApiUserQueryRequest();
        request.setOpenId(ensureOpenId());
        return toR(userClient.getUser(request));
    }

    /**
     * 查询当前用户可访问的智能体列表。
     */
    @GetMapping("/agents")
    public R<List<OpenApiAgentVO>> listAgents() {
        return toR(agentClient.listAgents());
    }

    /**
     * 根据智能体 ID 查询智能体详情。
     */
    @GetMapping("/agent/{agentId}")
    public R<OpenApiAgentVO> getAgent(@NotNull(message = "智能体ID不能为空") @PathVariable Long agentId) {
        OpenApiAgentIdentityRequest request = new OpenApiAgentIdentityRequest();
        request.setAgentId(agentId);
        return toR(agentClient.getAgent(request));
    }

    /**
     * 为指定智能体创建新会话。
     */
    @PostMapping("/agent/{agentId}/conversation")
    public R<OpenApiConversationVO> createConversation(
        @NotNull(message = "智能体ID不能为空") @PathVariable Long agentId,
        @RequestBody OpenApiCreateConversationRequest request) {
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        return toR(conversationClient.createConversation(request));
    }

    /**
     * 分页查询指定智能体下的会话列表。
     */
    @GetMapping("/agent/{agentId}/conversations")
    public R<PageResult<OpenApiConversationVO>> listConversations(
        @NotNull(message = "智能体ID不能为空") @PathVariable Long agentId,
        @Min(value = 1, message = "页码不能小于1") @RequestParam(defaultValue = "1") int page,
        @Min(value = 1, message = "每页条数不能小于1") @RequestParam(defaultValue = "10") int size) {
        OpenApiConversationQueryRequest request = new OpenApiConversationQueryRequest();
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        request.setPage(page);
        request.setSize(size);
        return toPageR(conversationClient.listConversations(request));
    }

    /**
     * 查询指定会话的消息历史。
     */
    @GetMapping("/agent/{agentId}/conversation/{conversationId}/messages")
    public R<List<OpenApiMessageVO>> getMessages(
        @NotNull(message = "智能体ID不能为空") @PathVariable Long agentId,
        @NotBlank(message = "会话ID不能为空") @PathVariable String conversationId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(ensureOpenId());
        return toR(conversationClient.getMessages(request));
    }

    /**
     * 删除指定会话。
     */
    @DeleteMapping("/agent/{agentId}/conversation/{conversationId}")
    public R<Void> deleteConversation(
        @NotNull(message = "智能体ID不能为空") @PathVariable Long agentId,
        @NotBlank(message = "会话ID不能为空") @PathVariable String conversationId) {
        OpenApiConversationIdentityRequest request = new OpenApiConversationIdentityRequest();
        request.setAgentId(agentId);
        request.setConversationId(conversationId);
        request.setOpenId(ensureOpenId());
        return toR(conversationClient.deleteConversation(request));
    }

    /**
     * 获取当前聊天发送模式。
     */
    @GetMapping("/chat/mode")
    public R<Map<String, String>> getChatMode() {
        String mode = "sync".equalsIgnoreCase(chatMode) ? "sync" : "stream";
        return R.ok(Map.of("mode", mode));
    }

    /**
     * 同步对话接口。
     */
    @PostMapping("/agent/{agentId}/chat/sync")
    public R<OpenApiChatSyncResponse> chatSync(
        @NotNull(message = "智能体ID不能为空") @PathVariable Long agentId,
        @RequestBody OpenApiChatRequest request) {
        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());
        log.info("Sync chat request: agentId={}", agentId);
        return toR(chatClient.chatSync(request));
    }

    /**
     * 流式对话接口，按 SSE 事件返回消息分片。
     */
    @PostMapping(value = "/agent/{agentId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
        @NotNull(message = "智能体ID不能为空") @PathVariable Long agentId,
        @RequestBody OpenApiChatRequest request,
        HttpServletResponse response) {
        prepareSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean closed = new AtomicBoolean(false);
        emitter.onTimeout(() -> {
            if (!closed.get()) {
                safeSend(emitter, closed, "error", "SSE stream timeout");
                safeSend(emitter, closed, "done", "");
                if (closed.compareAndSet(false, true)) {
                    emitter.complete();
                }
            }
        });
        emitter.onCompletion(() -> closed.set(true));
        emitter.onError(error -> {
            closed.set(true);
            log.warn("SSE emitter error: {}", error.getMessage());
        });

        request.setAgentId(agentId);
        request.setOpenId(ensureOpenId());

        log.info("Stream chat request: agentId={}", agentId);
        try {
            chatClient.chatStream(request, new SseEventListener() {
                /**
                 * 推送普通文本片段。
                 *
                 * @param text 文本片段
                 */
                @Override
                public void onText(String text) {
                    safeSend(emitter, closed, "text", text);
                }

                /**
                 * 推送思考内容片段。
                 *
                 * @param thinking 思考内容
                 */
                @Override
                public void onThinking(String thinking) {
                    safeSend(emitter, closed, "thinking", thinking);
                }

                /**
                 * 处理流式对话完成事件。
                 *
                 * @param data 完成数据
                 */
                @Override
                public void onComplete(String data) {
                    if (!closed.get()) {
                        safeSend(emitter, closed, "done", data);
                        log.info("Stream chat completed");
                        if (closed.compareAndSet(false, true)) {
                            emitter.complete();
                        }
                    }
                }

                /**
                 * 处理流式对话异常事件。
                 *
                 * @param errorMessage 错误信息
                 */
                @Override
                public void onError(String errorMessage) {
                    log.error("Stream chat error: {}", errorMessage);
                    if (!closed.get()) {
                        safeSend(emitter, closed, "error", errorMessage);
                        safeSend(emitter, closed, "done", "");
                        if (closed.compareAndSet(false, true)) {
                            emitter.complete();
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("Stream chat exception", e);
            if (!closed.get()) {
                safeSend(emitter, closed, "error", "stream exception: " + e.getMessage());
                safeSend(emitter, closed, "done", "");
                if (closed.compareAndSet(false, true)) {
                    emitter.complete();
                }
            }
        }

        return emitter;
    }

    /**
     * 转换 Snail AI 普通响应为项目统一响应。
     */
    private <T> R<T> toR(Result<T> result) {
        if (result == null) {
            return R.fail("Snail AI 服务返回为空");
        }
        if (result.getStatus() != SNAIL_AI_SUCCESS) {
            return R.fail(result.getMessage());
        }
        return R.ok(result.getData());
    }

    /**
     * 转换 Snail AI 分页响应为项目统一分页响应。
     */
    private <T> R<PageResult<T>> toPageR(com.aizuda.snail.ai.common.model.PageResult<List<T>> result) {
        R<List<T>> response = toR(result);
        if (R.isError(response)) {
            return R.fail(response.getMsg());
        }
        return R.ok(PageResult.build(response.getData(), result.getTotal()));
    }

    /**
     * 输出一条 SSE 事件。
     */
    private boolean safeSend(SseEmitter emitter, AtomicBoolean closed, String event, String data) {
        if (closed.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data == null ? "" : data));
            return true;
        } catch (IOException e) {
            closed.set(true);
            log.warn("SSE send failed, event={}", event, e);
            emitter.completeWithError(e);
            return false;
        }
    }

    /**
     * 设置 SSE 响应头，覆盖统一鉴权成功路径中的默认 JSON 响应类型。
     *
     * @param response 当前响应
     */
    private void prepareSseResponse(HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
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
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || userId == null) {
            throw new SnailAiException("当前登录用户为空");
        }

        OpenApiUserRegisterRequest registerRequest = new OpenApiUserRegisterRequest();
        registerRequest.setExternalId(String.valueOf(userId));
        registerRequest.setNickname(loginUser.getNickname());
        Result<OpenApiUserVO> registerResult = userClient.register(registerRequest);
        if (registerResult == null) {
            throw new SnailAiException("注册 OpenAPI 用户失败，返回为空");
        }
        if (registerResult.getStatus() != SNAIL_AI_SUCCESS) {
            throw new SnailAiException(registerResult.getMessage());
        }
        if (registerResult.getData() == null) {
            throw new SnailAiException("注册 OpenAPI 用户失败，返回为空");
        }
        return registerResult.getData();
    }
}

package org.dromara.de.conversation.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.de.conversation.bo.ConversationCompleteBo;
import org.dromara.de.conversation.bo.ConversationCreateBo;
import org.dromara.de.conversation.bo.LlmMessageBo;
import org.dromara.de.conversation.bo.MessageCreateBo;
import org.dromara.de.conversation.service.IDeConversationService;
import org.dromara.de.conversation.vo.ConversationVo;
import org.dromara.de.conversation.vo.MessageVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数字员工对话 Controller
 *
 * 实现 AC-09 / AC-10 / AC-11
 *
 * ★ Fix #3 (HIGH): 端点拆分
 * - /message (caller 端 @SaCheckLogin) = role=user
 * - /llm-message (内部 LLM-ingest) = role=assistant
 *   ★ 内部端点暂用 @SaCheckLogin（占位；须 R1 集成后改 mTLS / service token）
 *
 * @author AIP
 * @since 2026-09-22
 */
@SaCheckLogin
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/conversation")
public class DeConversationController extends BaseController {

    private final IDeConversationService conversationService;

    /**
     * AC-09 · 创建对话
     */
    @PostMapping
    public R<ConversationVo> create(@Valid @RequestBody ConversationCreateBo bo) {
        Long userId = LoginHelper.getUserId();
        return R.ok(conversationService.createConversation(bo, userId));
    }

    /**
     * AC-10 · 落一条用户消息（caller 端 = 仅 role=user）
     */
    @PostMapping("/{conversationId}/message")
    public R<MessageVo> message(@PathVariable String conversationId,
                                 @Valid @RequestBody MessageCreateBo bo) {
        Long userId = LoginHelper.getUserId();
        return R.ok(conversationService.appendMessage(conversationId, bo, userId));
    }

    /**
     * ★ Fix #3: 内部 LLM-ingest 端点（仅 service token）
     *   占位用 @SaCheckLogin；R1 集成后改为 mTLS / shared-secret header 校验
     */
    @PostMapping("/{conversationId}/llm-message")
    public R<MessageVo> llmMessage(@PathVariable String conversationId,
                                    @Valid @RequestBody LlmMessageBo bo) {
        Long userId = LoginHelper.getUserId();
        return R.ok(conversationService.appendLlmMessage(conversationId, bo, userId));
    }

    /**
     * AC-11 · 完成 + 回写实例运行态
     */
    @PostMapping("/{conversationId}/complete")
    public R<Void> complete(@PathVariable String conversationId,
                             @Valid @RequestBody ConversationCompleteBo bo) {
        Long userId = LoginHelper.getUserId();
        conversationService.completeConversation(conversationId, bo, userId);
        return R.ok();
    }
}
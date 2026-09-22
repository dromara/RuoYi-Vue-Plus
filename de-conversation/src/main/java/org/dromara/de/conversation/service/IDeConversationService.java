package org.dromara.de.conversation.service;

import org.dromara.de.conversation.bo.ConversationCompleteBo;
import org.dromara.de.conversation.bo.ConversationCreateBo;
import org.dromara.de.conversation.bo.LlmMessageBo;
import org.dromara.de.conversation.bo.MessageCreateBo;
import org.dromara.de.conversation.vo.ConversationVo;
import org.dromara.de.conversation.vo.MessageVo;

/**
 * 数字员工对话 Service 接口
 *
 * ★ Fix #3 (HIGH): caller 端只走 createConversation / appendMessage / completeConversation
 *   内部 LLM-ingest 走 appendLlmMessage（须服务间鉴权）
 *
 * @author AIP
 * @since 2026-09-22
 */
public interface IDeConversationService {

    /**
     * AC-09 · 创建对话
     */
    ConversationVo createConversation(ConversationCreateBo bo, Long userId);

    /**
     * AC-10 · 落一条用户消息（caller 端 = role=user）
     */
    MessageVo appendMessage(String conversationId, MessageCreateBo bo, Long userId);

    /**
     * AC-11 · 完成 + 回写实例运行态
     */
    void completeConversation(String conversationId, ConversationCompleteBo bo, Long userId);

    /**
     * ★ 内部 LLM-ingest：写 assistant 消息（含 thinking）
     * Fix #3 + Fix #6: 由 mTLS / service token 鉴权（本仓根模块仅写接口；密钥机制待 R1 集成）
     */
    MessageVo appendLlmMessage(String conversationId, LlmMessageBo bo, Long userId);
}
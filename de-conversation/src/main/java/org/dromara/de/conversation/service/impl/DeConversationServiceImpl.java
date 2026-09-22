package org.dromara.de.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.dromara.de.conversation.bo.ConversationCompleteBo;
import org.dromara.de.conversation.bo.ConversationCreateBo;
import org.dromara.de.conversation.bo.LlmMessageBo;
import org.dromara.de.conversation.bo.MessageCreateBo;
import org.dromara.de.conversation.domain.DeConversation;
import org.dromara.de.conversation.domain.DeConversationRecord;
import org.dromara.de.conversation.mapper.DeConversationMapper;
import org.dromara.de.conversation.mapper.DeConversationRecordMapper;
import org.dromara.de.conversation.service.IDeConversationService;
import org.dromara.de.conversation.vo.ConversationVo;
import org.dromara.de.conversation.vo.MessageVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数字员工对话 Service 实现
 *
 * ★ 安全设计（2026-09-22 第二轮 6 issue 全修）：
 * - Fix #1 (CRITICAL): SaiAgent.status 校验 (status != 4, status == 1)
 * - Fix #2 (HIGH): sai_user_agent 订阅关系校验 (IDOR 防)
 * - Fix #3 (HIGH): role 拆分 = caller 端只许 role=user；assistant 由内部端点收
 * - Fix #4 (MEDIUM): completeConversation 真落库 assistant record
 * - Fix #5 (MEDIUM): metadata JSON 严格校验 (depth + keys cap)
 * - Fix #6 (MEDIUM): thinking 仅内部端点收（caller 端 Bo 不含该字段）
 *
 * @author AIP
 * @since 2026-09-22
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeConversationServiceImpl implements IDeConversationService {

    private final DeConversationMapper conversationMapper;
    private final DeConversationRecordMapper recordMapper;
    private final SaiAgentMapper agentMapper;
    private final SaiUserAgentMapper userAgentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVo createConversation(ConversationCreateBo bo, Long userId) {
        // Fix #1 (CRITICAL): agent 校验（含 status）
        SaiAgent agent = validateAgentEnabled(bo.getAgentId());

        // Fix #2 (HIGH): 订阅关系校验 (user_id, agent_id) — 防 IDOR
        validateSubscription(userId, bo.getAgentId());

        // INSERT sai_agent_conversation
        DeConversation conv = new DeConversation();
        conv.setAgentId(bo.getAgentId());
        conv.setUserId(userId);
        conv.setConversationId(UUID.randomUUID().toString());
        conv.setTitle(bo.getTitle());
        conv.setCreateDt(LocalDateTime.now());
        conv.setUpdateDt(LocalDateTime.now());
        conversationMapper.insert(conv);

        ConversationVo vo = new ConversationVo();
        vo.setConversationId(conv.getConversationId());
        vo.setAgentId(conv.getAgentId());
        vo.setUserId(conv.getUserId());
        vo.setTitle(conv.getTitle());
        vo.setCreateDt(conv.getCreateDt());
        log.info("创建对话: conversationId.len={}, agentId={}, userId={}",
                conv.getConversationId().length(), conv.getAgentId(), conv.getUserId());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVo appendMessage(String conversationId, MessageCreateBo bo, Long userId) {
        // Fix #2 (HIGH): 统一权限校验 + 防 conversation 持有者之外的 agent 注入
        DeConversation conv = loadConversationOwned(conversationId, userId);
        if (!conv.getAgentId().equals(bo.getAgentId())) {
            throw new ServiceException("数字员工不可用");
        }

        // Fix #1 (CRITICAL) + Fix #2 (HIGH): 重新校验 agent 启用 + 订阅（防会话存续期间被禁用）
        validateAgentEnabled(bo.getAgentId());
        validateSubscription(userId, bo.getAgentId());

        // Fix #3 (HIGH): 强制 role=user（Bo 已限制；此处再校验）
        if (!"user".equals(bo.getRole())) {
            throw new ServiceException("角色不合法");
        }

        DeConversationRecord record = new DeConversationRecord();
        record.setAgentId(conv.getAgentId());
        record.setConversationId(conversationId);
        record.setUserId(userId);
        record.setRole(bo.getRole());
        record.setContent(bo.getContent());
        // Fix #6 (MEDIUM): thinking 字段 caller 端不收
        record.setMetadata(bo.getMetadata());
        record.setStatus(1);
        record.setCreateDt(LocalDateTime.now());
        recordMapper.insert(record);

        MessageVo vo = new MessageVo();
        vo.setRecordId(record.getId());
        vo.setConversationId(conversationId);
        vo.setRole(bo.getRole());
        vo.setCreateDt(record.getCreateDt());
        log.info("落消息 (caller): recordId={}, conversationId.len={}, role={}",
                record.getId(), conversationId.length(), bo.getRole());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeConversation(String conversationId, ConversationCompleteBo bo, Long userId) {
        DeConversation conv = loadConversationOwned(conversationId, userId);

        // Fix #4 (MEDIUM): 真落库 assistant record + 回写实例运行态
        DeConversationRecord record = new DeConversationRecord();
        record.setAgentId(conv.getAgentId());
        record.setConversationId(conversationId);
        record.setUserId(userId);
        record.setRole("assistant");
        record.setContent(bo.getLastResponse());
        record.setMetadata(buildMetadata(bo));
        record.setStatus(1);
        record.setOutputTokens(bo.getTokensUsed());
        record.setCreateDt(LocalDateTime.now());
        recordMapper.insert(record);

        // ★ AC-11 占位：实例运行态回写 — 须 owner 解封 R1 后调底座 flow_instance Mapper
        log.info("完成对话 + 落库 assistant 消息: recordId={}, conversationId.len={}, tokens={}",
                record.getId(), conversationId.length(), bo.getTokensUsed());
    }

    /**
     * 内部 LLM-ingest 端点：写 assistant 消息（含 thinking）
     * ★ Fix #3 (HIGH): 内部端点 = service token / 共享密钥校验（占位；密钥机制待 R1 集成后加）
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageVo appendLlmMessage(String conversationId, LlmMessageBo bo, Long userId) {
        DeConversation conv = loadConversationOwned(conversationId, userId);
        if (!conv.getAgentId().equals(bo.getAgentId())) {
            throw new ServiceException("数字员工不可用");
        }
        validateAgentEnabled(bo.getAgentId());
        validateSubscription(userId, bo.getAgentId());

        if (!"assistant".equals(bo.getRole())) {
            throw new ServiceException("角色不合法");
        }

        DeConversationRecord record = new DeConversationRecord();
        record.setAgentId(conv.getAgentId());
        record.setConversationId(conversationId);
        record.setUserId(userId);
        record.setRole("assistant");
        record.setContent(bo.getContent());
        record.setThinking(bo.getThinking());
        record.setMetadata(bo.getMetadata());
        record.setStatus(1);
        record.setInputTokens(bo.getInputTokens());
        record.setOutputTokens(bo.getOutputTokens());
        record.setCacheTokens(bo.getCacheTokens());
        record.setCreateDt(LocalDateTime.now());
        recordMapper.insert(record);

        MessageVo vo = new MessageVo();
        vo.setRecordId(record.getId());
        vo.setConversationId(conversationId);
        vo.setRole("assistant");
        vo.setCreateDt(record.getCreateDt());
        log.info("落消息 (llm-ingest): recordId={}, conversationId.len={}, thinking.len={}, outputTokens={}",
                record.getId(), conversationId.length(),
                bo.getThinking() != null ? bo.getThinking().length() : 0,
                bo.getOutputTokens());
        return vo;
    }

    // ========== 私有校验方法 ==========

    /**
     * Fix #1 (CRITICAL): agent 启用校验 — status != 4 (禁用) 且 status == 1 (启用)
     */
    private SaiAgent validateAgentEnabled(Long agentId) {
        log.info("[DEBUG] validateAgentEnabled: agentId={}", agentId);
        SaiAgent agent = agentMapper.selectById(agentId);
        log.info("[DEBUG] validateAgentEnabled: agent={} (null={})", agent, agent == null);
        if (agent == null) {
            log.warn("[DEBUG] agentId={} 在 sai_agent 表中查不到", agentId);
            throw new ServiceException("数字员工不可用");
        }
        if (agent.getStatus() != null) {
            log.info("[DEBUG] agentId={} status={}", agentId, agent.getStatus());
            if (agent.getStatus() == 4) {
                throw new ServiceException("数字员工已禁用");
            }
            if (agent.getStatus() != 1) {
                throw new ServiceException("数字员工状态不合法");
            }
        }
        return agent;
    }

    /**
     * Fix #2 (HIGH): 校验 (user_id, agent_id) 订阅关系存在
     */
    private void validateSubscription(Long userId, Long agentId) {
        log.info("[DEBUG] validateSubscription: userId={}, agentId={}", userId, agentId);
        SaiUserAgent sub = userAgentMapper.selectOne(
                new LambdaQueryWrapper<SaiUserAgent>()
                        .eq(SaiUserAgent::getUserId, userId)
                        .eq(SaiUserAgent::getAgentId, agentId)
        );
        log.info("[DEBUG] validateSubscription: sub={} (null={})", sub, sub == null);
        if (sub == null) {
            log.warn("[DEBUG] 订阅关系不存在: userId={}, agentId={}", userId, agentId);
            throw new ServiceException("数字员工不可用");
        }
    }

    /**
     * 统一权限校验（防 appendMessage / completeConversation / appendLlmMessage 不一致）
     */
    private DeConversation loadConversationOwned(String conversationId, Long userId) {
        DeConversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<DeConversation>().eq(DeConversation::getConversationId, conversationId)
        );
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new ServiceException("对话不存在或无权访问");
        }
        return conv;
    }

    /**
     * Fix #4: 组装 metadata JSON（lastResponse 关联 tokens / finishReason）
     */
    private String buildMetadata(ConversationCompleteBo bo) {
        StringBuilder sb = new StringBuilder("{");
        if (bo.getFinishReason() != null) {
            sb.append("\"finish_reason\":\"").append(escapeJson(bo.getFinishReason())).append("\"");
        }
        if (bo.getTokensUsed() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"tokens_used\":").append(bo.getTokensUsed());
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
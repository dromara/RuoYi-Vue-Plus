package org.dromara.de.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
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

import java.nio.charset.StandardCharsets;
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
    private final FlwInstanceMapper flwInstanceMapper;

    /** 线程安全，静态复用（会话实践复盘 · 审查 INFO：避免每次调用 new） */
    private static final ObjectMapper JSON = new ObjectMapper();

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
        // ★ S-08 信任模型（2026-09-22 安全审计 · 明文声明）：本端点允许**对话属主**把
        //   lastResponse/tokensUsed 作为 assistant 消息落库——与 /message 侧的 role 白名单
        //   （仅 user）不对称，属 AC-11 规格（集成步骤文档步骤 6）的**有意设计**：/complete 是
        //   收口端点，产出物由属主申报、属主可见。已知边界 = 属主可自报 token 计数与回复内容。
        //   若未来要求 assistant 内容仅经 llm-ingest（带服务 token 的内部通道）写入，须 owner
        //   改 AC-11 规格 + E2E 同步——本注释即该裁决的锚点。
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

        // ★ AC-11 实装（2026-09-22 · OW-13 阶段 1 · R1 已解封）：
        //   实例运行态回写 flow_instance.variable 的 de.ctx 子树（last_response / tokens_used /
        //   conversation_id / finish_reason），合并不覆盖（v / binding / task 原样保留）。
        //   真源：docs/AC-09-10-11集成步骤完整文档-2026-09-22.md 步骤 6；
        //   先例：DeBindingServiceImpl.updateVariable（selectById → set → updateById 旁路，
        //   D-003「是否改走底座 PUT 端点」仍待 owner，本处与其保持同一实装口径）。
        // ★ S-02 修正（2026-09-22 安全审计）：反查加回 create_by=当前登录用户名过滤
        //   （列-参数等值，参数强制力弱于列 ⇒ 无 1267）。错配/失败均降级 warn ——
        //   assistant 消息已落库，运行态回写独立失败不回滚整事务（审查 LOW#2 处置）；
        //   E2E 的 de.ctx.last_response 断言是本降级路径的兜底探针。
        try {
            Long instanceId = conversationMapper.findInstanceIdByBinding(conv.getAgentId(), LoginHelper.getUsername());
            if (instanceId == null) {
                log.warn("AC-11 回写跳过：agent={} user={}({}) 无本人发起的绑定实例（de.ctx.binding 反查 0 行）",
                    conv.getAgentId(), userId, LoginHelper.getUsername());
            } else {
                FlowInstance instance = flwInstanceMapper.selectById(instanceId);
                if (instance == null || instance.getVariable() == null) {
                    log.warn("AC-11 回写跳过：实例 {} 不存在或 variable 为空", instanceId);
                } else {
                    String newVariable = mergeDeCtx(instance.getVariable(), conversationId, bo);
                    instance.setVariable(newVariable);
                    flwInstanceMapper.updateById(instance);
                    log.info("AC-11 回写完成：instanceId={}, variable.bytes={}",
                        instanceId, newVariable.getBytes(StandardCharsets.UTF_8).length);
                }
            }
        } catch (Exception e) {
            log.warn("AC-11 回写降级（不影响已落库 assistant 消息）：{}", e.getMessage());
        }
        log.info("完成对话 + 落库 assistant 消息: recordId={}, conversationId.len={}, tokens={}",
                record.getId(), conversationId.length(), bo.getTokensUsed());
    }

    /**
     * AC-11：合并 de.ctx 完成态字段（不覆盖既有 v / binding / task 子树）。
     * 长度上限沿用 27 号 §5.2 真值（flow_instance.variable varchar 65536，
     * 与 DeVariableConstants.MAX_VARIABLE_LENGTH 同源）。
     */
    private String mergeDeCtx(String variableJson, String conversationId, ConversationCompleteBo bo) {
        try {
            JsonNode rootNode = JSON.readTree(variableJson);
            // 审查 LOW：根节点非 object（数组/标量）时给出显式错误而非 ClassCastException
            if (!rootNode.isObject()) {
                throw new ServiceException("AC-11 回写失败：variable 根节点非 JSON object（实际: " + rootNode.getNodeType() + "）");
            }
            ObjectNode root = (ObjectNode) rootNode;
            JsonNode deCtxNode = root.get(DeVariableConstants.VARIABLE_KEY);
            if (deCtxNode != null && !deCtxNode.isObject()) {
                throw new ServiceException("AC-11 回写失败：de.ctx 非 JSON object（实际: " + deCtxNode.getNodeType() + "）");
            }
            ObjectNode deCtx = (deCtxNode instanceof ObjectNode on) ? on : root.putObject(DeVariableConstants.VARIABLE_KEY);
            deCtx.put("last_response", bo.getLastResponse() == null ? "" : bo.getLastResponse());
            deCtx.put("tokens_used", bo.getTokensUsed() == null ? 0 : bo.getTokensUsed());
            deCtx.put("conversation_id", conversationId);
            deCtx.put("finish_reason", bo.getFinishReason() == null ? "" : bo.getFinishReason());
            String out = JSON.writeValueAsString(root);
            // ★ S-07 修正：上限按 UTF-8 字节计（列 = TEXT 65535 字节；Java length() 数
            //   UTF-16 码元，CJK 内容约 2.2 万字符即溢出 —— 2026-09-22 安全审计 S-07）
            if (out.getBytes(StandardCharsets.UTF_8).length > 65535) {
                throw new ServiceException("AC-11 回写超出 variable TEXT 65535 字节上限: "
                    + out.getBytes(StandardCharsets.UTF_8).length);
            }
            return out;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("AC-11 回写 variable 解析失败: " + e.getMessage());
        }
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
        String json = sb.toString();
        validateMetadataShape(json);
        return json;
    }

    /**
     * Fix #5 实装（2026-09-22 安全审计 S-06 订正）：类注释自称「metadata JSON 严格校验
     * (depth + keys cap)」但此前代码里不存在。现按声明实装：合法 JSON object + 深度 ≤ 8 + 键数 ≤ 32。
     */
    private void validateMetadataShape(String json) {
        try {
            JsonNode node = JSON.readTree(json);
            if (!node.isObject()) {
                throw new ServiceException("metadata 非 JSON object");
            }
            if (node.size() > 32) {
                throw new ServiceException("metadata 顶层键数超限（>32）");
            }
            if (depthOf(node) > 8) {
                throw new ServiceException("metadata 深度超限（>8）");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("metadata 非法 JSON: " + e.getMessage());
        }
    }

    private int depthOf(JsonNode node) {
        if (!node.isObject() && !node.isArray()) {
            return 0;
        }
        int max = 0;
        for (JsonNode child : node) {
            max = Math.max(max, depthOf(child));
        }
        return max + 1;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
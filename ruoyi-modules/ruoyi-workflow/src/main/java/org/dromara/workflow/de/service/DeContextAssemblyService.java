package org.dromara.workflow.de.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.domain.SaiRagDocument;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiRagDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * L3 上下文装配层（AU-08 · D-012 · 2026-09-23）。
 *
 * <p>34 号 §2.2 T-4 的落地：在把内容拼进数字员工这次模型调用之前过滤。
 * 硬蕴含：T-3 不可见 ⇒ T-4 不可用（不可配置）。
 * 「另一主体的上下文不混入本次对话」的唯一落地点。
 *
 * <p>判定链（每条上下文片段逐一判）：
 * <ol>
 *   <li>属主 → 放行</li>
 *   <li>ACL grantType="input" 命中 → 放行（AU-04/05/06 四维）</li>
 *   <li>T-3 关系 + 实例内 → 放行（方案 A，仅历史对话片段）</li>
 *   <li>其余 → 排除（不可见 = 不可用）</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeContextAssemblyService {

    private final SaiAgentMapper saiAgentMapper;
    private final SaiAgentConversationRecordMapper saiAgentConversationRecordMapper;
    private final SaiRagDocumentMapper saiRagDocumentMapper;
    private final DeAccessService deAccessService;
    private final ISaiObjectAclService saiObjectAclService;

    /**
     * 装配结果（不可变）。
     */
    public record AssembledContext(
        String agentInstruction,
        List<FilteredRecord> historyRecords,
        List<FilteredRag> ragDocuments,
        int excludedCount
    ) {}

    public record FilteredRecord(Long id, String role, String content) {}

    public record FilteredRag(Long id, String name, String content) {}

    /**
     * 装配 LLM 输入上下文（AU-08 · T-4 硬蕴含执行点）。
     *
     * @param agentId 数字员工 ID
     * @param conversationId 对话 ID（可空 = 新对话无历史）
     * @return 装配结果（已过滤）
     */
    public AssembledContext assemble(Long agentId, String conversationId) {
        Long userId = LoginHelper.getUserId();
        int excluded = 0;

        // ① agent 指令（属主维度——agent 公开的 instruction 无需过滤）
        SaiAgent agent = saiAgentMapper.selectById(agentId);
        String instruction = agent != null ? agent.getInstruction() : null;

        // ② 历史对话（T-4 硬蕴含：逐条过滤不可见的内容）
        List<FilteredRecord> history = new ArrayList<>();
        if (StrUtil.isNotBlank(conversationId)) {
            List<SaiAgentConversationRecord> records = saiAgentConversationRecordMapper.selectList(
                new LambdaQueryWrapper<SaiAgentConversationRecord>()
                    .eq(SaiAgentConversationRecord::getConversationId, conversationId)
                    .orderByAsc(SaiAgentConversationRecord::getId));
            if (records != null) {
                for (SaiAgentConversationRecord r : records) {
                    if (canUseAsInput(r, userId)) {
                        history.add(new FilteredRecord(r.getId(), r.getRole(), r.getContent()));
                    } else {
                        excluded++;
                        log.info("[L3] T-4 排除: recordId={} userId={} (T-3不可见→T-4不可用)",
                            r.getId(), userId);
                    }
                }
            }
        }

        // ③ RAG 知识库（agent.rag_ids 指向的文档，逐条判 ACL input 授权）
        List<FilteredRag> rags = new ArrayList<>();
        if (agent != null && agent.getRagEnabled() != null && agent.getRagEnabled() == 1
            && StrUtil.isNotBlank(agent.getRagIds())) {
            for (String idStr : agent.getRagIds().split(",")) {
                try {
                    Long ragDocId = Long.parseLong(idStr.trim());
                    SaiRagDocument doc = saiRagDocumentMapper.selectById(ragDocId);
                    if (doc == null) continue;
                    // RAG 文档的 T-4：ACL grantType="input" 命中即放行（AU-03 方案 B）
                    if (saiObjectAclService.hasAccess("rag_document", ragDocId, userId, "input", null)) {
                        rags.add(new FilteredRag(doc.getId(), doc.getName(), doc.getContent()));
                    } else {
                        excluded++;
                        log.info("[L3] T-4 排除 RAG: docId={} userId={} (无 input ACL)", ragDocId, userId);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        log.info("[L3] 装配完成: agentId={} convId={} history={} rags={} excluded={}",
            agentId, conversationId, history.size(), rags.size(), excluded);
        return new AssembledContext(instruction, history, rags, excluded);
    }

    /**
     * T-4 判定：这条对话记录能否作为 LLM 输入。
     * 硬蕴含 = T-3 不可见 ⇒ T-4 不可用。ACL grantType="input" 是唯一加宽口。
     */
    private boolean canUseAsInput(SaiAgentConversationRecord record, Long userId) {
        // 属主
        if (record.getUserId() != null && record.getUserId().equals(userId)) {
            return true;
        }
        // ACL input 授权（AU-03 方案 B 的 T-4 通道）
        if (saiObjectAclService.hasAccess("conversation_record", record.getId(), userId, "input", null)) {
            return true;
        }
        // T-3 可见（硬蕴含：可见 ⇒ 可用——L3 默认放行 T-3 可见的记录）
        // 注：T-3 可见但 ACL 无 input 授权 ⇒ 仍可用（T-3 是 T-4 的下界）
        return deAccessService.canReadConversationRecord(record);
    }
}

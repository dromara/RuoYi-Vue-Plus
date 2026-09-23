package org.dromara.workflow.de.mm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mm.domain.vo.DeMmConfigVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmConversationDetailVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmMemoryVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 记忆与经验 MM 域服务实现（typed CRUD：ServiceImpl&lt;Mapper, Entity&gt;）。
 *
 * <p>实体 {@link SaiAgentConversationRecord}，数据落点 sai_agent_conversation_record。
 * 记忆开关只读引用 sai_agent.memory_enabled（{@link SaiAgentMapper}）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeMmServiceImpl
        extends ServiceImpl<SaiAgentConversationRecordMapper, SaiAgentConversationRecord>
        implements IDeMmService {

    private final SaiAgentMapper saiAgentMapper;

    @Override
    public List<DeMmMemoryVo> listMemories(Long userId, Long agentId, String conversationId, String role, String keyword) {
        if (userId == null) {
            throw new ServiceException("userId 必填");
        }
        LambdaQueryWrapper<SaiAgentConversationRecord> qw = new LambdaQueryWrapper<SaiAgentConversationRecord>()
            .eq(SaiAgentConversationRecord::getUserId, userId)
            .orderByDesc(SaiAgentConversationRecord::getId);
        if (agentId != null) {
            qw.eq(SaiAgentConversationRecord::getAgentId, agentId);
        }
        if (conversationId != null && !conversationId.isBlank()) {
            qw.eq(SaiAgentConversationRecord::getConversationId, conversationId);
        }
        if (role != null && !role.isBlank()) {
            qw.eq(SaiAgentConversationRecord::getRole, role);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.like(SaiAgentConversationRecord::getContent, keyword);
        }
        List<SaiAgentConversationRecord> records = list(qw);
        List<DeMmMemoryVo> out = new ArrayList<>(records.size());
        for (SaiAgentConversationRecord r : records) {
            out.add(toMemoryVo(r));
        }
        return out;
    }

    @Override
    public DeMmConversationDetailVo conversationDetail(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ServiceException("conversationId 必填");
        }
        List<SaiAgentConversationRecord> records = list(
            new LambdaQueryWrapper<SaiAgentConversationRecord>()
                .eq(SaiAgentConversationRecord::getConversationId, conversationId)
                .orderByAsc(SaiAgentConversationRecord::getId));
        if (records.isEmpty()) {
            throw new ServiceException("会话记录不存在 conversationId=" + conversationId);
        }
        DeMmConversationDetailVo vo = new DeMmConversationDetailVo();
        vo.setConversationId(conversationId);
        vo.setRecordCount(records.size());

        Map<String, Long> roleDist = new TreeMap<>();
        Map<Integer, Long> statusDist = new TreeMap<>();
        long withMetadata = 0;
        for (SaiAgentConversationRecord r : records) {
            roleDist.merge(r.getRole(), 1L, Long::sum);
            statusDist.merge(r.getStatus(), 1L, Long::sum);
            if (r.getMetadata() != null && !r.getMetadata().isBlank()) {
                withMetadata++;
            }
        }
        vo.setRoleDistribution(roleDist);
        vo.setStatusDistribution(statusDist);
        vo.setRecordsWithMetadata(withMetadata);

        SaiAgentConversationRecord first = records.get(0);
        SaiAgentConversationRecord last = records.get(records.size() - 1);
        vo.setAgentId(first.getAgentId());
        vo.setUserId(first.getUserId());
        vo.setFirstContent(preview(first.getContent()));
        vo.setLastContent(preview(last.getContent()));
        vo.setLastRole(last.getRole());

        List<DeMmMemoryVo> items = new ArrayList<>(records.size());
        for (SaiAgentConversationRecord r : records) {
            items.add(toMemoryVo(r));
        }
        vo.setRecords(items);
        return vo;
    }

    @Override
    public DeMmConfigVo memoryConfig(Long agentId) {
        if (agentId == null) {
            throw new ServiceException("agentId 必填");
        }
        SaiAgent agent = saiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new ServiceException("数字员工不存在 agentId=" + agentId);
        }
        List<SaiAgentConversationRecord> records = list(
            new LambdaQueryWrapper<SaiAgentConversationRecord>()
                .eq(SaiAgentConversationRecord::getAgentId, agentId));

        DeMmConfigVo vo = new DeMmConfigVo();
        vo.setAgentId(agentId);
        // sai_agent.memory_enabled：底座记忆库开关（0 关 / 1 开）
        vo.setMemoryEnabled(agent.getMemoryEnabled() != null && agent.getMemoryEnabled() == 1);
        vo.setTotalRecords(records.size());
        long withMetadata = 0;
        java.util.Set<String> conversations = new java.util.HashSet<>();
        java.util.Set<Long> users = new java.util.HashSet<>();
        for (SaiAgentConversationRecord r : records) {
            if (r.getMetadata() != null && !r.getMetadata().isBlank()) {
                withMetadata++;
            }
            if (r.getConversationId() != null) {
                conversations.add(r.getConversationId());
            }
            if (r.getUserId() != null) {
                users.add(r.getUserId());
            }
        }
        vo.setRecordsWithMetadata(withMetadata);
        vo.setConversationCount(conversations.size());
        vo.setUserCount(users.size());
        return vo;
    }

    private DeMmMemoryVo toMemoryVo(SaiAgentConversationRecord r) {
        DeMmMemoryVo vo = new DeMmMemoryVo();
        vo.setRecordId(r.getId());
        vo.setAgentId(r.getAgentId());
        vo.setUserId(r.getUserId());
        vo.setConversationId(r.getConversationId());
        vo.setRole(r.getRole());
        vo.setContent(r.getContent());
        vo.setMetadata(r.getMetadata());
        vo.setStatus(r.getStatus());
        return vo;
    }

    private String preview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= 200 ? content : content.substring(0, 200);
    }
}

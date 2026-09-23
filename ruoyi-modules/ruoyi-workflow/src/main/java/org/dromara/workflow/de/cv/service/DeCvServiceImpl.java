package org.dromara.workflow.de.cv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.de.cv.domain.bo.DeCvExecuteBo;
import org.dromara.workflow.de.cv.domain.vo.DeCvExecuteResultVo;
import org.dromara.workflow.de.cv.domain.vo.DeCvSessionVo;
import org.dromara.workflow.de.cv.domain.vo.DeCvStatusVo;
import org.dromara.workflow.de.domain.SaiAgentConversation;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.mapper.SaiAgentConversationMapper;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话与执行 CV 域服务实现（typed CRUD：ServiceImpl&lt;Mapper, Entity&gt;）。
 *
 * <p>实体 {@link SaiAgentConversation}（sai_agent_conversation）；执行态读写经
 * {@link SaiAgentConversationRecordMapper}（sai_agent_conversation_record）；
 * 锚定实例只读引用 {@link FlwInstanceMapper}（flow_instance.flow_status）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeCvServiceImpl
        extends ServiceImpl<SaiAgentConversationMapper, SaiAgentConversation>
        implements IDeCvService {

    /** sai_agent_conversation_record.status：1=成功 */
    private static final int RECORD_STATUS_SUCCESS = 1;
    /** sai_agent_conversation_record.status：2=失败 */
    private static final int RECORD_STATUS_FAILED = 2;
    /** sai_agent_conversation_record.status：3=进行中 */
    private static final int RECORD_STATUS_RUNNING = 3;

    private final SaiAgentConversationRecordMapper recordMapper;
    private final FlwInstanceMapper flwInstanceMapper;

    @Override
    public List<DeCvSessionVo> listSessions(Long userId, Long agentId) {
        if (userId == null) {
            throw new ServiceException("userId 必填");
        }
        LambdaQueryWrapper<SaiAgentConversation> qw = new LambdaQueryWrapper<SaiAgentConversation>()
            .eq(SaiAgentConversation::getUserId, userId)
            .orderByDesc(SaiAgentConversation::getUpdateDt);
        if (agentId != null) {
            qw.eq(SaiAgentConversation::getAgentId, agentId);
        }
        List<SaiAgentConversation> conversations = list(qw);
        List<DeCvSessionVo> out = new ArrayList<>(conversations.size());
        for (SaiAgentConversation c : conversations) {
            DeCvSessionVo vo = new DeCvSessionVo();
            vo.setConversationId(c.getConversationId());
            vo.setAgentId(c.getAgentId());
            vo.setUserId(c.getUserId());
            vo.setInstanceId(c.getInstanceId());
            vo.setTitle(c.getTitle());
            vo.setCreateDt(c.getCreateDt());
            vo.setUpdateDt(c.getUpdateDt());
            out.add(vo);
        }
        return out;
    }

    @Override
    public DeCvExecuteResultVo execute(DeCvExecuteBo bo) {
        SaiAgentConversation conv = getOne(
            new LambdaQueryWrapper<SaiAgentConversation>()
                .eq(SaiAgentConversation::getConversationId, bo.getConversationId()));
        if (conv == null) {
            throw new ServiceException("会话不存在 conversationId=" + bo.getConversationId());
        }
        // 执行触发 = 落一条 user 记录，status=3（进行中）；
        // assistant 回复由 de-conversation 模块 /llm-message 落库（模块边界：不跨模块调用）。
        SaiAgentConversationRecord record = new SaiAgentConversationRecord();
        record.setAgentId(conv.getAgentId());
        record.setUserId(conv.getUserId());
        record.setConversationId(conv.getConversationId());
        record.setRole("user");
        record.setContent(bo.getContent());
        record.setStatus(RECORD_STATUS_RUNNING);
        recordMapper.insert(record);
        log.info("[de.cv] execute triggered conversationId={} recordId={}",
            conv.getConversationId(), record.getId());

        DeCvExecuteResultVo vo = new DeCvExecuteResultVo();
        vo.setRecordId(record.getId());
        vo.setConversationId(conv.getConversationId());
        vo.setAgentId(conv.getAgentId());
        vo.setUserId(conv.getUserId());
        vo.setInstanceId(conv.getInstanceId());
        vo.setRecordStatus(RECORD_STATUS_RUNNING);
        vo.setExecutionState("running");
        return vo;
    }

    @Override
    public DeCvStatusVo status(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ServiceException("conversationId 必填");
        }
        SaiAgentConversation conv = getOne(
            new LambdaQueryWrapper<SaiAgentConversation>()
                .eq(SaiAgentConversation::getConversationId, conversationId));
        if (conv == null) {
            throw new ServiceException("会话不存在 conversationId=" + conversationId);
        }
        SaiAgentConversationRecord latest = recordMapper.selectOne(
            new LambdaQueryWrapper<SaiAgentConversationRecord>()
                .eq(SaiAgentConversationRecord::getConversationId, conversationId)
                .orderByDesc(SaiAgentConversationRecord::getId)
                .last("LIMIT 1"));

        DeCvStatusVo vo = new DeCvStatusVo();
        vo.setConversationId(conversationId);
        vo.setAgentId(conv.getAgentId());
        vo.setUserId(conv.getUserId());
        vo.setInstanceId(conv.getInstanceId());
        vo.setTitle(conv.getTitle());
        if (latest == null) {
            // 会话已建、尚无记录 → 排队
            vo.setExecutionState("queued");
        } else {
            vo.setRecordId(latest.getId());
            vo.setRecordStatus(latest.getStatus());
            vo.setLatestRole(latest.getRole());
            vo.setLatestContentPreview(preview(latest.getContent()));
            vo.setExecutionState(mapExecutionState(latest.getStatus()));
        }
        // 锚定实例时并回 flow_instance.flow_status（CV-02 双轨：记录态 + 实例态）
        if (conv.getInstanceId() != null) {
            FlowInstance instance = flwInstanceMapper.selectById(conv.getInstanceId());
            if (instance != null) {
                vo.setFlowStatus(instance.getFlowStatus());
            }
        }
        return vo;
    }

    /**
     * CV-02 执行态映射：记录无 → queued；3=进行中 → running；1=成功 → done；2=失败 → failed。
     * 「等人」态需 flow 侧判定，经 flowStatus 原值透出，不在本域单轨拍板。
     */
    private String mapExecutionState(Integer recordStatus) {
        if (recordStatus == null) {
            return "queued";
        }
        if (recordStatus == RECORD_STATUS_RUNNING) {
            return "running";
        }
        if (recordStatus == RECORD_STATUS_SUCCESS) {
            return "done";
        }
        if (recordStatus == RECORD_STATUS_FAILED) {
            return "failed";
        }
        return "unknown";
    }

    private String preview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= 200 ? content : content.substring(0, 200);
    }
}

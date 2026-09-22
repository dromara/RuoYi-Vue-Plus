package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_conversation_record 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：{@link SaiAgentConversationRecord}（既有 domain POJO，7 列）。
 *
 * <p><b>与 {@code DeSaiConvController} 的关系</b>：该 controller 暴露的
 * {@code GET /de/sai-conv/record/{recordId}} 端点是单条读；
 * 本实现的 7 端点是独立 CRUD 接口（与该派生读不相互替代）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiAgentConversationRecordServiceImpl
        extends ServiceImpl<SaiAgentConversationRecordMapper, SaiAgentConversationRecord>
        implements ISaiAgentConversationRecordService {

    private static final String STUB_MSG = "sai_agent_conversation_record 接口存根，等待 owner 实现";

    @Override
    public Long add(SaiAgentConversationRecord bo) {
        log.warn("[de.sai-agent-conversation-record] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(SaiAgentConversationRecord bo) {
        log.warn("[de.sai-agent-conversation-record] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-agent-conversation-record] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public SaiAgentConversationRecord getInfo(Long id) {
        log.warn("[de.sai-agent-conversation-record] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiAgentConversationRecord> list(SaiAgentConversationRecord bo) {
        log.warn("[de.sai-agent-conversation-record] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<SaiAgentConversationRecord> page(SaiAgentConversationRecord bo) {
        log.warn("[de.sai-agent-conversation-record] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiAgentConversationRecord> listByFk(Long fkId) {
        log.warn("[de.sai-agent-conversation-record] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}
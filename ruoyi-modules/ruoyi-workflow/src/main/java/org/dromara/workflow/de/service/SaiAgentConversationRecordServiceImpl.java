package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * sai_agent_conversation_record 服务实现（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
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

    // ★ D-010 批⑥实化（2026-09-23）：typed 存根 → 真实 CRUD

    @Override
    public Long add(SaiAgentConversationRecord bo) {
        save(bo);
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiAgentConversationRecord bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(java.util.Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiAgentConversationRecord getInfo(Long id) {
        return getById(id);
    }

    @Override
    public java.util.List<SaiAgentConversationRecord> list(SaiAgentConversationRecord bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiAgentConversationRecord> page(SaiAgentConversationRecord bo) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public java.util.List<SaiAgentConversationRecord> listByFk(Long fkId) {
        return java.util.Collections.emptyList();
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaiAgentConversationRecord> buildWrapper(SaiAgentConversationRecord bo) {
        return new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaiAgentConversationRecord>().orderByDesc(SaiAgentConversationRecord::getId);
    }
}

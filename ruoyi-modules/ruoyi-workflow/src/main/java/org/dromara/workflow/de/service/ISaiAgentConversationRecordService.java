package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_conversation_record 服务接口（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：{@link SaiAgentConversationRecord}（既有 domain POJO，7 列）。
 *
 * <p><b>与 {@code DeSaiConvController} 的关系</b>：该 controller 暴露的
 * {@code GET /de/sai-conv/record/{recordId}} 端点是单条读；
 * 本接口的 7 端点是独立 CRUD 接口（与该派生读不相互替代）。
 *
 * @author AIP
 */
public interface ISaiAgentConversationRecordService extends IService<SaiAgentConversationRecord> {

    Long add(SaiAgentConversationRecord bo);

    Boolean edit(SaiAgentConversationRecord bo);

    Boolean remove(Collection<Long> ids);

    SaiAgentConversationRecord getInfo(Long id);

    List<SaiAgentConversationRecord> list(SaiAgentConversationRecord bo);

    PageResult<SaiAgentConversationRecord> page(SaiAgentConversationRecord bo);

    List<SaiAgentConversationRecord> listByFk(Long fkId);
}
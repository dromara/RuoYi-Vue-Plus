package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_conversation_record 服务接口（存根）。
 *
 * <p><b>存根说明</b>：本接口为 OW-03 决议清单的接口层存根（7 端点），
 * 实现由 {@link SaiAgentConversationRecordServiceImpl} 全部抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
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
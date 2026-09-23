package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentUsageStat;

import java.util.Collection;
import java.util.List;

/**
 * SaiAgentUsageStat 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiAgentUsageStatService {

    Long add(SaiAgentUsageStat bo);

    Boolean edit(SaiAgentUsageStat bo);

    Boolean remove(Collection<Long> ids);

    SaiAgentUsageStat getInfo(Long id);

    List<SaiAgentUsageStat> list(SaiAgentUsageStat bo);

    PageResult<SaiAgentUsageStat> page(SaiAgentUsageStat bo);

    List<SaiAgentUsageStat> listByFk(Long fkId);
}

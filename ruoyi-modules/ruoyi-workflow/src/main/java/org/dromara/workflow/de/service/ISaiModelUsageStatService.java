package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiModelUsageStat;

import java.util.Collection;
import java.util.List;

/**
 * SaiModelUsageStat 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiModelUsageStatService {

    Long add(SaiModelUsageStat bo);

    Boolean edit(SaiModelUsageStat bo);

    Boolean remove(Collection<Long> ids);

    SaiModelUsageStat getInfo(Long id);

    List<SaiModelUsageStat> list(SaiModelUsageStat bo);

    PageResult<SaiModelUsageStat> page(SaiModelUsageStat bo);

    List<SaiModelUsageStat> listByFk(Long fkId);
}

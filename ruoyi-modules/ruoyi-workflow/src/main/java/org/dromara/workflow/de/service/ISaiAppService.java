package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiApp;

import java.util.Collection;
import java.util.List;

/**
 * SaiApp 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiAppService {

    Long add(SaiApp bo);

    Boolean edit(SaiApp bo);

    Boolean remove(Collection<Long> ids);

    SaiApp getInfo(Long id);

    List<SaiApp> list(SaiApp bo);

    PageResult<SaiApp> page(SaiApp bo);

    List<SaiApp> listByFk(Long fkId);
}

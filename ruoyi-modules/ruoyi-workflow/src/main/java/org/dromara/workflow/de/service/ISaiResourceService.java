package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiResource;

import java.util.Collection;
import java.util.List;

/**
 * SaiResource 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiResourceService {

    Long add(SaiResource bo);

    Boolean edit(SaiResource bo);

    Boolean remove(Collection<Long> ids);

    SaiResource getInfo(Long id);

    List<SaiResource> list(SaiResource bo);

    PageResult<SaiResource> page(SaiResource bo);

    List<SaiResource> listByFk(Long fkId);
}

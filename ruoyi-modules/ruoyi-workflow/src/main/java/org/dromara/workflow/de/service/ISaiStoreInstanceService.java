package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiStoreInstance;

import java.util.Collection;
import java.util.List;

/**
 * SaiStoreInstance 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiStoreInstanceService {

    Long add(SaiStoreInstance bo);

    Boolean edit(SaiStoreInstance bo);

    Boolean remove(Collection<Long> ids);

    SaiStoreInstance getInfo(Long id);

    List<SaiStoreInstance> list(SaiStoreInstance bo);

    PageResult<SaiStoreInstance> page(SaiStoreInstance bo);

    List<SaiStoreInstance> listByFk(Long fkId);
}

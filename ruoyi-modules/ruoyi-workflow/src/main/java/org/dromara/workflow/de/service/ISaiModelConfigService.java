package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiModelConfig;

import java.util.Collection;
import java.util.List;

/**
 * SaiModelConfig 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiModelConfigService {

    Long add(SaiModelConfig bo);

    Boolean edit(SaiModelConfig bo);

    Boolean remove(Collection<Long> ids);

    SaiModelConfig getInfo(Long id);

    List<SaiModelConfig> list(SaiModelConfig bo);

    PageResult<SaiModelConfig> page(SaiModelConfig bo);

    List<SaiModelConfig> listByFk(Long fkId);
}

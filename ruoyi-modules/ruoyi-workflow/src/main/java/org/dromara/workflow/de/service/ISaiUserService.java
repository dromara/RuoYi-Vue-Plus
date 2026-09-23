package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiUser;

import java.util.Collection;
import java.util.List;

/**
 * SaiUser 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiUserService {

    Long add(SaiUser bo);

    Boolean edit(SaiUser bo);

    Boolean remove(Collection<Long> ids);

    SaiUser getInfo(Long id);

    List<SaiUser> list(SaiUser bo);

    PageResult<SaiUser> page(SaiUser bo);

    List<SaiUser> listByFk(Long fkId);
}

package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiOpenapiUser;

import java.util.Collection;
import java.util.List;

/**
 * SaiOpenapiUser 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiOpenapiUserService {

    Long add(SaiOpenapiUser bo);

    Boolean edit(SaiOpenapiUser bo);

    Boolean remove(Collection<Long> ids);

    SaiOpenapiUser getInfo(Long id);

    List<SaiOpenapiUser> list(SaiOpenapiUser bo);

    PageResult<SaiOpenapiUser> page(SaiOpenapiUser bo);

    List<SaiOpenapiUser> listByFk(Long fkId);
}

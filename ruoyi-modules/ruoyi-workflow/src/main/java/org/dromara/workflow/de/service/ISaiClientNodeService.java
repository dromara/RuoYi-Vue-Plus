package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiClientNode;

import java.util.Collection;
import java.util.List;

/**
 * SaiClientNode 服务接口（D-010 批⑥ typed 化）。
 */
public interface ISaiClientNodeService {

    Long add(SaiClientNode bo);

    Boolean edit(SaiClientNode bo);

    Boolean remove(Collection<Long> ids);

    SaiClientNode getInfo(Long id);

    List<SaiClientNode> list(SaiClientNode bo);

    PageResult<SaiClientNode> page(SaiClientNode bo);

    List<SaiClientNode> listByFk(Long fkId);
}

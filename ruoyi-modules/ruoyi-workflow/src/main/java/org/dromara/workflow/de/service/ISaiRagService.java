package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRag;

import java.util.Collection;
import java.util.List;

/**
 * SaiRag 服务接口（D-010 批④ typed 化）。
 *
 * @author AIP
 * @since 2026-09-22
 */
public interface ISaiRagService {

    Long add(SaiRag bo);

    Boolean edit(SaiRag bo);

    Boolean remove(Collection<Long> ids);

    SaiRag getInfo(Long id);

    List<SaiRag> list(SaiRag bo);

    PageResult<SaiRag> page(SaiRag bo);

    PageResult<SaiRag> page(SaiRag bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery);

    List<SaiRag> listByFk(Long fkId);
}

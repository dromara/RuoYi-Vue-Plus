package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRagDocument;

import java.util.Collection;
import java.util.List;

/**
 * SaiRagDocument 服务接口（D-010 批④ typed 化）。
 *
 * @author AIP
 * @since 2026-09-22
 */
public interface ISaiRagDocumentService {

    Long add(SaiRagDocument bo);

    Boolean edit(SaiRagDocument bo);

    Boolean remove(Collection<Long> ids);

    SaiRagDocument getInfo(Long id);

    List<SaiRagDocument> list(SaiRagDocument bo);

    PageResult<SaiRagDocument> page(SaiRagDocument bo);

    PageResult<SaiRagDocument> page(SaiRagDocument bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery);

    List<SaiRagDocument> listByFk(Long fkId);
}

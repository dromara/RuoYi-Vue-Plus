package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRagDocumentImage;

import java.util.Collection;
import java.util.List;

/**
 * sai_rag_document_image 服务接口（Map 存根 → typed，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
public interface ISaiRagDocumentImageService extends IService<SaiRagDocumentImage> {

    Long add(SaiRagDocumentImage bo);

    Boolean edit(SaiRagDocumentImage bo);

    Boolean remove(Collection<Long> ids);

    SaiRagDocumentImage getInfo(Long id);

    List<SaiRagDocumentImage> list(SaiRagDocumentImage bo);

    PageResult<SaiRagDocumentImage> page(SaiRagDocumentImage bo);

    List<SaiRagDocumentImage> listByFk(Long fkId);
}

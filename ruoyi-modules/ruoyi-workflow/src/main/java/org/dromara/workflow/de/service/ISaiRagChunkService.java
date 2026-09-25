package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRagChunk;

import java.util.Collection;
import java.util.List;

/**
 * sai_rag_chunk 服务接口（Map 存根 → typed，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
public interface ISaiRagChunkService extends IService<SaiRagChunk> {

    Long add(SaiRagChunk bo);

    Boolean edit(SaiRagChunk bo);

    Boolean remove(Collection<Long> ids);

    SaiRagChunk getInfo(Long id);

    List<SaiRagChunk> list(SaiRagChunk bo);

    PageResult<SaiRagChunk> page(SaiRagChunk bo);

    List<SaiRagChunk> listByFk(Long fkId);
}

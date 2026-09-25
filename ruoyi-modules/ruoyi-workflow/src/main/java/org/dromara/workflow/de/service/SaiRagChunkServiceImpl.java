package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRagChunk;
import org.dromara.workflow.de.mapper.SaiRagChunkMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_rag_chunk 服务实现（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL「4.3 RAG 分块表」）。
 *
 * <p>本表无唯一键（content_hash 仅为普通索引），故不做唯一校验；无逻辑删除列。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiRagChunkServiceImpl
        extends ServiceImpl<SaiRagChunkMapper, SaiRagChunk>
        implements ISaiRagChunkService {

    @Override
    public Long add(SaiRagChunk bo) {
        save(bo);
        log.info("[de.sai-rag-chunk] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiRagChunk bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiRagChunk getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiRagChunk> list(SaiRagChunk bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiRagChunk> page(SaiRagChunk bo) {
        var pg = page(new Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiRagChunk> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiRagChunk>()
            .eq(SaiRagChunk::getRagId, fkId)
            .orderByDesc(SaiRagChunk::getId));
    }

    private LambdaQueryWrapper<SaiRagChunk> buildWrapper(SaiRagChunk bo) {
        LambdaQueryWrapper<SaiRagChunk> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getRagId() != null) {
                qw.eq(SaiRagChunk::getRagId, bo.getRagId());
            }
            if (bo.getDocumentId() != null) {
                qw.eq(SaiRagChunk::getDocumentId, bo.getDocumentId());
            }
            if (bo.getSourceType() != null && !bo.getSourceType().isBlank()) {
                qw.eq(SaiRagChunk::getSourceType, bo.getSourceType());
            }
        }
        return qw.orderByDesc(SaiRagChunk::getId);
    }
}

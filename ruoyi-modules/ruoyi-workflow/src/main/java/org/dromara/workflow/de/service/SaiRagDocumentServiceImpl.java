package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRagDocument;
import org.dromara.workflow.de.mapper.SaiRagDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai-rag-document 服务实现（D-010 顺序推进批④实化：Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiRagDocumentServiceImpl
        extends ServiceImpl<SaiRagDocumentMapper, SaiRagDocument>
        implements ISaiRagDocumentService {

    @Override
    public Long add(SaiRagDocument bo) {
        save(bo);
        log.info("[de.sai-rag-document] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiRagDocument bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiRagDocument getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiRagDocument> list(SaiRagDocument bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiRagDocument> page(SaiRagDocument bo) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public PageResult<SaiRagDocument> page(SaiRagDocument bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageQuery.getPageNum(), pageQuery.getPageSize()), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiRagDocument> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiRagDocument>().eq(SaiRagDocument::getRagId, fkId));
    }

    private LambdaQueryWrapper<SaiRagDocument> buildWrapper(SaiRagDocument bo) {
        LambdaQueryWrapper<SaiRagDocument> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getRagId() != null) {
                qw.eq(SaiRagDocument::getRagId, bo.getRagId());
            }
            if (bo.getName() != null && !bo.getName().isBlank()) {
                qw.like(SaiRagDocument::getName, bo.getName());
            }
            if (bo.getStatus() != null) {
                qw.eq(SaiRagDocument::getStatus, bo.getStatus());
            }
        }
        return qw.orderByDesc(SaiRagDocument::getId);
    }
}

package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRagDocumentImage;
import org.dromara.workflow.de.mapper.SaiRagDocumentImageMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_rag_document_image 服务实现（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL「4.2 RAG 文档图片表」）。
 *
 * <p>本表无唯一键、无逻辑删除列。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiRagDocumentImageServiceImpl
        extends ServiceImpl<SaiRagDocumentImageMapper, SaiRagDocumentImage>
        implements ISaiRagDocumentImageService {

    @Override
    public Long add(SaiRagDocumentImage bo) {
        save(bo);
        log.info("[de.sai-rag-document-image] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiRagDocumentImage bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiRagDocumentImage getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiRagDocumentImage> list(SaiRagDocumentImage bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiRagDocumentImage> page(SaiRagDocumentImage bo) {
        var pg = page(new Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiRagDocumentImage> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiRagDocumentImage>()
            .eq(SaiRagDocumentImage::getRagId, fkId)
            .orderByDesc(SaiRagDocumentImage::getId));
    }

    private LambdaQueryWrapper<SaiRagDocumentImage> buildWrapper(SaiRagDocumentImage bo) {
        LambdaQueryWrapper<SaiRagDocumentImage> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getRagId() != null) {
                qw.eq(SaiRagDocumentImage::getRagId, bo.getRagId());
            }
            if (bo.getDocumentId() != null) {
                qw.eq(SaiRagDocumentImage::getDocumentId, bo.getDocumentId());
            }
        }
        return qw.orderByDesc(SaiRagDocumentImage::getId);
    }
}

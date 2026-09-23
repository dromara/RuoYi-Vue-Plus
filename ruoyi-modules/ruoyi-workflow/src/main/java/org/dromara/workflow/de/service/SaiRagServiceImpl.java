package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiRag;
import org.dromara.workflow.de.mapper.SaiRagMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai-rag 服务实现（D-010 顺序推进批④实化：Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiRagServiceImpl
        extends ServiceImpl<SaiRagMapper, SaiRag>
        implements ISaiRagService {

    @Override
    public Long add(SaiRag bo) {
        save(bo);
        log.info("[de.sai-rag] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiRag bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiRag getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiRag> list(SaiRag bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiRag> page(SaiRag bo) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public PageResult<SaiRag> page(SaiRag bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageQuery.getPageNum(), pageQuery.getPageSize()), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiRag> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiRag>().eq(SaiRag::getEmbeddingModelId, fkId));
    }

    private LambdaQueryWrapper<SaiRag> buildWrapper(SaiRag bo) {
        LambdaQueryWrapper<SaiRag> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getName() != null && !bo.getName().isBlank()) {
                qw.like(SaiRag::getName, bo.getName());
            }
        }
        return qw.orderByDesc(SaiRag::getId);
    }
}

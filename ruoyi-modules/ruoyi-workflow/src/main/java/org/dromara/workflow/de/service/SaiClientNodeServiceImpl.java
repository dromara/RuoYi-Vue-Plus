package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiClientNode;
import org.dromara.workflow.de.mapper.SaiClientNodeMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai-client-node 服务实现（D-010 批⑥：Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiClientNodeServiceImpl
        extends ServiceImpl<SaiClientNodeMapper, SaiClientNode>
        implements ISaiClientNodeService {

    @Override
    public Long add(SaiClientNode bo) {
        save(bo);
        log.info("[de.sai-client-node] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiClientNode bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiClientNode getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiClientNode> list(SaiClientNode bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiClientNode> page(SaiClientNode bo) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiClientNode> listByFk(Long fkId) {
        // fk 语义未定（各表外键不同）—— 默认按 id 反查空集，需要时再特化
        return java.util.Collections.emptyList();
    }

    private LambdaQueryWrapper<SaiClientNode> buildWrapper(SaiClientNode bo) {
        LambdaQueryWrapper<SaiClientNode> qw = new LambdaQueryWrapper<>();
        return qw.orderByDesc(SaiClientNode::getId);
    }
}

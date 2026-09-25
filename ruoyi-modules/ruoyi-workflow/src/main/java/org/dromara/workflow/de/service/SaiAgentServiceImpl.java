package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent 服务实现（D-010 实化：存根 → 真实 CRUD）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：{@link SaiAgent}（既有 domain POJO，24 列）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiAgentServiceImpl
        extends ServiceImpl<SaiAgentMapper, SaiAgent>
        implements ISaiAgentService {

    // ★ D-010 实化（2026-09-22 前端定制期 · owner 全量自决授权）：原 7 端点全为
    //   UnsupportedOperationException 存根——基座 ServiceImpl 的 CRUD 直接启用。

    @Override
    public Long add(SaiAgent bo) {
        save(bo);
        log.info("[de.sai-agent] add id={} name={}", bo.getId(), bo.getName());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiAgent bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiAgent getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiAgent> list(SaiAgent bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiAgent> page(SaiAgent bo) {
        // 分页参数经 ThreadLocal PageQuery 传递（controller 侧 PageQuery.startPage 同型仓内先例不足，
        // 故 bo-only 重载按默认 10 条返回首页 —— 实际分页走 controller 的 (bo, pageQuery) 重载）
        var page = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    public PageResult<SaiAgent> page(SaiAgent bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        var page = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageQuery.getPageNum(), pageQuery.getPageSize()), buildWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SaiAgent> listByFk(Long fkId) {
        // sai_agent 无外键维度 —— fkId 按创建人（creatorId）过滤
        return list(new LambdaQueryWrapper<SaiAgent>().eq(SaiAgent::getCreatorId, fkId));
    }

    private LambdaQueryWrapper<SaiAgent> buildWrapper(SaiAgent bo) {
        LambdaQueryWrapper<SaiAgent> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getName() != null && !bo.getName().isBlank()) {
                qw.like(SaiAgent::getName, bo.getName());
            }
            if (bo.getStatus() != null) {
                qw.eq(SaiAgent::getStatus, bo.getStatus());
            }
        }
        return qw.orderByDesc(SaiAgent::getId);
    }
}

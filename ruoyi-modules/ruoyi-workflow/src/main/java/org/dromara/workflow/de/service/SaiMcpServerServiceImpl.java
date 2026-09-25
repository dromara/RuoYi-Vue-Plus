package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiMcpServer;
import org.dromara.workflow.de.mapper.SaiMcpServerMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_mcp_server 服务实现（typed 存根 → 真 CRUD，列真源 ry_ai.sql DDL「5.1 MCP 服务配置表」）。
 *
 * <p>本表无唯一键（creator_id 仅为普通索引）、无逻辑删除列。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiMcpServerServiceImpl
        extends ServiceImpl<SaiMcpServerMapper, SaiMcpServer>
        implements ISaiMcpServerService {

    @Override
    public Long add(SaiMcpServer bo) {
        save(bo);
        log.info("[de.sai-mcp-server] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiMcpServer bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiMcpServer getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiMcpServer> list(SaiMcpServer bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiMcpServer> page(SaiMcpServer bo) {
        var pg = page(new Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiMcpServer> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiMcpServer>()
            .eq(SaiMcpServer::getCreatorId, fkId)
            .orderByDesc(SaiMcpServer::getId));
    }

    private LambdaQueryWrapper<SaiMcpServer> buildWrapper(SaiMcpServer bo) {
        LambdaQueryWrapper<SaiMcpServer> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getName() != null && !bo.getName().isBlank()) {
                qw.like(SaiMcpServer::getName, bo.getName());
            }
            if (bo.getTransportType() != null) {
                qw.eq(SaiMcpServer::getTransportType, bo.getTransportType());
            }
            if (bo.getCreatorId() != null) {
                qw.eq(SaiMcpServer::getCreatorId, bo.getCreatorId());
            }
        }
        return qw.orderByDesc(SaiMcpServer::getId);
    }
}

package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiAgentMcpServer;
import org.dromara.workflow.de.mapper.SaiAgentMcpServerMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_mcp_server 服务实现（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL「5.2 智能体与 MCP 服务关联表」）。
 *
 * <p>唯一键：{@code uk_agent_mcp(agent_id, mcp_server_id)}，新增 / 修改时校验（排除自身 id）；
 * 本表无更新时间列、无逻辑删除列。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiAgentMcpServerServiceImpl
        extends ServiceImpl<SaiAgentMcpServerMapper, SaiAgentMcpServer>
        implements ISaiAgentMcpServerService {

    @Override
    public Long add(SaiAgentMcpServer bo) {
        checkUnique(bo);
        save(bo);
        log.info("[de.sai-agent-mcp-server] add id={}", bo.getId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiAgentMcpServer bo) {
        checkUnique(bo);
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiAgentMcpServer getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiAgentMcpServer> list(SaiAgentMcpServer bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiAgentMcpServer> page(SaiAgentMcpServer bo) {
        var pg = page(new Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiAgentMcpServer> listByFk(Long fkId) {
        return list(new LambdaQueryWrapper<SaiAgentMcpServer>()
            .eq(SaiAgentMcpServer::getAgentId, fkId)
            .orderByDesc(SaiAgentMcpServer::getId));
    }

    /** 按 (agent_id, mcp_server_id) 唯一校验；修改时排除自身 id。 */
    private void checkUnique(SaiAgentMcpServer bo) {
        if (bo.getAgentId() != null && bo.getMcpServerId() != null) {
            boolean exists = exists(new LambdaQueryWrapper<SaiAgentMcpServer>()
                .eq(SaiAgentMcpServer::getAgentId, bo.getAgentId())
                .eq(SaiAgentMcpServer::getMcpServerId, bo.getMcpServerId())
                .ne(bo.getId() != null, SaiAgentMcpServer::getId, bo.getId()));
            if (exists) {
                throw new ServiceException("该智能体已绑定此 MCP 服务");
            }
        }
    }

    private LambdaQueryWrapper<SaiAgentMcpServer> buildWrapper(SaiAgentMcpServer bo) {
        LambdaQueryWrapper<SaiAgentMcpServer> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getAgentId() != null) {
                qw.eq(SaiAgentMcpServer::getAgentId, bo.getAgentId());
            }
            if (bo.getMcpServerId() != null) {
                qw.eq(SaiAgentMcpServer::getMcpServerId, bo.getMcpServerId());
            }
        }
        return qw.orderByDesc(SaiAgentMcpServer::getId);
    }
}

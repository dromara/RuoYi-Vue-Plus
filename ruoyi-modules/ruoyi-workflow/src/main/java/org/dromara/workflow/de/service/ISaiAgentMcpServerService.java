package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentMcpServer;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_mcp_server 服务接口（Map 存根 → typed，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
public interface ISaiAgentMcpServerService extends IService<SaiAgentMcpServer> {

    /** 新增（INSERT sai_agent_mcp_server），返回新主键 */
    Long add(SaiAgentMcpServer bo);

    /** 修改（UPDATE BY id） */
    Boolean edit(SaiAgentMcpServer bo);

    /** 删除（DELETE BY id 集合） */
    Boolean remove(Collection<Long> ids);

    /** 单条详情（SELECT BY id），未命中返回 null */
    SaiAgentMcpServer getInfo(Long id);

    /** 列表（无分页，按 BO 过滤） */
    List<SaiAgentMcpServer> list(SaiAgentMcpServer bo);

    /** 分页（按 BO 过滤） */
    PageResult<SaiAgentMcpServer> page(SaiAgentMcpServer bo);

    /** 按外键查询（按 agent_id） */
    List<SaiAgentMcpServer> listByFk(Long fkId);
}

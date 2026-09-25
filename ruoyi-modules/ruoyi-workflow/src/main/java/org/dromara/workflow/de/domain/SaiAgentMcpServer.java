package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_agent_mcp_server 实体（Map 型存根 domain 化——列真源 ry_ai.sql DDL「5.2 智能体与 MCP 服务关联表」）。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Data
@TableName("sai_agent_mcp_server")
public class SaiAgentMcpServer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 智能体ID */
    private Long agentId;

    /** MCP 服务ID */
    private Long mcpServerId;

    /** 创建时间（本表无更新时间列） */
    private Date createDt;
}

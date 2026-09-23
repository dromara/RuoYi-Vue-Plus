package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_mcp_server 实体（D-013 · 批⑥同模式 typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_mcp_server")
public class SaiMcpServer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 传输类型（整型枚举列，DDL 实测） */
    private Integer transportType;

    /** 基础 URI */
    private String baseUri;

    /** 端点 */
    private String endpoint;

    /** stdio 命令 */
    private String command;

    /** 参数 */
    private String args;

    /** 环境变量 */
    private String envVars;

    /** 超时（bigint 列，DDL 实测） */
    private Long timeout;

    /** 请求头 */
    private String headers;

    /** 最近连接时间 */
    private Date lastConnectDt;

    /** 创建者 */
    private Long creatorId;

    private Date createDt;

    private Date updateDt;
}

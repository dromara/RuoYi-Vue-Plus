package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_client_node 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_client_node")
public class SaiClientNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * app_id
     */
    private String appId;
    /**
     * host_id
     */
    private String hostId;
    /**
     * host_ip
     */
    private String hostIp;
    /**
     * grpc_port
     */
    private Integer grpcPort;
    /**
     * max_concurrent
     */
    private Integer maxConcurrent;
    /**
     * active_chats
     */
    private Integer activeChats;
    /**
     * supported_providers
     */
    private String supportedProviders;
    /**
     * labels
     */
    private String labels;
    /**
     * expire_dt
     */
    private Date expireDt;
    /**
     * create_dt
     */
    private Date createDt;
    /**
     * update_dt
     */
    private Date updateDt;
}

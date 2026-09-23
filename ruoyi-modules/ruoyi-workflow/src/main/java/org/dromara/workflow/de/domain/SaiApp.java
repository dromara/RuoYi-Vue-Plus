package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_app 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_app")
public class SaiApp implements Serializable {

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
     * app_name
     */
    private String appName;
    /**
     * description
     */
    private String description;
    /**
     * token
     */
    private String token;
    /**
     * route_strategy
     */
    private String routeStrategy;
    /**
     * status
     */
    private Integer status;
    /**
     * create_dt
     */
    private Date createDt;
    /**
     * update_dt
     */
    private Date updateDt;
}

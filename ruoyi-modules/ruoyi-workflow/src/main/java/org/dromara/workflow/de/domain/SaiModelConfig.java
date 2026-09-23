package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_model_config 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_model_config")
public class SaiModelConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * provider_id
     */
    private Long providerId;
    /**
     * model_name
     */
    private String modelName;
    /**
     * model_key
     */
    private String modelKey;
    /**
     * model_type
     */
    private String modelType;
    /**
     * adapter_key
     */
    private String adapterKey;
    /**
     * description
     */
    private String description;
    /**
     * api_key
     */
    private String apiKey;
    /**
     * api_endpoint
     */
    private String apiEndpoint;
    /**
     * config_json
     */
    private String configJson;
    /**
     * owner_id
     */
    private Long ownerId;
    /**
     * scope
     */
    private String scope;
    /**
     * is_default
     */
    private Integer isDefault;
    /**
     * is_enabled
     */
    private Integer isEnabled;
    /**
     * created_dt
     */
    private Date createdDt;
    /**
     * updated_dt
     */
    private Date updatedDt;
}

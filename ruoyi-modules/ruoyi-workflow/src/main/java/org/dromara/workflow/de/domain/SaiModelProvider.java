package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_model_provider 实体（Map 型存根 domain 化——列真源 ry_ai.sql DDL「2.1 AI 模型提供商表」）。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Data
@TableName("sai_model_provider")
public class SaiModelProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 提供商名称 */
    private String providerName;

    /** 提供商标识符 */
    private String providerKey;

    /** 提供商描述 */
    private String description;

    /** LOGO 图标URL */
    private String iconUrl;

    /** 是否启用 */
    private Integer isEnabled;

    /** 创建时间（DDL 列名为 created_dt） */
    private Date createdDt;

    /** 更新时间（DDL 列名为 updated_dt） */
    private Date updatedDt;
}

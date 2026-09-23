package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_resource 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_resource")
public class SaiResource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * storage_key
     */
    private String storageKey;
    /**
     * original_name
     */
    private String originalName;
    /**
     * file_size
     */
    private Long fileSize;
    /**
     * mime_type
     */
    private String mimeType;
    /**
     * storage_type
     */
    private String storageType;
    /**
     * access_url
     */
    private String accessUrl;
    /**
     * biz_type
     */
    private String bizType;
    /**
     * biz_id
     */
    private Long bizId;
    /**
     * creator_id
     */
    private Long creatorId;
    /**
     * create_dt
     */
    private Date createDt;
    /**
     * update_dt
     */
    private Date updateDt;
}

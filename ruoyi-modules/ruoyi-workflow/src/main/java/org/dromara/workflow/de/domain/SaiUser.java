package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_user 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_user")
public class SaiUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * role
     */
    private Integer role;
    /**
     * totals
     */
    private Integer totals;
    /**
     * username
     */
    private String username;
    /**
     * nickname
     */
    private String nickname;
    /**
     * email
     */
    private String email;
    /**
     * password
     */
    private String password;
    /**
     * resource_id
     */
    private Long resourceId;
    /**
     * create_dt
     */
    private Date createDt;
    /**
     * update_dt
     */
    private Date updateDt;
}

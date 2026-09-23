package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_object_acl 实体（D-011 · AU-03 方案 B + AU-04/05/06 四维）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_object_acl")
public class SaiObjectAcl implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 对象类型（conversation_record/rag_document 等） */
    private String objectType;

    /** 对象 ID */
    private Long objectId;

    /** user/role/dept */
    private String subjectType;

    private Long subjectId;

    /** read(T-3)/input(T-4)/publish(T-5) */
    private String grantType;

    /** 目的维（AU-04，空=不限） */
    private String purpose;

    /** 期限维（AU-05，空=永久） */
    private Date expiresAt;

    /** 策略版本维（AU-06） */
    private Integer policyVer;

    private Date createDt;

    private Date updateDt;
}

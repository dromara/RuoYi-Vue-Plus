package org.dromara.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例对象 act_hi_procinst
 *
 * @author may
 * @date 2023-07-22
 */
@Data
@TableName("act_hi_procinst")
public class ActHiProcinst implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId(value = "ID_")
    private String id;

    /**
     *
     */
    @TableField(value = "REV_")
    private Long rev;

    /**
     *
     */
    @TableField(value = "PROC_INST_ID_")
    private String procInstId;

    /**
     *
     */
    @TableField(value = "BUSINESS_KEY_")
    private String businessKey;

    /**
     *
     */
    @TableField(value = "PROC_DEF_ID_")
    private String procDefId;

    /**
     *
     */
    @TableField(value = "START_TIME_")
    private Date startTime;

    /**
     *
     */
    @TableField(value = "END_TIME_")
    private Date endTime;

    /**
     *
     */
    @TableField(value = "DURATION_")
    private Long duration;

    /**
     *
     */
    @TableField(value = "START_USER_ID_")
    private String startUserId;

    /**
     *
     */
    @TableField(value = "START_ACT_ID_")
    private String startActId;

    /**
     *
     */
    @TableField(value = "END_ACT_ID_")
    private String endActId;

    /**
     *
     */
    @TableField(value = "SUPER_PROCESS_INSTANCE_ID_")
    private String superProcessInstanceId;

    /**
     *
     */
    @TableField(value = "DELETE_REASON_")
    private String deleteReason;

    /**
     *
     */
    @TableField(value = "TENANT_ID_")
    private String tenantId;

    /**
     *
     */
    @TableField(value = "NAME_")
    private String name;

    /**
     *
     */
    @TableField(value = "CALLBACK_ID_")
    private String callbackId;

    /**
     *
     */
    @TableField(value = "CALLBACK_TYPE_")
    private String callbackType;

    /**
     *
     */
    @TableField(value = "REFERENCE_ID_")
    private String referenceId;

    /**
     *
     */
    @TableField(value = "REFERENCE_TYPE_")
    private String referenceType;

    /**
     *
     */
    @TableField(value = "PROPAGATED_STAGE_INST_ID_")
    private String propagatedStageInstId;

    /**
     *
     */
    @TableField(value = "BUSINESS_STATUS_")
    private String businessStatus;


}

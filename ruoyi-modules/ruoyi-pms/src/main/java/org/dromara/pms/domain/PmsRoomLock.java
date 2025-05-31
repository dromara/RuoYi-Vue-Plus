package org.dromara.pms.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 房间锁定管理对象 pms_room_locks
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_room_locks")
public class PmsRoomLock extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 锁定记录唯一ID
     */
    @TableId(value = "lock_id")
    private Long lockId;

    /**
     * 部门ID (门店)
     */
    private Long deptId;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 锁定类型
     */
    private String lockType;

    /**
     * 锁定开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockStartTime;

    /**
     * 锁定结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockEndTime;

    /**
     * 锁定原因
     */
    private String lockReason;

    /**
     * 锁定状态
     */
    private String lockStatus;

    /**
     * 解锁时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date unlockTime;

    /**
     * 解锁操作人
     */
    private Long unlockBy;

    /**
     * 解锁原因
     */
    private String unlockReason;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

}

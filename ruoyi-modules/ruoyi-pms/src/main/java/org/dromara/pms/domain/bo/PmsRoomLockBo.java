package org.dromara.pms.domain.bo;

import org.dromara.pms.domain.PmsRoomLock;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 房间锁定管理业务对象 pms_room_locks
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsRoomLock.class, reverseConvertGenerate = false)
public class PmsRoomLockBo extends BaseEntity {

    /**
     * 锁定记录唯一ID
     */
    @NotNull(message = "锁定记录唯一ID不能为空", groups = { EditGroup.class })
    private Long lockId;

    /**
     * 部门ID (门店)
     */
    @NotNull(message = "部门ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long deptId;

    /**
     * 房间ID
     */
    @NotNull(message = "房间ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long roomId;

    /**
     * 锁定类型
     */
    @NotBlank(message = "锁定类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String lockType;

    /**
     * 锁定开始时间
     */
    @NotNull(message = "锁定开始时间不能为空", groups = { AddGroup.class, EditGroup.class })
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
    @NotBlank(message = "锁定原因不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 500, message = "锁定原因长度不能超过500个字符")
    private String lockReason;

    /**
     * 锁定状态
     */
    @NotBlank(message = "锁定状态不能为空", groups = { AddGroup.class, EditGroup.class })
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
    @Size(max = 500, message = "解锁原因长度不能超过500个字符")
    private String unlockReason;

}

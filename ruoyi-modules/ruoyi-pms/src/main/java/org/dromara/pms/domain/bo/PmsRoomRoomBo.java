package org.dromara.pms.domain.bo;

import org.dromara.pms.domain.PmsRoomRoom;
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
 * 房间管理业务对象 pms_room_rooms
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsRoomRoom.class, reverseConvertGenerate = false)
public class PmsRoomRoomBo extends BaseEntity {

    /**
     * 房间唯一ID
     */
    @NotNull(message = "房间唯一ID不能为空", groups = { EditGroup.class })
    private Long roomId;

    /**
     * 部门ID (门店)
     */
    @NotNull(message = "部门ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long deptId;

    /**
     * 房型ID
     */
    @NotNull(message = "房型ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long roomTypeId;

    /**
     * 房间号
     */
    @NotBlank(message = "房间号不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 50, message = "房间号长度不能超过50个字符")
    private String roomNumber;

    /**
     * 楼层
     */
    @Size(max = 20, message = "楼层长度不能超过20个字符")
    private String floor;

    /**
     * 房间物理状态
     */
    @NotBlank(message = "房间物理状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String roomStatus;

    /**
     * 清洁状态
     */
    @NotBlank(message = "清洁状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String cleaningStatus;

    /**
     * 房间描述
     */
    @Size(max = 500, message = "房间描述长度不能超过500个字符")
    private String description;

    /**
     * 房间特殊设施
     */
    @Size(max = 1000, message = "房间特殊设施长度不能超过1000个字符")
    private String specialAmenities;

    /**
     * 最后清洁时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCleaningTime;

    /**
     * 最后维护时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMaintenanceTime;

    /**
     * 房间状态备注
     */
    @Size(max = 500, message = "房间状态备注长度不能超过500个字符")
    private String statusRemarks;

    /**
     * 排序值
     */
    private Integer sortOrder;

}

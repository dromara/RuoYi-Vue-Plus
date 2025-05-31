package org.dromara.pms.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 房间管理对象 pms_room_rooms
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_room_rooms")
public class PmsRoomRoom extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房间唯一ID
     */
    @TableId(value = "room_id")
    private Long roomId;

    /**
     * 部门ID (门店)
     */
    private Long deptId;

    /**
     * 房型ID
     */
    private Long roomTypeId;

    /**
     * 房间号
     */
    private String roomNumber;

    /**
     * 楼层
     */
    private String floor;

    /**
     * 房间物理状态
     */
    private String roomStatus;

    /**
     * 清洁状态
     */
    private String cleaningStatus;

    /**
     * 房间描述
     */
    private String description;

    /**
     * 房间特殊设施
     */
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
    private String statusRemarks;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

}

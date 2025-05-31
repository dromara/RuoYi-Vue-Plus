package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsRoomRoom;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;

/**
 * 房间管理视图对象 pms_room_rooms
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsRoomRoom.class)
public class PmsRoomRoomVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房间唯一ID
     */
    @ExcelProperty(value = "房间唯一ID")
    private Long roomId;

    /**
     * 部门ID (门店)
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 部门名称
     */
    @ExcelProperty(value = "门店名称")
    private String deptName;

    /**
     * 房型ID
     */
    @ExcelProperty(value = "房型ID")
    private Long roomTypeId;

    /**
     * 房型名称
     */
    @ExcelProperty(value = "房型名称")
    private String typeName;

    /**
     * 房间号
     */
    @ExcelProperty(value = "房间号")
    private String roomNumber;

    /**
     * 楼层
     */
    @ExcelProperty(value = "楼层")
    private String floor;

    /**
     * 房间物理状态
     */
    @ExcelProperty(value = "房间物理状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_room_status")
    private String roomStatus;

    /**
     * 清洁状态
     */
    @ExcelProperty(value = "清洁状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_cleaning_status")
    private String cleaningStatus;

    /**
     * 房间描述
     */
    @ExcelProperty(value = "房间描述")
    private String description;

    /**
     * 房间特殊设施
     */
    @ExcelProperty(value = "房间特殊设施")
    private String specialAmenities;

    /**
     * 最后清洁时间
     */
    @ExcelProperty(value = "最后清洁时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCleaningTime;

    /**
     * 最后维护时间
     */
    @ExcelProperty(value = "最后维护时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMaintenanceTime;

    /**
     * 房间状态备注
     */
    @ExcelProperty(value = "房间状态备注")
    private String statusRemarks;

    /**
     * 排序值
     */
    @ExcelProperty(value = "排序值")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}

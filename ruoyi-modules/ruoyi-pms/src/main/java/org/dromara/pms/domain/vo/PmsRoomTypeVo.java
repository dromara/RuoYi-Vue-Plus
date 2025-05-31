package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsRoomType;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.math.BigDecimal;

import java.io.Serial;
import java.io.Serializable;

/**
 * 房型管理视图对象 pms_room_types
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsRoomType.class)
public class PmsRoomTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房型唯一ID
     */
    @ExcelProperty(value = "房型唯一ID")
    private Long roomTypeId;

    /**
     * 部门ID (门店)
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 房型名称
     */
    @ExcelProperty(value = "房型名称")
    private String typeName;

    /**
     * 房型代码
     */
    @ExcelProperty(value = "房型代码")
    private String typeCode;

    /**
     * 房型描述
     */
    @ExcelProperty(value = "房型描述")
    private String description;

    /**
     * 标准入住人数
     */
    @ExcelProperty(value = "标准入住人数")
    private Integer standardOccupancy;

    /**
     * 最大入住人数
     */
    @ExcelProperty(value = "最大入住人数")
    private Integer maxOccupancy;

    /**
     * 房间面积(平方米)
     */
    @ExcelProperty(value = "房间面积")
    private BigDecimal roomArea;

    /**
     * 床型配置
     */
    @ExcelProperty(value = "床型配置")
    private String bedConfiguration;

    /**
     * 房间设施
     */
    @ExcelProperty(value = "房间设施")
    private String amenities;

    /**
     * 默认价格
     */
    @ExcelProperty(value = "默认价格")
    private BigDecimal defaultPrice;

    /**
     * 房型状态
     */
    @ExcelProperty(value = "房型状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_room_type_status")
    private String status;

    /**
     * 排序值
     */
    @ExcelProperty(value = "排序值")
    private Integer sortOrder;

    /**
     * 房型图片
     */
    @ExcelProperty(value = "房型图片")
    private String images;

    /**
     * 部门名称（关联查询）
     */
    private String deptName;

    /**
     * 房间数量（统计字段）
     */
    private Integer roomCount;

}

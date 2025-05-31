package org.dromara.pms.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;

/**
 * 房型管理对象 pms_room_types
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_room_types")
public class PmsRoomType extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房型唯一ID
     */
    @TableId(value = "room_type_id")
    private Long roomTypeId;

    /**
     * 部门ID (门店)
     */
    private Long deptId;

    /**
     * 房型名称
     */
    private String typeName;

    /**
     * 房型代码
     */
    private String typeCode;

    /**
     * 房型描述
     */
    private String description;

    /**
     * 标准入住人数
     */
    private Integer standardOccupancy;

    /**
     * 最大入住人数
     */
    private Integer maxOccupancy;

    /**
     * 房间面积(平方米)
     */
    private BigDecimal roomArea;

    /**
     * 床型配置
     */
    private String bedConfiguration;

    /**
     * 房间设施
     */
    private String amenities;

    /**
     * 默认价格
     */
    private BigDecimal defaultPrice;

    /**
     * 房型状态
     */
    private String status;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 房型图片
     */
    private String images;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

}

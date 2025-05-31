package org.dromara.pms.domain.bo;

import org.dromara.pms.domain.PmsRoomType;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 房型管理业务对象 pms_room_types
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsRoomType.class, reverseConvertGenerate = false)
public class PmsRoomTypeBo extends BaseEntity {

    /**
     * 房型唯一ID
     */
    @NotNull(message = "房型唯一ID不能为空", groups = { EditGroup.class })
    private Long roomTypeId;

    /**
     * 部门ID (门店)
     */
    private Long deptId;

    /**
     * 房型名称
     */
    @NotBlank(message = "房型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "房型名称长度不能超过100个字符")
    private String typeName;

    /**
     * 房型代码
     */
    @NotBlank(message = "房型代码不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 50, message = "房型代码长度不能超过50个字符")
    private String typeCode;

    /**
     * 房型描述
     */
    @Size(max = 500, message = "房型描述长度不能超过500个字符")
    private String description;

    /**
     * 标准入住人数
     */
    @NotNull(message = "标准入住人数不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 1, message = "标准入住人数不能小于1")
    @Max(value = 20, message = "标准入住人数不能大于20")
    private Integer standardOccupancy;

    /**
     * 最大入住人数
     */
    @NotNull(message = "最大入住人数不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 1, message = "最大入住人数不能小于1")
    @Max(value = 20, message = "最大入住人数不能大于20")
    private Integer maxOccupancy;

    /**
     * 房间面积(平方米)
     */
    @DecimalMin(value = "0.01", message = "房间面积必须大于0")
    @DecimalMax(value = "9999.99", message = "房间面积不能超过9999.99平方米")
    private BigDecimal roomArea;

    /**
     * 床型配置
     */
    @Size(max = 200, message = "床型配置长度不能超过200个字符")
    private String bedConfiguration;

    /**
     * 房间设施
     */
    @Size(max = 1000, message = "房间设施长度不能超过1000个字符")
    private String amenities;

    /**
     * 默认价格
     */
    @NotNull(message = "默认价格不能为空", groups = { AddGroup.class, EditGroup.class })
    @DecimalMin(value = "0.01", message = "默认价格必须大于0")
    @DecimalMax(value = "99999.99", message = "默认价格不能超过99999.99")
    private BigDecimal defaultPrice;

    /**
     * 房型状态
     */
    @NotBlank(message = "房型状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 排序值
     */
    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder;

    /**
     * 房型图片
     */
    @Size(max = 2000, message = "房型图片URL长度不能超过2000个字符")
    private String images;

}

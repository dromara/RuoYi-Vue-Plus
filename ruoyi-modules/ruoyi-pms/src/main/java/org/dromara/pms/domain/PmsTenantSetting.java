package org.dromara.pms.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import jakarta.validation.constraints.*;

/**
 * 租户配置对象 pms_tenant_settings
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_tenant_settings")
public class PmsTenantSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(value = "setting_id", type = IdType.ASSIGN_ID)
    private Long settingId;

    /**
     * 租户ID
     */
    @NotBlank(message = "租户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tenantId;

    /**
     * 部门ID (门店)，NULL表示租户级配置
     */
    private Long deptId;

    /**
     * 配置分组
     */
    @NotBlank(message = "配置分组不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 50, message = "配置分组长度不能超过50个字符")
    private String settingGroup;

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "配置键长度不能超过100个字符")
    private String settingKey;

    /**
     * 配置值
     */
    @Size(max = 2000, message = "配置值长度不能超过2000个字符")
    private String settingValue;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "配置名称长度不能超过100个字符")
    private String settingName;

    /**
     * 配置描述
     */
    @Size(max = 500, message = "配置描述长度不能超过500个字符")
    private String settingDescription;

    /**
     * 配置类型
     */
    @NotBlank(message = "配置类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String settingType;

    /**
     * 是否敏感配置
     */
    @NotNull(message = "是否敏感配置不能为空", groups = { AddGroup.class, EditGroup.class })
    private Boolean isSensitive;

    /**
     * 是否可编辑
     */
    @NotNull(message = "是否可编辑不能为空", groups = { AddGroup.class, EditGroup.class })
    private Boolean isEditable;

    /**
     * 是否系统配置
     */
    @NotNull(message = "是否系统配置不能为空", groups = { AddGroup.class, EditGroup.class })
    private Boolean isSystem;

    /**
     * 排序值
     */
    @NotNull(message = "排序值不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;

    /**
     * 状态
     */
    @NotBlank(message = "状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    // 非数据库字段，用于前端展示和查询

    /**
     * 门店名称
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 配置类型文本
     */
    @TableField(exist = false)
    private String settingTypeText;

    /**
     * 状态文本
     */
    @TableField(exist = false)
    private String statusText;

    /**
     * 是否继承配置
     */
    @TableField(exist = false)
    private Boolean isInherited;

    /**
     * 父级配置值
     */
    @TableField(exist = false)
    private String parentValue;
}

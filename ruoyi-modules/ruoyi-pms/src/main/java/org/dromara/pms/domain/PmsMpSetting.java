package org.dromara.pms.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import jakarta.validation.constraints.*;

/**
 * 小程序配置对象 pms_mp_settings
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_mp_settings")
public class PmsMpSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    @TableId(value = "mp_setting_id", type = IdType.ASSIGN_ID)
    private Long mpSettingId;

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
     * 小程序AppID
     */
    @NotBlank(message = "小程序AppID不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 50, message = "小程序AppID长度不能超过50个字符")
    private String appId;

    /**
     * 小程序名称
     */
    @NotBlank(message = "小程序名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "小程序名称长度不能超过100个字符")
    private String appName;

    /**
     * 小程序密钥
     */
    @Size(max = 200, message = "小程序密钥长度不能超过200个字符")
    private String appSecret;

    /**
     * 主题配置
     */
    @Size(max = 2000, message = "主题配置长度不能超过2000个字符")
    private String themeConfig;

    /**
     * 功能开关配置
     */
    @Size(max = 2000, message = "功能开关配置长度不能超过2000个字符")
    private String featureConfig;

    /**
     * 支付配置
     */
    @Size(max = 2000, message = "支付配置长度不能超过2000个字符")
    private String paymentConfig;

    /**
     * 通知配置
     */
    @Size(max = 2000, message = "通知配置长度不能超过2000个字符")
    private String notificationConfig;

    /**
     * 自定义配置
     */
    @Size(max = 5000, message = "自定义配置长度不能超过5000个字符")
    private String customConfig;

    /**
     * 版本号
     */
    @Size(max = 20, message = "版本号长度不能超过20个字符")
    private String version;

    /**
     * 是否启用
     */
    @NotNull(message = "是否启用不能为空", groups = { AddGroup.class, EditGroup.class })
    private Boolean isEnabled;

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
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

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
     * 状态文本
     */
    @TableField(exist = false)
    private String statusText;

    /**
     * 是否继承配置
     */
    @TableField(exist = false)
    private Boolean isInherited;
}

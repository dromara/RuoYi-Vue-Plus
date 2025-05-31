package org.dromara.pms.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 用户设备对象 pms_tenant_user_devices
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_tenant_user_devices")
public class PmsTenantUserDevice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @TableId(value = "device_id", type = IdType.ASSIGN_ID)
    private Long deviceId;

    /**
     * 租户ID
     */
    @NotBlank(message = "租户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tenantId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 设备唯一标识
     */
    @NotBlank(message = "设备唯一标识不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "设备唯一标识长度不能超过100个字符")
    private String deviceUuid;

    /**
     * 设备名称
     */
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    private String deviceName;

    /**
     * 设备类型
     */
    @NotBlank(message = "设备类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String deviceType;

    /**
     * 设备型号
     */
    @Size(max = 100, message = "设备型号长度不能超过100个字符")
    private String deviceModel;

    /**
     * 操作系统
     */
    @Size(max = 50, message = "操作系统长度不能超过50个字符")
    private String osName;

    /**
     * 操作系统版本
     */
    @Size(max = 50, message = "操作系统版本长度不能超过50个字符")
    private String osVersion;

    /**
     * 应用版本
     */
    @Size(max = 20, message = "应用版本长度不能超过20个字符")
    private String appVersion;

    /**
     * 推送令牌
     */
    @Size(max = 200, message = "推送令牌长度不能超过200个字符")
    private String pushToken;

    /**
     * 设备状态
     */
    @NotBlank(message = "设备状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String deviceStatus;

    /**
     * 是否在线
     */
    @NotNull(message = "是否在线不能为空", groups = { AddGroup.class, EditGroup.class })
    private Boolean isOnline;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 注册时间
     */
    private LocalDateTime registerTime;

    /**
     * 登录次数
     */
    @NotNull(message = "登录次数不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 0, message = "登录次数不能小于0")
    private Integer loginCount;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Size(max = 50, message = "最后登录IP长度不能超过50个字符")
    private String lastLoginIp;

    /**
     * 设备信息
     */
    @Size(max = 1000, message = "设备信息长度不能超过1000个字符")
    private String deviceInfo;

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
     * 用户名
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 用户昵称
     */
    @TableField(exist = false)
    private String nickName;

    /**
     * 设备类型文本
     */
    @TableField(exist = false)
    private String deviceTypeText;

    /**
     * 设备状态文本
     */
    @TableField(exist = false)
    private String deviceStatusText;

    /**
     * 在线状态文本
     */
    @TableField(exist = false)
    private String onlineStatusText;
}

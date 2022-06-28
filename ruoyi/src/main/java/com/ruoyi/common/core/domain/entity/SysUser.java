package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.enums.SensitiveStrategy;
import com.ruoyi.common.xss.Xss;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

/**
 * 用户对象 sys_user
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@ApiModel("用户信息业务对象")
public class SysUser extends BaseEntity {

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    @TableId(value = "user_id")
    private Long userId;

    /**
     * 部门ID
     */
    @ApiModelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 用户账号
     */
    @ApiModelProperty(value = "用户账号")
    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
    private String userName;

    /**
     * 用户昵称
     */
    @ApiModelProperty(value = "用户昵称")
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    private String nickName;

    /**
     * 用户类型（sys_user系统用户）
     */
    @ApiModelProperty(value = "用户类型")
    private String userType;

    /**
     * 用户邮箱
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    @ApiModelProperty(value = "用户邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /**
     * 手机号码
     */
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ApiModelProperty(value = "手机号码")
    private String phonenumber;

    /**
     * 用户性别
     */
    @ApiModelProperty(value = "用户性别")
    private String sex;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "用户头像")
    private String avatar;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码")
    @TableField(
        insertStrategy = FieldStrategy.NOT_EMPTY,
        updateStrategy = FieldStrategy.NOT_EMPTY,
        whereStrategy = FieldStrategy.NOT_EMPTY
    )
    private String password;

    /**
     * 帐号状态（0正常 1停用）
     */
    @ApiModelProperty(value = "帐号状态（0正常 1停用）")
    private String status;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @ApiModelProperty(value = "删除标志（0代表存在 2代表删除）")
    @TableLogic
    private String delFlag;

    /**
     * 最后登录IP
     */
    @ApiModelProperty(value = "最后登录IP")
    private String loginIp;

    /**
     * 最后登录时间
     */
    @ApiModelProperty(value = "最后登录时间")
    private Date loginDate;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 部门对象
     */
    @ApiModelProperty(value = "部门对象")
    @TableField(exist = false)
    private SysDept dept;

    /**
     * 角色对象
     */
    @ApiModelProperty(value = "角色对象")
    @TableField(exist = false)
    private List<SysRole> roles;

    /**
     * 角色组
     */
    @ApiModelProperty(value = "角色组")
    @TableField(exist = false)
    private Long[] roleIds;

    /**
     * 岗位组
     */
    @ApiModelProperty(value = "岗位组")
    @TableField(exist = false)
    private Long[] postIds;

    /**
     * 数据权限 当前角色ID
     */
    @ApiModelProperty(value = "角色ID")
    @TableField(exist = false)
    private Long roleId;

    public SysUser(Long userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "是否管理员")
    public boolean isAdmin() {
        return UserConstants.ADMIN_ID.equals(this.userId);
    }

}

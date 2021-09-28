package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户对象 sys_user
 *
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_user")
public class SysUser implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId(value = "user_id", type = IdType.AUTO)
	private Long userId;

	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 用户账号
	 */
	@NotBlank(message = "用户账号不能为空")
	@Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
	private String userName;

	/**
	 * 用户昵称
	 */
	@Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
	private String nickName;

	/**
	 * 用户邮箱
	 */
	@Email(message = "邮箱格式不正确")
	@Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
	private String email;

	/**
	 * 手机号码
	 */
	private String phonenumber;

	/**
	 * 用户性别
	 */
	private String sex;

	/**
	 * 用户头像
	 */
	private String avatar;

	/**
	 * 密码
	 */
	@TableField(
			insertStrategy = FieldStrategy.NOT_EMPTY,
			updateStrategy = FieldStrategy.NOT_EMPTY,
			whereStrategy = FieldStrategy.NOT_EMPTY
	)
	private String password;

	@JsonIgnore
	@JsonProperty
	public String getPassword() {
		return password;
	}

	/**
	 * 帐号状态（0正常 1停用）
	 */
	private String status;

	/**
	 * 删除标志（0代表存在 2代表删除）
	 */
	@TableLogic
	private String delFlag;

	/**
	 * 最后登录IP
	 */
	private String loginIp;

	/**
	 * 最后登录时间
	 */
	private Date loginDate;

	/**
	 * 创建者
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	private Date createTime;

	/**
	 * 更新者
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Date updateTime;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 请求参数
	 */
	@TableField(exist = false)
	private Map<String, Object> params = new HashMap<>();

	/**
	 * 部门对象
	 */
	@TableField(exist = false)
	private SysDept dept;

	/**
	 * 角色对象
	 */
	@TableField(exist = false)
	private List<SysRole> roles;

	/**
	 * 角色组
	 */
	@TableField(exist = false)
	private Long[] roleIds;

	/**
	 * 岗位组
	 */
	@TableField(exist = false)
	private Long[] postIds;

	/**
	 * 角色ID
	 */
	@TableField(exist = false)
	private Long roleId;

	public SysUser(Long userId) {
		this.userId = userId;
	}

	public boolean isAdmin() {
		return isAdmin(this.userId);
	}

	public static boolean isAdmin(Long userId) {
		return userId != null && 1L == userId;
	}

}

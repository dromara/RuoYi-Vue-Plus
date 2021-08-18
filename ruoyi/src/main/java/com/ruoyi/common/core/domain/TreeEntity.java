package com.ruoyi.common.core.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree基类
 *
 * @author ruoyi
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TreeEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 父菜单名称
	 */
	@ApiModelProperty(value = "父菜单名称")
	private String parentName;

	/**
	 * 父菜单ID
	 */
	@ApiModelProperty(value = "父菜单ID")
	private Long parentId;

	/**
	 * 显示顺序
	 */
	@ApiModelProperty(value = "显示顺序")
	private Integer orderNum;

	/**
	 * 祖级列表
	 */
	@ApiModelProperty(value = "祖级列表")
	private String ancestors;

	/**
	 * 子部门
	 */
	@ApiModelProperty(value = "子部门")
	private List<?> children = new ArrayList<>();

}

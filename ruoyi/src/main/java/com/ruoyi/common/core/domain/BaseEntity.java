package com.ruoyi.common.core.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity基类
 *
 * @author Lion Li
 */

@Data
@Accessors(chain = true)
public class BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 搜索值
	 */
	@ApiModelProperty(value = "搜索值")
	@TableField(exist = false)
	private String searchValue;

	/**
	 * 创建者
	 */
	@ApiModelProperty(value = "创建者")
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 创建时间
	 */
	@ApiModelProperty(value = "创建时间")
	@TableField(fill = FieldFill.INSERT)
	private Date createTime;

	/**
	 * 更新者
	 */
	@ApiModelProperty(value = "更新者")
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

	/**
	 * 更新时间
	 */
	@ApiModelProperty(value = "更新时间")
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Date updateTime;

	/**
	 * 请求参数
	 */
	@ApiModelProperty(value = "请求参数")
	@TableField(exist = false)
	private Map<String, Object> params = new HashMap<>();

}

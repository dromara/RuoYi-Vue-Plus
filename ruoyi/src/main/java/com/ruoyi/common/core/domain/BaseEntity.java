package com.ruoyi.common.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity基类
 *
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 搜索值
	 */
	@ApiModelProperty(value = "搜索值")
	private String searchValue;

	/**
	 * 创建者
	 */
	@ApiModelProperty(value = "创建者")
	private String createBy;

	/**
	 * 创建时间
	 */
	@ApiModelProperty(value = "创建时间")
	private Date createTime;

	/**
	 * 更新者
	 */
	@ApiModelProperty(value = "更新者")
	private String updateBy;

	/**
	 * 更新时间
	 */
	@ApiModelProperty(value = "更新时间")
	private Date updateTime;

	/**
	 * 备注
	 */
	@ApiModelProperty(value = "备注")
	private String remark;

	/**
	 * 请求参数
	 */
	@JsonIgnore
	@ApiModelProperty(value = "请求参数")
	private Map<String, Object> params = new HashMap<>();

}

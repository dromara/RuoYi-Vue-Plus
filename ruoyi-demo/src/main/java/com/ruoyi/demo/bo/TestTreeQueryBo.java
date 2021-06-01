package com.ruoyi.demo.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 测试树表分页查询对象 test_tree
 *
 * @author Lion Li
 * @date 2021-05-30
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("测试树表分页查询对象")
public class TestTreeQueryBo extends BaseEntity {

	/** 分页大小 */
	@ApiModelProperty("分页大小")
	private Integer pageSize;
	/** 当前页数 */
	@ApiModelProperty("当前页数")
	private Integer pageNum;
	/** 排序列 */
	@ApiModelProperty("排序列")
	private String orderByColumn;
	/** 排序的方向desc或者asc */
	@ApiModelProperty(value = "排序的方向", example = "asc,desc")
	private String isAsc;


	/** 树节点名 */
	@ApiModelProperty("树节点名")
	private String treeName;

}

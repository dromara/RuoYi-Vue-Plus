package com.ruoyi.demo.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试单表分页查询对象 test_demo
 *
 * @author Lion Li
 * @date 2021-05-30
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("测试单表分页查询对象")
public class TestDemoQueryBo extends BaseEntity {

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


	/** key键 */
	@ApiModelProperty("key键")
	private String testKey;
	/** 值 */
	@ApiModelProperty("值")
	private String value;

}

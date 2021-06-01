package com.ruoyi.demo.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试分页查询对象 chkj_test
 *
 * @author Lion Li
 * @date 2021-05-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("测试分页查询对象")
public class ChkjTestQueryBo extends BaseEntity {

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
	/** 版本 */
	@ApiModelProperty("版本")
	private Long version;
	/** 删除标志 */
	@ApiModelProperty("删除标志")
	private Long deleted;
	/** 父id */
	@ApiModelProperty("父id")
	private Long parentId;
	/** 排序号 */
	@ApiModelProperty("排序号")
	private Long orderNum;

}

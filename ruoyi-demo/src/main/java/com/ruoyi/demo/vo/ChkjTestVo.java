package com.ruoyi.demo.vo;

import com.ruoyi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 测试视图对象 mall_package
 *
 * @author Lion Li
 * @date 2021-05-14
 */
@Data
@ApiModel("测试视图对象")
public class ChkjTestVo {
	private static final long serialVersionUID = 1L;

	/** 主键 */
	@ApiModelProperty("主键")
	private Long id;

	/** key键 */
	@Excel(name = "key键")
	@ApiModelProperty("key键")
	private String testKey;
	/** 值 */
	@Excel(name = "值")
	@ApiModelProperty("值")
	private String value;
	/** 版本 */
	@Excel(name = "版本")
	@ApiModelProperty("版本")
	private Long version;
	/** 创建时间 */
	@Excel(name = "创建时间" , width = 30, dateFormat = "yyyy-MM-dd")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty("创建时间")
	private Date createTime;
	/** 删除标志 */
	@Excel(name = "删除标志")
	@ApiModelProperty("删除标志")
	private Long deleted;
	/** 父id */
	@Excel(name = "父id")
	@ApiModelProperty("父id")
	private Long parentId;
	/** 排序号 */
	@Excel(name = "排序号")
	@ApiModelProperty("排序号")
	private Long orderNum;

}

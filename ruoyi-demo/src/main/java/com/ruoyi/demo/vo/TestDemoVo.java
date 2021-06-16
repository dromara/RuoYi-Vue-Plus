package com.ruoyi.demo.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;



/**
 * 测试单表视图对象 test_demo
 *
 * @author Lion Li
 * @date 2021-05-30
 */
@Data
@ApiModel("测试单表视图对象")
public class TestDemoVo {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 * 如果是自定义id 或者 雪花id
	 * 需要增加序列化为字符串注解 因为Long到前端会失真
	 */
	@ApiModelProperty("主键")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/** 部门id */
	@Excel(name = "部门id")
	@ApiModelProperty("部门id")
	private Long deptId;

	/** 用户id */
	@Excel(name = "用户id")
	@ApiModelProperty("用户id")
	private Long userId;

	/** 排序号 */
	@Excel(name = "排序号")
	@ApiModelProperty("排序号")
	private Long orderNum;

	/** key键 */
	@Excel(name = "key键")
	@ApiModelProperty("key键")
	private String testKey;

	/** 值 */
	@Excel(name = "值")
	@ApiModelProperty("值")
	private String value;

	/** 创建时间 */
	@Excel(name = "创建时间" , width = 30, dateFormat = "yyyy-MM-dd")
//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty("创建时间")
	private Date createTime;

	/** 创建人 */
	@Excel(name = "创建人")
	@ApiModelProperty("创建人")
	private String createBy;

	/** 更新时间 */
	@Excel(name = "更新时间" , width = 30, dateFormat = "yyyy-MM-dd")
//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty("更新时间")
	private Date updateTime;

	/** 更新人 */
	@Excel(name = "更新人")
	@ApiModelProperty("更新人")
	private String updateBy;


}

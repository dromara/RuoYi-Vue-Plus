package com.ruoyi.demo.domain.vo;

import com.ruoyi.common.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 测试单表视图对象 test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@ApiModel("测试单表视图对象")
public class TestDemoVo {

	private static final long serialVersionUID = 1L;

	/**
     *  主键
     */
	@ApiModelProperty("主键")
	private Long id;

    /**
     * 部门id
     */
	@Excel(name = "部门id")
	@ApiModelProperty("部门id")
	private Long deptId;

    /**
     * 用户id
     */
	@Excel(name = "用户id")
	@ApiModelProperty("用户id")
	private Long userId;

    /**
     * 排序号
     */
	@Excel(name = "排序号")
	@ApiModelProperty("排序号")
	private Long orderNum;

    /**
     * key键
     */
	@Excel(name = "key键")
	@ApiModelProperty("key键")
	private String testKey;

    /**
     * 值
     */
	@Excel(name = "值")
	@ApiModelProperty("值")
	private String value;

    /**
     * 创建时间
     */
	@Excel(name = "创建时间" , width = 30, dateFormat = "yyyy-MM-dd")
	@ApiModelProperty("创建时间")
	private Date createTime;

    /**
     * 创建人
     */
	@Excel(name = "创建人")
	@ApiModelProperty("创建人")
	private String createBy;

    /**
     * 更新时间
     */
	@Excel(name = "更新时间" , width = 30, dateFormat = "yyyy-MM-dd")
	@ApiModelProperty("更新时间")
	private Date updateTime;

    /**
     * 更新人
     */
	@Excel(name = "更新人")
	@ApiModelProperty("更新人")
	private String updateBy;


}

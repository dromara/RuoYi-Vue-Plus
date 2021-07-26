package com.ruoyi.demo.domain.vo;

import com.ruoyi.common.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 测试树表视图对象 test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@ApiModel("测试树表视图对象")
public class TestTreeVo {

	private static final long serialVersionUID = 1L;

	/**
     *  主键
     */
	@ApiModelProperty("主键")
	private Long id;

    /**
     * 父id
     */
	@Excel(name = "父id")
	@ApiModelProperty("父id")
	private Long parentId;

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
     * 树节点名
     */
	@Excel(name = "树节点名")
	@ApiModelProperty("树节点名")
	private String treeName;

    /**
     * 创建时间
     */
	@Excel(name = "创建时间" , width = 30, dateFormat = "yyyy-MM-dd")
	@ApiModelProperty("创建时间")
	private Date createTime;


}

package com.ruoyi.demo.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 测试单表业务对象 test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("测试单表业务对象")
public class TestDemoBo extends BaseEntity {

    /**
     * 主键
     */
	@ApiModelProperty("主键")
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 部门id
     */
	@ApiModelProperty("部门id")
    @NotNull(message = "部门id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long deptId;

    /**
     * 用户id
     */
	@ApiModelProperty("用户id")
    @NotNull(message = "用户id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 排序号
     */
	@ApiModelProperty("排序号")
    @NotNull(message = "排序号不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long orderNum;

    /**
     * key键
     */
	@ApiModelProperty("key键")
    @NotBlank(message = "key键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String testKey;

    /**
     * 值
     */
	@ApiModelProperty("值")
    @NotBlank(message = "值不能为空", groups = { AddGroup.class, EditGroup.class })
    private String value;


    /**
     * 分页大小
     */
    @ApiModelProperty("分页大小")
    private Integer pageSize;

    /**
     * 当前页数
     */
    @ApiModelProperty("当前页数")
    private Integer pageNum;

    /**
     * 排序列
     */
    @ApiModelProperty("排序列")
    private String orderByColumn;

    /**
     * 排序的方向desc或者asc
     */
    @ApiModelProperty(value = "排序的方向", example = "asc,desc")
    private String isAsc;

}

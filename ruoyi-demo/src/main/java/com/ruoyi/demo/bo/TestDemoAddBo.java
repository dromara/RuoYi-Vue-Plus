package com.ruoyi.demo.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;



/**
 * 测试单表添加对象 test_demo
 *
 * @author Lion Li
 * @date 2021-05-30
 */
@Data
@ApiModel("测试单表添加对象")
public class TestDemoAddBo {

    /** 部门id */
    @ApiModelProperty("部门id")
    private Long deptId;

    /** 用户id */
    @ApiModelProperty("用户id")
    private Long userId;

    /** 排序号 */
    @ApiModelProperty("排序号")
    private Long orderNum;

    /** key键 */
    @ApiModelProperty("key键")
    @NotBlank(message = "key键不能为空")
    private String testKey;

    /** 值 */
    @ApiModelProperty("值")
    @NotBlank(message = "值不能为空")
    private String value;

}

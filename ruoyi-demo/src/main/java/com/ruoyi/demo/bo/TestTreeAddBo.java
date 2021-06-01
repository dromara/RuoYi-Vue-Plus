package com.ruoyi.demo.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import javax.validation.constraints.*;



/**
 * 测试树表添加对象 test_tree
 *
 * @author Lion Li
 * @date 2021-05-30
 */
@Data
@ApiModel("测试树表添加对象")
public class TestTreeAddBo {

    /** 父id */
    @ApiModelProperty("父id")
    private Long parentId;

    /** 部门id */
    @ApiModelProperty("部门id")
    private Long deptId;

    /** 用户id */
    @ApiModelProperty("用户id")
    private Long userId;

    /** 树节点名 */
    @ApiModelProperty("树节点名")
    @NotBlank(message = "树节点名不能为空")
    private String treeName;

}

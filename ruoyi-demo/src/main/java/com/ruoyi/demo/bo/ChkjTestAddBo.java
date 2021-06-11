package com.ruoyi.demo.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;



/**
 * 测试添加对象 chkj_test
 *
 * @author Lion Li
 * @date 2021-05-14
 */
@Data
@ApiModel("测试添加对象")
public class ChkjTestAddBo {

    /** key键 */
    @ApiModelProperty("key键")
    @NotBlank(message = "key键不能为空")
    private String testKey;
    /** 值 */
    @ApiModelProperty("值")
    @NotBlank(message = "值不能为空")
    private String value;
    /** 版本 */
    @ApiModelProperty("版本")
    private Long version;
    /** 创建时间 */
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 删除标志 */
    @ApiModelProperty("删除标志")
    private Long deleted;
    /** 父id */
    @ApiModelProperty("父id")
    @NotNull(message = "父id不能为空")
    private Long parentId;
    /** 排序号 */
    @ApiModelProperty("排序号")
    @NotNull(message = "排序号不能为空")
    private Long orderNum;
}

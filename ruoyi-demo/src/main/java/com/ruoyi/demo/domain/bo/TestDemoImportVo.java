package com.ruoyi.demo.domain.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 测试单表业务对象 test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@Schema(name = "测试单表业务对象")
public class TestDemoImportVo {

    /**
     * 部门id
     */
    @Schema(name = "部门id")
    @NotNull(message = "部门id不能为空")
    @ExcelProperty(value = "部门id")
    private Long deptId;

    /**
     * 用户id
     */
    @Schema(name = "用户id")
    @NotNull(message = "用户id不能为空")
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 排序号
     */
    @Schema(name = "排序号")
    @NotNull(message = "排序号不能为空")
    @ExcelProperty(value = "排序号")
    private Long orderNum;

    /**
     * key键
     */
    @Schema(name = "key键")
    @NotBlank(message = "key键不能为空")
    @ExcelProperty(value = "key键")
    private String testKey;

    /**
     * 值
     */
    @Schema(name = "值")
    @NotBlank(message = "值不能为空")
    @ExcelProperty(value = "值")
    private String value;

}

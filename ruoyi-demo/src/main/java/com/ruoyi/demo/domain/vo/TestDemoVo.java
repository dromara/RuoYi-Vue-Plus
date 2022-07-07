package com.ruoyi.demo.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;


/**
 * 测试单表视图对象 test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@Schema(name = "测试单表视图对象")
@ExcelIgnoreUnannotated
public class TestDemoVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    @Schema(name = "主键")
    private Long id;

    /**
     * 部门id
     */
    @ExcelProperty(value = "部门id")
    @Schema(name = "部门id")
    private Long deptId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    @Schema(name = "用户id")
    private Long userId;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    @Schema(name = "排序号")
    private Integer orderNum;

    /**
     * key键
     */
    @ExcelProperty(value = "key键")
    @Schema(name = "key键")
    private String testKey;

    /**
     * 值
     */
    @ExcelProperty(value = "值")
    @Schema(name = "值")
    private String value;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    @Schema(name = "创建时间")
    private Date createTime;

    /**
     * 创建人
     */
    @ExcelProperty(value = "创建人")
    @Schema(name = "创建人")
    private String createBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    @Schema(name = "更新时间")
    private Date updateTime;

    /**
     * 更新人
     */
    @ExcelProperty(value = "更新人")
    @Schema(name = "更新人")
    private String updateBy;


}

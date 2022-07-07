package com.ruoyi.system.domain;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统访问记录表 sys_logininfor
 *
 * @author Lion Li
 */

@Data
@TableName("sys_logininfor")
@ExcelIgnoreUnannotated
@Schema(name = "系统访问记录业务对象")
public class SysLogininfor implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(name = "访问ID")
    @ExcelProperty(value = "序号")
    @TableId(value = "info_id")
    private Long infoId;

    /**
     * 用户账号
     */
    @Schema(name = "用户账号")
    @ExcelProperty(value = "用户账号")
    private String userName;

    /**
     * 登录状态 0成功 1失败
     */
    @Schema(name = "登录状态 0成功 1失败")
    @ExcelProperty(value = "登录状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private String status;

    /**
     * 登录IP地址
     */
    @Schema(name = "登录IP地址")
    @ExcelProperty(value = "登录地址")
    private String ipaddr;

    /**
     * 登录地点
     */
    @Schema(name = "登录地点")
    @ExcelProperty(value = "登录地点")
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @Schema(name = "浏览器类型")
    @ExcelProperty(value = "浏览器")
    private String browser;

    /**
     * 操作系统
     */
    @Schema(name = "操作系统")
    @ExcelProperty(value = "操作系统")
    private String os;

    /**
     * 提示消息
     */
    @Schema(name = "提示消息")
    @ExcelProperty(value = "提示消息")
    private String msg;

    /**
     * 访问时间
     */
    @Schema(name = "访问时间")
    @ExcelProperty(value = "访问时间")
    private Date loginTime;

    /**
     * 请求参数
     */
    @Schema(name = "请求参数")
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

}

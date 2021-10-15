package com.ruoyi.quartz.domain;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务调度日志表 sys_job_log
 *
 * @deprecated 3.5.0删除 迁移至xxl-job
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_job_log")
@ExcelIgnoreUnannotated
public class SysJobLog
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @ExcelProperty(value = "日志序号")
    @TableId(value = "job_log_id", type = IdType.AUTO)
    private Long jobLogId;

    /** 任务名称 */
    @ExcelProperty(value = "任务名称")
    private String jobName;

    /** 任务组名 */
    @ExcelProperty(value = "任务组名", converter = ExcelDictConvert.class)
	@ExcelDictFormat(dictType = "sys_job_group")
    private String jobGroup;

    /** 调用目标字符串 */
    @ExcelProperty(value = "调用目标字符串")
    private String invokeTarget;

    /** 日志信息 */
    @ExcelProperty(value = "日志信息")
    private String jobMessage;

    /** 执行状态（0正常 1失败） */
    @ExcelProperty(value = "执行状态", converter = ExcelDictConvert.class)
	@ExcelDictFormat(dictType = "sys_common_status")
    private String status;

    /** 异常信息 */
    @ExcelProperty(value = "异常信息")
    private String exceptionInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 请求参数
     */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

    /** 开始时间 */
    @TableField(exist = false)
    private Date startTime;

    /** 停止时间 */
    @TableField(exist = false)
    private Date stopTime;

}

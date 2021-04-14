package com.ruoyi.quartz.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务调度日志表 sys_job_log
 * 
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_job_log")
public class SysJobLog
{
    private static final long serialVersionUID = 1L;

    /** ID */
    @Excel(name = "日志序号")
    @TableId(value = "job_log_id", type = IdType.AUTO)
    private Long jobLogId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String jobName;

    /** 任务组名 */
    @Excel(name = "任务组名")
    private String jobGroup;

    /** 调用目标字符串 */
    @Excel(name = "调用目标字符串")
    private String invokeTarget;

    /** 日志信息 */
    @Excel(name = "日志信息")
    private String jobMessage;

    /** 执行状态（0正常 1失败） */
    @Excel(name = "执行状态", readConverterExp = "0=正常,1=失败")
    private String status;

    /** 异常信息 */
    @Excel(name = "异常信息")
    private String exceptionInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

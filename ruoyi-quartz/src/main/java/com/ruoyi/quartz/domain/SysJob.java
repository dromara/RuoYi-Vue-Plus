package com.ruoyi.quartz.domain;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.constant.ScheduleConstants;
import com.ruoyi.common.convert.ExcelDictConvert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.quartz.util.CronUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务调度表 sys_job
 *
 * @deprecated 3.5.0删除 迁移至xxl-job
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_job")
@ExcelIgnoreUnannotated
public class SysJob implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @ExcelProperty(value = "任务序号")
    @TableId(value = "job_id", type = IdType.AUTO)
    private Long jobId;

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(min = 0, max = 64, message = "任务名称不能超过64个字符")
    @ExcelProperty(value = "任务名称")
    private String jobName;

    /**
     * 任务组名
     */
	@ExcelProperty(value = "任务组名", converter = ExcelDictConvert.class)
	@ExcelDictFormat(dictType = "sys_job_group")
    private String jobGroup;

    /**
     * 调用目标字符串
     */
    @NotBlank(message = "调用目标字符串不能为空")
    @Size(min = 0, max = 500, message = "调用目标字符串长度不能超过500个字符")
    @ExcelProperty(value = "调用目标字符串")
    private String invokeTarget;

    /**
     * cron执行表达式
     */
    @NotBlank(message = "Cron执行表达式不能为空")
    @Size(min = 0, max = 255, message = "Cron执行表达式不能超过255个字符")
    @ExcelProperty(value = "执行表达式")
    private String cronExpression;

    /**
     * cron计划策略
     */
    @ExcelProperty(value = "计划策略 ", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=默认,1=立即触发执行,2=触发一次执行,3=不触发立即执行")
	private String misfirePolicy = ScheduleConstants.MISFIRE_DEFAULT;

    /**
     * 是否并发执行（0允许 1禁止）
     */
    @ExcelProperty(value = "并发执行", converter = ExcelDictConvert.class)
	@ExcelDictFormat(readConverterExp = "0=允许,1=禁止")
    private String concurrent;

    /**
     * 任务状态（0正常 1暂停）
     */
    @ExcelProperty(value = "任务状态", converter = ExcelDictConvert.class)
	@ExcelDictFormat(dictType = "sys_job_status")
    private String status;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 请求参数
     */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getNextValidTime() {
        if (StringUtils.isNotEmpty(cronExpression)) {
            return CronUtils.getNextExecution(cronExpression);
        }
        return null;
    }

}

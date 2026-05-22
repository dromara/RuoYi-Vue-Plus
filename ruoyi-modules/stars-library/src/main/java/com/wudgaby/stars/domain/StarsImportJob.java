package com.wudgaby.stars.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导入任务 stars_import_job
 */
@Data
@TableName("stars_import_job")
public class StarsImportJob implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * RuoYi 用户 ID
     */
    private Long userId;

    /**
     * 任务类型：self_sync | import_user
     */
    private String jobType;

    /**
     * 他人导入时的 GitHub username
     */
    private String sourceLogin;

    /**
     * 计划导入条数上限
     */
    private Integer importLimit;

    /**
     * 状态：pending | running | done | failed | partial
     */
    private String status;

    /**
     * 总条数
     */
    private Integer totalCount;

    /**
     * 已处理条数
     */
    private Integer processedCount;

    /**
     * 失败条数
     */
    private Integer failedCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

}

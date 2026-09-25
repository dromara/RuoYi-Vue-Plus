package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * {@code sj_job} 镜像实体（AT-01 自动化创建 · 2026-09-25）。
 *
 * <p><b>底座表</b>：{@code sj_job} —— DDL
 * {@code 04-backend-code/script/sql/ry_job.sql:279-318}。
 * 列名与该 DDL 逐字一致；插入时未设置的 NOT NULL 列依赖 DDL DEFAULT
 * （全局 {@code field-strategy: not_null}，application.yml:339）。
 *
 * <p>与 SnailJob server（独立微服务 {@code ruoyi-snailjob-server}）共用同库同表：
 * SnailJob server 数据源指向 {@code ry-vue}
 * （{@code ruoyi-extend/ruoyi-snailjob-server/src/main/resources/application-dev.yml}）。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Data
@TableName("sj_job")
public class DeSjJob implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sj_job.id（主键，DDL AUTO_INCREMENT） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** sj_job.namespace_id */
    private String namespaceId;

    /** sj_job.biz_id（同 namespace 内唯一，uk_sj_job_01） */
    private String bizId;

    /** sj_job.group_name */
    private String groupName;

    /** sj_job.job_name */
    private String jobName;

    /** sj_job.args_str */
    private String argsStr;

    /** sj_job.args_type */
    private Integer argsType;

    /** sj_job.next_trigger_at（epoch milli） */
    private Long nextTriggerAt;

    /** sj_job.job_status（0、关闭 1、开启） */
    private Integer jobStatus;

    /** sj_job.task_type（1、集群 2、广播 3、切片） */
    private Integer taskType;

    /** sj_job.route_key */
    private Integer routeKey;

    /** sj_job.executor_type */
    private Integer executorType;

    /** sj_job.executor_info */
    private String executorInfo;

    /** sj_job.trigger_type（1.CRON 2.固定时间） */
    private Integer triggerType;

    /** sj_job.trigger_interval */
    private String triggerInterval;

    /** sj_job.block_strategy（1、丢弃 2、覆盖 3、并行 4、恢复） */
    private Integer blockStrategy;

    /** sj_job.executor_timeout */
    private Integer executorTimeout;

    /** sj_job.max_retry_times */
    private Integer maxRetryTimes;

    /** sj_job.parallel_num */
    private Integer parallelNum;

    /** sj_job.retry_interval */
    private Integer retryInterval;

    /** sj_job.bucket_index */
    private Integer bucketIndex;

    /** sj_job.resident */
    private Integer resident;

    /** sj_job.notify_ids */
    private String notifyIds;

    /** sj_job.owner_id（负责人 id） */
    private Long ownerId;

    /** sj_job.labels */
    private String labels;

    /** sj_job.description */
    private String description;

    /** sj_job.ext_attrs */
    private String extAttrs;

    /** sj_job.deleted（逻辑删除） */
    private Integer deleted;

    /** sj_job.create_dt */
    private Date createDt;

    /** sj_job.update_dt */
    private Date updateDt;
}

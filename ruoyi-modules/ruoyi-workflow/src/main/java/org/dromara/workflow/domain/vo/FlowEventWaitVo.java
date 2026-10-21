package org.dromara.workflow.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务（事件等待记录）视图对象 flow_event_wait。
 * <p>字段来自引擎 flow_event_wait；flowName / businessId 由服务层按定义与实例主键装配，
 * eventTypeName / statusName 为展示文案。</p>
 *
 * @author may
 * @since 2026/10/21
 */
@Data
public class FlowEventWaitVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 流程实例id（定时启动事件为空）
     */
    private Long instanceId;

    /**
     * 业务流程主键
     */
    private String businessId;

    /**
     * 事件节点编码
     */
    private String nodeCode;

    /**
     * 事件类型（TIMER_START / TIMER_CATCH / TIMER_BOUNDARY / …）
     */
    private String eventType;

    /**
     * 事件类型文案
     */
    private String eventTypeName;

    /**
     * 事件名称（消息名 / 错误码）
     */
    private String eventName;

    /**
     * 边界事件关联的待办任务id
     */
    private Long attachedTaskId;

    /**
     * 定时触发策略（AFTER / FIXED / POINT_IN_TIME）
     */
    private String triggerType;

    /**
     * 触发间隔表达式
     */
    private String triggerInterval;

    /**
     * 下次触发时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextTriggerAt;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 已触发次数
     */
    private Integer repeatCount;

    /**
     * 最大触发次数
     */
    private Integer maxRepeatCount;

    /**
     * 等待状态（WAITING / RETRY / RUNNING / CANCELLED / DEAD / SUCCESS）
     */
    private String status;

    /**
     * 等待状态文案
     */
    private String statusName;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 下次重试时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date retryAt;

    /**
     * 载荷
     */
    private String payload;

    /**
     * 最近一次错误
     */
    private String lastError;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 定时引擎扫描开关是否开启（warm-flow.timer-enabled）。
     * 关闭时等待记录仍会生成但不会被自动触发，页面据此给出提醒。
     */
    private Boolean timerEnabled;
}

package org.dromara.workflow.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.util.Date;

/**
 * 任务视图
 *
 * @author may
 */
@Data
public class TaskVo {

    /**
     * 任务id
     */
    private String id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 负责此任务的人员的用户id
     */
    private String owner;

    /**
     * 办理人id
     */
    private Long assignee;

    /**
     * 办理人
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "assignee")
    private String assigneeName;


    /**
     * 流程实例id
     */
    private String processInstanceId;

    /**
     * 执行id
     */
    private String executionId;

    /**
     * 无用
     */
    private String taskDefinitionId;

    /**
     * 流程定义id
     */
    private String processDefinitionId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 节点id
     */
    private String taskDefinitionKey;

    /**
     * 任务截止日期
     */
    private Date dueDate;

    /**
     * 流程类别
     */
    private String category;

    /**
     * 父级任务id
     */
    private String parentTaskId;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 认领时间
     */
    private Date claimTime;

    /**
     * 流程状态
     */
    private String businessStatus;

    /**
     * 流程状态
     */
    private String businessStatusName;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程定义key
     */
    private String processDefinitionKey;

    /**
     * 参与者
     */
    private ParticipantVo participantVo;

    /**
     * 是否会签
     */
    private Boolean multiInstance;
}

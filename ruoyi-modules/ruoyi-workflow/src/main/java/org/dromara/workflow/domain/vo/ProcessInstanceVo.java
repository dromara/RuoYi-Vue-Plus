package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 流程实例视图
 *
 * @author may
 */
@Data
public class ProcessInstanceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    private String id;

    /**
     * 流程定义id
     */
    private String processDefinitionId;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程定义key
     */
    private String processDefinitionKey;

    /**
     * 流程定义版本
     */
    private Integer processDefinitionVersion;

    /**
     * 部署id
     */
    private String deploymentId;

    /**
     * 业务id
     */
    private String businessKey;

    /**
     * 是否挂起
     */
    private Boolean isSuspended;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 启动时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 启动人id
     */
    private String startUserId;

    /**
     * 流程状态
     */
    private String businessStatus;

    /**
     * 流程状态
     */
    private String businessStatusName;

    /**
     * 待办任务集合
     */
    private List<TaskVo> taskVoList;

    /**
     * 节点配置
     */
    private WfNodeConfigVo wfNodeConfigVo;
}

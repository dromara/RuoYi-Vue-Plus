package org.dromara.workflow.domain.vo;

import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 历史任务视图
 *
 * @author may
 */
@Data
public class FlowHisTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 删除标记
     */
    private String delFlag;

    /**
     * 对应flow_definition表的id
     */
    private Long definitionId;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 流程实例表id
     */
    private Long instanceId;

    /**
     * 任务表id
     */
    private Long taskId;

    /**
     * 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
     */
    private Integer cooperateType;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 开始节点编码
     */
    private String nodeCode;

    /**
     * 开始节点名称
     */
    private String nodeName;

    /**
     * 开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 目标节点编码
     */
    private String targetNodeCode;

    /**
     * 结束节点名称
     */
    private String targetNodeName;

    /**
     * 审批者
     */
    private String approver;

    /**
     * 协作人(只有转办、会签、票签、委派)
     */
    private String collaborator;

    /**
     * 权限标识 permissionFlag的list形式
     */
    private List<String> permissionList;

    /**
     * 跳转类型（PASS通过 REJECT退回 NONE无动作）
     */
    private String skipType;

    /**
     * 流程状态（1审批中 2 审批通过 9已退回 10失效）
     */
    private Integer flowStatus;

    /**
     * 审批意见
     */
    private String message;

    /**
     * 业务详情 存业务类的json
     */
    private String ext;

    /**
     * 创建者
     */
    private String createBy;


    /**
     * 审批表单是否自定义（Y是 2否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 审批者
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "approver")
    private String approverName;
}

package org.dromara.workflow.de.wd.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * WD-01 节点详情 VO —— 「普通员工从首页进入自己的节点」的读回形状。
 *
 * <p><b>落点真源</b>（全部为底座已建列，不新造表、不新造列）：
 * <ul>
 *   <li>{@code flow_task}：id · instance_id · definition_id · node_code · node_name ·
 *       node_type · flow_status · form_custom · form_path · create_time · update_time</li>
 *   <li>{@code flow_user}：type · processed_by · associated（本人办理权）</li>
 *   <li>{@code flow_instance}：flow_status · business_id · create_by（实例级状态）</li>
 * </ul>
 *
 * @author AIP
 */
@Data
public class DeWdNodeDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_task.id */
    private Long taskId;

    /** flow_task.instance_id */
    private Long instanceId;

    /** flow_task.definition_id */
    private Long definitionId;

    /** flow_task.node_code */
    private String nodeCode;

    /** flow_task.node_name */
    private String nodeName;

    /** flow_task.node_type（0开始 1中间 2结束 3互斥网关 4并行网关） */
    private Integer nodeType;

    /** flow_task.flow_status */
    private String flowStatus;

    /** flow_task.form_custom（Y/N） */
    private String formCustom;

    /** flow_task.form_path（节点表单路径；打开节点即渲染它） */
    private String formPath;

    /** flow_task.create_time */
    private Date createTime;

    /** flow_task.update_time */
    private Date updateTime;

    /** 本人在该任务上的办理权类型（flow_user.type：1审批人 2转办人 3委托人 4抄送人） */
    private String myAssigneeType;

    /** 办理权类型中文描述（TaskAssigneeType.description） */
    private String myAssigneeTypeDesc;

    /** 本人在该任务上的权限行标识（flow_user.processed_by） */
    private String processedBy;

    /** flow_instance.flow_status（实例级状态） */
    private String instanceFlowStatus;

    /** flow_instance.business_id（实例业务 id） */
    private String instanceBusinessId;

    /** flow_instance.create_by（发起人，varchar 列型） */
    private String instanceCreateBy;

    /**
     * 能否办理（是否显示「开始 / 提交」类动作按钮）。
     *
     * <p>判定 = 本人在该任务上有 1/2/3 类办理权（审批人 / 转办人 / 委托人）；
     * 4 类（抄送）只读 ⇒ false。超管 / 监控角色无本人权限行时为 null。
     */
    private Boolean canOperate;
}

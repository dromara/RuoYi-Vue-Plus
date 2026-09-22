package org.dromara.workflow.de.wd.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工作台任务项 VO（WD-03 / WD-05 / WD-06 共用形状）。
 *
 * @author AIP
 */
@Data
public class DeWdTaskListVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_task.id */
    private Long taskId;

    /** flow_instance.id */
    private Long instanceId;

    /** 节点名 */
    private String nodeName;

    /** 流程名 */
    private String flowName;

    /** 数字员工 step（pending / running / done / failed；来自 de.ctx.task.step） */
    private String agentStep;

    /** flow_task.flow_status */
    private String flowStatus;

    /** 创建时间 */
    private Date createTime;

    /** 需我处理分类：exception/help/review/resource-lost */
    private String actionCategory;
}
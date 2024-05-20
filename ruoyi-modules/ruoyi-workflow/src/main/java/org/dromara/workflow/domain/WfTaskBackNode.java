package org.dromara.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 节点驳回记录 wf_task_back_node
 *
 * @author may
 * @date 2024-03-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_task_back_node")
public class WfTaskBackNode extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 实例id
     */
    private String instanceId;

    /**
     * 节点id
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 排序
     */
    private Integer orderNo;

    /**
     * 节点类型
     */
    private String taskType;

    /**
     * 办理人
     */
    private String assignee;

}

package org.dromara.common.core.domain.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程办理监听
 *
 * @author may
 */

@Data
public class ProcessTaskEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义key
     */
    private String key;

    /**
     * 审批节点key
     */
    private String taskDefinitionKey;

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 业务id
     */
    private String businessKey;

}

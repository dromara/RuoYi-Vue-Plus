package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务请求对象
 *
 * @author may
 */
@Data
public class TaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程定义key
     */
    private String processDefinitionKey;
}

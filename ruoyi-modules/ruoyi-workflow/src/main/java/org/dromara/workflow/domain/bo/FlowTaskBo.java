package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.Future;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 任务请求对象
 *
 * @author may
 */
@Data
public class FlowTaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    private String nodeName;

    /**
     * 流程定义名称
     */
    private String flowName;

    /**
     * 流程定义编码
     */
    private String flowCode;

    private Long instanceId;

    private List<String> permissionList;
}

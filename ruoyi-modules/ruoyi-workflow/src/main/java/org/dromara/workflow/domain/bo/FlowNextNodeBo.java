package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 下一节点信息
 *
 * @author may
 */
@Data
public class FlowNextNodeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new HashMap<>(16);
            return variables;
        }
        variables.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return variables;
    }
}

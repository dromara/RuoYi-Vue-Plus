package org.dromara.workflow.domain.bo;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 启动流程对象
 *
 * @author may
 */
@Data
public class StartProcessBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务唯一值id
     */
    private String businessKey;

    /**
     * 流程执行key
     */
    private String processKey;

    /**
     * 流程变量，前端会提交一个元素{'entity': {业务详情数据对象}}
     */
    private Map<String, Object> variables;

    public Map<String, Object> getVariables() {
        if (variables == null) {
            return new HashMap<>(16);
        }
        variables.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return variables;
    }
}

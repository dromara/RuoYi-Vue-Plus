package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 任务催办
 *
 * @author may
 */
@Data
public class TaskUrgingBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    private String processInstanceId;

    /**
     * 消息类型
     */
    private List<String> messageType;

    /**
     * 催办内容（为空默认系统内置信息）
     */
    private String message;
}

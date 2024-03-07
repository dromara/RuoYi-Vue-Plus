package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程实例请求对象
 *
 * @author may
 */
@Data
public class ProcessInstanceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 流程key
     */
    private String key;

    /**
     * 任务发起人
     */
    private String startUserId;

    /**
     * 业务id
     */
    private String businessKey;

    /**
     * 模型分类
     */
    private String categoryCode;
}

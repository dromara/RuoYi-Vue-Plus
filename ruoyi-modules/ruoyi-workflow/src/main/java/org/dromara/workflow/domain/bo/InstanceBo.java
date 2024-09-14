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
public class InstanceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 流程编码
     */
    private String flowCode;

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

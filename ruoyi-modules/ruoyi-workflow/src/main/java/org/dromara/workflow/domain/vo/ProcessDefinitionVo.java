package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义视图
 *
 * @author may
 */
@Data
public class ProcessDefinitionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义id
     */
    private String id;

    /**
     * 流程定义名称
     */
    private String name;

    /**
     * 流程定义标识key
     */
    private String key;

    /**
     * 流程定义版本
     */
    private int version;

    /**
     * 流程定义挂起或激活 1激活 2挂起
     */
    private int suspensionState;

    /**
     * 流程xml名称
     */
    private String resourceName;

    /**
     * 流程图片名称
     */
    private String diagramResourceName;

    /**
     * 流程部署id
     */
    private String deploymentId;

    /**
     * 流程部署时间
     */
    private Date deploymentTime;

    /**
     * 流程定义配置
     */
    private WfDefinitionConfigVo wfDefinitionConfigVo;

}

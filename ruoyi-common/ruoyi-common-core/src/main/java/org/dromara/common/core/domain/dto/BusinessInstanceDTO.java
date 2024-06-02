package org.dromara.common.core.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 业务与流程实例关联对象
 *
 * @author may
 */
@Data
@NoArgsConstructor
public class BusinessInstanceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    private String id;

    /**
     * 流程定义id
     */
    private String processDefinitionId;

    /**
     * 流程定义名称
     */
    private String name;

    /**
     * 业务id
     */
    private String businessKey;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 启动时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 启动人id
     */
    private String startUserId;

    /**
     * 流程状态
     */
    private String businessStatus;

    /**
     * 流程状态
     */
    private String businessStatusName;
}

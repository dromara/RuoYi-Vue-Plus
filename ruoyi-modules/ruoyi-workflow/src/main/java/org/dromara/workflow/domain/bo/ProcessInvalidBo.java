package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程实例作废请求对象
 *
 * @author may
 */
@Data
public class ProcessInvalidBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    private String processInstId;

    /**
     * 作废原因
     */
    private String deleteReason;
}

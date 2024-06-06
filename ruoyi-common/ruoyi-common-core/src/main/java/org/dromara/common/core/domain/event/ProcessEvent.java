package org.dromara.common.core.domain.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 总体流程监听
 *
 * @author may
 */

@Data
public class ProcessEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义key
     */
    private String key;

    /**
     * 业务id
     */
    private String businessKey;

    /**
     * 状态
     */
    private String status;

    /**
     * 当为true时为申请人节点办理
     */
    private boolean submit;


}

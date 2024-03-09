package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程变量
 *
 * @author may
 */
@Data
public class VariableVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 变量key
     */
    private String key;

    /**
     * 变量值
     */
    private String value;
}

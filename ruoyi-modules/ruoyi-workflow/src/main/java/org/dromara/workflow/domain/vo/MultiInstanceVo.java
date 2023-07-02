package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 多实例信息
 *
 * @author may
 */
@Data
public class MultiInstanceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会签类型（串行，并行）
     */
    private Object type;

    /**
     * 会签人员KEY
     */
    private String assignee;

    /**
     * 会签人员集合KEY
     */
    private String assigneeList;
}

package org.dromara.workflow.domain.vo;

import com.warm.flow.orm.entity.FlowTask;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务视图
 *
 * @author may
 */
@Data
public class FlowTaskVo extends FlowTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 流程状态
     */
    private String flowStatus;
}

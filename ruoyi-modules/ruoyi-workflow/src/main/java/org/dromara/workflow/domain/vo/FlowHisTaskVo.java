package org.dromara.workflow.domain.vo;

import com.warm.flow.orm.entity.FlowHisTask;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 历史任务视图
 *
 * @author may
 */
@Data
public class FlowHisTaskVo extends FlowHisTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义编码
     */
    private String flowCode;
}

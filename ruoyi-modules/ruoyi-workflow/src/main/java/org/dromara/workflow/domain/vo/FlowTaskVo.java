package org.dromara.workflow.domain.vo;

import com.warm.flow.orm.entity.FlowTask;
import lombok.Data;
import org.dromara.common.core.domain.dto.UserDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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
    private Integer flowStatus;

    /**
     * 流程状态
     */
    private String flowStatusName;

    /**
     * 办理人
     */
    private List<UserDTO> userDTOList;
}

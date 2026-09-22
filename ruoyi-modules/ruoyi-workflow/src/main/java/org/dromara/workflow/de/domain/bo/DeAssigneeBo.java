package org.dromara.workflow.de.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 待办责任人绑定请求 BO（对应 POST /de/binding/assignee）。
 *
 * <p>落点：flow_user(type='1' 审批人, processed_by, associated=flow_task.id)。
 *
 * @author AIP
 */
@Data
public class DeAssigneeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance.id */
    @NotNull
    private Long instanceId;

    /** flow_task.id（落 flow_user.associated） */
    @NotNull
    private Long taskId;

    /** sys_user.user_id（落 flow_user.processed_by；varchar(80) 须显式转型） */
    @NotNull
    private Long userId;
}
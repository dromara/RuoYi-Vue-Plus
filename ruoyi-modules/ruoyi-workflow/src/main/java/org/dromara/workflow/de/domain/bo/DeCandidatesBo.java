package org.dromara.workflow.de.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 更新数字员工候选集请求 BO（对应 PUT /de/binding/candidates）。
 *
 * <p>candidates 是纯 int 数组（27 号 §5.2 真源）；列表元素约束为 sai_agent.id 的数值形式。
 *
 * @author AIP
 */
@Data
public class DeCandidatesBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance.id */
    @NotNull
    private Long instanceId;

    /** 数字员工 id 列表（纯 int） */
    @NotEmpty(message = "候选集不能为空")
    private List<Long> candidates;
}
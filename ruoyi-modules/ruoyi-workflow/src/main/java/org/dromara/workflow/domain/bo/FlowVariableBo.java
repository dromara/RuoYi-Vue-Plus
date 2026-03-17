package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import org.dromara.common.core.validate.AddGroup;

/**
 * 流程变量参数
 *
 * @param instanceId 流程实例 ID
 * @param key        变量键
 * @param value      变量值
 * @author may
 */
public record FlowVariableBo(
    @NotNull(message = "流程实例id为空", groups = AddGroup.class)
    Long instanceId,
    @NotNull(message = "流程变量key为空", groups = AddGroup.class)
    String key,
    @NotNull(message = "流程变量value为空", groups = AddGroup.class)
    String value
) {
}

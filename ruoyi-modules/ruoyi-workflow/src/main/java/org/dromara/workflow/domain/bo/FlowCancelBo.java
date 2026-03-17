package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import org.dromara.common.core.validate.AddGroup;

/**
 * 撤销流程请求对象。
 *
 * @param businessId 业务 ID
 * @param message    撤销说明
 * @author may
 */
public record FlowCancelBo(
    @NotBlank(message = "业务ID不能为空", groups = AddGroup.class)
    String businessId,
    String message
) {
}

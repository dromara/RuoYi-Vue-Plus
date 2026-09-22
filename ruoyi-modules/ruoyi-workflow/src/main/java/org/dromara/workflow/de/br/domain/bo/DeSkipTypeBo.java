package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-15 修改 / 删除 / 撤销 / 备份处理流程请求。
 *
 * <p>落点：{@code flow_skip.skip_type}（varchar(40)；物理 PASS / REJECT）。
 */
@Data
public class DeSkipTypeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_definition.id */
    @NotNull(message = "definitionId 不能为空")
    private Long definitionId;

    /** 候选 skip_type（应用层语义） */
    @NotBlank(message = "skipType 不能为空")
    private String skipType; // PASS | REJECT

    /** 操作类型 */
    @NotBlank(message = "operation 不能为空")
    private String operation; // MODIFY | DELETE | REVOKE | BACKUP

    /** 原因 / 备注（BACKUP / DELETE 必填） */
    private String reason;
}

package org.dromara.workflow.de.at.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * AT-01 自动化创建请求 BO。
 *
 * @author AIP
 */
@Data
public class DeAtCreateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 个人 / Agent / 节点 三选一 */
    @NotBlank
    private String scope;

    /** scope 对应 ID（userId / agentId / nodeId） */
    @NotNull
    private Long scopeId;

    /** cron 表达式 或 trigger 标识 */
    @NotBlank
    private String cron;

    /** 执行目标 nodeId 或 taskId */
    @NotNull
    private Long targetId;
}
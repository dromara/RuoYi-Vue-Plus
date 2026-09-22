package org.dromara.workflow.de.hr.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * HR-05 待办任务动作 BO（催办 / 延期 / 请人 / 退回补充 / 升级）。
 *
 * @author AIP
 */
@Data
public class DeHrTaskActionBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long instanceId;

    @NotNull
    private Long taskId;

    /** 动作类型：urge(催办) / delay(延期) / ask(请人) / reject(退回) / upgrade(升级) */
    @NotBlank
    private String action;

    /** 动作说明（落 flow_his_task.message） */
    private String message;

    /** 延期至何时（ISO 字符串；仅 delay 生效） */
    private String dueAt;

    /** 请人的目标 ID / 升级的目标岗位 ID */
    private Long targetId;
}
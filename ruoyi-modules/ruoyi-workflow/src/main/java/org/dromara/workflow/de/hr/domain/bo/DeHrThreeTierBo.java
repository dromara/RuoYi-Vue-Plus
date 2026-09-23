package org.dromara.workflow.de.hr.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * HR-04 三分位落点请求 BO（2026-09-23）。
 *
 * <p>落点：flow_user(type=1 审批人 / 2 转办人 / 3 委托人, processed_by, associated=taskId)
 * + flow_instance.variable.de.ctx.hr.three_tier JSON 分段。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeHrThreeTierBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例 ID（必填） */
    @NotNull
    private Long instanceId;

    /** 任务 ID（可选；指定后落 flow_user；为空则只解析 variable） */
    private Long taskId;

    /** 三分位 JSON 字符串（写实例 variable 时使用） */
    private String threeTierJson;

    /** 审批人 processed_by（type=1） */
    private String approver;

    /** 转办人 processed_by（type=2） */
    private String transferTo;

    /** 委托人 processed_by（type=3） */
    private String delegateTo;
}
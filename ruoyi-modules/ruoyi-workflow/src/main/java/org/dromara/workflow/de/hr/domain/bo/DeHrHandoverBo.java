package org.dromara.workflow.de.hr.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * HR-06 人员离岗 / 休假 / 停用 后任务与数字资产交接 BO。
 *
 * @author AIP
 */
@Data
public class DeHrHandoverBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 离岗 / 休假 / 停用 的人员 userId */
    @NotNull
    private Long fromUserId;

    /** 接收人 userId */
    @NotNull
    private Long toUserId;

    /** 受影响 task id 列表（落 flow_user.type='1' → '3' 委托，type='2' 转办） */
    private List<Long> taskIds;

    /** 受影响 agent id 列表（落 sai_user_agent 的接收人；本仓仅暴露入口，详见 N-05） */
    private List<Long> agentIds;

    /** 原因：leave / vacation / disable */
    private String reason;
}
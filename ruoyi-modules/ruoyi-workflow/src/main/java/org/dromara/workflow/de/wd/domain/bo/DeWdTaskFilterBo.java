package org.dromara.workflow.de.wd.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * WD-05 工作台任务筛选请求 BO。
 *
 * @author AIP
 */
@Data
public class DeWdTaskFilterBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前用户 ID（必填） */
    @NotNull
    private Long userId;

    /** 流程 ID 列表（可选，按 flow_definition.id 过滤） */
    private List<Long> flowIds;

    /** 项目 / 业务线 ID（落 flow_instance.business_id 字符串前缀） */
    private String projectKey;

    /** 期限（字符串形式 ISO 日期） */
    private String dueBefore;

    /** 责任人 ID */
    private Long assigneeId;

    /** 数字员工 ID */
    private Long agentId;
}
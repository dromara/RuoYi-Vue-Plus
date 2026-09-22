package org.dromara.workflow.de.ag.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AG-04 数字员工详情页 VO。
 *
 * @author AIP
 */
@Data
public class DeAgOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent.id */
    private Long agentId;

    /** sai_agent.name */
    private String name;

    /** sai_agent.description */
    private String description;

    /** sai_agent.avatar */
    private String avatar;

    /** sai_agent.instruction（System Prompt） */
    private String instruction;

    /** sai_agent.status（1 活跃 2 非活跃 3 已废弃 4 已禁用） */
    private Integer status;

    /** 能力数（skill 启用数） */
    private Integer capabilityCount;

    /** 任务总数（统计；本接口期仅作占位，count 留给后续 owner 裁 AU-13 后填真实值） */
    private Long taskTotal;

    /** 记忆启用？ */
    private Integer memoryEnabled;

    /** RAG 启用？ */
    private Integer ragEnabled;

    /** 更新时间 */
    private Date updateDt;

    /** 当前用户与该 Agent 的关系集合（AG-05） */
    private List<String> relations;
}
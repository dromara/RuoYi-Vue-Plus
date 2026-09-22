package org.dromara.workflow.de.sk.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * SK-02 技能卡 VO。
 *
 * @author AIP
 */
@Data
public class DeSkCardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_skill.id */
    private Long skillId;

    /** sai_skill.name */
    private String name;

    /** 适用任务 */
    private String applicableTasks;

    /** 输入 schema */
    private String inputSchema;

    /** 输出 schema */
    private String outputSchema;

    /** 依赖列表 */
    private List<String> dependencies;

    /** 版本号 */
    private String version;

    /** 维护者 userId */
    private Long maintainerId;

    /** 更新时间 */
    private Date updateDt;
}
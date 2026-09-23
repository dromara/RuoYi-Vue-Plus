package org.dromara.workflow.de.op.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * OP 域调度口径读 VO（OP-05 / OP-06：统计维度 + 实数据窗口）。
 *
 * @author AIP
 */
@Data
public class DeOpConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认统计窗口天数 */
    private Integer defaultWindowDays;

    /** 可用统计维度（sai_agent_usage_stat 的聚合键） */
    private List<String> metricDimensions;

    /** 表内总行数（空表 = 0） */
    private Long totalRows;

    /** 实数据窗口下界（min stat_date；空表 = null） */
    private Date minStatDate;

    /** 实数据窗口上界（max stat_date；空表 = null） */
    private Date maxStatDate;
}

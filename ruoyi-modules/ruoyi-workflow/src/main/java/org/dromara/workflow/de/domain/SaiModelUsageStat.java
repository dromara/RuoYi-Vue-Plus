package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_model_usage_stat 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_model_usage_stat")
public class SaiModelUsageStat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * model_id
     */
    private Long modelId;
    /**
     * user_id
     */
    private Long userId;
    /**
     * total_calls
     */
    private Long totalCalls;
    /**
     * success_calls
     */
    private Long successCalls;
    /**
     * failed_calls
     */
    private Long failedCalls;
    /**
     * total_tokens_used
     */
    private Long totalTokensUsed;
    /**
     * total_cost
     */
    private java.math.BigDecimal totalCost;
    /**
     * avg_response_time
     */
    private Long avgResponseTime;
    /**
     * last_used_dt
     */
    private Date lastUsedDt;
    /**
     * created_dt
     */
    private Date createdDt;
    /**
     * updated_dt
     */
    private Date updatedDt;
}

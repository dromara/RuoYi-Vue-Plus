package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_agent_usage_stat 实体（D-010 顺序推进批⑥ typed 化，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sai_agent_usage_stat")
public class SaiAgentUsageStat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * agent_id
     */
    private Long agentId;
    /**
     * user_id
     */
    private Long userId;
    /**
     * message_count
     */
    private Integer messageCount;
    /**
     * conversation_count
     */
    private Integer conversationCount;
    /**
     * stat_date
     */
    private Date statDate;
    /**
     * create_dt
     */
    private Date createDt;
    /**
     * update_dt
     */
    private Date updateDt;
}

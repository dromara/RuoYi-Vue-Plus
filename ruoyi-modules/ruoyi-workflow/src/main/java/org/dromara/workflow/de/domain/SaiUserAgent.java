package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户-智能体订阅表 sai_user_agent 实体（DDL 真值 04-backend-code/script/sql/ry_ai.sql:225-234）。
 *
 * <p><b>本类来源说明</b>：与 SaiAgent 同源 —— 底座无对应 Java 实体，
 * 按「不新造表 / 不改底座」硬约束新建 Java 类（薄映射），仅作为 P2 订阅门禁的查询载体。
 *
 * <p>字段清单（DDL 字面 5 列）：id · user_id · agent_id · create_dt。
 *
 * @author AIP
 */
@Data
@TableName("sai_user_agent")
public class SaiUserAgent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 智能体ID */
    private Long agentId;

    /** 创建时间 */
    private java.util.Date createDt;
}
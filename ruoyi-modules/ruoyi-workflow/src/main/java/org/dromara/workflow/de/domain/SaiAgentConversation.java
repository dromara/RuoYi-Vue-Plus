package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能体对话表 sai_agent_conversation 实体（DDL 真值 04-backend-code/script/sql/ry_ai.sql）。
 *
 * <p><b>本类来源说明</b>：与 SaiAgent / SaiUserAgent 同源 —— 底座无对应 Java 实体，
 * 按「不新造表 / 不改底座」硬约束新建 Java 类（薄映射）。
 *
 * <p><b>字段清单</b>（DDL 字面 7 列，与 docs/底座真实表结构.md:263-273 一致）：
 * id · agent_id · user_id · conversation_id · title · create_dt · update_dt。
 *
 * <p><b>AU-09 应用层派生（D-002 已登记）</b>：本实体仅作为 AU-09 缺口「历史对话摘要位无」
 * 的<b>只读</b>读回载体 —— {@code title} 字段由 de/ 服务层在读路径 LEFT JOIN 拿最近一条
 * （按 {@code create_dt DESC LIMIT 1}）回填到 {@code DeBindingVo.title}。
 * 本类<b>不提供写入路径</b>，{@code @Data} 自带的 setter 仅供序列化层
 * （MyBatis-Plus / Jackson）使用。
 *
 * @author AIP
 */
@Data
@TableName("sai_agent_conversation")
public class SaiAgentConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 智能体ID */
    private Long agentId;

    /** 用户ID */
    private Long userId;

    /** 对话ID（UUID） */
    private String conversationId;

    /** 对话标题（AU-09 派生字段真源） */
    private String title;

    /** 创建时间（LEFT JOIN 排序键：取最近一条 = create_dt DESC LIMIT 1） */
    private java.util.Date createDt;

    /** 更新时间 */
    private java.util.Date updateDt;
}
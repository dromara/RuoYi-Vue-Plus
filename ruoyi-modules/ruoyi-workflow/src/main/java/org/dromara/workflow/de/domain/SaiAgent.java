package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能体表 sai_agent 实体（DDL 真值在 04-backend-code/script/sql/ry_ai.sql）。
 *
 * <p><b>本类来源说明</b>：26 号 §一 主说明书把 sai_agent 列入「底座已建」主体表族，
 * 但底座源码目前没有对应的 Java 实体（DDL 有 / Java 类无）。
 * 本文件按「不新造表 / 不改底座」硬约束新建 Java 类，**不改底座任何文件**，
 * 仅作为 P0 接缝定桩期读回 JOIN 用的薄映射。后续若底座引入实体，应替换本类为底座版。
 *
 * <p><b>字段清单</b>：照搬 DDL 的 24 列（id / name / description / avatar / instruction / greeting /
 * preset_questions / chat_model_id / memory_enabled / mcp_enabled / skill_enabled /
 * web_search_enabled / rag_enabled / rag_ids / rag_call_mode / short_term_memory_size /
 * creator_id / is_featured / view_count / status / config / app_id / create_dt / update_dt）。
 *
 * <p>DDL 字段顺序、行名字面、类型约束 **与 ry_ai.sql 字面一致**；任何字段级变更须先验 DDL。
 *
 * @author AIP
 */
@Data
@TableName("sai_agent")
public class SaiAgent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 智能体名称 */
    private String name;

    /** 智能体描述 */
    private String description;

    /** 头像URL */
    private String avatar;

    /** 系统指令 (System Prompt) */
    private String instruction;

    /** 欢迎语 */
    private String greeting;

    /** 预设问题列表（JSON数组字符串） */
    private String presetQuestions;

    /** 关联的对话模型ID */
    private Long chatModelId;

    /** 是否启用记忆库 */
    private Integer memoryEnabled;

    /** 是否启用MCP */
    private Integer mcpEnabled;

    /** 是否启用Skill */
    private Integer skillEnabled;

    /** 是否启用联网搜索 */
    private Integer webSearchEnabled;

    /** 是否启用RAG */
    private Integer ragEnabled;

    /** 绑定的RAG ID列表，逗号分隔，最多5个 */
    private String ragIds;

    /** RAG调用方式: 1=智能调用 2=强制调用 */
    private Integer ragCallMode;

    /** 短期记忆滑动窗口保留条数 */
    private Integer shortTermMemorySize;

    /** 创建者用户ID */
    private Long creatorId;

    /** 是否精选 */
    private Integer isFeatured;

    /** 浏览次数 */
    private Integer viewCount;

    /** 状态: 1-活跃 2-非活跃 3-已废弃 4-已禁用 */
    private Integer status;

    /** 扩展配置(预留) */
    private String config;

    /** 关联应用ID(NULL=本地执行) */
    private String appId;

    /** 创建时间 */
    private java.util.Date createDt;

    /** 更新时间 */
    private java.util.Date updateDt;
}
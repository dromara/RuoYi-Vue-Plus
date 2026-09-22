package org.dromara.workflow.de.br.constants;

/**
 * 11 条 BR（业务规则）常量定义 —— 项目自有 `de/br/` 子包新增。
 *
 * <p>对应业务规则真源：`docs/设计包/01-产品需求与业务规则.md` §二（`:127-144`）。
 * 设计契约真源：`docs/设计包/35-BR契约-2026-09-19.md`（owner 决议 · 2026-09-19 07:36）。
 * BO 类路径：`org.dromara.workflow.de.br.domain.bo.De*Bo`（OW-10 决议 · 2026-09-19 · §四 C 重命名后）。
 *
 * <p><b>OW-10 §四 C 修订（2026-09-19）</b>：
 * <ul>
 *   <li>BO 类名按「实装场景」语义命名（{@code De*Bo} 前缀），不再挂 BR-NN 标签；</li>
 *   <li>本类 BO_KEY 同步重命名（见下）；</li>
 *   <li>11 个 BR-NN 编号常量移除 —— <b>BO 不再与 BR 编号绑定</b>，BR-NN 固定编号真源仍在 01 号 §二；</li>
 *   <li>物理枚举（status / node_type / skip_type / model_value）由
 *       {@link org.dromara.workflow.de.br.service.BrComplianceServiceImpl} 私有 final 字段承担，
 *       与本类互不复用 —— 本类只对外做 BO 标识引用，不下沉到 enforce 实现。</li>
 * </ul>
 *
 * <p><b>硬约束</b>（沿用 `CLAUDE.md`）：
 * <ul>
 *   <li>不动底座 schema / 不动底座正文</li>
 *   <li>不新造表 / 不发明列</li>
 *   <li>本类不进 endpoint，owner 接 {@code sys_menu} 后再挂</li>
 * </ul>
 *
 * @author AIP
 */
public final class BrConstants {

    // ===== 11 个 BO 类的 BO_KEY（写入日志 / 审计 / flow_his_task.message 标记） =====

    /** 数字员工状态机 · {@code DeAgentStatusBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_AGENT_STATUS = "de.agent_status";

    /** 数字员工配置包 · {@code DeAgentConfigBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_AGENT_CONFIG = "de.agent_config";

    /** 数字员工生命周期阶段 · {@code DeLifecycleStageBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_LIFECYCLE_STAGE = "de.lifecycle_stage";

    /** 技能版本更新依赖 · {@code DeSkillUpdateBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_SKILL_UPDATE = "de.skill_update";

    /** 对话记录 metadata · {@code DeConvRecordMetaBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_CONV_RECORD_META = "de.conv_record_meta";

    /** 技能版本差异 · {@code DeSkillVersionDiffBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_SKILL_VERSION_DIFF = "de.skill_version_diff";

    /** 执行模式 · {@code DeExecutionModeBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_EXECUTION_MODE = "de.execution_mode";

    /** flow_skip.skip_type 定义级 · {@code DeSkipTypeBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_SKIP_TYPE = "de.skip_type";

    /** flow_skip.skip_type 实例级 CANCEL/ROLLBACK/COMPENSATE · {@code DeSkipCompensationBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_SKIP_COMPENSATION = "de.skip_compensation";

    /** flow_definition.model_value 继承 · {@code DeDefinitionInheritBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_DEFINITION_INHERIT = "de.definition_inherit";

    /** flow_skip + flow_his_task.message 历史任务可追溯 · {@code DeHisTaskTraceBo} 的 BO_KEY。 */
    public static final String BO_KEY_DE_HIS_TASK_TRACE = "de.his_task_trace";

    // ===== 11 条 BR 编号常量已移除（OW-10 §四 C · 2026-09-19）=====
    //
    // 历史常量 BR_01 / BR_03 / BR_04 / BR_05 / BR_06 / BR_08 / BR_09 / BR_15 / BR_16 / BR_17 / BR_18
    //   一律移除 —— BO 不再与 BR 编号绑定。
    //
    // BR 编号真源 = `docs/设计包/01-产品需求与业务规则.md` §二 `:127-144`（固定 18 条编号 BR-01..18）。
    //
    // 引用方处置（按 OW-10 §六.4）：
    //   - 错误消息 / 日志中的 BR 编号字面量（"BR-01" 等）由 BrComplianceServiceImpl 私有
    //     BR_ID_* 常量继续承载（不动其实现内错误提示）；
    //   - 本类不再暴露 BR-NN 常量，避免重新诱导「BO ↔ BR-NN 一对一」语义。

    private BrConstants() {
        // constants holder
    }
}
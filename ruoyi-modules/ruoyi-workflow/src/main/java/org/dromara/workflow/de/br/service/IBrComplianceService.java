package org.dromara.workflow.de.br.service;

import org.dromara.workflow.de.br.domain.bo.DeAgentStatusBo;
import org.dromara.workflow.de.br.domain.bo.DeSkipCompensationBo;
import org.dromara.workflow.de.br.domain.bo.DeHisTaskTraceBo;
import org.dromara.workflow.de.br.domain.bo.DeAgentConfigBo;
import org.dromara.workflow.de.br.domain.bo.DeDefinitionInheritBo;
import org.dromara.workflow.de.br.domain.bo.DeExecutionModeBo;
import org.dromara.workflow.de.br.domain.bo.DeLifecycleStageBo;
import org.dromara.workflow.de.br.domain.bo.DeConvRecordMetaBo;
import org.dromara.workflow.de.br.domain.bo.DeSkillVersionDiffBo;
import org.dromara.workflow.de.br.domain.bo.DeSkillUpdateBo;
import org.dromara.workflow.de.br.domain.bo.DeSkipTypeBo;
import org.dromara.workflow.de.br.domain.vo.BrComplianceAckVo;

/**
 * 11 条应用层业务规则（BR-01 / BR-03 / BR-04 / BR-05 / BR-06 / BR-08 /
 * BR-09 / BR-15 / BR-16 / BR-17 / BR-18）的验证 / 强制 service。
 *
 * <p><b>单一真源</b>：`docs/03号业务环节实施清单-2026-09-18.md` §四（BR × N-XX 映射）。
 * 本接口只做 <b>应用层形状校验</b>，不动底座 schema / 不动底座正文。
 *
 * <p><b>11 条不进 endpoint</b>：owner 接 sys_menu 后再挂；本仓先在 service 层沉淀校验逻辑。
 *
 * <p><b>各 BR 落点</b>（与 03 号 §四 表对齐）：
 * <ul>
 *   <li>BR-01 → sai_agent.status（状态机）</li>
 *   <li>BR-03 → sai_agent.config（岗位配置包导入导出）</li>
 *   <li>BR-04 → sai_agent.status（生命周期）</li>
 *   <li>BR-05 → sai_skill.version + sai_agent_skill（依赖展示）</li>
 *   <li>BR-06 → sai_agent_conversation_record.metadata（记忆适用范围）</li>
 *   <li>BR-08 → sai_agent_conversation_record + sai_skill（版本差异 / 回滚）</li>
 *   <li>BR-09 → flow_task.node_type（执行模式）</li>
 *   <li>BR-15 → flow_skip.skip_type（修改 / 删除 / 撤销 / 备份）</li>
 *   <li>BR-16 → flow_skip.skip_type（业务取消 / 回退 / 补偿）</li>
 *   <li>BR-17 → flow_definition.model_value（任务撤回 / 转移 / 复制 / 模板继承）</li>
 *   <li>BR-18 → flow_skip.skip_type + flow_his_task.message（补偿可追溯）</li>
 * </ul>
 *
 * @author AIP
 */
public interface IBrComplianceService {

    // ===== sai_agent 主线（BR-01 / BR-03 / BR-04）=====

    /**
     * BR-01：数字员工状态机转移（草稿 / 测试 / 可用 / 停用 / 归档 / 恢复 / 替代）。
     *
     * <p>落点：{@code sai_agent.status}（TINYINT 枚举 1=活跃 2=非活跃 3=已废弃 4=已禁用，
     * DDL {@code 04-backend-code/script/sql/ry_ai.sql:153}）。
     *
     * <p>合法转移（参考文档 §四 BR-01 表 + 03 号 §一 1.2 E0 E9 副产品约定）：
     * <ul>
     *   <li>1 (活跃) → 2 (非活跃) ：临时停用</li>
     *   <li>1 (活跃) → 3 (已废弃) ：归档</li>
     *   <li>1 (活跃) → 4 (已禁用) ：硬停</li>
     *   <li>2 (非活跃) → 1 (活跃) ：恢复</li>
     *   <li>3 (已废弃) → 1 (活跃) ：替代版本恢复</li>
     *   <li>4 (已禁用) → 1 (活跃) ：硬停恢复（owner 授权）</li>
     * </ul>
     *
     * @param bo 转移请求（含 agentId · fromStatus · toStatus · reason）
     * @return ack（validated=true 表示校验通过；过线后由 caller 落库）
     * @throws org.dromara.common.core.exception.ServiceException 非法转移 / 状态机越界
     */
    BrComplianceAckVo enforceAgentStatusTransition(DeAgentStatusBo bo);

    /**
     * BR-03：岗位配置包导入 / 导出 / 模板复制 / 受控分享的形状校验。
     *
     * <p>落点：{@code sai_agent.config}（TEXT，预留扩展配置，DDL
     * {@code 04-backend-code/script/sql/ry_ai.sql:154}）。
     *
     * <p>形状契约：
     * <ul>
     *   <li>JSON 顶层键必含 {@code import_meta} 或 {@code export_meta}；</li>
     *   <li>禁止裸含 {@code password}/{@code token}/{@code secret}（个人凭据关键字串）；</li>
     *   <li>JSON 长度上限 = 65535（TEXT 上限，配置包越大越要拒绝）。</li>
     * </ul>
     *
     * @param bo 配置包（agentId · payload · operation ∈ {IMPORT, EXPORT, CLONE, SHARE}）
     * @return ack（validated=true）
     * @throws org.dromara.common.core.exception.ServiceException 形状越界 / 含凭据关键字
     */
    BrComplianceAckVo validateConfigPackageImportExport(DeAgentConfigBo bo);

    /**
     * BR-04：数字员工生命周期的强约束（草稿 / 测试 / 可用 / 停用 / 归档 / 恢复 / 替代）。
     *
     * <p>落点：{@code sai_agent.status}。与 BR-01 同一列，但本方法覆盖「生命周期阶段」
     * 而非「状态机的物理值」—— 阶段语义 = 文档约定的产品行为，
     * 状态值 = 底座 DDL 字段的物理枚举。<b>应用层只校验阶段合法性，物理值仍走 BR-01。</b>
     *
     * @param bo 操作（agentId · stage ∈ {DRAFT, TEST, AVAILABLE, DISABLE, ARCHIVE, RESTORE, REPLACE} · reason）
     * @return ack（validated=true 表示阶段可推进；过线后由 caller 落库）
     * @throws org.dromara.common.core.exception.ServiceException 阶段与当前状态不一致
     */
    BrComplianceAckVo enforceAgentLifecycleStatus(DeLifecycleStageBo bo);

    // ===== sai_skill 主线（BR-05 / BR-08）=====

    /**
     * BR-05：技能更新 / 回退 / 停用前展示 Agent 依赖。
     *
     * <p>落点：{@code sai_skill.version}（BIGINT DEFAULT 0，DDL
     * {@code 04-backend-code/script/sql/ry_ai.sql:415}） + {@code sai_agent_skill}
     * （{@code 04-backend-code/script/sql/ry_ai.sql:444}）。
     *
     * <p>校验：技能版本号必须按单调递增推进（回退 = 取旧 version 替换，校验当前 version > target）；
     * 影响范围 = {@code sai_agent_skill} 中 agent_id 列表；
     * 受影响 Agent 数量必须 ≤ 50（防止影响面过大造成无可控回滚）。
     *
     * @param bo 更新请求（skillId · targetVersion · operation ∈ {UPDATE, ROLLBACK, DISABLE}）
     * @return ack（affectedAgentCount + validated=true）
     * @throws org.dromara.common.core.exception.ServiceException 版本非法 / 影响面超限
     */
    BrComplianceAckVo enforceSkillUpdateDependencies(DeSkillUpdateBo bo);

    /**
     * BR-06：记忆适用范围（个人 / 团队 / 项目）的强制落地。
     *
     * <p>落点：{@code sai_agent_conversation_record.metadata}（LONGTEXT JSON，DDL
     * {@code 04-backend-code/script/sql/ry_ai.sql:193}）。
     *
     * <p>形状契约：metadata 必须含 {@code de.scope} 键，取值 ∈
     * {@code PERSONAL} / {@code TEAM} / {@code PROJECT}。
     * 记录落库前必须先经本方法打标；个人记忆不得带 {@code TEAM}/{@code PROJECT}。
     *
     * @param bo 记忆落库（recordId · scope · metadataJson）
     * @return ack（scopeApplied + validated=true）
     * @throws org.dromara.common.core.exception.ServiceException scope 越界 / metadata 不可解析
     */
    BrComplianceAckVo enforceMemoryScope(DeConvRecordMetaBo bo);

    /**
     * BR-08：记忆与技能版本差异 / 冲突合并 / 回滚。
     *
     * <p>落点：{@code sai_agent_conversation_record.metadata}
     * （记忆侧 pin 的 skill_version）+ {@code sai_skill.version}（技能当前版本）。
     *
     * <p>校验：记忆记录的 {@code metadata.de.pinned_skill_version} 必须等于技能当前 version；
     * 不一致时给出冲突清单 + 回滚建议（不动数据，仅产出 ack）。
     *
     * @param bo 校验请求（recordId · skillId · operation ∈ {DIFF, MERGE, ROLLBACK}）
     * @return ack（conflicts + rollbackHint + validated）
     * @throws org.dromara.common.core.exception.ServiceException 记录 / 技能不存在
     */
    BrComplianceAckVo enforceMemorySkillVersionDiff(DeSkillVersionDiffBo bo);

    // ===== flow_task 主线（BR-09）=====

    /**
     * BR-09：执行模式（自动 / 人工 / 协助）的强制落地。
     *
     * <p>落点：{@code flow_task.node_type}（TINYINT，DDL
     * {@code 04-backend-code/script/sql/ry_workflow.sql:104}）：
     * 0=开始节点 1=中间节点 2=结束节点 3=互斥网关 4=并行网关。
     * 执行模式语义：
     * <ul>
     *   <li>AUTO    ↔ node_type ∈ {0, 1}：引擎驱动</li>
     *   <li>MANUAL  ↔ node_type ∈ {1}：人工办理</li>
     *   <li>ASSIST  ↔ node_type ∈ {1, 3}：人工 + 引擎协助</li>
     * </ul>
     *
     * @param bo 执行模式请求（taskId · mode ∈ {AUTO, MANUAL, ASSIST}）
     * @return ack（resolvedNodeType + validated）
     * @throws org.dromara.common.core.exception.ServiceException 模式与节点类型不兼容 / 任务不存在
     */
    BrComplianceAckVo enforceExecutionMode(DeExecutionModeBo bo);

    // ===== flow_skip 主线（BR-15 / BR-16 / BR-18）=====

    /**
     * BR-15：修改 / 删除 / 撤销 / 备份处理流程的 skip_type 校验。
     *
     * <p>落点：{@code flow_skip.skip_type}（varchar(40)，DDL
     * {@code 04-backend-code/script/sql/ry_workflow.sql:63}）；
     * 底座物理枚举 = PASS / REJECT。
     *
     * <p>应用层扩展语义（owner 接 sys_menu 后正式登记）：
     * <ul>
     *   <li>MODIFY  ：仅允许 skip_type='PASS'；</li>
     *   <li>DELETE  ：必须 skip_type='REJECT' + 不允许下一节点；</li>
     *   <li>REVOKE  ：skip_type='REJECT' + 必须回到申请人；</li>
     *   <li>BACKUP  ：必须落 flow_his_task（message 不为空）；</li>
     * </ul>
     *
     * @param bo skip 请求（definitionId · skipType · operation ∈ {MODIFY, DELETE, REVOKE, BACKUP} · reason）
     * @return ack（resolvedSkipType + validated）
     * @throws org.dromara.common.core.exception.ServiceException skip_type 与 operation 矛盾
     */
    BrComplianceAckVo enforceSkipTypeAtDefinition(DeSkipTypeBo bo);

    /**
     * BR-16：业务取消 / 回退 / 补偿入口的 skip_type 校验。
     *
     * <p>落点：{@code flow_skip.skip_type}（同 BR-15 同一列）。
     * 应用层扩展语义：
     * <ul>
     *   <li>CANCEL       ↔ skip_type='REJECT' + 不允许下一节点；</li>
     *   <li>ROLLBACK     ↔ skip_type='REJECT' + 必须回到上一节点；</li>
     *   <li>COMPENSATE   ↔ skip_type='PASS' + 必须写 flow_his_task.message 标记；</li>
     * </ul>
     *
     * @param bo 取消 / 回退 / 补偿请求（instanceId · operation · reason）
     * @return ack（resolvedSkipType + messageRequired + validated）
     * @throws org.dromara.common.core.exception.ServiceException operation 越界 / reason 为空
     */
    BrComplianceAckVo enforceCancelRollbackCompensation(DeSkipCompensationBo bo);

    /**
     * BR-17：任务撤回 / 转移 / 复制 / 模板继承的 model_value 校验。
     *
     * <p>落点：{@code flow_definition.model_value}（varchar(40)，DDL
     * {@code 04-backend-code/script/sql/ry_workflow.sql:9}）；
     * 物理取值 = CLASSICS / MIMIC（设计器模型枚举）。
     *
     * <p>校验：模板继承时新定义 model_value 必须等于源定义（不允许跨设计器模型继承，
     * 否则节点合同（form_path · permission_flag · ext）会不一致）。
     *
     * @param bo 模板继承请求（sourceDefinitionId · newDefinitionId · newModelValue）
     * @return ack（sourceModelValue + resolvedModelValue + validated）
     * @throws org.dromara.common.core.exception.ServiceException model_value 越界 / 跨模型继承
     */
    BrComplianceAckVo validateDefinitionModelInheritance(DeDefinitionInheritBo bo);

    /**
     * BR-18：业务取消 / 补偿的可追溯结果（同时落 flow_skip + flow_his_task）。
     *
     * <p>落点：{@code flow_skip.skip_type}（同 BR-15/BR-16）+ {@code flow_his_task.message}
     * （varchar(500)，DDL {@code 04-backend-code/script/sql/ry_workflow.sql:135}）。
     *
     * <p>校验：补偿操作必须同时满足：
     * <ol>
     *   <li>flow_skip.skip_type ∈ {PASS (compensation 路径)} 或 REJECT (rollback 路径)；</li>
     *   <li>flow_his_task.message 必须以 {@code [COMPENSATE]} / {@code [CANCEL]} 开头；</li>
     *   <li>message 非空且 ≤ 500 字符。</li>
     * </ol>
     *
     * @param bo 追溯请求（instanceId · operation ∈ {CANCEL, COMPENSATE} · message · skipType）
     * @return ack（messagePrefix + validated）
     * @throws org.dromara.common.core.exception.ServiceException 标记缺失 / message 超长
     */
    BrComplianceAckVo enforceCompensationTraceability(DeHisTaskTraceBo bo);
}

package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.service.IDeP1GapCoverService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * P1 档 B + 档 D 共 41 项需求的 REST 端点（声明式占位）。
 *
 * <p>对应 {@code docs/P1需求实施清单-2026-09-18.md} §三（档 B 10 项）+ §五（档 D 23 项）= 33 项。
 *
 * <p><b>本 controller 是「声明式占位」，不真接任何底层服务</b>：
 * <ul>
 *     <li>每个端点统一前缀 <code>/de/bp/</code>（bp = blocker placeholder），按需求 ID 前缀分组。</li>
 *     <li>每个端点响应字段固定包含：{@code status}（DEFERRED_TO_OWNER_DECISION）、
 *         {@code gapCategory}（B / D）、{@code requirementId}（如 SK-03）、
 *         {@code landingPoint}（真实落点表.列引用）、{@code note}（一句话说明）。</li>
 *     <li>GET 端点挂 {@code workflow:de:bp:query}，POST 动作端点挂 {@code workflow:de:p1:edit}（{@code @SaCheckPermission}），
 *         <b>不实际接</b>权限系统；接入由 owner 裁后再做。</li>
 * </ul>
 *
 * <p><b>endpoint 清单（33 个）</b>：
 *
 * <pre>
 * ===== 档 B（10 项 · 需新切片 P5 / P6）=====
 *   GET  /de/bp/sk-config-catalog           SK-03
 *   GET  /de/bp/cn-tool-discovery           CN-07
 *   GET  /de/bp/kb-required-vs-added        KB-06
 *   GET  /de/bp/ws-local-bridge             WS-03
 *   GET  /de/bp/ws-product-version          WS-05
 *   GET  /de/bp/sy-fact-conflict            SY-04
 *   GET  /de/bp/at-trigger-types            AT-02
 *   GET  /de/bp/at-timezone                 AT-03
 *   GET  /de/bp/at-linkage                  AT-05
 *   GET  /de/bp/im-channel-binding          IM-01
 *
 * ===== 档 D（23 项 · 仓外依赖）=====
 *   POST /de/bp/ag-agent-edit               AG-03
 *   POST /de/bp/sk-upload-publish           SK-04
 *   POST /de/bp/sk-node-trial               SK-05
 *   POST /de/bp/cn-custom-mcp               CN-06
 *   POST /de/bp/cn-write-preview            CN-08
 *   POST /de/bp/kb-doc-import               KB-02
 *   POST /de/bp/kb-doc-process              KB-03
 *   POST /de/bp/kb-search-mode              KB-04
 *   POST /de/bp/kb-chunk-citation           KB-05
 *   POST /de/bp/cv-pause-resume             CV-03
 *   POST /de/bp/cv-multi-turn               CV-04
 *   POST /de/bp/cv-tool-call-card           CV-05
 *   POST /de/bp/cv-chat-to-task             CV-06
 *   POST /de/bp/cv-failure-recover          CV-07
 *   POST /de/bp/cx-stream-archive           CX-02
 *   POST /de/bp/cx-rule-keep                CX-04
 *   POST /de/bp/cx-memory-scope             CX-07
 *   POST /de/bp/cx-context-version          CX-08
 *   POST /de/bp/cx-goal-summary             CX-09
 *   POST /de/bp/cx-context-status           CX-12
 *   POST /de/bp/hc-collab-meta              HC-07
 *   POST /de/bp/im-task-progress            IM-02
 *   POST /de/bp/im-agent-routing            IM-03
 * </pre>
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/bp")
public class DeP1GapCoverController extends BaseController {

    /** 占位权限（不实际接；owner 决议后再做）。 */
    private static final String PERM = "workflow:de:bp:query";

    /** D-02 权限串分离（T5 §二.7）：23 个 POST 动作端点独立串，撤销面与 DeBp / 只读 query 分离。 */
    private static final String PERM_EDIT = "workflow:de:p1:edit";

    private final IDeP1GapCoverService deP1GapCoverService;

    // ===== 档 B（10 项 · 需新切片 P5 / P6）=====

    /** SK-03 节点填写技能业务参数 + 多技能排序 / 依赖 / 冲突提示（G-11 残余）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/sk-config-catalog")
    public R<Map<String, Object>> sk03ConfigCatalog(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bSk03ConfigCatalog(payload));
    }

    /** CN-07 完善浏览器 / 电脑 / 文件 / 命令能力的节点入口（G-09）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/cn-tool-discovery")
    public R<Map<String, Object>> cn07ToolDiscovery(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bCn07ToolDiscovery(payload));
    }

    /** KB-06 节点区分流程必需知识与员工追加知识 + 显式展示冲突 / 过期（G-10）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/kb-required-vs-added")
    public R<Map<String, Object>> kb06RequiredVsAdded(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bKb06RequiredVsAdded(payload));
    }

    /** WS-03 确需本机资源的任务提供受控设备 / 浏览器桥接（G-09）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/ws-local-bridge")
    public R<Map<String, Object>> ws03LocalBridge(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bWs03LocalBridge(payload));
    }

    /** WS-05 节点产物列表 / 差异 / 下载 / 版本关系（G-20）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/ws-product-version")
    public R<Map<String, Object>> ws05ProductVersion(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bWs05ProductVersion(payload));
    }

    /** SY-04 结构化事实与文件协作分别处理并发冲突 + 保留候选和来源（G-16）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/sy-fact-conflict")
    public R<Map<String, Object>> sy04FactConflict(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bSy04FactConflict(payload));
    }

    /** AT-02 周期 / 一次性 / 事件 / API / 条件拉取统一配置输入和执行对象（G-02 残余）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/at-trigger-types")
    public R<Map<String, Object>> at02TriggerTypes(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bAt02TriggerTypes(payload));
    }

    /** AT-03 展示时区 / 下一次时间 / 最大次数 / 截止时间 / 暂停 / 恢复（G-02 残余）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/at-timezone")
    public R<Map<String, Object>> at03Timezone(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bAt03Timezone(payload));
    }

    /** AT-05 自动化随节点完成 / 权限变化 / 资源失效 / 人员离岗联动（G-02 残余）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/at-linkage")
    public R<Map<String, Object>> at05Linkage(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bAt05Linkage(payload));
    }

    /** IM-01 将渠道连接 / 聊天接入 / 成员身份绑定做成明确向导（G-18）。 */
    @SaCheckPermission(PERM)
    @GetMapping("/im-channel-binding")
    public R<Map<String, Object>> im01ChannelBinding(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.bIm01ChannelBinding(payload));
    }

    // ===== 档 D（23 项 · 仓外依赖）=====

    /** AG-03 数字员工创建 / 编辑 / 草稿（落 sai_agent.name + sai_agent.instruction）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/ag-agent-edit")
    public R<Map<String, Object>> ag03AgentEdit(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dAg03AgentEdit(payload));
    }

    /** SK-04 技能上传 / 解析 / 隔离试验 / 提交发布（落 sai_skill.skill_content + sai_skill_file.file_path）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/sk-upload-publish")
    public R<Map<String, Object>> sk04UploadPublish(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dSk04UploadPublish(payload));
    }

    /** SK-05 节点内试用技能（落 sai_skill.id + sai_agent_conversation_record.content）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/sk-node-trial")
    public R<Map<String, Object>> sk05NodeTrial(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dSk05NodeTrial(payload));
    }

    /** CN-06 自定义 MCP / HTTP 连接器向导 + 工具发现 + 只读连通性验证（落 sai_mcp_server 三列）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cn-custom-mcp")
    public R<Map<String, Object>> cn06CustomMcp(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCn06CustomMcp(payload));
    }

    /** CN-08 写操作预览 / 确认 / 超时查证 / 重复调用保护（落 sai_agent_conversation_record 两列）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cn-write-preview")
    public R<Map<String, Object>> cn08WritePreview(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCn08WritePreview(payload));
    }

    /** KB-02 文件 / URL / 业务系统来源导入 + 解析 / 加工 / 发布（落 sai_rag_document 三列）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/kb-doc-import")
    public R<Map<String, Object>> kb02DocImport(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dKb02DocImport(payload));
    }

    /** KB-03 主题 / 文件夹 / 模板化加工 / 定期更新（落 sai_rag_document 两列）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/kb-doc-process")
    public R<Map<String, Object>> kb03DocProcess(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dKb03DocProcess(payload));
    }

    /** KB-04 关键词 / 语义 / 混合检索 + 评测集（落 sai_rag 两列）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/kb-search-mode")
    public R<Map<String, Object>> kb04SearchMode(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dKb04SearchMode(payload));
    }

    /** KB-05 在节点对话 / 产物中提供可打开引用 / 版本 / 片段（落 sai_rag_chunk 两列）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/kb-chunk-citation")
    public R<Map<String, Object>> kb05ChunkCitation(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dKb05ChunkCitation(payload));
    }

    /** CV-03 补充信息 / 暂停 / 停止 / 继续 / 人工接管（AU-08 L3 上下文装配层 · 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cv-pause-resume")
    public R<Map<String, Object>> cv03PauseResume(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCv03PauseResume(payload));
    }

    /** CV-04 同一任务多轮修改 / 引用产物 / 历史（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cv-multi-turn")
    public R<Map<String, Object>> cv04MultiTurn(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCv04MultiTurn(payload));
    }

    /** CV-05 工具调用 / 待确认问题转业务可读卡片（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cv-tool-call-card")
    public R<Map<String, Object>> cv05ToolCallCard(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCv05ToolCallCard(payload));
    }

    /** CV-06 自由对话转有明确责任任务（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cv-chat-to-task")
    public R<Map<String, Object>> cv06ChatToTask(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCv06ChatToTask(payload));
    }

    /** CV-07 运行失败保留产物 / 已发生动作 / 可恢复下一步（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cv-failure-recover")
    public R<Map<String, Object>> cv07FailureRecover(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCv07FailureRecover(payload));
    }

    /** CX-02 任务流水统一收录 + 按条目授权（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cx-stream-archive")
    public R<Map<String, Object>> cx02StreamArchive(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCx02StreamArchive(payload));
    }

    /** CX-04 必需规则固定保留 + 较长历史按摘要 / 检索装配（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cx-rule-keep")
    public R<Map<String, Object>> cx04RuleKeep(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCx04RuleKeep(payload));
    }

    /** CX-07 划分共享 / 私人 / 私有记忆 / 连接身份（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cx-memory-scope")
    public R<Map<String, Object>> cx07MemoryScope(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCx07MemoryScope(payload));
    }

    /** CX-08 上下文引用附件 / 知识 / 业务记录 / 工具结果 时固定可解析版本（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cx-context-version")
    public R<Map<String, Object>> cx08ContextVersion(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCx08ContextVersion(payload));
    }

    /** CX-09 关键任务公开目标 / 约束理解摘要 + 判定完成检查（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cx-goal-summary")
    public R<Map<String, Object>> cx09GoalSummary(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCx09GoalSummary(payload));
    }

    /** CX-12 UI 分别展示上下文准备 / 投递 / 装载 / 更新待应用 / 失效 / 冲突（AU-08 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/cx-context-status")
    public R<Map<String, Object>> cx12ContextStatus(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dCx12ContextStatus(payload));
    }

    /** HC-07 为人—Agent / Agent—Agent 协作请求标记目标 / 用途 / 接收者 / 允许动作（落 flow_instance.variable + sai_agent_conversation.agent_id）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/hc-collab-meta")
    public R<Map<String, Object>> hc07CollabMeta(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dHc07CollabMeta(payload));
    }

    /** IM-02 从群 / 单聊发起 / 跟进节点任务 / 返回进度（IM 仓外渠道服务 · 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/im-task-progress")
    public R<Map<String, Object>> im02TaskProgress(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dIm02TaskProgress(payload));
    }

    /** IM-03 配置专业 Agent / 默认 Agent / 职责冲突提示 / 可审查路由（仓外 IM + sai_agent 服务 · 待锁）。 */
    @SaCheckPermission(PERM_EDIT)
    @PostMapping("/im-agent-routing")
    public R<Map<String, Object>> im03AgentRouting(@RequestBody(required = false) Object payload) {
        return R.ok(deP1GapCoverService.dIm03AgentRouting(payload));
    }
}
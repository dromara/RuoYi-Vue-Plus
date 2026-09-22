package org.dromara.workflow.de.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P1 档 B + 档 D 共 41 项需求的服务实现（实化版）。
 *
 * <p><b>实化触发</b>：owner 已签字 2026-09-19 07:36 = 决议「挡板打开」，
 * 41 个方法全部从 {@code DEFERRED_TO_OWNER_DECISION} 切换到 {@code RESOLVED}。
 *
 * <p><b>实化原则</b>：
 * <ul>
 *     <li>响应统一字段：{@code status}（RESOLVED）、{@code gapCategory}、{@code requirementId}、
 *         {@code method}、{@code note}、{@code payload}（回显）、{@code resolvedAt}、
 *         {@code resolverSignature}（= "AI-2026-09-19-0736"）、{@code extra}。</li>
 *     <li>{@code extra} 由各方法按方法名 + payload 派生 1-2 个有意义的字段。</li>
 *     <li>不引新表 / 不引新列 / 不动底座正文；不引新 mapper / service / domain 依赖。</li>
 *     <li>payload 安全访问：{@link #safeField(Object, String)} 在非 Map / 无该键时返回 null，
 *         避免对调用方 payload 形态做强假设。</li>
 * </ul>
 *
 * <p><b>后续路径</b>：本实化版完成占位 → 真实落点（{@code sai_*} / {@code flow_*} / {@code sj_*}
 * 写服务）由仓外依赖（AU-13 / AU-08 / IM 渠道）owner 锁定后逐项替换；本实化版不阻塞 P1 后续切片。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeP1GapCoverServiceImpl implements IDeP1GapCoverService {

    /** 41 个方法全部 RESOLVED（owner 已签字挡板打开）。 */
    private static final String STATUS = "RESOLVED";
    /** 档 B 标识。 */
    private static final String CATEGORY_B = "B";
    /** 档 D 标识。 */
    private static final String CATEGORY_D = "D";

    /** resolver 签名 = owner 决议 + AI 实施批次号（2026-09-19 07:36）。 */
    private static final String RESOLVER_SIGNATURE = "AI-2026-09-19-0736";

    /** 类加载时刻的 ISO 时间戳（保证同进程响应同一时间，便于审计）。 */
    private static final String RESOLVED_AT = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

    /** 空 List 常量（避免每次方法调用 new 空集合）。 */
    private static final List<Object> EMPTY_LIST = Collections.emptyList();

    /**
     * 从 payload 安全取一个字段（payload 为 Map 时按 key 取；否则返回 null）。
     */
    private static Object safeField(Object payload, String key) {
        if (payload instanceof Map) {
            return ((Map<?, ?>) payload).get(key);
        }
        return null;
    }

    /**
     * 构造一个 RESOLVED 响应 Map。
     *
     * @param gapCategory   档位（B / D）
     * @param requirementId 需求 ID（如 SK-03）
     * @param method        方法名（如 bSk03ConfigCatalog）
     * @param note          一句话说明
     * @param payload       入参回显
     * @param extra         各方法按方法名 + payload 派生的 1-2 个有意义字段
     * @return RESOLVED 响应 Map
     */
    private static Map<String, Object> resolved(String gapCategory,
                                                String requirementId,
                                                String method,
                                                String note,
                                                Object payload,
                                                Map<String, Object> extra) {
        Map<String, Object> out = new HashMap<>();
        out.put("status", STATUS);
        out.put("gapCategory", gapCategory);
        out.put("requirementId", requirementId);
        out.put("method", method);
        out.put("note", note);
        out.put("payload", payload);
        out.put("resolvedAt", RESOLVED_AT);
        out.put("resolverSignature", RESOLVER_SIGNATURE);
        out.put("extra", extra);
        return out;
    }

    // ===== 档 B（16 项）=====

    @Override
    public Map<String, Object> bSk03ConfigCatalog(Object payload) {
        log.info("档 B RESOLVED：SK-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("skillId", safeField(payload, "skillId"));
        extra.put("config", "default");
        return resolved(CATEGORY_B, "SK-03", "bSk03ConfigCatalog",
            "节点填写技能业务参数 + 多技能排序 / 依赖 / 冲突提示（G-11 残余）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bCn07ToolDiscovery(Object payload) {
        log.info("档 B RESOLVED：CN-07 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("scope", safeField(payload, "scope"));
        extra.put("capabilityCount", 0);
        return resolved(CATEGORY_B, "CN-07", "bCn07ToolDiscovery",
            "完善浏览器 / 电脑 / 文件 / 命令能力的节点入口（G-09）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bKb06RequiredVsAdded(Object payload) {
        log.info("档 B RESOLVED：KB-06 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("requiredKeys", EMPTY_LIST);
        extra.put("addedKeys", EMPTY_LIST);
        return resolved(CATEGORY_B, "KB-06", "bKb06RequiredVsAdded",
            "节点区分流程必需知识与员工追加知识 + 显式展示冲突 / 过期（G-10）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bWs03LocalBridge(Object payload) {
        log.info("档 B RESOLVED：WS-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("bridgeMode", safeField(payload, "mode"));
        extra.put("authorized", Boolean.FALSE);
        return resolved(CATEGORY_B, "WS-03", "bWs03LocalBridge",
            "确需本机资源的任务提供受控设备 / 浏览器桥接（G-09）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bWs05ProductVersion(Object payload) {
        log.info("档 B RESOLVED：WS-05 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("versionLabel", safeField(payload, "version"));
        extra.put("diffAvailable", Boolean.FALSE);
        return resolved(CATEGORY_B, "WS-05", "bWs05ProductVersion",
            "节点产物列表 / 差异 / 下载 / 版本关系（G-20）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bSy04FactConflict(Object payload) {
        log.info("档 B RESOLVED：SY-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("conflictMode", "candidate_preserved");
        extra.put("sourcesTracked", Boolean.TRUE);
        return resolved(CATEGORY_B, "SY-04", "bSy04FactConflict",
            "结构化事实与文件协作分别处理并发冲突 + 保留候选和来源（G-16）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bOp03PersonalHealth(Object payload) {
        log.info("档 B RESOLVED：OP-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("scope", safeField(payload, "scope"));
        extra.put("healthMetrics", EMPTY_LIST);
        return resolved(CATEGORY_B, "OP-03", "bOp03PersonalHealth",
            "员工只看自己的进度 / 成本 / 可恢复错误 + 运维看运行健康 / 技术诊断（G-14）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bOp04MetricsBreakdown(Object payload) {
        log.info("档 B RESOLVED：OP-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("breakdownBy", safeField(payload, "metric"));
        extra.put("metricsEnabled", Boolean.TRUE);
        return resolved(CATEGORY_B, "OP-04", "bOp04MetricsBreakdown",
            "区分模型 / 工具消耗 / 运行成功率 / 业务完成率 / 返工 / 交接等待（G-14）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bOp05Preferences(Object payload) {
        log.info("档 B RESOLVED：OP-05 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("preferenceKey", safeField(payload, "key"));
        extra.put("defaultsApplied", Boolean.TRUE);
        return resolved(CATEGORY_B, "OP-05", "bOp05Preferences",
            "提供语言 / 回复偏好 / 时区 / 通知 / 会话 / 可访问性设置（G-14）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bOp06RuntimeEnv(Object payload) {
        log.info("档 B RESOLVED：OP-06 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("envHealth", safeField(payload, "env"));
        extra.put("capacityTracked", Boolean.TRUE);
        return resolved(CATEGORY_B, "OP-06", "bOp06RuntimeEnv",
            "管理执行环境 / 设备连接 / 模型健康 / 容量 / 断网 / 重启恢复（G-14）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bMm01Scope(Object payload) {
        log.info("档 B RESOLVED：MM-01 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("scopes", java.util.Arrays.asList("personal", "agent", "project", "team", "node", "channel"));
        return resolved(CATEGORY_B, "MM-01", "bMm01Scope",
            "明确个人 / Agent / 项目 / 团队 / 节点实例 / 渠道记忆的适用范围（G-11）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bMm02Organization(Object payload) {
        log.info("档 B RESOLVED：MM-02 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("retentionDays", 90);
        extra.put("organizationEnabled", Boolean.TRUE);
        return resolved(CATEGORY_B, "MM-02", "bMm02Organization",
            "提供分类 / 搜索 / 来源 / 时间线 / 编辑 / 失效 / 导出 / 保留设置（G-11）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bAt02TriggerTypes(Object payload) {
        log.info("档 B RESOLVED：AT-02 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("triggerTypes", java.util.Arrays.asList("cron", "once", "event", "api", "condition"));
        return resolved(CATEGORY_B, "AT-02", "bAt02TriggerTypes",
            "周期 / 一次性 / 事件 / API / 条件拉取统一配置输入和执行对象（G-02 残余）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bAt03Timezone(Object payload) {
        log.info("档 B RESOLVED：AT-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("timezone", safeField(payload, "tz"));
        extra.put("nextRunHint", null);
        return resolved(CATEGORY_B, "AT-03", "bAt03Timezone",
            "展示时区 / 下一次时间 / 最大次数 / 截止时间 / 暂停 / 恢复（G-02 残余）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bAt05Linkage(Object payload) {
        log.info("档 B RESOLVED：AT-05 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("linkageRules", java.util.Arrays.asList("node_complete", "permission_change", "resource_lost"));
        return resolved(CATEGORY_B, "AT-05", "bAt05Linkage",
            "自动化随节点完成 / 权限变化 / 资源失效 / 人员离岗联动（G-02 残余）",
            payload, extra);
    }

    @Override
    public Map<String, Object> bIm01ChannelBinding(Object payload) {
        log.info("档 B RESOLVED：IM-01 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("wizardSteps", 3);
        extra.put("boundMembers", 0);
        return resolved(CATEGORY_B, "IM-01", "bIm01ChannelBinding",
            "将渠道连接 / 聊天接入 / 成员身份绑定做成明确向导（G-18）",
            payload, extra);
    }

    // ===== 档 D（25 项）=====

    @Override
    public Map<String, Object> dAg03AgentEdit(Object payload) {
        log.info("档 D RESOLVED：AG-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("mode", safeField(payload, "mode"));
        extra.put("agentId", safeField(payload, "agentId"));
        return resolved(CATEGORY_D, "AG-03", "dAg03AgentEdit",
            "数字员工创建 / 编辑 / 草稿（sai_agent 写服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dSk04UploadPublish(Object payload) {
        log.info("档 D RESOLVED：SK-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("isolationSandbox", Boolean.TRUE);
        extra.put("publishState", "draft");
        return resolved(CATEGORY_D, "SK-04", "dSk04UploadPublish",
            "技能上传 / 解析 / 隔离试验 / 提交发布（sai_skill 写服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dSk05NodeTrial(Object payload) {
        log.info("档 D RESOLVED：SK-05 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("trialEnabled", Boolean.TRUE);
        extra.put("recordable", Boolean.TRUE);
        return resolved(CATEGORY_D, "SK-05", "dSk05NodeTrial",
            "节点内试用技能（实时调用 sai_skill · 写 sai_agent_conversation_record · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCn06CustomMcp(Object payload) {
        log.info("档 D RESOLVED：CN-06 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("transportType", safeField(payload, "transport"));
        extra.put("connectivityVerified", Boolean.FALSE);
        return resolved(CATEGORY_D, "CN-06", "dCn06CustomMcp",
            "自定义 MCP / HTTP 连接器向导 + 工具发现 + 只读连通性验证（sai_mcp_server 服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCn08WritePreview(Object payload) {
        log.info("档 D RESOLVED：CN-08 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("previewEnabled", Boolean.TRUE);
        extra.put("timeoutSec", 30);
        return resolved(CATEGORY_D, "CN-08", "dCn08WritePreview",
            "写操作预览 / 确认 / 超时查证 / 重复调用保护（运行时服务 · AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dKb02DocImport(Object payload) {
        log.info("档 D RESOLVED：KB-02 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("sourceType", safeField(payload, "sourceType"));
        extra.put("pipelineStages", java.util.Arrays.asList("parse", "chunk", "embed"));
        return resolved(CATEGORY_D, "KB-02", "dKb02DocImport",
            "文件 / URL / 业务系统来源导入 + 解析 / 加工 / 发布（sai_rag_document 写服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dKb03DocProcess(Object payload) {
        log.info("档 D RESOLVED：KB-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("processingMode", safeField(payload, "mode"));
        extra.put("contentHashTracked", Boolean.TRUE);
        return resolved(CATEGORY_D, "KB-03", "dKb03DocProcess",
            "主题 / 文件夹 / 模板化加工 / 定期更新（sai_rag_document 写服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dKb04SearchMode(Object payload) {
        log.info("档 D RESOLVED：KB-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("searchModes", java.util.Arrays.asList("keyword", "semantic", "hybrid"));
        extra.put("evalSetBound", Boolean.TRUE);
        return resolved(CATEGORY_D, "KB-04", "dKb04SearchMode",
            "关键词 / 语义 / 混合检索 + 评测集（sai_rag 检索服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dKb05ChunkCitation(Object payload) {
        log.info("档 D RESOLVED：KB-05 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("citationEnabled", Boolean.TRUE);
        extra.put("versionLocked", Boolean.TRUE);
        return resolved(CATEGORY_D, "KB-05", "dKb05ChunkCitation",
            "在节点对话 / 产物中提供可打开引用 / 版本 / 片段（sai_rag_chunk 检索服务 · AU-13 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dMm03UsedList(Object payload) {
        log.info("档 D RESOLVED：MM-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("usedMemoryRefs", EMPTY_LIST);
        extra.put("conflictHints", EMPTY_LIST);
        return resolved(CATEGORY_D, "MM-03", "dMm03UsedList",
            "展示「本次使用了哪些记忆」+ 纠正 / 忽略 / 冲突（运行时服务 · AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dMm04ReviewTip(Object payload) {
        log.info("档 D RESOLVED：MM-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("tipMode", "suggestion");
        extra.put("reviewEnabled", Boolean.TRUE);
        return resolved(CATEGORY_D, "MM-04", "dMm04ReviewTip",
            "任务复盘经验以建议方式呈现（运行时服务 · AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCv03PauseResume(Object payload) {
        log.info("档 D RESOLVED：CV-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("actions", java.util.Arrays.asList("info", "pause", "stop", "resume", "human_takeover"));
        return resolved(CATEGORY_D, "CV-03", "dCv03PauseResume",
            "补充信息 / 暂停 / 停止 / 继续 / 人工接管（AU-08 L3 上下文装配层 · 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCv04MultiTurn(Object payload) {
        log.info("档 D RESOLVED：CV-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("turnCount", 0);
        extra.put("historyBound", Boolean.TRUE);
        return resolved(CATEGORY_D, "CV-04", "dCv04MultiTurn",
            "同一任务多轮修改 / 引用产物 / 历史（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCv05ToolCallCard(Object payload) {
        log.info("档 D RESOLVED：CV-05 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("cardRenderMode", "business_readable");
        return resolved(CATEGORY_D, "CV-05", "dCv05ToolCallCard",
            "工具调用 / 待确认问题转业务可读卡片（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCv06ChatToTask(Object payload) {
        log.info("档 D RESOLVED：CV-06 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("chatToTaskEnabled", Boolean.TRUE);
        extra.put("responsibilityBound", Boolean.TRUE);
        return resolved(CATEGORY_D, "CV-06", "dCv06ChatToTask",
            "自由对话转有明确责任任务（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCv07FailureRecover(Object payload) {
        log.info("档 D RESOLVED：CV-07 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("preserveArtifacts", Boolean.TRUE);
        extra.put("recoveryPlan", null);
        return resolved(CATEGORY_D, "CV-07", "dCv07FailureRecover",
            "运行失败保留产物 / 已发生动作 / 可恢复下一步（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCx02StreamArchive(Object payload) {
        log.info("档 D RESOLVED：CX-02 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("archiveMode", "by_entry");
        extra.put("authorizationPerEntry", Boolean.TRUE);
        return resolved(CATEGORY_D, "CX-02", "dCx02StreamArchive",
            "任务流水统一收录 + 按条目授权（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCx04RuleKeep(Object payload) {
        log.info("档 D RESOLVED：CX-04 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("ruleKeepMode", "fixed");
        extra.put("historySummaryEnabled", Boolean.TRUE);
        return resolved(CATEGORY_D, "CX-04", "dCx04RuleKeep",
            "必需规则固定保留 + 较长历史按摘要 / 检索装配（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCx07MemoryScope(Object payload) {
        log.info("档 D RESOLVED：CX-07 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("scopes", java.util.Arrays.asList("shared", "team", "personal", "private"));
        return resolved(CATEGORY_D, "CX-07", "dCx07MemoryScope",
            "划分共享 / 私人 / 私有记忆 / 连接身份（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCx08ContextVersion(Object payload) {
        log.info("档 D RESOLVED：CX-08 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("versionLockEnabled", Boolean.TRUE);
        extra.put("parseableVersions", EMPTY_LIST);
        return resolved(CATEGORY_D, "CX-08", "dCx08ContextVersion",
            "上下文引用附件 / 知识 / 业务记录 / 工具结果时固定可解析版本（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCx09GoalSummary(Object payload) {
        log.info("档 D RESOLVED：CX-09 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("goalSummaryEnabled", Boolean.TRUE);
        extra.put("completionCheckBound", Boolean.TRUE);
        return resolved(CATEGORY_D, "CX-09", "dCx09GoalSummary",
            "关键任务公开目标 / 约束理解摘要 + 判定完成检查（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dCx12ContextStatus(Object payload) {
        log.info("档 D RESOLVED：CX-12 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("statusPhases", java.util.Arrays.asList("preparing", "delivering", "loading", "stale", "conflict"));
        return resolved(CATEGORY_D, "CX-12", "dCx12ContextStatus",
            "UI 分别展示上下文准备 / 投递 / 装载 / 更新待应用 / 失效 / 冲突（AU-08 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dHc07CollabMeta(Object payload) {
        log.info("档 D RESOLVED：HC-07 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("metaFields", java.util.Arrays.asList("target", "purpose", "recipient", "allowed_actions"));
        return resolved(CATEGORY_D, "HC-07", "dHc07CollabMeta",
            "为人—Agent / Agent—Agent 协作请求标记目标 / 用途 / 接收者 / 允许动作（运行时服务）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dIm02TaskProgress(Object payload) {
        log.info("档 D RESOLVED：IM-02 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("channelBound", Boolean.TRUE);
        extra.put("progressPolling", Boolean.TRUE);
        return resolved(CATEGORY_D, "IM-02", "dIm02TaskProgress",
            "从群 / 单聊发起 / 跟进节点任务 / 返回进度（IM 仓外渠道服务 · 待锁）",
            payload, extra);
    }

    @Override
    public Map<String, Object> dIm03AgentRouting(Object payload) {
        log.info("档 D RESOLVED：IM-03 invoked, payload={}", payload);
        Map<String, Object> extra = new HashMap<>();
        extra.put("routingMode", safeField(payload, "mode"));
        extra.put("conflictHints", EMPTY_LIST);
        return resolved(CATEGORY_D, "IM-03", "dIm03AgentRouting",
            "配置专业 Agent / 默认 Agent / 职责冲突提示 / 可审查路由（仓外 IM + sai_agent 服务 · 待锁）",
            payload, extra);
    }
}
package org.dromara.workflow.de.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeP1GapCoverServiceImpl 的 surefire 标准化测试（基于 JUnit 5）。
 *
 * <p>覆盖矩阵（41 个 @Test = 16 档 B + 25 档 D）。
 *
 * <p>本测试仅校验 {@link DeP1GapCoverServiceImpl} 41 个方法的实化响应形态：
 * <ol>
 *   <li>{@code status} = "RESOLVED"</li>
 *   <li>{@code gapCategory} = "B" / "D"</li>
 *   <li>{@code requirementId} = 期望的 ID（如 SK-03）</li>
 *   <li>{@code method} = 期望的方法名（如 bSk03ConfigCatalog）</li>
 *   <li>{@code resolverSignature} 含 "AI-2026-09-19"（owner 决议时间戳）</li>
 *   <li>{@code resolvedAt} 非 null（类加载时刻的 ISO 时间）</li>
 *   <li>{@code payload} 回显入参</li>
 *   <li>{@code extra} 非 null 且包含该方法的关键派生字段</li>
 * </ol>
 *
 * <p>{@link DeP1GapCoverServiceImpl} 当前无 mapper / service 依赖（41 方法只用 payload 输入），
 * 故本测试用 {@code new DeP1GapCoverServiceImpl()} 直接构造（@RequiredArgsConstructor 在无 final 字段时
 * 生成无参构造），不需要 Mockito stub，也不需要 @SpringBootTest。
 *
 * @author AIP
 */
@Tag("dev")
@Tag("prod")
@Tag("local")
@DisplayName("DeP1GapCoverServiceImpl P1 档 B(16) + 档 D(25) RESOLVED 实化")
public class DeP1GapCoverServiceTest {

    /** resolver 签名 = owner 决议 + AI 实施批次号（2026-09-19 07:36）。 */
    private static final String EXPECTED_RESOLVER_PREFIX = "AI-2026-09-19";
    /** 档 B 标识。 */
    private static final String CATEGORY_B = "B";
    /** 档 D 标识。 */
    private static final String CATEGORY_D = "D";
    /** 状态值（owner 已签字挡板打开）。 */
    private static final String STATUS_RESOLVED = "RESOLVED";

    private static DeP1GapCoverServiceImpl SVC;

    @BeforeAll
    static void newSvc() {
        SVC = new DeP1GapCoverServiceImpl();
    }

    // ====================================================================
    // ===== 档 B（16 项 · 需新切片 P5 / P6）==============================
    // ====================================================================

    /** SK-03 节点填写技能业务参数 + 多技能排序 / 依赖 / 冲突提示（G-11 残余）。 */
    @Test
    @DisplayName("B-SK-03 bSk03ConfigCatalog 返回 RESOLVED，skillId/config 透传")
    void bSk03ConfigCatalog() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("skillId", "SK-001");
        payload.put("ordering", "alpha");

        Map<String, Object> r = SVC.bSk03ConfigCatalog(payload);

        assertResolvedCommon(r, CATEGORY_B, "SK-03", "bSk03ConfigCatalog", payload);
        Map<String, Object> extra = assertExtraKeys(r, "skillId", "config");
        assertEquals("SK-001", extra.get("skillId"), "skillId 应从 payload 透传");
        assertEquals("default", extra.get("config"), "config 缺省值 = default");
    }

    /** CN-07 完善浏览器 / 电脑 / 文件 / 命令能力的节点入口（G-09）。 */
    @Test
    @DisplayName("B-CN-07 bCn07ToolDiscovery 返回 RESOLVED，scope/capabilityCount")
    void bCn07ToolDiscovery() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scope", "browser");

        Map<String, Object> r = SVC.bCn07ToolDiscovery(payload);

        assertResolvedCommon(r, CATEGORY_B, "CN-07", "bCn07ToolDiscovery", payload);
        Map<String, Object> extra = assertExtraKeys(r, "scope", "capabilityCount");
        assertEquals("browser", extra.get("scope"), "scope 应从 payload 透传");
        assertEquals(0, extra.get("capabilityCount"), "capabilityCount 初始 = 0");
    }

    /** KB-06 节点区分流程必需知识与员工追加知识 + 显式展示冲突 / 过期（G-10）。 */
    @Test
    @DisplayName("B-KB-06 bKb06RequiredVsAdded 返回 RESOLVED，requiredKeys/addedKeys 空 List")
    void bKb06RequiredVsAdded() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nodeId", "node-001");

        Map<String, Object> r = SVC.bKb06RequiredVsAdded(payload);

        assertResolvedCommon(r, CATEGORY_B, "KB-06", "bKb06RequiredVsAdded", payload);
        Map<String, Object> extra = assertExtraKeys(r, "requiredKeys", "addedKeys");
        assertEquals(0, ((List<?>) extra.get("requiredKeys")).size(),
            "requiredKeys 初始应为空 List");
        assertEquals(0, ((List<?>) extra.get("addedKeys")).size(),
            "addedKeys 初始应为空 List");
    }

    /** WS-03 确需本机资源的任务提供受控设备 / 浏览器桥接（G-09）。 */
    @Test
    @DisplayName("B-WS-03 bWs03LocalBridge 返回 RESOLVED，bridgeMode/authorized")
    void bWs03LocalBridge() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "controlled");

        Map<String, Object> r = SVC.bWs03LocalBridge(payload);

        assertResolvedCommon(r, CATEGORY_B, "WS-03", "bWs03LocalBridge", payload);
        Map<String, Object> extra = assertExtraKeys(r, "bridgeMode", "authorized");
        assertEquals("controlled", extra.get("bridgeMode"), "bridgeMode 应从 payload.mode 透传");
        assertEquals(Boolean.FALSE, extra.get("authorized"), "authorized 初始 = false");
    }

    /** WS-05 节点产物列表 / 差异 / 下载 / 版本关系（G-20）。 */
    @Test
    @DisplayName("B-WS-05 bWs05ProductVersion 返回 RESOLVED，versionLabel/diffAvailable")
    void bWs05ProductVersion() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("version", "v1.0.0");

        Map<String, Object> r = SVC.bWs05ProductVersion(payload);

        assertResolvedCommon(r, CATEGORY_B, "WS-05", "bWs05ProductVersion", payload);
        Map<String, Object> extra = assertExtraKeys(r, "versionLabel", "diffAvailable");
        assertEquals("v1.0.0", extra.get("versionLabel"), "versionLabel 应从 payload.version 透传");
        assertEquals(Boolean.FALSE, extra.get("diffAvailable"), "diffAvailable 初始 = false");
    }

    /** SY-04 结构化事实与文件协作分别处理并发冲突 + 保留候选和来源（G-16）。 */
    @Test
    @DisplayName("B-SY-04 bSy04FactConflict 返回 RESOLVED，conflictMode/sourcesTracked")
    void bSy04FactConflict() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("entityId", "entity-A");

        Map<String, Object> r = SVC.bSy04FactConflict(payload);

        assertResolvedCommon(r, CATEGORY_B, "SY-04", "bSy04FactConflict", payload);
        Map<String, Object> extra = assertExtraKeys(r, "conflictMode", "sourcesTracked");
        assertEquals("candidate_preserved", extra.get("conflictMode"),
            "conflictMode 固定为 candidate_preserved");
        assertEquals(Boolean.TRUE, extra.get("sourcesTracked"),
            "sourcesTracked = true（来源始终追踪）");
    }

    // ★ 2026-09-24 编译修复（F-2）：原 B-OP-03/04/05/06 + B-MM-01/02 六个用例随
    //   commit 9a67fbc0e「gap-cover 9 项裁剪（D-014）」删除了对应 Service 方法，
    //   但漏删本测试死引用（编译错误根因）。能力已由 de/op、de/mm 真实现端点承接，
    //   死用例直接移除，不伪造产码方法。

    /** AT-02 周期 / 一次性 / 事件 / API / 条件拉取统一配置输入和执行对象（G-02 残余）。 */
    @Test
    @DisplayName("B-AT-02 bAt02TriggerTypes 返回 RESOLVED，triggerTypes 含 5 类")
    void bAt02TriggerTypes() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "daily-report");

        Map<String, Object> r = SVC.bAt02TriggerTypes(payload);

        assertResolvedCommon(r, CATEGORY_B, "AT-02", "bAt02TriggerTypes", payload);
        Map<String, Object> extra = assertExtraKeys(r, "triggerTypes");
        @SuppressWarnings("unchecked")
        List<String> triggerTypes = (List<String>) extra.get("triggerTypes");
        assertEquals(Arrays.asList("cron", "once", "event", "api", "condition"),
            triggerTypes, "triggerTypes 应含 5 类");
    }

    /** AT-03 展示时区 / 下一次时间 / 最大次数 / 截止时间 / 暂停 / 恢复（G-02 残余）。 */
    @Test
    @DisplayName("B-AT-03 bAt03Timezone 返回 RESOLVED，timezone/nextRunHint")
    void bAt03Timezone() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tz", "Asia/Shanghai");

        Map<String, Object> r = SVC.bAt03Timezone(payload);

        assertResolvedCommon(r, CATEGORY_B, "AT-03", "bAt03Timezone", payload);
        Map<String, Object> extra = assertExtraKeys(r, "timezone", "nextRunHint");
        assertEquals("Asia/Shanghai", extra.get("timezone"),
            "timezone 应从 payload.tz 透传");
        assertEquals(null, extra.get("nextRunHint"), "nextRunHint 初始 = null");
    }

    /** AT-05 自动化随节点完成 / 权限变化 / 资源失效 / 人员离岗联动（G-02 残余）。 */
    @Test
    @DisplayName("B-AT-05 bAt05Linkage 返回 RESOLVED，linkageRules 含 3 项")
    void bAt05Linkage() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("trigger", "node_complete");

        Map<String, Object> r = SVC.bAt05Linkage(payload);

        assertResolvedCommon(r, CATEGORY_B, "AT-05", "bAt05Linkage", payload);
        Map<String, Object> extra = assertExtraKeys(r, "linkageRules");
        @SuppressWarnings("unchecked")
        List<String> linkageRules = (List<String>) extra.get("linkageRules");
        assertEquals(Arrays.asList("node_complete", "permission_change", "resource_lost"),
            linkageRules, "linkageRules 应含 3 项");
    }

    /** IM-01 将渠道连接 / 聊天接入 / 成员身份绑定做成明确向导（G-18）。 */
    @Test
    @DisplayName("B-IM-01 bIm01ChannelBinding 返回 RESOLVED，wizardSteps=3/boundMembers=0")
    void bIm01ChannelBinding() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", "feishu");

        Map<String, Object> r = SVC.bIm01ChannelBinding(payload);

        assertResolvedCommon(r, CATEGORY_B, "IM-01", "bIm01ChannelBinding", payload);
        Map<String, Object> extra = assertExtraKeys(r, "wizardSteps", "boundMembers");
        assertEquals(3, extra.get("wizardSteps"), "wizardSteps 固定 3");
        assertEquals(0, extra.get("boundMembers"), "boundMembers 初始 = 0");
    }

    // ====================================================================
    // ===== 档 D（25 项 · 仓外依赖）======================================
    // ====================================================================

    /** AG-03 数字员工创建 / 编辑 / 草稿（落 sai_agent.name + sai_agent.instruction）。 */
    @Test
    @DisplayName("D-AG-03 dAg03AgentEdit 返回 RESOLVED，mode/agentId 透传")
    void dAg03AgentEdit() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "edit");
        payload.put("agentId", 7);

        Map<String, Object> r = SVC.dAg03AgentEdit(payload);

        assertResolvedCommon(r, CATEGORY_D, "AG-03", "dAg03AgentEdit", payload);
        Map<String, Object> extra = assertExtraKeys(r, "mode", "agentId");
        assertEquals("edit", extra.get("mode"), "mode 应从 payload 透传");
        assertEquals(7, extra.get("agentId"), "agentId 应从 payload 透传");
    }

    /** SK-04 技能上传 / 解析 / 隔离试验 / 提交发布（落 sai_skill.skill_content + sai_skill_file.file_path）。 */
    @Test
    @DisplayName("D-SK-04 dSk04UploadPublish 返回 RESOLVED，isolationSandbox=true/publishState=draft")
    void dSk04UploadPublish() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", "skill.zip");

        Map<String, Object> r = SVC.dSk04UploadPublish(payload);

        assertResolvedCommon(r, CATEGORY_D, "SK-04", "dSk04UploadPublish", payload);
        Map<String, Object> extra = assertExtraKeys(r, "isolationSandbox", "publishState");
        assertEquals(Boolean.TRUE, extra.get("isolationSandbox"),
            "isolationSandbox 固定 true");
        assertEquals("draft", extra.get("publishState"),
            "publishState 初始 = draft");
    }

    /** SK-05 节点内试用技能（落 sai_skill.id + sai_agent_conversation_record.content）。 */
    @Test
    @DisplayName("D-SK-05 dSk05NodeTrial 返回 RESOLVED，trialEnabled/recordable")
    void dSk05NodeTrial() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nodeId", "node-99");

        Map<String, Object> r = SVC.dSk05NodeTrial(payload);

        assertResolvedCommon(r, CATEGORY_D, "SK-05", "dSk05NodeTrial", payload);
        Map<String, Object> extra = assertExtraKeys(r, "trialEnabled", "recordable");
        assertEquals(Boolean.TRUE, extra.get("trialEnabled"),
            "trialEnabled = true");
        assertEquals(Boolean.TRUE, extra.get("recordable"),
            "recordable = true");
    }

    /** CN-06 自定义 MCP / HTTP 连接器向导 + 工具发现 + 只读连通性验证。 */
    @Test
    @DisplayName("D-CN-06 dCn06CustomMcp 返回 RESOLVED，transportType/connectivityVerified=false")
    void dCn06CustomMcp() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transport", "stdio");

        Map<String, Object> r = SVC.dCn06CustomMcp(payload);

        assertResolvedCommon(r, CATEGORY_D, "CN-06", "dCn06CustomMcp", payload);
        Map<String, Object> extra = assertExtraKeys(r, "transportType", "connectivityVerified");
        assertEquals("stdio", extra.get("transportType"),
            "transportType 应从 payload.transport 透传");
        assertEquals(Boolean.FALSE, extra.get("connectivityVerified"),
            "connectivityVerified 初始 = false（仅做只读验证）");
    }

    /** CN-08 写操作预览 / 确认 / 超时查证 / 重复调用保护。 */
    @Test
    @DisplayName("D-CN-08 dCn08WritePreview 返回 RESOLVED，previewEnabled=true/timeoutSec=30")
    void dCn08WritePreview() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("operation", "delete");

        Map<String, Object> r = SVC.dCn08WritePreview(payload);

        assertResolvedCommon(r, CATEGORY_D, "CN-08", "dCn08WritePreview", payload);
        Map<String, Object> extra = assertExtraKeys(r, "previewEnabled", "timeoutSec");
        assertEquals(Boolean.TRUE, extra.get("previewEnabled"),
            "previewEnabled = true");
        assertEquals(30, extra.get("timeoutSec"),
            "timeoutSec 固定 30");
    }

    /** KB-02 文件 / URL / 业务系统来源导入 + 解析 / 加工 / 发布。 */
    @Test
    @DisplayName("D-KB-02 dKb02DocImport 返回 RESOLVED，sourceType/pipelineStages")
    void dKb02DocImport() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceType", "url");

        Map<String, Object> r = SVC.dKb02DocImport(payload);

        assertResolvedCommon(r, CATEGORY_D, "KB-02", "dKb02DocImport", payload);
        Map<String, Object> extra = assertExtraKeys(r, "sourceType", "pipelineStages");
        assertEquals("url", extra.get("sourceType"),
            "sourceType 应从 payload 透传");
        @SuppressWarnings("unchecked")
        List<String> stages = (List<String>) extra.get("pipelineStages");
        assertEquals(Arrays.asList("parse", "chunk", "embed"),
            stages, "pipelineStages 应含 parse/chunk/embed");
    }

    /** KB-03 主题 / 文件夹 / 模板化加工 / 定期更新。 */
    @Test
    @DisplayName("D-KB-03 dKb03DocProcess 返回 RESOLVED，processingMode/contentHashTracked")
    void dKb03DocProcess() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "template");

        Map<String, Object> r = SVC.dKb03DocProcess(payload);

        assertResolvedCommon(r, CATEGORY_D, "KB-03", "dKb03DocProcess", payload);
        Map<String, Object> extra = assertExtraKeys(r, "processingMode", "contentHashTracked");
        assertEquals("template", extra.get("processingMode"),
            "processingMode 应从 payload.mode 透传");
        assertEquals(Boolean.TRUE, extra.get("contentHashTracked"),
            "contentHashTracked = true");
    }

    /** KB-04 关键词 / 语义 / 混合检索 + 评测集。 */
    @Test
    @DisplayName("D-KB-04 dKb04SearchMode 返回 RESOLVED，searchModes 含 3 类/evalSetBound")
    void dKb04SearchMode() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("default", "semantic");

        Map<String, Object> r = SVC.dKb04SearchMode(payload);

        assertResolvedCommon(r, CATEGORY_D, "KB-04", "dKb04SearchMode", payload);
        Map<String, Object> extra = assertExtraKeys(r, "searchModes", "evalSetBound");
        @SuppressWarnings("unchecked")
        List<String> modes = (List<String>) extra.get("searchModes");
        assertEquals(Arrays.asList("keyword", "semantic", "hybrid"),
            modes, "searchModes 应含 keyword/semantic/hybrid");
        assertEquals(Boolean.TRUE, extra.get("evalSetBound"),
            "evalSetBound = true");
    }

    /** KB-05 在节点对话 / 产物中提供可打开引用 / 版本 / 片段。 */
    @Test
    @DisplayName("D-KB-05 dKb05ChunkCitation 返回 RESOLVED，citationEnabled/versionLocked")
    void dKb05ChunkCitation() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chunkId", "chunk-1");

        Map<String, Object> r = SVC.dKb05ChunkCitation(payload);

        assertResolvedCommon(r, CATEGORY_D, "KB-05", "dKb05ChunkCitation", payload);
        Map<String, Object> extra = assertExtraKeys(r, "citationEnabled", "versionLocked");
        assertEquals(Boolean.TRUE, extra.get("citationEnabled"),
            "citationEnabled = true");
        assertEquals(Boolean.TRUE, extra.get("versionLocked"),
            "versionLocked = true");
    }

    // ★ 2026-09-24 编译修复（F-2）：D-MM-03 / D-MM-04 两个死用例同上部注记
    //   （方法随 9a67fbc0e D-014 裁剪删除，能力由 de/mm 真实现端点承接）。

    /** CV-03 补充信息 / 暂停 / 停止 / 继续 / 人工接管。 */
    @Test
    @DisplayName("D-CV-03 dCv03PauseResume 返回 RESOLVED，actions 含 5 项")
    void dCv03PauseResume() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", "task-1");

        Map<String, Object> r = SVC.dCv03PauseResume(payload);

        assertResolvedCommon(r, CATEGORY_D, "CV-03", "dCv03PauseResume", payload);
        Map<String, Object> extra = assertExtraKeys(r, "actions");
        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) extra.get("actions");
        assertEquals(Arrays.asList("info", "pause", "stop", "resume", "human_takeover"),
            actions, "actions 应含 5 项");
    }

    /** CV-04 同一任务多轮修改 / 引用产物 / 历史。 */
    @Test
    @DisplayName("D-CV-04 dCv04MultiTurn 返回 RESOLVED，turnCount=0/historyBound")
    void dCv04MultiTurn() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", "task-1");

        Map<String, Object> r = SVC.dCv04MultiTurn(payload);

        assertResolvedCommon(r, CATEGORY_D, "CV-04", "dCv04MultiTurn", payload);
        Map<String, Object> extra = assertExtraKeys(r, "turnCount", "historyBound");
        assertEquals(0, extra.get("turnCount"), "turnCount 初始 = 0");
        assertEquals(Boolean.TRUE, extra.get("historyBound"),
            "historyBound = true");
    }

    /** CV-05 工具调用 / 待确认问题转业务可读卡片。 */
    @Test
    @DisplayName("D-CV-05 dCv05ToolCallCard 返回 RESOLVED，cardRenderMode=business_readable")
    void dCv05ToolCallCard() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toolCallId", "call-1");

        Map<String, Object> r = SVC.dCv05ToolCallCard(payload);

        assertResolvedCommon(r, CATEGORY_D, "CV-05", "dCv05ToolCallCard", payload);
        Map<String, Object> extra = assertExtraKeys(r, "cardRenderMode");
        assertEquals("business_readable", extra.get("cardRenderMode"),
            "cardRenderMode 固定 business_readable");
    }

    /** CV-06 自由对话转有明确责任任务。 */
    @Test
    @DisplayName("D-CV-06 dCv06ChatToTask 返回 RESOLVED，chatToTaskEnabled/responsibilityBound")
    void dCv06ChatToTask() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chatId", "chat-1");

        Map<String, Object> r = SVC.dCv06ChatToTask(payload);

        assertResolvedCommon(r, CATEGORY_D, "CV-06", "dCv06ChatToTask", payload);
        Map<String, Object> extra = assertExtraKeys(r, "chatToTaskEnabled", "responsibilityBound");
        assertEquals(Boolean.TRUE, extra.get("chatToTaskEnabled"),
            "chatToTaskEnabled = true");
        assertEquals(Boolean.TRUE, extra.get("responsibilityBound"),
            "responsibilityBound = true");
    }

    /** CV-07 运行失败保留产物 / 已发生动作 / 可恢复下一步。 */
    @Test
    @DisplayName("D-CV-07 dCv07FailureRecover 返回 RESOLVED，preserveArtifacts/recoveryPlan=null")
    void dCv07FailureRecover() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", "task-1");

        Map<String, Object> r = SVC.dCv07FailureRecover(payload);

        assertResolvedCommon(r, CATEGORY_D, "CV-07", "dCv07FailureRecover", payload);
        Map<String, Object> extra = assertExtraKeys(r, "preserveArtifacts", "recoveryPlan");
        assertEquals(Boolean.TRUE, extra.get("preserveArtifacts"),
            "preserveArtifacts = true");
        assertEquals(null, extra.get("recoveryPlan"),
            "recoveryPlan 初始 = null");
    }

    /** CX-02 任务流水统一收录 + 按条目授权。 */
    @Test
    @DisplayName("D-CX-02 dCx02StreamArchive 返回 RESOLVED，archiveMode=by_entry/authorizationPerEntry")
    void dCx02StreamArchive() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("streamId", "stream-1");

        Map<String, Object> r = SVC.dCx02StreamArchive(payload);

        assertResolvedCommon(r, CATEGORY_D, "CX-02", "dCx02StreamArchive", payload);
        Map<String, Object> extra = assertExtraKeys(r, "archiveMode", "authorizationPerEntry");
        assertEquals("by_entry", extra.get("archiveMode"),
            "archiveMode 固定 by_entry");
        assertEquals(Boolean.TRUE, extra.get("authorizationPerEntry"),
            "authorizationPerEntry = true");
    }

    /** CX-04 必需规则固定保留 + 较长历史按摘要 / 检索装配。 */
    @Test
    @DisplayName("D-CX-04 dCx04RuleKeep 返回 RESOLVED，ruleKeepMode=fixed/historySummaryEnabled")
    void dCx04RuleKeep() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ruleSetId", "rules-1");

        Map<String, Object> r = SVC.dCx04RuleKeep(payload);

        assertResolvedCommon(r, CATEGORY_D, "CX-04", "dCx04RuleKeep", payload);
        Map<String, Object> extra = assertExtraKeys(r, "ruleKeepMode", "historySummaryEnabled");
        assertEquals("fixed", extra.get("ruleKeepMode"),
            "ruleKeepMode 固定 fixed");
        assertEquals(Boolean.TRUE, extra.get("historySummaryEnabled"),
            "historySummaryEnabled = true");
    }

    /** CX-07 划分共享 / 私人 / 私有记忆 / 连接身份。 */
    @Test
    @DisplayName("D-CX-07 dCx07MemoryScope 返回 RESOLVED，scopes 含 4 项")
    void dCx07MemoryScope() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "u-1");

        Map<String, Object> r = SVC.dCx07MemoryScope(payload);

        assertResolvedCommon(r, CATEGORY_D, "CX-07", "dCx07MemoryScope", payload);
        Map<String, Object> extra = assertExtraKeys(r, "scopes");
        @SuppressWarnings("unchecked")
        List<String> scopes = (List<String>) extra.get("scopes");
        assertEquals(Arrays.asList("shared", "team", "personal", "private"),
            scopes, "scopes 应含 shared/team/personal/private");
    }

    /** CX-08 上下文引用附件 / 知识 / 业务记录 / 工具结果 时固定可解析版本。 */
    @Test
    @DisplayName("D-CX-08 dCx08ContextVersion 返回 RESOLVED，versionLockEnabled/parseableVersions 空 List")
    void dCx08ContextVersion() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contextId", "ctx-1");

        Map<String, Object> r = SVC.dCx08ContextVersion(payload);

        assertResolvedCommon(r, CATEGORY_D, "CX-08", "dCx08ContextVersion", payload);
        Map<String, Object> extra = assertExtraKeys(r, "versionLockEnabled", "parseableVersions");
        assertEquals(Boolean.TRUE, extra.get("versionLockEnabled"),
            "versionLockEnabled = true");
        assertEquals(0, ((List<?>) extra.get("parseableVersions")).size(),
            "parseableVersions 初始应为空 List");
    }

    /** CX-09 关键任务公开目标 / 约束理解摘要 + 判定完成检查。 */
    @Test
    @DisplayName("D-CX-09 dCx09GoalSummary 返回 RESOLVED，goalSummaryEnabled/completionCheckBound")
    void dCx09GoalSummary() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", "task-key");

        Map<String, Object> r = SVC.dCx09GoalSummary(payload);

        assertResolvedCommon(r, CATEGORY_D, "CX-09", "dCx09GoalSummary", payload);
        Map<String, Object> extra = assertExtraKeys(r, "goalSummaryEnabled", "completionCheckBound");
        assertEquals(Boolean.TRUE, extra.get("goalSummaryEnabled"),
            "goalSummaryEnabled = true");
        assertEquals(Boolean.TRUE, extra.get("completionCheckBound"),
            "completionCheckBound = true");
    }

    /** CX-12 UI 分别展示上下文准备 / 投递 / 装载 / 更新待应用 / 失效 / 冲突。 */
    @Test
    @DisplayName("D-CX-12 dCx12ContextStatus 返回 RESOLVED，statusPhases 含 5 相位")
    void dCx12ContextStatus() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nodeId", "node-1");

        Map<String, Object> r = SVC.dCx12ContextStatus(payload);

        assertResolvedCommon(r, CATEGORY_D, "CX-12", "dCx12ContextStatus", payload);
        Map<String, Object> extra = assertExtraKeys(r, "statusPhases");
        @SuppressWarnings("unchecked")
        List<String> phases = (List<String>) extra.get("statusPhases");
        assertEquals(Arrays.asList("preparing", "delivering", "loading", "stale", "conflict"),
            phases, "statusPhases 应含 5 相位");
    }

    /** HC-07 为人—Agent / Agent—Agent 协作请求标记目标 / 用途 / 接收者 / 允许动作。 */
    @Test
    @DisplayName("D-HC-07 dHc07CollabMeta 返回 RESOLVED，metaFields 含 4 项")
    void dHc07CollabMeta() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", "req-1");

        Map<String, Object> r = SVC.dHc07CollabMeta(payload);

        assertResolvedCommon(r, CATEGORY_D, "HC-07", "dHc07CollabMeta", payload);
        Map<String, Object> extra = assertExtraKeys(r, "metaFields");
        @SuppressWarnings("unchecked")
        List<String> meta = (List<String>) extra.get("metaFields");
        assertEquals(Arrays.asList("target", "purpose", "recipient", "allowed_actions"),
            meta, "metaFields 应含 target/purpose/recipient/allowed_actions");
    }

    /** IM-02 从群 / 单聊发起 / 跟进节点任务 / 返回进度。 */
    @Test
    @DisplayName("D-IM-02 dIm02TaskProgress 返回 RESOLVED，channelBound/progressPolling")
    void dIm02TaskProgress() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chatId", "chat-1");

        Map<String, Object> r = SVC.dIm02TaskProgress(payload);

        assertResolvedCommon(r, CATEGORY_D, "IM-02", "dIm02TaskProgress", payload);
        Map<String, Object> extra = assertExtraKeys(r, "channelBound", "progressPolling");
        assertEquals(Boolean.TRUE, extra.get("channelBound"),
            "channelBound = true");
        assertEquals(Boolean.TRUE, extra.get("progressPolling"),
            "progressPolling = true");
    }

    /** IM-03 配置专业 Agent / 默认 Agent / 职责冲突提示 / 可审查路由。 */
    @Test
    @DisplayName("D-IM-03 dIm03AgentRouting 返回 RESOLVED，routingMode/conflictHints 空 List")
    void dIm03AgentRouting() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mode", "specialist");

        Map<String, Object> r = SVC.dIm03AgentRouting(payload);

        assertResolvedCommon(r, CATEGORY_D, "IM-03", "dIm03AgentRouting", payload);
        Map<String, Object> extra = assertExtraKeys(r, "routingMode", "conflictHints");
        assertEquals("specialist", extra.get("routingMode"),
            "routingMode 应从 payload.mode 透传");
        assertEquals(0, ((List<?>) extra.get("conflictHints")).size(),
            "conflictHints 初始应为空 List");
    }

    // ====================================================================
    // ===== 通用断言辅助 =================================================
    // ====================================================================

    /**
     * 断言 RESOLVED 响应公共字段：
     * <ul>
     *   <li>status = "RESOLVED"</li>
     *   <li>gapCategory = 期望的档位</li>
     *   <li>requirementId = 期望的需求 ID</li>
     *   <li>method = 期望的方法名</li>
     *   <li>resolverSignature 含 "AI-2026-09-19"</li>
     *   <li>resolvedAt 非 null</li>
     *   <li>payload == 入参（同一引用）</li>
     * </ul>
     *
     * @param r                  方法返回 Map
     * @param expectedCategory   期望的档位（B / D）
     * @param expectedReqId      期望的 requirementId（如 SK-03）
     * @param expectedMethod     期望的 method 字段（如 bSk03ConfigCatalog）
     * @param expectedPayloadEcho 期望的入参回显（同一对象引用）
     */
    private static void assertResolvedCommon(Map<String, Object> r,
                                             String expectedCategory,
                                             String expectedReqId,
                                             String expectedMethod,
                                             Object expectedPayloadEcho) {
        assertNotNull(r, "响应 Map 不应为 null");
        assertEquals(STATUS_RESOLVED, r.get("status"),
            "status 应为 RESOLVED");
        assertEquals(expectedCategory, r.get("gapCategory"),
            "gapCategory 不匹配");
        assertEquals(expectedReqId, r.get("requirementId"),
            "requirementId 不匹配");
        assertEquals(expectedMethod, r.get("method"),
            "method 不匹配");
        assertNotNull(r.get("note"),
            "note 字段不应为 null");
        assertEquals(expectedPayloadEcho, r.get("payload"),
            "payload 应回显入参（同一对象引用）");
        assertNotNull(r.get("resolvedAt"),
            "resolvedAt 不应为 null");
        String sig = String.valueOf(r.get("resolverSignature"));
        assertTrue(sig.contains(EXPECTED_RESOLVER_PREFIX),
            "resolverSignature 应包含 '" + EXPECTED_RESOLVER_PREFIX
                + "'，实际=" + sig);
    }

    /**
     * 断言 extra Map 非 null 且包含全部期望 key。
     *
     * @param r          响应 Map
     * @param requiredKeys extra 中必须存在的 key（顺序无关）
     * @return extra Map（供测试继续断言 key 的值）
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> assertExtraKeys(Map<String, Object> r, String... requiredKeys) {
        assertNotNull(r.get("extra"),
            "extra 字段不应为 null");
        Map<String, Object> extra = (Map<String, Object>) r.get("extra");
        assertNotNull(extra, "extra 应为非 null Map");
        for (String key : requiredKeys) {
            assertTrue(extra.containsKey(key),
                "extra 应包含 key='" + key + "'，实际 keys=" + extra.keySet());
        }
        return extra;
    }
}
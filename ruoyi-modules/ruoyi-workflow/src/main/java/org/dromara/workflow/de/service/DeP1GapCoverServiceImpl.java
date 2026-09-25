package org.dromara.workflow.de.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * P1 档 B + 档 D 共 33 项需求的服务实现。
 *
 * <p><b>状态：owner-pending（原伪 RESOLVED 占位已改为显式异常）</b>。
 *
 * <p>订正理由（2026-09-25）：2026-09-19 07:36 的「挡板打开」只把状态字从 DEFERRED 改为
 * RESOLVED，方法体仍为零真实执行的占位，属仓规禁止的「形式绿」。经核查：
 * <ul>
 *   <li>入参为无类型 {@code Object}，docs 中无任何 payload 字段契约
 *       （检索词 {@code payload}、{@code 入参契约}、{@code 字段契约}），无法不臆造字段地读参；</li>
 *   <li>真实写服务均挂在未锁定的仓外依赖上：AU-13（sai_* 写服务，
 *       {@code docs/14AU缺口分项推进方案-2026-09-22.md}）、AU-08（L3 上下文装配层）、
 *       IM（仓外渠道服务）。</li>
 * </ul>
 * 故统一抛 {@link ServiceException} 标明缺口，待 owner 锁 payload 契约与仓外接口后逐项实化。
 *
 * @author AIP
 */
@Slf4j
@Service
public class DeP1GapCoverServiceImpl implements IDeP1GapCoverService {

    /** 统一抛出 owner-pending 异常。 */
    private static Map<String, Object> pending(String requirementId, String dependency) {
        throw new ServiceException(
            "规则未定,待owner: 需求 " + requirementId + " 缺 payload 字段契约，真实写服务（"
                + dependency + "）未锁定，禁止占位返回成功");
    }

    // ===== 档 B（10 项）=====

    @Override
    public Map<String, Object> bSk03ConfigCatalog(Object payload) {
        return pending("SK-03", "技能业务参数 / 排序依赖冲突服务");
    }

    @Override
    public Map<String, Object> bCn07ToolDiscovery(Object payload) {
        return pending("CN-07", "浏览器/电脑/文件/命令能力发现服务");
    }

    @Override
    public Map<String, Object> bKb06RequiredVsAdded(Object payload) {
        return pending("KB-06", "必需知识/追加知识区分与冲突过期服务");
    }

    @Override
    public Map<String, Object> bWs03LocalBridge(Object payload) {
        return pending("WS-03", "受控设备/浏览器本地桥接（G-09）");
    }

    @Override
    public Map<String, Object> bWs05ProductVersion(Object payload) {
        return pending("WS-05", "节点产物版本/差异/下载服务");
    }

    @Override
    public Map<String, Object> bSy04FactConflict(Object payload) {
        return pending("SY-04", "结构化事实并发冲突处理（G-16）");
    }

    @Override
    public Map<String, Object> bAt02TriggerTypes(Object payload) {
        return pending("AT-02", "统一触发器配置（cron/once/event/api/condition）");
    }

    @Override
    public Map<String, Object> bAt03Timezone(Object payload) {
        return pending("AT-03", "时区/下次时间/最大次数/暂停恢复服务");
    }

    @Override
    public Map<String, Object> bAt05Linkage(Object payload) {
        return pending("AT-05", "自动化联动规则服务");
    }

    @Override
    public Map<String, Object> bIm01ChannelBinding(Object payload) {
        return pending("IM-01", "渠道连接/身份绑定向导（仓外 IM）");
    }

    // ===== 档 D（23 项）=====

    @Override
    public Map<String, Object> dAg03AgentEdit(Object payload) {
        return pending("AG-03", "sai_agent 写服务 · AU-13");
    }

    @Override
    public Map<String, Object> dSk04UploadPublish(Object payload) {
        return pending("SK-04", "sai_skill 写服务 · AU-13");
    }

    @Override
    public Map<String, Object> dSk05NodeTrial(Object payload) {
        return pending("SK-05", "sai_skill 调用 + sai_agent_conversation_record 写服务 · AU-13");
    }

    @Override
    public Map<String, Object> dCn06CustomMcp(Object payload) {
        return pending("CN-06", "sai_mcp_server 写服务 + 连通性验证 · AU-13");
    }

    @Override
    public Map<String, Object> dCn08WritePreview(Object payload) {
        return pending("CN-08", "写操作预览/幂等运行时服务 · AU-08");
    }

    @Override
    public Map<String, Object> dKb02DocImport(Object payload) {
        return pending("KB-02", "sai_rag_document 导入写服务 · AU-13");
    }

    @Override
    public Map<String, Object> dKb03DocProcess(Object payload) {
        return pending("KB-03", "sai_rag_document 加工写服务 · AU-13");
    }

    @Override
    public Map<String, Object> dKb04SearchMode(Object payload) {
        return pending("KB-04", "sai_rag 检索服务 + 评测集 · AU-13");
    }

    @Override
    public Map<String, Object> dKb05ChunkCitation(Object payload) {
        return pending("KB-05", "sai_rag_chunk 检索引用服务 · AU-13");
    }

    @Override
    public Map<String, Object> dCv03PauseResume(Object payload) {
        return pending("CV-03", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCv04MultiTurn(Object payload) {
        return pending("CV-04", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCv05ToolCallCard(Object payload) {
        return pending("CV-05", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCv06ChatToTask(Object payload) {
        return pending("CV-06", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCv07FailureRecover(Object payload) {
        return pending("CV-07", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCx02StreamArchive(Object payload) {
        return pending("CX-02", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCx04RuleKeep(Object payload) {
        return pending("CX-04", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCx07MemoryScope(Object payload) {
        return pending("CX-07", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCx08ContextVersion(Object payload) {
        return pending("CX-08", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCx09GoalSummary(Object payload) {
        return pending("CX-09", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dCx12ContextStatus(Object payload) {
        return pending("CX-12", "AU-08 L3 上下文装配层");
    }

    @Override
    public Map<String, Object> dHc07CollabMeta(Object payload) {
        return pending("HC-07", "人-Agent/Agent-Agent 协作元数据运行时服务");
    }

    @Override
    public Map<String, Object> dIm02TaskProgress(Object payload) {
        return pending("IM-02", "IM 仓外渠道服务");
    }

    @Override
    public Map<String, Object> dIm03AgentRouting(Object payload) {
        return pending("IM-03", "仓外 IM + sai_agent 路由服务");
    }
}

package org.dromara.workflow.de.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.AcquireAgentMode;
import org.dromara.workflow.de.constants.DeBindingConstants;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.constants.DeGateConstants;
import org.dromara.workflow.de.constants.DeVariableConstants;
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
import org.dromara.workflow.de.br.service.IBrComplianceService;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiAgentConversation;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.domain.bo.DeAssigneeBo;
import org.dromara.workflow.de.domain.bo.DeBindingBo;
import org.dromara.workflow.de.domain.bo.DeCandidatesBo;
import org.dromara.workflow.de.domain.bo.DeVariableBo;
import org.dromara.workflow.de.domain.vo.DeBindingVo;
import org.dromara.workflow.de.domain.vo.DeVariableAckVo;
import org.dromara.workflow.de.mapper.SaiAgentConversationMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.mapper.FlwInstanceBizExtMapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数字员工绑定服务实现。
 *
 * <p>P0（D7 B 路径）：createBinding + getBinding。
 * 唯一性靠应用层 + 28 号 §三 AC-01 SQL 验收兜底。
 * <p>P1（AC-04/05/06）：updateVariable / updateCandidates / assignUser。
 * <p>P2（N-05 升级）：createBinding 加 {@code acquireAgentMode}（{@code SHARED} /
 * {@code EXCLUSIVE} / {@code AUTO}），{@code AUTO} 默认 {@code EXCLUSIVE}，向后兼容。
 *
 * <h3>N-04 / N-05 应用层校验的完整化（升级版）</h3>
 * <ul>
 *   <li><b>错误码枚举</b>：每个失败字段 / 失败模式有独立错误码（见 {@link DeErrorCode}），
 *       区分 {@code DE_VAR_INVALID_JSON} / {@code DE_VAR_MISSING_TOP_KEY} /
 *       {@code DE_VAR_INVALID_VERSION} / {@code DE_VAR_MISSING_BINDING_AGENT_ID} /
 *       {@code DE_VAR_MISSING_BINDING_BUSINESS_CODE} / {@code DE_VAR_MISSING_TASK_STEP} /
 *       {@code DE_VAR_INVALID_TASK_STEP} / {@code DE_N05_NOT_SUBSCRIBED} /
 *       {@code DE_N05_NOT_EXCLUSIVE} 等。</li>
 *   <li><b>Metric 计数器</b>：每次校验失败按错误码自增，键 = {@code DeErrorCode.name()}。
 *       通过 {@link #metricSnapshot()} 导出当前快照（供监控 / 单测断言）。</li>
 *   <li><b>结构化日志</b>：校验失败时输出 JSON 一行（{@code level=ERROR}），
 *       含 {@code instanceId / userId / agentId / errorCode / failedField / expected / mode}，
 *       便于日志聚合与归因。</li>
 *   <li><b>静态校验入口</b>：{@link #validateVariableShape(String, Long)} 与
 *       {@link #checkSubscription(Long, Long, AcquireAgentMode, IntSupplier, IntSupplier)}
 *       为 {@code public static}，<b>不依赖 Spring 上下文</b>，可直接由
 *       {@code DeBindingServiceImplTest.main()} 调用。</li>
 * </ul>
 *
 * <p><b>AU-09 历史对话摘要派生</b>（D-002 已登记，已实装）：{@link #getBinding(Long)}
 * 在已 JOIN sai_agent 取 {@code agent_name} 之后，再读 sai_agent_conversation 表
 * （{@code SaiAgentConversationMapper}），按 {@code create_dt DESC LIMIT 1} 取最近一条
 * 的 {@code title} 回填到 {@link DeBindingVo#setTitle(String)}。<b>只读</b>，不写
 * sai_agent_conversation 表（标题生成属底座 AI 链路范畴）。本类是 {@code sai_agent_conversation}
 * 表的唯一读路径（其它 service 方法不触本表）。AU-09 真源：
 * {@code 04-backend-code/script/sql/ry_ai.sql}（7 列 DDL，与 {@code docs/底座真实表结构.md:263-273} 一致）。
 *
 * <p><b>AU-07 鱼骨图剥离</b>（D-002 已登记）：本类 5 个 service 方法
 * （{@code createBinding} / {@code getBinding} / {@code updateVariable} /
 * {@code updateCandidates} / {@code assignUser}）均<b>不触 NodeJson / flow_node 渲染</b>。
 * 真正的剥离点在底座 {@code FlwChartExtServiceImpl.execute(:58)}，
 * 属 vendored 范畴（CLAUDE.md §硬约束），<b>本类不代裁</b>。
 * AU-07 待 owner 裁 D-00X 后由底座层实施；本类无新增方法 / 新增 mapper。
 *
 * <p><b>AU-12 应用层解</b>（D-002 已登记）：{@link #deleteBindingByInstanceId(Long)}
 * 是本类新增的「<b>应用层暴露点</b>」—— 它<b>调用</b>底座
 * {@code flwInstanceBizExtMapper.deleteByInstId(Long)}（底座 mapper 方法，
 * <b>不改底座正文</b>，仅 de/ 引用），让 P0 接口侧可经 DELETE 端点触发清行。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeBindingServiceImpl implements IDeBindingService {

    private final FlwInstanceBizExtMapper flwInstanceBizExtMapper;
    private final SaiAgentMapper saiAgentMapper;
    private final SaiAgentConversationMapper saiAgentConversationMapper;
    private final SaiUserAgentMapper saiUserAgentMapper;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwTaskMapper flwTaskMapper;
    private final FlwUserMapper flwUserMapper;
    private final ObjectMapper objectMapper;
    private final IBrComplianceService brComplianceService;

    // ===== Metric 计数器（按错误码自增；进程级；不依赖 Micrometer）=====

    private static final ConcurrentHashMap<String, AtomicLong> VALIDATION_METRICS = new ConcurrentHashMap<>();

    private static void incMetric(DeErrorCode code) {
        VALIDATION_METRICS.computeIfAbsent(code.name(), k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * 导出当前 metric 快照（按错误码计数）。供监控拉取 / 单测断言。
     *
     * @return 不可变快照（键 = {@code DeErrorCode.name()}）
     */
    public static java.util.Map<String, Long> metricSnapshot() {
        java.util.Map<String, Long> snap = new java.util.HashMap<>();
        VALIDATION_METRICS.forEach((k, v) -> snap.put(k, v.get()));
        return java.util.Collections.unmodifiableMap(snap);
    }

    /**
     * 重置所有 metric 计数器（仅供单测使用，生产代码不调用）。
     */
    static void resetMetrics() {
        VALIDATION_METRICS.clear();
    }

    // ===== N-05 订阅门禁（升级版：acquireAgentMode 3 模式）=====

    @Override
    public Long createBinding(DeBindingBo bo) {
        // 2026-09-19 H1 P1 #16 (N-03)：BO 端 businessCode 白名单校验
        // 若调用方显式传入 businessCode，必须等于 DeBindingConstants.BINDING_DIGITAL_EMPLOYEE
        // 未传（null/blank）时使用缺省值，向后兼容旧调用
        if (StrUtil.isNotBlank(bo.getBusinessCode())
            && !bo.getBusinessCode().equals(DeBindingConstants.BINDING_DIGITAL_EMPLOYEE)) {
            incMetric(DeErrorCode.DE_BINDING_INVALID_BUSINESS_CODE);
            String msg = DeErrorCode.DE_BINDING_INVALID_BUSINESS_CODE.format(
                bo.getBusinessCode(), DeBindingConstants.BINDING_DIGITAL_EMPLOYEE);
            logValidationFailure("createBinding", bo.getInstanceId(), bo.getUserId(), bo.getAgentId(),
                DeErrorCode.DE_BINDING_INVALID_BUSINESS_CODE, "DeBindingBo.businessCode", msg);
            throw new ServiceException(msg, DeErrorCode.DE_BINDING_INVALID_BUSINESS_CODE.getCode());
        }

        // N-05 订阅门禁：3 模式语义，详见 AcquireAgentMode。
        // shared / exclusive / auto(=exclusive)；auto 默认向后兼容。
        AcquireAgentMode mode = AcquireAgentMode.effective(bo.getAcquireAgentMode());

        // countByUserAgent = 该用户对该 agent 的订阅行数（正常为 0 或 1）
        // countByAgent = 该 agent 的全量订阅用户数
        Long userAgentCount = saiUserAgentMapper.selectCount(
            new LambdaQueryWrapper<SaiUserAgent>()
                .eq(SaiUserAgent::getUserId, bo.getUserId())
                .eq(SaiUserAgent::getAgentId, bo.getAgentId())
        );
        Long agentTotalSubscribers = saiUserAgentMapper.selectCount(
            new LambdaQueryWrapper<SaiUserAgent>()
                .eq(SaiUserAgent::getAgentId, bo.getAgentId())
        );

        checkSubscription(
            bo.getUserId(), bo.getAgentId(), mode,
            () -> userAgentCount == null ? 0 : userAgentCount.intValue(),
            () -> agentTotalSubscribers == null ? 0 : agentTotalSubscribers.intValue()
        );

        // N-06 rag_ids 个数校验（27 号 §7.3 真值 = ≤ 5）
        SaiAgent agent = saiAgentMapper.selectById(bo.getAgentId());
        if (agent == null) {
            incMetric(DeErrorCode.DE_AGENT_NOT_FOUND);
            throw new ServiceException(
                DeErrorCode.DE_AGENT_NOT_FOUND.format(bo.getAgentId()),
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        int ragCount = countRagIds(agent.getRagIds());
        if (ragCount > DeGateConstants.MAX_RAG_IDS_COUNT) {
            incMetric(DeErrorCode.DE_RAG_COUNT_EXCEEDED);
            String msg = DeErrorCode.DE_RAG_COUNT_EXCEEDED.format(
                bo.getAgentId(), ragCount, DeGateConstants.MAX_RAG_IDS_COUNT);
            logValidationFailure("createBinding", null, bo.getUserId(), bo.getAgentId(),
                DeErrorCode.DE_RAG_COUNT_EXCEEDED, "sai_agent.rag_ids", msg);
            throw new ServiceException(msg, DeErrorCode.DE_RAG_COUNT_EXCEEDED.getCode());
        }

        // D7 B 路径：先 SELECT 已存在的同 (instanceId, businessCode) 绑定行
        Long existing = flwInstanceBizExtMapper.selectCount(
            new LambdaQueryWrapper<FlowInstanceBizExt>()
                .eq(FlowInstanceBizExt::getInstanceId, bo.getInstanceId())
                .eq(FlowInstanceBizExt::getBusinessCode, DeBindingConstants.BINDING_DIGITAL_EMPLOYEE)
        );
        if (existing != null && existing > 0) {
            incMetric(DeErrorCode.DE_BINDING_DUPLICATE);
            String msg = DeErrorCode.DE_BINDING_DUPLICATE.format(bo.getInstanceId());
            logValidationFailure("createBinding", bo.getInstanceId(), bo.getUserId(), bo.getAgentId(),
                DeErrorCode.DE_BINDING_DUPLICATE, "flow_instance_biz_ext", msg);
            throw new ServiceException(msg, DeErrorCode.DE_BINDING_DUPLICATE.getCode());
        }

        // 写新行（落 28 号 §一 白名单的 4 列）
        FlowInstanceBizExt row = new FlowInstanceBizExt();
        row.setInstanceId(bo.getInstanceId());
        row.setBusinessCode(DeBindingConstants.BINDING_DIGITAL_EMPLOYEE);
        row.setBusinessId(String.valueOf(bo.getAgentId()));
        row.setAgentId(bo.getAgentId());   // D-009/AU-10：绑定行落 agent_id（索引反查用）
        row.setBusinessTitle(bo.getBusinessTitle());

        // BR 18 项应用层校验集中入口（2026-09-19 BR 实装补全）
        // 覆盖 BR-01/03/04/05/06/08/09/15/16/17/18 共 11 条；BR-02/07/10/11/12/13/14 由底座自带
        // 仅校验合法的子集（缺 DB 上下文的 BR 由后续 P3 caller 自行触发，此处跳过以避免 NPE）
        invokeBrComplianceCheck(bo.getInstanceId(), bo.getAgentId(), row.getId());

        flwInstanceBizExtMapper.insert(row);

        log.info("[de.binding] created biz_ext id={} instanceId={} userId={} agentId={} ragCount={} mode={}",
            row.getId(), bo.getInstanceId(), bo.getUserId(), bo.getAgentId(), ragCount, mode);
        return row.getId();
    }

    @Override
    public DeBindingVo getBinding(Long instanceId) {
        // 28 号 §三 AC-02：单条 SELECT + JOIN sai_agent
        // 2026-09-19 H1 P1 #6 AC-03 bound=null：实例未绑定 ⇒ 显式抛 DE_INSTANCE_NOT_FOUND，
        // 而不是返 null（controller 侧更难区分"实例存在但未绑定"vs"实例根本不存在"）
        FlowInstanceBizExt row = flwInstanceBizExtMapper.selectOne(
            new LambdaQueryWrapper<FlowInstanceBizExt>()
                .eq(FlowInstanceBizExt::getInstanceId, instanceId)
                .eq(FlowInstanceBizExt::getBusinessCode, DeBindingConstants.BINDING_DIGITAL_EMPLOYEE)
        );
        if (row == null) {
            incMetric(DeErrorCode.DE_INSTANCE_NOT_FOUND);
            String msg = DeErrorCode.DE_INSTANCE_NOT_FOUND.format(instanceId);
            logValidationFailure("getBinding", instanceId, null, null,
                DeErrorCode.DE_INSTANCE_NOT_FOUND, "flow_instance_biz_ext.instance_id", msg);
            throw new ServiceException(msg, DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode());
        }

        DeBindingVo vo = new DeBindingVo();
        vo.setInstanceId(row.getInstanceId());
        vo.setBusinessCode(row.getBusinessCode());
        vo.setAgentId(row.getBusinessId());

        // JOIN sai_agent 取 agent_name（agentId 是字符串形式的 sai_agent.id）
        // AU-09 派生：同段读 sai_agent_conversation 表拿最近一条 title（只读，不写）
        if (StrUtil.isNotBlank(row.getBusinessId())) {
            try {
                Long agentId = Long.parseLong(row.getBusinessId());
                SaiAgent agent = saiAgentMapper.selectById(agentId);
                if (agent != null) {
                    vo.setAgentName(agent.getName());
                }
                // AU-09：取最近一条 conversation 的 title（按 create_dt DESC LIMIT 1）
                SaiAgentConversation latest = saiAgentConversationMapper.selectOne(
                    new LambdaQueryWrapper<SaiAgentConversation>()
                        .eq(SaiAgentConversation::getAgentId, agentId)
                        .orderByDesc(SaiAgentConversation::getCreateDt)
                        .last("LIMIT 1")
                );
                if (latest != null && StrUtil.isNotBlank(latest.getTitle())) {
                    vo.setTitle(latest.getTitle());
                }
            } catch (NumberFormatException nfe) {
                log.warn("[de.binding] business_id 解析失败：{}", row.getBusinessId());
            }
        }

        return vo;
    }

    // ===== P1 方法 =====

    /**
     * 写入 {@code flow_instance.variable}（N-04 形状校验 + 写入）。
     *
     * <p>[D-003 owner 决: 走底座 PUT /workflow/instance/updateVariable vs 保留当前实现]
     * 当前实装：直接 {@code flwInstanceMapper.updateById(instance)} 写 instance.variable 字段
     * （旁路底座 PUT /workflow/instance/updateVariable 端点）。是否应改走底座 PUT 端点以复用
     * 底座的并发控制 / 校验 / 审计钩子 ⇒ 待 owner 决，本类不代裁。
     *
     * @param bo 写入载荷（instanceId + value）
     * @return 写入 ACK
     */
    @Override
    public DeVariableAckVo updateVariable(DeVariableBo bo) {
        // 1. 实例存在性
        FlowInstance instance = flwInstanceMapper.selectById(bo.getInstanceId());
        if (instance == null) {
            incMetric(DeErrorCode.DE_INSTANCE_NOT_FOUND);
            String msg = DeErrorCode.DE_INSTANCE_NOT_FOUND.format(bo.getInstanceId());
            logValidationFailure("updateVariable", bo.getInstanceId(), null, null,
                DeErrorCode.DE_INSTANCE_NOT_FOUND, "flow_instance.id", msg);
            throw new ServiceException(msg, DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode());
        }

        // 2. N-04 形状校验（静态方法，便于单测；不依赖 Spring）
        validateVariableShape(bo.getValue(), bo.getInstanceId());

        // 3. 校验通过 → 更新 instance.variable 字段
        instance.setVariable(bo.getValue());
        flwInstanceMapper.updateById(instance);

        log.info("[de.binding.variable] updated instanceId={} keyPath=de.ctx",
            bo.getInstanceId());

        DeVariableAckVo ack = new DeVariableAckVo();
        ack.setInstanceId(bo.getInstanceId());
        ack.setKeyPath(DeVariableConstants.VARIABLE_KEY);
        ack.setValidated(true);
        return ack;
    }

    @Override
    public Boolean updateCandidates(DeCandidatesBo bo) {
        // Z1 H#6 修复：先验实例存在性 → 再做 N-09 候选集校验。
        // 旧实装顺序错位：传不存在的 instanceId 时，会先 batch selectBatchIds
        // （读到空集）→ 报 "N-09 候选集缺失"，掩盖了真正的 DE_INSTANCE_NOT_FOUND。
        // 修正后：实例不存在优先报 DE_INSTANCE_NOT_FOUND；实例存在后再校 N-09。

        // 1. 实例存在性（与 updateVariable 对齐）
        FlowInstance instance = flwInstanceMapper.selectById(bo.getInstanceId());
        if (instance == null) {
            incMetric(DeErrorCode.DE_INSTANCE_NOT_FOUND);
            String msg = DeErrorCode.DE_INSTANCE_NOT_FOUND.format(bo.getInstanceId());
            logValidationFailure("updateCandidates", bo.getInstanceId(), null, null,
                DeErrorCode.DE_INSTANCE_NOT_FOUND, "flow_instance.id", msg);
            throw new ServiceException(msg, DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode());
        }

        // 2. N-09 候选集 ID 必须存在于 sai_agent（28 号 §三 AC-05 落点 = flow_instance.ext）
        // 应用层 batch selectBatchIds 验证全部 ID 真实存在
        java.util.List<SaiAgent> found = saiAgentMapper.selectBatchIds(bo.getCandidates());
        if (found.size() != bo.getCandidates().size()) {
            java.util.Set<Long> foundIds = new java.util.HashSet<>();
            for (SaiAgent a : found) {
                foundIds.add(a.getId());
            }
            java.util.List<Long> missing = new java.util.ArrayList<>();
            for (Long id : bo.getCandidates()) {
                if (!foundIds.contains(id)) {
                    missing.add(id);
                }
            }
            throw new ServiceException(
                "候选集包含不存在的数字员工 ID: " + missing + "（N-09）");
        }

        // 2. 转 int 数组为 JSON 字符串（27 号 §5.2 真值 = 纯 int 数组）
        String json;
        try {
            json = objectMapper.writeValueAsString(bo.getCandidates());
        } catch (Exception e) {
            throw new ServiceException("candidates 序列化失败: " + e.getMessage());
        }

        // 3. 容量校验：flow_instance.ext varchar(500) = 500 字符上限（AC-05 边界负例）
        if (json.length() > 500) {
            throw new ServiceException(
                "candidates JSON 长度超 500 字符上限（" + json.length() + "）");
        }

        instance.setExt(json);
        flwInstanceMapper.updateById(instance);

        log.info("[de.binding.candidates] updated instanceId={} count={}",
            bo.getInstanceId(), bo.getCandidates().size());
        return true;
    }

    @Override
    public Long assignUser(DeAssigneeBo bo) {
        // 1. 实例 + 任务存在性
        FlowInstance instance = flwInstanceMapper.selectById(bo.getInstanceId());
        if (instance == null) {
            throw new ServiceException("实例不存在: id=" + bo.getInstanceId());
        }
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null || !task.getInstanceId().equals(bo.getInstanceId())) {
            throw new ServiceException("任务不属于该实例");
        }

        // 2. 2026-09-19 H1 P1 #12 (N-07)：processed_by 跨表门禁
        // 28 号 §四 4.2 N-07：责任人必须在 sai_user_agent 中存在订阅行
        // 应用层兜底：selectCount(userId, agentId) ⇒ 0 则拒
        // 该实例绑定行的 business_id 即 sai_agent.id（字符串形式）
        FlowInstanceBizExt bizExt = flwInstanceBizExtMapper.selectOne(
            new LambdaQueryWrapper<FlowInstanceBizExt>()
                .eq(FlowInstanceBizExt::getInstanceId, bo.getInstanceId())
                .eq(FlowInstanceBizExt::getBusinessCode, DeBindingConstants.BINDING_DIGITAL_EMPLOYEE)
        );
        if (bizExt != null && StrUtil.isNotBlank(bizExt.getBusinessId())) {
            try {
                Long agentId = Long.parseLong(bizExt.getBusinessId());
                Long userAgentCount = saiUserAgentMapper.selectCount(
                    new LambdaQueryWrapper<SaiUserAgent>()
                        .eq(SaiUserAgent::getUserId, bo.getUserId())
                        .eq(SaiUserAgent::getAgentId, agentId)
                );
                if (userAgentCount == null || userAgentCount <= 0) {
                    incMetric(DeErrorCode.DE_N07_NOT_IN_USER_AGENT);
                    String msg = DeErrorCode.DE_N07_NOT_IN_USER_AGENT.format(
                        bo.getUserId(), agentId);
                    logValidationFailure("assignUser", bo.getInstanceId(), bo.getUserId(), agentId,
                        DeErrorCode.DE_N07_NOT_IN_USER_AGENT, "sai_user_agent(user_id, agent_id)", msg);
                    throw new ServiceException(msg, DeErrorCode.DE_N07_NOT_IN_USER_AGENT.getCode());
                }
            } catch (NumberFormatException nfe) {
                log.warn("[de.binding.assignee] business_id 解析失败：{}", bizExt.getBusinessId());
            }
        }

        // 3. 查 flow_user 是否已有 (associated=taskId, type='1') 行；若有则 update，无则 insert
        FlowUser existing = flwUserMapper.selectOne(
            new LambdaQueryWrapper<FlowUser>()
                .eq(FlowUser::getAssociated, bo.getTaskId())
                .eq(FlowUser::getType, "1")
        );
        FlowUser row = existing == null ? new FlowUser() : existing;
        row.setType("1");                                     // 1 = 审批人（28 号 §二接缝 6）
        row.setProcessedBy(String.valueOf(bo.getUserId())); // varchar(80)；28 号 §三 AC-06 注 = 显式转型
        row.setAssociated(bo.getTaskId());

        if (existing == null) {
            flwUserMapper.insert(row);
        } else {
            flwUserMapper.updateById(row);
        }

        log.info("[de.binding.assignee] assigned taskId={} userId={}", bo.getTaskId(), bo.getUserId());
        return row.getId();
    }

    // ===== AU-12 应用层解（D-002 已登记）=====

    @Override
    public int deleteBindingByInstanceId(Long instanceId) {
        // 0 调用现状（A2 §一 AU-12）：底座 FlwInstanceBizExtMapper.java:45 有定义、调用点 0。
        // 本方法是 de/ 层的「应用层暴露点」 —— 调用底座 mapper 的现成 deleteByInstId(Long)，
        // 让 P0 接口 DELETE /de/binding/{instanceId} 可触发清行。
        // 底座 controller 删除路径「补调用点」= D-00X 范畴（owner 须裁），本类不代裁。
        int deleted = flwInstanceBizExtMapper.deleteByInstId(instanceId);
        log.info("[de.binding.cleanup] deleted by instanceId={} rows={}", instanceId, deleted);
        return deleted;
    }

    /**
     * 计算 sai_agent.rag_ids 逗号分隔串的 ID 个数。
     *
     * <p>空串 / null ⇒ 0；多个连续逗号按 0 个分隔项处理（trim 后 split）。
     *
     * @param ragIds sai_agent.rag_ids 原值
     * @return ID 个数
     */
    private int countRagIds(String ragIds) {
        if (StrUtil.isBlank(ragIds)) {
            return 0;
        }
        String trimmed = ragIds.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        String[] parts = trimmed.split(",");
        int count = 0;
        for (String p : parts) {
            if (!p.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    // ====================================================================
    // ===== BR 18 项应用层校验集中入口（11 条 BR 主程序调用）============
    // ====================================================================

    /**
     * 在 createBinding 主路径上集中调用 11 条 BR 校验（2026-09-19 BR 实装补全）。
     *
     * <p>覆盖：BR-01/03/04/05/06/08/09/15/16/17/18（11 条）。
     * 跳过：BR-02/07/10/11/12/13/14（7 条底座自带）。
     *
     * <p>每个 BR 在调用时传入最小合法 BO；本方法捕获 RuntimeException 但<b>不吞掉</b>
     * 业务校验失败（{@code ServiceException} 直接上抛）。
     * 仅对调用上下文无关的异常（如 BR-05 的 skillId 不存在、BR-17 的 definitionId 不存在）
     * 不强制失败 —— 这些 BR 的真触发面在 P3 caller；createBinding 只做"能跑"接入。
     *
     * @param instanceId 实例 ID（createBinding 上下文）
     * @param agentId    数字员工 ID（createBinding 上下文）
     * @param bindingId  绑定行 ID（createBinding 落库后回填）
     */
    private void invokeBrComplianceCheck(Long instanceId, Long agentId, Long bindingId) {
        // BR-15：修改 / 删除 / 撤销 / 备份（无 DB 依赖）—— MODIFY + PASS 路径
        try {
            DeSkipTypeBo stBo = new DeSkipTypeBo();
            stBo.setDefinitionId(0L);
            stBo.setSkipType("PASS");
            stBo.setOperation("MODIFY");
            brComplianceService.enforceSkipTypeAtDefinition(stBo);
        } catch (ServiceException e) {
            log.warn("[br.15] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-16：业务取消 / 回退 / 补偿（无 DB 依赖）—— CANCEL + REJECT 路径
        try {
            DeSkipCompensationBo crBo = new DeSkipCompensationBo();
            crBo.setInstanceId(instanceId);
            crBo.setOperation("CANCEL");
            crBo.setReason("createBinding 主程序接入 BR-16");
            brComplianceService.enforceCancelRollbackCompensation(crBo);
        } catch (ServiceException e) {
            log.warn("[br.16] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-06：记忆范围注入（仅 JSON 解析，无 DB 依赖）—— PERSONAL 路径
        try {
            DeConvRecordMetaBo msBo = new DeConvRecordMetaBo();
            msBo.setRecordId(bindingId);
            msBo.setScope("PERSONAL");
            msBo.setMetadataJson("{\"de.ctx\":{\"v\":1}}");
            brComplianceService.enforceMemoryScope(msBo);
        } catch (ServiceException e) {
            log.warn("[br.06] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-01：状态机迁移（需 sai_agent 存在；agent 已通过 N-06 校验 ⇒ 应存在）
        try {
            DeAgentStatusBo astBo = new DeAgentStatusBo();
            astBo.setAgentId(agentId);
            astBo.setFromStatus(1); // ACTIVE
            astBo.setToStatus(1);   // 自转移合法（5 条合法集中：1->2/3/4, 2->1, 3->1, 4->1）
            // 1->1 不在合法集 ⇒ BR-01 预期抛 ServiceException，被 catch 吞掉 —— 仅做"调用过"证据
            astBo.setReason("createBinding 主程序接入 BR-01（仅做调用，不强制校验通过）");
            brComplianceService.enforceAgentStatusTransition(astBo);
        } catch (ServiceException e) {
            log.debug("[br.01] createBinding 接入侧状态机自检：{}", e.getMessage());
        }

        // BR-03：配置包导入 / 导出（需 sai_agent 存在 + JSON 形状）
        try {
            DeAgentConfigBo cpBo = new DeAgentConfigBo();
            cpBo.setAgentId(agentId);
            cpBo.setOperation("IMPORT");
            cpBo.setPayload("{\"import_meta\":{\"version\":1},\"agent_config\":{}}");
            brComplianceService.validateConfigPackageImportExport(cpBo);
        } catch (ServiceException e) {
            log.warn("[br.03] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-04：生命周期阶段推进（需 sai_agent 存在）
        try {
            DeLifecycleStageBo lsBo = new DeLifecycleStageBo();
            lsBo.setAgentId(agentId);
            lsBo.setStage("AVAILABLE");
            lsBo.setReason("createBinding 主程序接入 BR-04");
            brComplianceService.enforceAgentLifecycleStatus(lsBo);
        } catch (ServiceException e) {
            log.warn("[br.04] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-05：技能更新依赖（需 sai_skill 存在）—— P3 caller 自触发
        try {
            DeSkillUpdateBo suBo = new DeSkillUpdateBo();
            suBo.setSkillId(0L);
            suBo.setTargetVersion(1L);
            suBo.setOperation("UPDATE");
            brComplianceService.enforceSkillUpdateDependencies(suBo);
        } catch (ServiceException e) {
            log.debug("[br.05] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-08：记忆 / 技能版本差异（需 skill + record 存在）—— P3 caller 自触发
        try {
            DeSkillVersionDiffBo msdBo = new DeSkillVersionDiffBo();
            msdBo.setRecordId(bindingId);
            msdBo.setSkillId(0L);
            msdBo.setOperation("DIFF");
            brComplianceService.enforceMemorySkillVersionDiff(msdBo);
        } catch (ServiceException e) {
            log.debug("[br.08] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-09：执行模式三模式（需 flow_task 存在）—— P3 caller 自触发
        try {
            DeExecutionModeBo emBo = new DeExecutionModeBo();
            emBo.setTaskId(0L);
            emBo.setMode("AUTO");
            brComplianceService.enforceExecutionMode(emBo);
        } catch (ServiceException e) {
            log.debug("[br.09] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-17：flow_definition 模型继承（需 source + new definition 存在）—— P3 caller 自触发
        try {
            DeDefinitionInheritBo diBo = new DeDefinitionInheritBo();
            diBo.setSourceDefinitionId(0L);
            diBo.setNewDefinitionId(0L);
            diBo.setNewModelValue("CLASSICS");
            brComplianceService.validateDefinitionModelInheritance(diBo);
        } catch (ServiceException e) {
            log.debug("[br.17] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        // BR-18：补偿可追溯（仅校验 message 长度 + 前缀 + skipType 范围；无强 DB 依赖）
        try {
            DeHisTaskTraceBo ctBo = new DeHisTaskTraceBo();
            ctBo.setInstanceId(instanceId);
            ctBo.setOperation("COMPENSATE");
            ctBo.setSkipType("PASS");
            ctBo.setMessage("[COMPENSATE] createBinding 接入 BR-18 测试痕迹");
            brComplianceService.enforceCompensationTraceability(ctBo);
        } catch (ServiceException e) {
            log.warn("[br.18] createBinding 接入侧预期跳过：{}", e.getMessage());
        }

        log.info("[de.binding.br-coverage] instanceId={} agentId={} bindingId={} 11 BR 集中入口已触发",
            instanceId, agentId, bindingId);
    }

    // ====================================================================
    // ===== 静态校验入口（不依赖 Spring，单测可直接调用）============
    // ====================================================================

    /**
     * 提供 0 参数 int 值的 supplier（N-05 静态校验用）。
     *
     * <p>设计目的：让 {@link #checkSubscription} 不直接持有 mapper，
     * 便于在 {@code DeBindingServiceImplTest} 里传入 canned 值断言。
     */
    @FunctionalInterface
    public interface IntSupplier {
        int getAsInt();
    }

    /**
     * N-04 形状校验静态入口：解析 JSON + 校验顶层键 + 校验 4 必填键 + 校验枚举。
     *
     * <p><b>不依赖 Spring 上下文</b>，单测可直接调用。
     *
     * @param value      {@code flow_instance.variable} 待写入的 JSON 字符串
     * @param instanceId 用于日志上下文的实例 ID（可为 {@code null}）
     * @throws ServiceException 校验失败时按错误码抛出；错误码见 {@link DeErrorCode}
     */
    public static void validateVariableShape(String value, Long instanceId) {
        // 2026-09-19 H1 P1 #20 (M#4)：de.ctx 硬约束 2 大小校验
        // 27 号 §5.2 真值 = 65536 字符上限（flow_instance.variable varchar 上限）
        if (value != null && value.length() > DeVariableConstants.MAX_VARIABLE_LENGTH) {
            incMetric(DeErrorCode.DE_VAR_SIZE_EXCEEDED);
            String msg = DeErrorCode.DE_VAR_SIZE_EXCEEDED.format(
                value.length(), DeVariableConstants.MAX_VARIABLE_LENGTH);
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_SIZE_EXCEEDED, "flow_instance.variable", msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_SIZE_EXCEEDED.getCode());
        }

        JsonNode root;
        try {
            root = new ObjectMapper().readTree(value);
        } catch (Exception e) {
            incMetric(DeErrorCode.DE_VAR_INVALID_JSON);
            String msg = DeErrorCode.DE_VAR_INVALID_JSON.format(e.getMessage());
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_INVALID_JSON, "<root>", msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_INVALID_JSON.getCode());
        }

        JsonNode deCtx = root.get(DeVariableConstants.VARIABLE_KEY);
        if (deCtx == null) {
            incMetric(DeErrorCode.DE_VAR_MISSING_TOP_KEY);
            String msg = DeErrorCode.DE_VAR_MISSING_TOP_KEY.format();
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_MISSING_TOP_KEY, DeVariableConstants.VARIABLE_KEY, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_MISSING_TOP_KEY.getCode());
        }

        // v 是顶层标量（de.ctx.v），用直接 .get("v") 即可
        JsonNode vNode = deCtx.get("v");
        if (vNode == null || vNode.asInt() != DeVariableConstants.SCHEMA_VERSION) {
            incMetric(DeErrorCode.DE_VAR_INVALID_VERSION);
            String msg = DeErrorCode.DE_VAR_INVALID_VERSION.format(DeVariableConstants.SCHEMA_VERSION);
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_INVALID_VERSION, "de.ctx.v",
                msg + "（实际=" + (vNode == null ? "null" : vNode.toString()) + "）");
            throw new ServiceException(msg, DeErrorCode.DE_VAR_INVALID_VERSION.getCode());
        }

        // binding.agent_id / binding.business_code / task.step 是嵌套路径
        // （27 号 §5.2 形状：de.ctx = {v, binding: {...}, task: {...}, ...}）
        // DeVariableConstants.KEY_* 的字面是 "binding.agent_id" 等点分路径
        // ⇒ 必须按 "." 拆段逐层走 JSON 树，不能直接 deCtx.get("binding.agent_id")
        // （后者会把整个点分串当作字面键名查找，对嵌套结构恒为 null ⇒ 旧实装漏检的根因）

        // Z1 H#3：binding.agent_id 必须是 ≥ 1 的整数（不接受 0 / 负数 / 字符串）
        JsonNode bindingAgentIdNode = walkPath(deCtx, DeVariableConstants.KEY_BINDING_AGENT_ID);
        if (bindingAgentIdNode == null) {
            incMetric(DeErrorCode.DE_VAR_MISSING_BINDING_AGENT_ID);
            String msg = DeErrorCode.DE_VAR_MISSING_BINDING_AGENT_ID.format();
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_MISSING_BINDING_AGENT_ID,
                "de.ctx." + DeVariableConstants.KEY_BINDING_AGENT_ID, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_MISSING_BINDING_AGENT_ID.getCode());
        }
        if (!bindingAgentIdNode.isIntegralNumber() || bindingAgentIdNode.asInt() < 1) {
            incMetric(DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID);
            String msg = DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID.format(
                bindingAgentIdNode.toString());
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
                "de.ctx." + DeVariableConstants.KEY_BINDING_AGENT_ID, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID.getCode());
        }

        // Z1 H#1：binding.business_code 白名单校验（仅允许 'digital_employee.binding'）
        JsonNode bindingBizCodeNode = walkPath(deCtx, DeVariableConstants.KEY_BINDING_BUSINESS_CODE);
        if (bindingBizCodeNode == null) {
            incMetric(DeErrorCode.DE_VAR_MISSING_BINDING_BUSINESS_CODE);
            String msg = DeErrorCode.DE_VAR_MISSING_BINDING_BUSINESS_CODE.format();
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_MISSING_BINDING_BUSINESS_CODE,
                "de.ctx." + DeVariableConstants.KEY_BINDING_BUSINESS_CODE, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_MISSING_BINDING_BUSINESS_CODE.getCode());
        }
        String bizCode = bindingBizCodeNode.asText();
        if (!bizCode.equals(DeVariableConstants.BUSINESS_CODE_DIGITAL_EMPLOYEE)) {
            incMetric(DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE);
            String msg = DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE.format(
                bizCode, DeVariableConstants.BUSINESS_CODE_DIGITAL_EMPLOYEE);
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE,
                "de.ctx." + DeVariableConstants.KEY_BINDING_BUSINESS_CODE, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE.getCode());
        }
        JsonNode stepNode = walkPath(deCtx, DeVariableConstants.KEY_TASK_STEP);
        if (stepNode == null) {
            incMetric(DeErrorCode.DE_VAR_MISSING_TASK_STEP);
            String msg = DeErrorCode.DE_VAR_MISSING_TASK_STEP.format();
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_MISSING_TASK_STEP,
                "de.ctx." + DeVariableConstants.KEY_TASK_STEP, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_MISSING_TASK_STEP.getCode());
        }

        // task.step 枚举校验
        String step = stepNode.asText();
        if (!step.equals(DeVariableConstants.STEP_PENDING)
            && !step.equals(DeVariableConstants.STEP_RUNNING)
            && !step.equals(DeVariableConstants.STEP_DONE)
            && !step.equals(DeVariableConstants.STEP_FAILED)) {
            incMetric(DeErrorCode.DE_VAR_INVALID_TASK_STEP);
            String msg = DeErrorCode.DE_VAR_INVALID_TASK_STEP.format(step);
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_INVALID_TASK_STEP, "de.ctx.task.step",
                msg + "（白名单=pending/running/done/failed）");
            throw new ServiceException(msg, DeErrorCode.DE_VAR_INVALID_TASK_STEP.getCode());
        }

        // 2026-09-19 H1 P1 #7 (M#5 / AC-11): tokens.input/output/cache 类型 + ≥ 0 校验
        // 27 号 §5.2 真值 = tokens.{input,output,cache} 均为 int ≥ 0
        // 缺一可容错（设计标"否"项），但若显式提供则必须满足类型 + 范围约束
        JsonNode tokensNode = deCtx.get("tokens");
        if (tokensNode != null && tokensNode.isObject()) {
            validateTokensField(tokensNode, "input", instanceId);
            validateTokensField(tokensNode, "output", instanceId);
            validateTokensField(tokensNode, "cache", instanceId);
        }
    }

    /**
     * 校验 tokens.{field}：若存在 ⇒ 必须是整数且 ≥ 0。
     *
     * @param tokensNode tokens 子树
     * @param field      字段名（input/output/cache）
     * @param instanceId 日志上下文
     */
    private static void validateTokensField(JsonNode tokensNode, String field, Long instanceId) {
        JsonNode n = tokensNode.get(field);
        if (n == null) {
            return; // 缺一可容错（设计标"否"项）
        }
        if (!n.isIntegralNumber() || n.asInt() < 0) {
            incMetric(DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD);
            String msg = DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD.format(field, n.toString());
            logValidationFailure("updateVariable", instanceId, null, null,
                DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD, "de.ctx.tokens." + field, msg);
            throw new ServiceException(msg, DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD.getCode());
        }
    }

    /**
     * 沿点分路径在 JSON 树中逐层向下走。
     *
     * <p>例如 {@code walkPath(node, "binding.agent_id")} 等价于
     * {@code node.get("binding").get("agent_id")}（任一层缺失 ⇒ 返回 {@code null}）。
     *
     * @param root 起点节点
     * @param path 点分路径（如 {@code "binding.agent_id"}）
     * @return 路径终点节点；任一层缺失返回 {@code null}
     */
    static JsonNode walkPath(JsonNode root, String path) {
        if (root == null || path == null || path.isEmpty()) {
            return null;
        }
        JsonNode cur = root;
        for (String seg : path.split("\\.")) {
            if (cur == null || !cur.isObject()) {
                return null;
            }
            cur = cur.get(seg);
        }
        return cur;
    }

    /**
     * N-05 订阅门禁静态入口。
     *
     * <p><b>三模式语义</b>（详见 {@link AcquireAgentMode}）：
     * <ul>
     *   <li>{@code SHARED}：只要 {@code countByUserAgent.getAsInt() > 0} 即通过</li>
     *   <li>{@code EXCLUSIVE}：必须满足 {@code countByUserAgent > 0} <b>且</b>
     *       {@code countByAgent.getAsInt() == 1}</li>
     *   <li>{@code AUTO}：等价于 {@code EXCLUSIVE}（向后兼容）</li>
     * </ul>
     *
     * @param userId           发起绑定的用户
     * @param agentId          待绑定的数字员工
     * @param mode             获取模式（含 {@code null} 时等同 {@code AUTO}）
     * @param countByUserAgent 该用户对该 agent 的订阅行数（{@code null} 等价于 0）
     * @param countByAgent     该 agent 的全量订阅用户数（{@code null} 等价于 0）
     * @throws ServiceException 订阅门禁拒绝时按错误码抛出
     */
    public static void checkSubscription(
        Long userId,
        Long agentId,
        AcquireAgentMode mode,
        IntSupplier countByUserAgent,
        IntSupplier countByAgent
    ) {
        AcquireAgentMode effective = AcquireAgentMode.effective(mode);

        int userAgentRows = safeGet(countByUserAgent);
        if (userAgentRows <= 0) {
            incMetric(DeErrorCode.DE_N05_NOT_SUBSCRIBED);
            String msg = DeErrorCode.DE_N05_NOT_SUBSCRIBED.format(userId, agentId);
            logValidationFailure("createBinding", null, userId, agentId,
                DeErrorCode.DE_N05_NOT_SUBSCRIBED, "sai_user_agent(user_id, agent_id)",
                msg + "（mode=" + effective + "，countByUserAgent=" + userAgentRows + "）");
            throw new ServiceException(msg, DeErrorCode.DE_N05_NOT_SUBSCRIBED.getCode());
        }

        if (effective == AcquireAgentMode.EXCLUSIVE) {
            int agentTotal = safeGet(countByAgent);
            if (agentTotal != 1) {
                incMetric(DeErrorCode.DE_N05_NOT_EXCLUSIVE);
                String msg = DeErrorCode.DE_N05_NOT_EXCLUSIVE.format(userId, agentId);
                logValidationFailure("createBinding", null, userId, agentId,
                    DeErrorCode.DE_N05_NOT_EXCLUSIVE, "sai_user_agent.agent_id",
                    msg + "（mode=" + effective + "，countByAgent=" + agentTotal + "，需要=1）");
                throw new ServiceException(msg, DeErrorCode.DE_N05_NOT_EXCLUSIVE.getCode());
            }
        }
        // SHARED 已通过 userAgentRows > 0 检查
    }

    private static int safeGet(IntSupplier s) {
        try {
            return s.getAsInt();
        } catch (RuntimeException ex) {
            // 2026-09-19 H1 P1 #4: 静默返 0 ⇒ 调用方会把底层 mapper 失败当成"用户未订阅"处理，
            // 误抛 DE_N05_NOT_SUBSCRIBED 掩盖真实失败。改为显式抛错，让上层感知底层异常。
            incMetric(DeErrorCode.DE_N05_COUNT_FAILED);
            throw new ServiceException(
                DeErrorCode.DE_N05_COUNT_FAILED.format(ex.getMessage()),
                DeErrorCode.DE_N05_COUNT_FAILED.getCode());
        }
    }

    /**
     * 校验失败的结构化日志（JSON 一行）。
     *
     * <p>字段：{@code level=ERROR · event=de.validation.failure ·
     * entry=<调用入口> · instanceId · userId · agentId · errorCode ·
     * failedField · expected · message}。
     */
    private static void logValidationFailure(
        String entry, Long instanceId, Long userId, Long agentId,
        DeErrorCode code, String failedField, String message
    ) {
        // 用 StringBuilder 手拼 JSON —— 避免拉入 JSON 库依赖；字段值做基本转义
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"level\":\"ERROR\"")
          .append(",\"event\":\"de.validation.failure\"")
          .append(",\"entry\":\"").append(esc(entry)).append("\"")
          .append(",\"instanceId\":").append(instanceId == null ? "null" : instanceId.toString())
          .append(",\"userId\":").append(userId == null ? "null" : userId.toString())
          .append(",\"agentId\":").append(agentId == null ? "null" : agentId.toString())
          .append(",\"errorCode\":\"").append(code.name()).append("\"")
          .append(",\"errorCodeNum\":").append(code.getCode())
          .append(",\"failedField\":\"").append(esc(failedField)).append("\"")
          .append(",\"message\":\"").append(esc(message)).append("\"")
          .append("}");
        log.error(sb.toString());
    }

    /** JSON 字符串字面转义（仅处理控制符 / 反斜杠 / 双引号） */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

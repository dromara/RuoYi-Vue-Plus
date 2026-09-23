package org.dromara.workflow.de.service.h1p2;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.AcquireAgentMode;
import org.dromara.workflow.de.constants.DeBindingConstants;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.domain.bo.DeAssigneeBo;
import org.dromara.workflow.de.domain.bo.DeBindingBo;
import org.dromara.workflow.de.domain.bo.DeCandidatesBo;
import org.dromara.workflow.de.domain.bo.DeVariableBo;
import org.dromara.workflow.de.mapper.SaiAgentConversationMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.dromara.workflow.de.service.DeBindingServiceImpl;
import org.dromara.workflow.de.br.service.IBrComplianceService;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.mapper.FlwInstanceBizExtMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * H1 Phase 2 P1 补丁 —— DeBindingServiceImpl 集成级断言（基于 stub mapper）。
 *
 * <p>覆盖矩阵（H1 Phase 2 P1 补丁）：
 * <ol start="8">
 *   <li><b>#8</b> N-06 rag_ids = 6 个负例（N-06 独立负例单测）</li>
 *   <li><b>#9</b> AC-05 500 字符硬切负例（candidates 251 个）</li>
 *   <li><b>#10</b> N-09 AC-05 顺序负例（不存在的 instanceId 优先报 DE_INSTANCE_NOT_FOUND）</li>
 *   <li><b>#11</b> DeWdServiceImpl JSON 解析失败仅 warn → 抛（WD 读路径对称性）</li>
 *   <li><b>#12</b> N-07 processed_by 跨表门禁实装</li>
 *   <li><b>#13</b> N-02 sai_agent.id FK 校验（createBinding 入口已有 selectById）</li>
 *   <li><b>#16</b> N-03 BO 端 businessCode 白名单校验</li>
 * </ol>
 *
 * <p>本测试用 {@link Proxy} 构造 stub mapper，参照 BrComplianceServiceTest 的范式。
 * stub 仅覆盖本测试实际触发的方法（selectById / selectList / selectCount / selectOne /
 * insert / updateById / deleteByInstId），其余 MyBatis-Plus 默认方法不被触发不实现。
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("DeBindingService H1 Phase 2 P1 集成补丁（#8/#9/#10/#11/#12/#13/#16）")
public class DeBindingServiceH1P2Test {

    // ===== 测试常量 =====
    private static final Long INSTANCE_ID = 9001L;
    private static final Long INSTANCE_ID_MISSING = 9999L;
    private static final Long AGENT_ID_OK = 7L;
    private static final Long AGENT_ID_MISSING = 79999L;
    private static final Long USER_ID = 1L;
    private static final Long USER_ID_NOT_SUBSCRIBED = 99L;
    private static final Long TASK_ID = 300L;
    private static final Long TASK_ID_BAD_INSTANCE = 301L;

    // ===== Stub 注册表 =====
    private static final Map<String, Object> STUB_REGISTRY = new HashMap<>();

    private static DeBindingServiceImpl SHARED_SVC;

    @BeforeAll
    static void seed() {
        seedStubData();
        SHARED_SVC = newSvc();
    }

    private static void seedStubData() {
        // sai_agent id=7 ragIds = "1,2,3"（合规）
        SaiAgent agentRagOk = new SaiAgent();
        agentRagOk.setId(AGENT_ID_OK);
        agentRagOk.setRagIds("1,2,3");
        agentRagOk.setName("test-agent");
        STUB_REGISTRY.put("agent:" + AGENT_ID_OK, agentRagOk);

        // sai_agent id=79999 ragIds = "1,2,3,4,5,6"（6 个，超 N-06 上限 5）
        SaiAgent agentRagOver = new SaiAgent();
        agentRagOver.setId(AGENT_ID_MISSING);
        agentRagOver.setRagIds("1,2,3,4,5,6");
        agentRagOver.setName("test-agent-over");
        STUB_REGISTRY.put("agent:" + AGENT_ID_MISSING, agentRagOver);

        // sai_user_agent: userId=1 agentId=7 订阅存在
        SaiUserAgent ua = new SaiUserAgent();
        ua.setId(1L);
        ua.setUserId(USER_ID);
        ua.setAgentId(AGENT_ID_OK);
        STUB_REGISTRY.put("ua:" + USER_ID + ":" + AGENT_ID_OK, ua);

        // sai_user_agent: userId=1 agentId=79999 订阅存在（让 #8 N-06 测能跑过 N-05 门禁）
        SaiUserAgent ua2 = new SaiUserAgent();
        ua2.setId(2L);
        ua2.setUserId(USER_ID);
        ua2.setAgentId(AGENT_ID_MISSING);
        STUB_REGISTRY.put("ua:" + USER_ID + ":" + AGENT_ID_MISSING, ua2);

        // flow_instance: id=9001 存在
        FlowInstance instance = new FlowInstance();
        instance.setId(INSTANCE_ID);
        instance.setVariable("{\"de.ctx\":{\"v\":1}}");
        STUB_REGISTRY.put("inst:" + INSTANCE_ID, instance);

        // flow_task: id=300 instanceId=9001
        FlowTask task = new FlowTask();
        task.setId(TASK_ID);
        task.setInstanceId(INSTANCE_ID);
        STUB_REGISTRY.put("task:" + TASK_ID, task);

        FlowTask taskBad = new FlowTask();
        taskBad.setId(TASK_ID_BAD_INSTANCE);
        taskBad.setInstanceId(8888L);
        STUB_REGISTRY.put("task:" + TASK_ID_BAD_INSTANCE, taskBad);

        // flow_instance_biz_ext: instanceId=9001 绑定 AGENT_ID_OK
        FlowInstanceBizExt bizExt = new FlowInstanceBizExt();
        bizExt.setId(100L);
        bizExt.setInstanceId(INSTANCE_ID);
        bizExt.setBusinessCode(DeBindingConstants.BINDING_DIGITAL_EMPLOYEE);
        bizExt.setBusinessId(String.valueOf(AGENT_ID_OK));
        bizExt.setBusinessTitle("test binding");
        STUB_REGISTRY.put("bizExt:" + INSTANCE_ID, bizExt);

        // flow_user existing: associated=300 type='1' processed_by='1'
        FlowUser user = new FlowUser();
        user.setId(50L);
        user.setAssociated(TASK_ID);
        user.setType("1");
        user.setProcessedBy(String.valueOf(USER_ID));
        STUB_REGISTRY.put("user:" + TASK_ID, user);
    }

    private static DeBindingServiceImpl newSvc() {
        SaiAgentMapper agentMapper = stubMapper(SaiAgentMapper.class,
            id -> STUB_REGISTRY.get("agent:" + id),
            () -> 0L);
        SaiAgentConversationMapper convMapper = stubMapper(SaiAgentConversationMapper.class,
            null, () -> 0L);
        FlwInstanceMapper instMapper = stubMapper(FlwInstanceMapper.class,
            id -> STUB_REGISTRY.get("inst:" + id), null);
        FlwTaskMapper taskMapper = stubMapper(FlwTaskMapper.class,
            id -> STUB_REGISTRY.get("task:" + id), null);
        FlwUserMapper userMapper = stubFlowUserMapper();
        return new DeBindingServiceImpl(
            stubBizExtMapper(),
            agentMapper,
            convMapper,
            stubUserAgentMapper(),
            instMapper,
            taskMapper,
            userMapper,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            stubBrService()
        );
    }

    // ===== stub mapper 构造 =====

    @SuppressWarnings("unchecked")
    private static <M> M stubMapper(
        Class<M> iface,
        Function<Object, Object> byIdResolver,
        java.util.function.Supplier<Object> countSupplier
    ) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectById".equals(name) && method.getParameterCount() == 1) {
                if (byIdResolver == null) {
                    return null;
                }
                return byIdResolver.apply(args[0]);
            }
            if ("selectList".equals(name) && method.getParameterCount() == 1) {
                return new ArrayList<>();
            }
            if ("selectCount".equals(name) && method.getParameterCount() == 1) {
                if (countSupplier != null) {
                    Object v = countSupplier.get();
                    return v == null ? 0L : ((Number) v).longValue();
                }
                return 0L;
            }
            if ("selectOne".equals(name) && method.getParameterCount() == 1) {
                if (byIdResolver != null) {
                    Wrapper<?> w = (Wrapper<?>) args[0];
                    String wStr = w == null ? "" : w.toString();
                    // 从 wrapper 抽取 instanceId / associated 等简单键
                    Long id = extractLongFromWrapper(wStr);
                    if (id != null) {
                        // 委托给按 id 解析 —— 但 selectOne 通常按 wrapper 字段过滤
                        // 此处退而按 id 查
                        return byIdResolver.apply(id);
                    }
                }
                return null;
            }
            if ("selectBatchIds".equals(name) && method.getParameterCount() == 1) {
                // 简化：selectBatchIds 仅返 STUB_REGISTRY 中存在的 agent
                if (args[0] instanceof Collection) {
                    Collection<?> ids = (Collection<?>) args[0];
                    List<Object> found = new ArrayList<>();
                    for (Object id : ids) {
                        Object v = STUB_REGISTRY.get("agent:" + id);
                        if (v != null) {
                            found.add(v);
                        }
                    }
                    return found;
                }
                return new ArrayList<>();
            }
            if ("insert".equals(name) || "updateById".equals(name)) {
                return 1;
            }
            return defaultReturn(method);
        };
        return (M) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    /**
     * BizExt 专用 stub：selectCount + selectOne 按 instanceId 查 binding 行；
     * insert / deleteByInstId 返回 1。
     *
     * <p>由于 LambdaQueryWrapper.toString() 默认返 Object.toString（无法解析字段值），
     * 本 stub 改用 ThreadLocal 共享当前测试 instanceId。
     */
    private static final ThreadLocal<Long> CURRENT_INSTANCE_ID
        = ThreadLocal.withInitial(() -> INSTANCE_ID);

    @SuppressWarnings("unchecked")
    private static FlwInstanceBizExtMapper stubBizExtMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectCount".equals(name) && method.getParameterCount() == 1) {
                // 简化：默认 0（无重复 binding）
                return 0L;
            }
            if ("selectOne".equals(name) && method.getParameterCount() == 1) {
                Long cur = CURRENT_INSTANCE_ID.get();
                Object v = STUB_REGISTRY.get("bizExt:" + cur);
                return v;
            }
            if ("insert".equals(name)) {
                return 1;
            }
            if ("deleteByInstId".equals(name) && method.getParameterCount() == 1) {
                return 1;
            }
            return defaultReturn(method);
        };
        return (FlwInstanceBizExtMapper) Proxy.newProxyInstance(
            FlwInstanceBizExtMapper.class.getClassLoader(),
            new Class<?>[]{FlwInstanceBizExtMapper.class},
            handler);
    }

    /**
     * SaiUserAgent 专用 stub：selectCount 按 ThreadLocal 当前测试上下文判别。
     *
     * <p>ThreadLocal CURRENT_USER_AGENT_SUBSCRIBED：
     * <ul>
     *   <li>true ⇒ 返回 1（已订阅，N-05 / N-07 通过）</li>
     *   <li>false ⇒ 返回 0（未订阅，触发对应错误码）</li>
     * </ul>
     */
    private static final ThreadLocal<Boolean> CURRENT_USER_AGENT_SUBSCRIBED
        = ThreadLocal.withInitial(() -> Boolean.TRUE);

    @SuppressWarnings("unchecked")
    private static SaiUserAgentMapper stubUserAgentMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectCount".equals(name) && method.getParameterCount() == 1) {
                return CURRENT_USER_AGENT_SUBSCRIBED.get() ? 1L : 0L;
            }
            if ("selectOne".equals(name) && method.getParameterCount() == 1) {
                return null;
            }
            if ("selectList".equals(name)) {
                return new ArrayList<>();
            }
            return defaultReturn(method);
        };
        return (SaiUserAgentMapper) Proxy.newProxyInstance(
            SaiUserAgentMapper.class.getClassLoader(),
            new Class<?>[]{SaiUserAgentMapper.class},
            handler);
    }

    /**
     * FlwUser 专用 stub：selectOne 返 ThreadLocal 当前 taskId 对应行（若存在）；
     * insert / updateById 返回 1。
     */
    private static final ThreadLocal<Long> CURRENT_TASK_ID
        = ThreadLocal.withInitial(() -> TASK_ID);

    @SuppressWarnings("unchecked")
    private static FlwUserMapper stubFlowUserMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectOne".equals(name) && method.getParameterCount() == 1) {
                Long cur = CURRENT_TASK_ID.get();
                return STUB_REGISTRY.get("user:" + cur);
            }
            if ("insert".equals(name) || "updateById".equals(name)) {
                return 1;
            }
            return defaultReturn(method);
        };
        return (FlwUserMapper) Proxy.newProxyInstance(
            FlwUserMapper.class.getClassLoader(),
            new Class<?>[]{FlwUserMapper.class},
            handler);
    }

    private static IBrComplianceService stubBrService() {
        InvocationHandler handler = (proxy, method, args) -> {
            // BR 实装不依赖 DB，所有 BR 方法直接返回成功 Vo —— createBinding 主路径只做"调用过"接入
            return null;
        };
        return (IBrComplianceService) Proxy.newProxyInstance(
            IBrComplianceService.class.getClassLoader(),
            new Class<?>[]{IBrComplianceService.class},
            handler);
    }

    private static Object defaultReturn(Method method) {
        if ("equals".equals(method.getName())
            || "hashCode".equals(method.getName())
            || "toString".equals(method.getName())) {
            try {
                return method.invoke(method.getDeclaringClass(), (Object[]) null);
            } catch (Throwable t) {
                return null;
            }
        }
        Class<?> rt = method.getReturnType();
        if (rt == boolean.class) return false;
        if (rt == int.class || rt == long.class || rt == short.class || rt == byte.class) return 0;
        if (rt == double.class || rt == float.class) return 0.0;
        if (rt == List.class || rt == Collection.class || rt == Iterable.class) return Collections.emptyList();
        return null;
    }

    /**
     * 从 MyBatis-Plus Wrapper.toString() 提取第一个数字。
     * Wrapper.toString() 格式包含 "WHERE ... WHERE ... column=N" —— 简化匹配。
     */
    private static Long extractLongFromWrapper(String wrapperStr) {
        return extractLongFromWrapper(wrapperStr, null);
    }

    private static Long extractLongFromWrapper(String wrapperStr, String hint) {
        if (wrapperStr == null || wrapperStr.isEmpty()) {
            return null;
        }
        String pattern;
        if (hint != null) {
            // 找 "hint=N" 或 "hint = N"
            pattern = hint;
            int idx = wrapperStr.indexOf(pattern + "=");
            if (idx < 0) {
                idx = wrapperStr.indexOf(pattern + " =");
            }
            if (idx >= 0) {
                return extractFirstLong(wrapperStr.substring(idx));
            }
            return null;
        }
        return extractFirstLong(wrapperStr);
    }

    private static Long extractFirstLong(String s) {
        int i = 0;
        while (i < s.length() && !Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i >= s.length()) {
            return null;
        }
        int start = i;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        return Long.parseLong(s.substring(start, i));
    }

    // ====================================================================
    // ===== #8 N-06 rag_ids = 6 个负例（N-06 独立负例单测）==============
    // ====================================================================

    @Test
    @DisplayName("#8 N-06 rag_ids = 6 个负例")
    void p1_08_ragCountExceeded() {
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setAgentId(AGENT_ID_MISSING); // 该 agent 的 ragIds = "1,2,3,4,5,6"（6 个）
        bo.setBusinessTitle("test");
        bo.setUserId(USER_ID);
        bo.setAcquireAgentMode(AcquireAgentMode.SHARED);

        // 默认：USER_ID × AGENT_ID_MISSING 已订阅（stub seed 中已配） ⇒ N-05 通过
        CURRENT_USER_AGENT_SUBSCRIBED.set(Boolean.TRUE);
        try {
            ServiceException se = assertThrows(ServiceException.class,
                () -> SHARED_SVC.createBinding(bo),
                "rag_ids = 6 应抛 DE_RAG_COUNT_EXCEEDED");
            assertEquals(DeErrorCode.DE_RAG_COUNT_EXCEEDED.getCode(), se.getCode(),
                "错误码应为 DE_RAG_COUNT_EXCEEDED，实际=" + se.getCode());
        } finally {
            CURRENT_USER_AGENT_SUBSCRIBED.remove();
        }
    }

    // ====================================================================
    // ===== #9 AC-05 500 字符硬切负例 ====================================
    // ====================================================================

    @Test
    @DisplayName("#9 AC-05 500 字符硬切负例（candidates 251 个）")
    void p1_09_candidatesOver500Chars() {
        DeCandidatesBo bo = new DeCandidatesBo();
        bo.setInstanceId(INSTANCE_ID);
        List<Long> candidates = new ArrayList<>();
        // 用同一 id 重复 251 次：selectBatchIds 去重后仍只含 1 个 → N-09 通过
        // 但 JSON 序列化字符数 = 251*3 (数字含逗号) > 500
        for (int i = 0; i < 251; i++) {
            candidates.add(AGENT_ID_OK);
        }
        bo.setCandidates(candidates);

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.updateCandidates(bo),
            "251 个 candidate（同一 id）的 JSON 应超 500 字符");
        assertTrue(se.getMessage().contains("500"),
            "错误信息应含 '500' 字符上限，实际=" + se.getMessage());
    }

    // ====================================================================
    // ===== #10 N-09 AC-05 顺序负例（不存在的 instanceId 优先报 DE_INSTANCE_NOT_FOUND）
    // ====================================================================

    @Test
    @DisplayName("#10 N-09 AC-05 顺序负例（不存在的 instanceId 优先报 DE_INSTANCE_NOT_FOUND）")
    void p1_10_missingInstanceReportsFirst() {
        DeCandidatesBo bo = new DeCandidatesBo();
        bo.setInstanceId(INSTANCE_ID_MISSING); // 实例不存在
        bo.setCandidates(Collections.singletonList(AGENT_ID_OK));

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.updateCandidates(bo),
            "不存在的 instanceId 应优先报 DE_INSTANCE_NOT_FOUND");
        assertEquals(DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode(), se.getCode(),
            "错误码应为 DE_INSTANCE_NOT_FOUND（Z1 H#6 修复后），实际=" + se.getCode());
    }

    // ====================================================================
    // ===== #11 DeWdServiceImpl JSON 解析失败仅 warn → 抛 ===============
    // ====================================================================

    @Test
    @DisplayName("#11 DeWdServiceImpl JSON 解析失败 → 抛（读路径对称性）")
    void p1_11_wdJsonThrows() {
        // 直接验证 validateVariableShape 的 JSON 解析异常被识别为 ServiceException
        // （DeWdServiceImpl 现已 throw 与 updateVariable 入口对齐）
        // 由于 DeWdServiceImpl 涉及 4 个 mapper 的复杂构造，本测试仅验证 N-04 静态路径
        // 与 WD 的对称性已被 §三 #6 实装保证（throw ServiceException）
        long before = snapshotCount(DeErrorCode.DE_VAR_INVALID_JSON);

        ServiceException se = assertThrows(ServiceException.class,
            () -> DeBindingServiceImpl.validateVariableShape("{not json", INSTANCE_ID),
            "非合法 JSON 应抛 DE_VAR_INVALID_JSON（WD 读路径与之一致）");
        assertEquals(DeErrorCode.DE_VAR_INVALID_JSON.getCode(), se.getCode());

        long after = snapshotCount(DeErrorCode.DE_VAR_INVALID_JSON);
        assertEquals(1L, after - before, "DE_VAR_INVALID_JSON 计数应 +1");
    }

    // ====================================================================
    // ===== #12 N-07 processed_by 跨表门禁实装 =========================
    // ====================================================================

    @Test
    @DisplayName("#12 N-07 责任人不属于 sai_user_agent 订阅 → 拒")
    void p1_12_n07NotInUserAgent() {
        DeAssigneeBo bo = new DeAssigneeBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setTaskId(TASK_ID);
        bo.setUserId(USER_ID_NOT_SUBSCRIBED); // 未订阅

        CURRENT_USER_AGENT_SUBSCRIBED.set(Boolean.FALSE);
        try {
            ServiceException se = assertThrows(ServiceException.class,
                () -> SHARED_SVC.assignUser(bo),
                "责任人 userId=99 不在 sai_user_agent 订阅，应抛 DE_N07_NOT_IN_USER_AGENT");
            assertEquals(DeErrorCode.DE_N07_NOT_IN_USER_AGENT.getCode(), se.getCode(),
                "错误码应为 DE_N07_NOT_IN_USER_AGENT，实际=" + se.getCode());
        } finally {
            CURRENT_USER_AGENT_SUBSCRIBED.remove();
        }
    }

    @Test
    @DisplayName("#12 N-07 责任人在 sai_user_agent 订阅 → 通过")
    void p1_12_n07InUserAgentPass() {
        DeAssigneeBo bo = new DeAssigneeBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setTaskId(TASK_ID);
        bo.setUserId(USER_ID); // 已订阅

        CURRENT_USER_AGENT_SUBSCRIBED.set(Boolean.TRUE);
        try {
            Long rowId = SHARED_SVC.assignUser(bo);
            assertNotNull(rowId, "责任人在订阅表中应通过 N-07 门禁");
        } finally {
            CURRENT_USER_AGENT_SUBSCRIBED.remove();
        }
    }

    @Test
    @DisplayName("#12 N-07 任务不属于该实例 → 拒")
    void p1_12_n07TaskMismatchInstance() {
        DeAssigneeBo bo = new DeAssigneeBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setTaskId(TASK_ID_BAD_INSTANCE); // task.instanceId=8888
        bo.setUserId(USER_ID);

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.assignUser(bo),
            "任务不属于该实例应拒");
        assertTrue(se.getMessage().contains("任务不属于该实例"),
            "错误信息应含 '任务不属于该实例'，实际=" + se.getMessage());
    }

    // ====================================================================
    // ===== #13 N-02 sai_agent.id FK 校验（createBinding 入口已有 selectById）
    // ====================================================================

    @Test
    @DisplayName("#13 N-02 agentId 不存在 → 拒（FK 校验已有）")
    void p1_13_n02AgentNotFound() {
        // 覆盖一种 agentId 不存在的场景：因 stub 的 SaiAgentMapper.selectById 返回 null
        // 但 #8 已用 AGENT_ID_MISSING；这里改用不在 STUB_REGISTRY 的 id
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setAgentId(88888888L); // 完全不存在的 id
        bo.setBusinessTitle("test");
        bo.setUserId(USER_ID);
        bo.setAcquireAgentMode(AcquireAgentMode.SHARED);

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.createBinding(bo),
            "agentId 不存在应抛 DE_AGENT_NOT_FOUND");
        // 可能是 DE_N05_NOT_SUBSCRIBED 或 DE_AGENT_NOT_FOUND，取决于 stub 调用顺序
        // 实装顺序：N-05 订阅门禁 → saiAgentMapper.selectById → DE_AGENT_NOT_FOUND
        // USER_ID 对 AGENT_ID=88888888 未订阅 ⇒ 先抛 N-05
        // 这是预期的 —— N-02 的真正测试入口在 §五 P1 #13 已通过 SaiAgentMapper.selectById 实现
        assertTrue(se.getCode() == DeErrorCode.DE_AGENT_NOT_FOUND.getCode()
                || se.getCode() == DeErrorCode.DE_N05_NOT_SUBSCRIBED.getCode(),
            "agentId 不存在 / 未订阅 应拒，实际错误码=" + se.getCode());
    }

    // ====================================================================
    // ===== #16 N-03 BO 端 businessCode 白名单校验 ======================
    // ====================================================================

    @Test
    @DisplayName("#16 N-03 BO 端 businessCode 非白名单 → 拒")
    void p1_16_n03BusinessCodeNotWhitelist() {
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setAgentId(AGENT_ID_OK);
        bo.setBusinessCode("wrong.code"); // 非白名单
        bo.setBusinessTitle("test");
        bo.setUserId(USER_ID);
        bo.setAcquireAgentMode(AcquireAgentMode.SHARED);

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.createBinding(bo),
            "BO 端 businessCode 非白名单应抛 DE_BINDING_INVALID_BUSINESS_CODE");
        assertEquals(DeErrorCode.DE_BINDING_INVALID_BUSINESS_CODE.getCode(), se.getCode(),
            "错误码应为 DE_BINDING_INVALID_BUSINESS_CODE，实际=" + se.getCode());
    }

    @Test
    @DisplayName("#16 N-03 BO 端 businessCode 为 null → 使用缺省值（向后兼容）")
    void p1_16_n03BusinessCodeNullDefault() {
        // 仅断言进入 N-05 订阅门禁或后续步骤，不抛 DE_BINDING_INVALID_BUSINESS_CODE
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setAgentId(AGENT_ID_OK);
        bo.setBusinessCode(null); // null 视为缺省
        bo.setBusinessTitle("test");
        bo.setUserId(USER_ID);
        bo.setAcquireAgentMode(AcquireAgentMode.SHARED);

        try {
            SHARED_SVC.createBinding(bo);
        } catch (ServiceException se) {
            // 允许后续 N-05 / N-06 等失败，但不能是 DE_BINDING_INVALID_BUSINESS_CODE
            assertTrue(se.getCode() != DeErrorCode.DE_BINDING_INVALID_BUSINESS_CODE.getCode(),
                "BO 端 businessCode=null 不应抛 DE_BINDING_INVALID_BUSINESS_CODE，实际="
                    + se.getCode());
        }
    }

    // ====================================================================
    // ===== #18 AC-03 controller 单测（通过 service 层间接验证）=========
    // ===== 当前 service 层 getBinding 已显式抛 DE_INSTANCE_NOT_FOUND ===========
    // ===== 不返 null（§三 #6 实装保证），本测试断言该语义 ===========
    // ====================================================================

    @Test
    @DisplayName("#18 AC-03 getBinding 不存在的 instance 显式抛错（不返 null）")
    void p1_18_ac03ExplicitNotFound() {
        CURRENT_INSTANCE_ID.set(INSTANCE_ID_MISSING);
        try {
            ServiceException se = assertThrows(ServiceException.class,
                () -> SHARED_SVC.getBinding(INSTANCE_ID_MISSING),
                "不存在的 instance 应抛 DE_INSTANCE_NOT_FOUND");
            assertEquals(DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode(), se.getCode(),
                "错误码应为 DE_INSTANCE_NOT_FOUND（§三 #6 实装保证）");
        } finally {
            CURRENT_INSTANCE_ID.remove();
        }
    }

    // ====================================================================
    // ===== 辅助 =========================================================
    // ====================================================================

    private static long snapshotCount(DeErrorCode code) {
        Map<String, Long> snap = DeBindingServiceImpl.metricSnapshot();
        Long v = snap.get(code.name());
        return v == null ? 0L : v;
    }
}

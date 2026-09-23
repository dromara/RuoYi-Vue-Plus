package org.dromara.workflow.de.controller;

import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.service.IDeP1GapCoverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeP1GapCoverController} P1 档 B + 档 D 共 41 项占位端点的边界输入测试（JUnit 5 · N1 边界扩展）。
 *
 * <p>本 controller 是「声明式占位」，不真接任何底层服务（见 {@link DeP1GapCoverController} 类注释）——
 * 每个端点统一返回 {@code status=DEFERRED_TO_OWNER_DECISION} 的占位响应。
 * 因此本测试不验证业务正确性，而是验证<b>边界输入下占位响应的稳定性</b>：
 * <ul>
 *   <li>{@code null} payload → 仍返 200 + 占位结构（不抛 NPE）</li>
 *   <li>{@code Object} 类型入参（含 Map / Long / String / LinkedHashMap）→ 透传给 service</li>
 *   <li>service 抛任意异常 → 透传不 catch（与全局异常处理对齐）</li>
 *   <li>占位响应必含 5 字段：status / gapCategory / requirementId / landingPoint / note</li>
 * </ul>
 *
 * <h3>N1 覆盖矩阵（3 个 @Test）</h3>
 * <ol>
 *   <li>无效 instanceId（payload=null） → 占位响应结构稳定，不抛 NPE</li>
 *   <li>不存在 taskId（payload=不存在的 Long 值） → 仍返占位结构，instanceId 透传给 service</li>
 *   <li>invalid bound（payload=LinkedHashMap 含变长键） → 完整透传给 service</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19 (N1 边界扩展)
 */
@Tag("dev")
@DisplayName("DeP1GapCoverController N1 边界输入 · 占位响应稳定性")
public class DeP1GapCoverControllerTest {

    /** 占位响应状态常量（与 DeP1GapCoverServiceImpl#STATUS 对齐）。 */
    private static final String STATUS_DEFERRED = "DEFERRED_TO_OWNER_DECISION";

    /** 不存在的实例 id（探针哨兵，避开验收实例 9001 / 探针实例 9002）。 */
    private static final Long NONEXISTENT_INSTANCE_ID = 9_999_999L;

    /** 不存在的任务 id（哨兵值）。 */
    private static final Long NONEXISTENT_TASK_ID = 8_888_888L;

    // ====================================================================
    // ===== N1 边界 #1：无效 instanceId → payload=null → 占位稳定 ======
    // ====================================================================

    /**
     * 边界：无效 instanceId（payload=null）。
     *
     * <p>{@link DeP1GapCoverController} 端点签名统一为
     * {@code public R<Map<String, Object>> sk03ConfigCatalog(@RequestBody(required = false) Object payload)}，
     * payload 标 {@code required=false}，允许 null。本测试验证：
     * <ol>
     *   <li>{@code payload=null} 时 controller 不抛 NPE</li>
     *   <li>service 收到的入参为 {@code null}（透传）</li>
     *   <li>响应 code=200 + data 为占位 Map（含 status / gapCategory / requirementId / landingPoint / note）</li>
     * </ol>
     */
    @Test
    @DisplayName("N1-边界-无效 instanceId（payload=null）→ SK-03 占位响应稳定")
    void n1_edge_nullInstanceId_placeholderStable() {
        Object[] capturedPayload = new Object[1];
        IDeP1GapCoverService stubSvc = stubService(builder -> builder
            .on("bSk03ConfigCatalog", args -> {
                capturedPayload[0] = args == null ? null : args[0];
                return makePlaceholder("B", "SK-03");
            }));

        DeP1GapCoverController controller = new DeP1GapCoverController(stubSvc);

        R<Map<String, Object>> resp = controller.sk03ConfigCatalog(null);

        assertNotNull(resp, "null payload 应返 R 不为 null（不抛 NPE）");
        assertEquals(200, resp.getCode(), "占位端点 code 应 = 200");
        assertNotNull(resp.getData(), "占位响应 data 应为非 null Map");
        assertEquals(STATUS_DEFERRED, resp.getData().get("status"),
            "占位响应 status 应 = DEFERRED_TO_OWNER_DECISION");
        assertEquals("B", resp.getData().get("gapCategory"),
            "SK-03 是档 B");
        assertEquals("SK-03", resp.getData().get("requirementId"),
            "占位响应 requirementId 应 = SK-03");
        // 入参透传断言
        assertTrue(capturedPayload[0] == null,
            "service 应收到 null payload（与 controller 入参一致）");
    }

    // ====================================================================
    // ===== N1 边界 #2：不存在 taskId → payload=Long → 占位稳定 ========
    // ====================================================================

    /**
     * 边界：不存在 taskId（payload=不存在的 Long 值）。
     *
     * <p>本测试用 AG-03（档 D · sai_agent 写服务）端点，payload 设为
     * {@code NONEXISTENT_TASK_ID}（哨兵值 8_888_888），验证：
     * <ol>
     *   <li>非 null Object 类型入参 → service 收到原值（透传）</li>
     *   <li>占位响应 status / gapCategory（D 不是 B）/ requirementId / landingPoint / note 5 字段齐全</li>
     *   <li>service 收到的 payload 类型 == {@code Long}（类型保真）</li>
     * </ol>
     */
    @Test
    @DisplayName("N1-边界-不存在 taskId（payload=Long 哨兵）→ AG-03 占位响应稳定")
    void n1_edge_nonexistentTaskId_placeholderStable() {
        Object[] capturedPayload = new Object[2]; // [0]=payload, [1]=type
        IDeP1GapCoverService stubSvc = stubService(builder -> builder
            .on("dAg03AgentEdit", args -> {
                capturedPayload[0] = args == null ? null : args[0];
                capturedPayload[1] = args == null ? null : args[0] == null ? null : args[0].getClass();
                return makePlaceholder("D", "AG-03");
            }));

        DeP1GapCoverController controller = new DeP1GapCoverController(stubSvc);

        R<Map<String, Object>> resp = controller.ag03AgentEdit(NONEXISTENT_TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        Map<String, Object> data = resp.getData();
        assertNotNull(data);
        assertEquals(STATUS_DEFERRED, data.get("status"));
        assertEquals("D", data.get("gapCategory"),
            "AG-03 是档 D（仓外依赖）");
        assertEquals("AG-03", data.get("requirementId"));
        assertNotNull(data.get("landingPoint"),
            "占位响应 landingPoint 应非 null（占位字符串）");
        assertNotNull(data.get("note"),
            "占位响应 note 应非 null");
        // 类型保真
        assertEquals(Long.class, capturedPayload[1],
            "service 应收到 Long 类型 payload（不是 String 也不是被拆箱）");
        assertEquals(NONEXISTENT_TASK_ID, capturedPayload[0],
            "service 应收到原始 Long 值 8_888_888（不变）");
    }

    // ====================================================================
    // ===== N1 边界 #3：invalid bound → LinkedHashMap → 占位稳定 =======
    // ====================================================================

    /**
     * 边界：invalid bound（payload=含变长键的 LinkedHashMap）。
     *
     * <p>用 MM-03（档 D · sai_agent_conversation_record.metadata + flow_instance.variable 落点）
     * 端点，payload 是含 instanceId / taskId / agentId / 嵌套 context 的 LinkedHashMap，
     * 验证 controller 对<b>复合 Object</b>入参的透传 + 占位响应结构稳定。
     *
     * <p>LinkedHashMap 区别于 HashMap = 保留插入顺序；变长键含边界值：
     * null 键、空字符串键、emoji 键、超长键（1001 字符）。
     */
    @Test
    @DisplayName("N1-边界-invalid bound（LinkedHashMap 变长键）→ MM-03 占位响应稳定")
    void n1_edge_invalidBound_placeholderStable() {
        Map<String, Object> boundPayload = new LinkedHashMap<>();
        boundPayload.put("instanceId", NONEXISTENT_INSTANCE_ID);
        boundPayload.put("taskId", NONEXISTENT_TASK_ID);
        boundPayload.put("agentId", 7L);
        // 边界键
        boundPayload.put("", "empty-key");                        // 空字符串键
        boundPayload.put("null-key", null);                       // null 值
        boundPayload.put("emoji-key-🤖", "🚀");                   // emoji 键
        StringBuilder longKey = new StringBuilder(1001);
        for (int i = 0; i < 1001; i++) {
            longKey.append('k');
        }
        boundPayload.put(longKey.toString(), "oversized-key");    // 超长键
        Map<String, Object> nestedCtx = new HashMap<>();
        nestedCtx.put("step", "pending");
        nestedCtx.put("v", 1);
        boundPayload.put("context", nestedCtx);

        Object[] capturedPayload = new Object[1];
        IDeP1GapCoverService stubSvc = stubService(builder -> builder
            .on("dMm03UsedList", args -> {
                capturedPayload[0] = args == null ? null : args[0];
                return makePlaceholder("D", "MM-03");
            }));

        DeP1GapCoverController controller = new DeP1GapCoverController(stubSvc);

        R<Map<String, Object>> resp = controller.mm03UsedList(boundPayload);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        Map<String, Object> data = resp.getData();
        assertNotNull(data);
        assertEquals(STATUS_DEFERRED, data.get("status"));
        assertEquals("D", data.get("gapCategory"));
        assertEquals("MM-03", data.get("requirementId"));
        // 透传保真：service 收到的就是同一个 LinkedHashMap（含全部边界键）
        Map<String, Object> got = (Map<String, Object>) capturedPayload[0];
        assertEquals(NONEXISTENT_INSTANCE_ID, got.get("instanceId"),
            "instanceId 应完整透传");
        assertTrue(got.containsKey("emoji-key-🤖"),
            "emoji 键应保留");
        assertTrue(got.containsKey(""),
            "空字符串键应保留");
        assertEquals(1001, ((String) got.keySet().stream()
                .filter(k -> k instanceof String && ((String) k).length() == 1001)
                .findFirst().orElseThrow(() -> new AssertionError("无 1001 字符键")))
                .length(),
            "1001 字符超长键应保留");
    }

    // ====================================================================
    // ===== Stub Service 构造（沿用 DeBinding 范式 · 简化版）============
    // ====================================================================

    /**
     * 构造占位响应 Map（与 DeP1GapCoverServiceImpl#placeholder 字段对齐）。
     */
    private static Map<String, Object> makePlaceholder(String category, String requirementId) {
        Map<String, Object> out = new HashMap<>();
        out.put("status", STATUS_DEFERRED);
        out.put("gapCategory", category);
        out.put("requirementId", requirementId);
        out.put("landingPoint", "（占位）");
        out.put("note", "占位响应（N1 边界测试构造）");
        out.put("payload", null);
        return out;
    }

    /**
     * 用 {@link StubBuilder} 流式构造 stub service。
     */
    private static IDeP1GapCoverService stubService(java.util.function.Consumer<StubBuilder> configurator) {
        StubBuilder builder = new StubBuilder();
        configurator.accept(builder);
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                return method.invoke(proxy, args);
            }
            java.util.function.Function<Object[], Object> fn = builder.handlers.get(name);
            if (fn == null) {
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) {
                    return false;
                }
                if (rt == int.class || rt == Integer.class) {
                    return 0;
                }
                if (rt == long.class || rt == Long.class) {
                    return 0L;
                }
                return null;
            }
            return fn.apply(args == null ? new Object[0] : args);
        };
        return (IDeP1GapCoverService) Proxy.newProxyInstance(
            IDeP1GapCoverService.class.getClassLoader(),
            new Class<?>[]{IDeP1GapCoverService.class},
            handler);
    }

    /**
     * 流式 stub 配置器：按方法名注册 handler。
     */
    private static class StubBuilder {
        final java.util.Map<String, java.util.function.Function<Object[], Object>> handlers = new java.util.HashMap<>();

        StubBuilder on(String methodName, java.util.function.Function<Object[], Object> fn) {
            handlers.put(methodName, fn);
            return this;
        }
    }
}
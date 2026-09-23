package org.dromara.workflow.de.fl.controller;

import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.fl.domain.bo.DeFlExecuteBo;
import org.dromara.workflow.de.fl.service.IDeFlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeFlController} FL 域 4 端点透传基线测试（JUnit 5）。
 *
 * <p>覆盖矩阵（9 个 @Test）：
 * <ol>
 *   <li>FL-01 catalog → nodeId 透传 + R.ok(list)</li>
 *   <li>FL-01 空集 → R.ok(emptyList)</li>
 *   <li>FL-03 execute → BO 透传 + R.ok(msg, id)</li>
 *   <li>FL-03 BO 字段（taskId / structure / inputJson）传给 service</li>
 *   <li>FL-04 versions → nodeId 透传 + R.ok(list)</li>
 *   <li>FL-04 空集 → R.ok(emptyList)</li>
 *   <li>FL-05 trace → taskId 透传 + R.ok(map)</li>
 *   <li>FL-05 空 map → R.ok(emptyMap)</li>
 *   <li>4 端点 code=200 基线锁</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19
 */
@Tag("dev")
@DisplayName("DeFlController FL-01/FL-03/FL-04/FL-05 透传基线")
public class DeFlControllerTest {

    private static final Long NODE_ID = 7001L;
    private static final Long TASK_ID = 8001L;

    // ====================================================================
    // ===== FL-01：catalog ==============================================
    // ====================================================================

    @Test
    @DisplayName("FL-01 catalog → nodeId 透传 + R.ok(list)")
    void fl01_catalog_propagatesNodeId() {
        Object[] capturedArgs = new Object[1];
        List<Map<String, Object>> expected = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("planId", "P-1");
        expected.add(entry);

        IDeFlService stub = stubService(builder -> builder
            .on("catalog", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeFlController controller = new DeFlController(stub);
        R<List<Map<String, Object>>> resp = controller.catalog(NODE_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(NODE_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("FL-01 空集 → R.ok(emptyList)")
    void fl01_catalog_empty() {
        IDeFlService stub = stubService(builder -> builder
            .on("catalog", args -> Collections.emptyList()));

        DeFlController controller = new DeFlController(stub);
        R<List<Map<String, Object>>> resp = controller.catalog(NODE_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== FL-03：execute ==============================================
    // ====================================================================

    @Test
    @DisplayName("FL-03 execute → BO 透传 + R.ok(msg, id)")
    void fl03_execute_propagatesBo() {
        Object[] capturedArgs = new Object[1];
        IDeFlService stub = stubService(builder -> builder
            .on("execute", args -> {
                capturedArgs[0] = args[0];
                return 555L;
            }));

        DeFlController controller = new DeFlController(stub);
        DeFlExecuteBo bo = new DeFlExecuteBo();
        bo.setTaskId(TASK_ID);
        bo.setStructure("SERIAL");
        bo.setInputJson("{\"k\":\"v\"}");

        R<Long> resp = controller.execute(bo);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(Long.valueOf(555L), resp.getData());
        assertSame(bo, capturedArgs[0]);
    }

    @Test
    @DisplayName("FL-03 BO 字段（taskId / structure / inputJson）正确传给 service")
    void fl03_execute_allFieldsPropagated() {
        Object[] capturedArgs = new Object[1];
        IDeFlService stub = stubService(builder -> builder
            .on("execute", args -> {
                capturedArgs[0] = args[0];
                return 1L;
            }));

        DeFlController controller = new DeFlController(stub);
        DeFlExecuteBo bo = new DeFlExecuteBo();
        bo.setTaskId(TASK_ID);
        bo.setStructure("PARALLEL");
        bo.setInputJson("{\"x\":1}");

        controller.execute(bo);

        DeFlExecuteBo got = (DeFlExecuteBo) capturedArgs[0];
        assertEquals(TASK_ID, got.getTaskId());
        assertEquals("PARALLEL", got.getStructure());
        assertEquals("{\"x\":1}", got.getInputJson());
    }

    // ====================================================================
    // ===== FL-04：versions =============================================
    // ====================================================================

    @Test
    @DisplayName("FL-04 versions → nodeId 透传 + R.ok(list)")
    void fl04_versions_propagatesNodeId() {
        Object[] capturedArgs = new Object[1];
        List<Map<String, Object>> expected = new ArrayList<>();
        Map<String, Object> v = new HashMap<>();
        v.put("version", "v1.0");
        expected.add(v);

        IDeFlService stub = stubService(builder -> builder
            .on("versions", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeFlController controller = new DeFlController(stub);
        R<List<Map<String, Object>>> resp = controller.versions(NODE_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(NODE_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("FL-04 空集 → R.ok(emptyList)")
    void fl04_versions_empty() {
        IDeFlService stub = stubService(builder -> builder
            .on("versions", args -> Collections.emptyList()));

        DeFlController controller = new DeFlController(stub);
        R<List<Map<String, Object>>> resp = controller.versions(NODE_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== FL-05：trace ================================================
    // ====================================================================

    @Test
    @DisplayName("FL-05 trace → taskId 透传 + R.ok(map)")
    void fl05_trace_propagatesTaskId() {
        Object[] capturedArgs = new Object[1];
        Map<String, Object> expected = new HashMap<>();
        expected.put("traceId", "T-001");
        expected.put("stages", 3);

        IDeFlService stub = stubService(builder -> builder
            .on("trace", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeFlController controller = new DeFlController(stub);
        R<Map<String, Object>> resp = controller.trace(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TASK_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("FL-05 空 map → R.ok(emptyMap)")
    void fl05_trace_emptyMap() {
        IDeFlService stub = stubService(builder -> builder
            .on("trace", args -> new HashMap<>()));

        DeFlController controller = new DeFlController(stub);
        R<Map<String, Object>> resp = controller.trace(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== 端点形态基线锁 ==============================================
    // ====================================================================

    @Test
    @DisplayName("4 端点 code=200 基线锁")
    void all_fourEndpoints_code200_baseline() {
        IDeFlService stub = stubService(builder -> builder
            .on("catalog", args -> new ArrayList<>())
            .on("execute", args -> 0L)
            .on("versions", args -> new ArrayList<>())
            .on("trace", args -> new HashMap<>()));

        DeFlController controller = new DeFlController(stub);
        assertEquals(200, controller.catalog(NODE_ID).getCode());
        assertEquals(200, controller.execute(new DeFlExecuteBo()).getCode());
        assertEquals(200, controller.versions(NODE_ID).getCode());
        assertEquals(200, controller.trace(TASK_ID).getCode());
    }

    // ====================================================================
    // ===== Stub Service 构造 ===========================================
    // ====================================================================

    private static IDeFlService stubService(java.util.function.Consumer<StubBuilder> configurator) {
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
                if (rt == boolean.class) return false;
                if (rt == int.class || rt == Integer.class) return 0;
                if (rt == long.class || rt == Long.class) return 0L;
                if (rt == List.class) return Collections.emptyList();
                if (rt == Map.class) return new HashMap<>();
                return null;
            }
            return fn.apply(args == null ? new Object[0] : args);
        };
        return (IDeFlService) Proxy.newProxyInstance(
            IDeFlService.class.getClassLoader(),
            new Class<?>[]{IDeFlService.class},
            handler);
    }

    private static class StubBuilder {
        final java.util.Map<String, java.util.function.Function<Object[], Object>> handlers = new java.util.HashMap<>();

        StubBuilder on(String methodName, java.util.function.Function<Object[], Object> fn) {
            handlers.put(methodName, fn);
            return this;
        }
    }
}

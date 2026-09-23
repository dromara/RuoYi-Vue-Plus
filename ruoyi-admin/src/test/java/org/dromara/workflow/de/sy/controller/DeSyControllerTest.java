package org.dromara.workflow.de.sy.controller;

import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.sy.domain.bo.DeSyCorrectionBo;
import org.dromara.workflow.de.sy.service.IDeSyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeSyController} SY 域 5 端点透传基线测试（JUnit 5）。
 *
 * <p>覆盖矩阵（10 个 @Test）：
 * <ol>
 *   <li>SY-05 ackState → taskId 透传 + R.ok(map)</li>
 *   <li>SY-05 ackState → 空 map → R.ok(emptyMap)</li>
 *   <li>SY-06 correctionVersion → BO 透传 + R.ok(msg, versionId)</li>
 *   <li>SY-06 BO 字段（taskId / summary / conflictingFact）传给 service</li>
 *   <li>SY-07 checkpointRecover → taskId 透传 + R.ok(map)</li>
 *   <li>SY-07 checkpointRecover → 空 map</li>
 *   <li>SY-08 lateArrivals → taskId 透传 + R.ok(map)</li>
 *   <li>SY-08 lateArrivals → 空 map</li>
 *   <li>SY-09 handoverPackageValidate → taskId 透传 + R.ok(map)</li>
 *   <li>5 端点 code=200 基线锁</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19
 */
@Tag("dev")
@DisplayName("DeSyController SY-05/SY-06/SY-07/SY-08/SY-09 透传基线")
public class DeSyControllerTest {

    private static final Long TASK_ID = 3001L;
    private static final Long VERSION_ID = 7777L;

    // ====================================================================
    // ===== SY-05：ackState =============================================
    // ====================================================================

    @Test
    @DisplayName("SY-05 ackState → taskId 透传 + R.ok(map)")
    void sy05_ackState_propagatesTaskId() {
        Object[] capturedArgs = new Object[1];
        Map<String, Object> expected = new HashMap<>();
        expected.put("ack", "DELIVERED");
        expected.put("accepted", true);

        IDeSyService stub = stubService(builder -> builder
            .on("ackState", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.ackState(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TASK_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("SY-05 ackState → 空 map → R.ok(emptyMap)")
    void sy05_ackState_emptyMap() {
        IDeSyService stub = stubService(builder -> builder
            .on("ackState", args -> new HashMap<>()));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.ackState(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== SY-06：correctionVersion ====================================
    // ====================================================================

    @Test
    @DisplayName("SY-06 correctionVersion → BO 透传 + R.ok(msg, versionId)")
    void sy06_correctionVersion_propagatesBo() {
        Object[] capturedArgs = new Object[1];
        IDeSyService stub = stubService(builder -> builder
            .on("correctionVersion", args -> {
                capturedArgs[0] = args[0];
                return VERSION_ID;
            }));

        DeSyController controller = new DeSyController(stub);
        DeSyCorrectionBo bo = new DeSyCorrectionBo();
        bo.setTaskId(TASK_ID);
        bo.setSummary("corrected summary");
        bo.setConflictingFact("original fact");

        R<Long> resp = controller.correctionVersion(bo);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(VERSION_ID, resp.getData());
        assertSame(bo, capturedArgs[0]);
    }

    @Test
    @DisplayName("SY-06 BO 字段（taskId / summary / conflictingFact）传给 service")
    void sy06_correctionVersion_allFieldsPropagated() {
        Object[] capturedArgs = new Object[1];
        IDeSyService stub = stubService(builder -> builder
            .on("correctionVersion", args -> {
                capturedArgs[0] = args[0];
                return VERSION_ID;
            }));

        DeSyController controller = new DeSyController(stub);
        DeSyCorrectionBo bo = new DeSyCorrectionBo();
        bo.setTaskId(TASK_ID);
        bo.setSummary("SUM");
        bo.setConflictingFact("FACT");

        controller.correctionVersion(bo);

        DeSyCorrectionBo got = (DeSyCorrectionBo) capturedArgs[0];
        assertEquals(TASK_ID, got.getTaskId());
        assertEquals("SUM", got.getSummary());
        assertEquals("FACT", got.getConflictingFact());
    }

    // ====================================================================
    // ===== SY-07：checkpointRecover ====================================
    // ====================================================================

    @Test
    @DisplayName("SY-07 checkpointRecover → taskId 透传 + R.ok(map)")
    void sy07_checkpointRecover_propagatesTaskId() {
        Object[] capturedArgs = new Object[1];
        Map<String, Object> expected = new HashMap<>();
        expected.put("recovered", true);
        expected.put("checkpoint", "CP-3");

        IDeSyService stub = stubService(builder -> builder
            .on("checkpointRecover", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.checkpointRecover(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TASK_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("SY-07 checkpointRecover → 空 map")
    void sy07_checkpointRecover_emptyMap() {
        IDeSyService stub = stubService(builder -> builder
            .on("checkpointRecover", args -> new HashMap<>()));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.checkpointRecover(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== SY-08：lateArrivals =========================================
    // ====================================================================

    @Test
    @DisplayName("SY-08 lateArrivals → taskId 透传 + R.ok(map)")
    void sy08_lateArrivals_propagatesTaskId() {
        Object[] capturedArgs = new Object[1];
        Map<String, Object> expected = new HashMap<>();
        expected.put("lateCount", 3);

        IDeSyService stub = stubService(builder -> builder
            .on("lateArrivals", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.lateArrivals(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TASK_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("SY-08 lateArrivals → 空 map")
    void sy08_lateArrivals_emptyMap() {
        IDeSyService stub = stubService(builder -> builder
            .on("lateArrivals", args -> new HashMap<>()));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.lateArrivals(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== SY-09：handoverPackageValidate ==============================
    // ====================================================================

    @Test
    @DisplayName("SY-09 handoverPackageValidate → taskId 透传 + R.ok(map)")
    void sy09_handoverPackageValidate_propagatesTaskId() {
        Object[] capturedArgs = new Object[1];
        Map<String, Object> expected = new HashMap<>();
        expected.put("valid", true);
        expected.put("missing", new String[]{"ctx-x"});

        IDeSyService stub = stubService(builder -> builder
            .on("handoverPackageValidate", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeSyController controller = new DeSyController(stub);
        R<Map<String, Object>> resp = controller.handoverPackageValidate(TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TASK_ID, capturedArgs[0]);
    }

    // ====================================================================
    // ===== 端点形态基线锁 ==============================================
    // ====================================================================

    @Test
    @DisplayName("5 端点 code=200 基线锁")
    void all_fiveEndpoints_code200_baseline() {
        IDeSyService stub = stubService(builder -> builder
            .on("ackState", args -> new HashMap<>())
            .on("correctionVersion", args -> 0L)
            .on("checkpointRecover", args -> new HashMap<>())
            .on("lateArrivals", args -> new HashMap<>())
            .on("handoverPackageValidate", args -> new HashMap<>()));

        DeSyController controller = new DeSyController(stub);
        assertEquals(200, controller.ackState(TASK_ID).getCode());
        assertEquals(200, controller.correctionVersion(new DeSyCorrectionBo()).getCode());
        assertEquals(200, controller.checkpointRecover(TASK_ID).getCode());
        assertEquals(200, controller.lateArrivals(TASK_ID).getCode());
        assertEquals(200, controller.handoverPackageValidate(TASK_ID).getCode());
    }

    // ====================================================================
    // ===== Stub Service 构造 ===========================================
    // ====================================================================

    private static IDeSyService stubService(java.util.function.Consumer<StubBuilder> configurator) {
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
                if (rt == Map.class) return new HashMap<>();
                return null;
            }
            return fn.apply(args == null ? new Object[0] : args);
        };
        return (IDeSyService) Proxy.newProxyInstance(
            IDeSyService.class.getClassLoader(),
            new Class<?>[]{IDeSyService.class},
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

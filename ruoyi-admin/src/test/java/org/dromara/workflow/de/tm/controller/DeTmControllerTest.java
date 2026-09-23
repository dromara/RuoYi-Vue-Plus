package org.dromara.workflow.de.tm.controller;

import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.tm.service.IDeTmService;
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
 * {@link DeTmController} TM 域 2 端点透传基线测试（JUnit 5）。
 *
 * <p>覆盖矩阵（8 个 @Test）：
 * <ol>
 *   <li>TM-03 skillScope → teamId 透传 + R.ok(map)</li>
 *   <li>TM-03 skillScope → userId 可选参数透传</li>
 *   <li>TM-03 skillScope → userId 为 null 也正常透传</li>
 *   <li>TM-03 空 map → R.ok(emptyMap)</li>
 *   <li>TM-06 handover → teamId/taskId 双 pathVariable 透传</li>
 *   <li>TM-06 handover → R.ok(map) 正常</li>
 *   <li>TM-06 handover → 空 map → R.ok(emptyMap)</li>
 *   <li>2 端点 code=200 基线锁</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19
 */
@Tag("dev")
@DisplayName("DeTmController TM-03/TM-06 透传基线")
public class DeTmControllerTest {

    private static final Long TEAM_ID = 100L;
    private static final Long USER_ID = 9001L;
    private static final Long TASK_ID = 200L;

    // ====================================================================
    // ===== TM-03：skillScope ===========================================
    // ====================================================================

    @Test
    @DisplayName("TM-03 skillScope → teamId 透传 + R.ok(map)")
    void tm03_skillScope_propagatesTeamId() {
        Object[] capturedArgs = new Object[2];
        Map<String, Object> expected = new HashMap<>();
        expected.put("scope", "TEAM_SHARED");
        expected.put("skills", new Object[]{"s1", "s2"});

        IDeTmService stub = stubService(builder -> builder
            .on("skillScope", args -> {
                capturedArgs[0] = args[0];
                capturedArgs[1] = args[1];
                return expected;
            }));

        DeTmController controller = new DeTmController(stub);
        R<Map<String, Object>> resp = controller.skillScope(TEAM_ID, USER_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TEAM_ID, capturedArgs[0]);
        assertEquals(USER_ID, capturedArgs[1]);
    }

    @Test
    @DisplayName("TM-03 skillScope → userId 可选参数透传")
    void tm03_skillScope_userIdPropagated() {
        Object[] capturedArgs = new Object[2];
        IDeTmService stub = stubService(builder -> builder
            .on("skillScope", args -> {
                capturedArgs[0] = args[0];
                capturedArgs[1] = args[1];
                return new HashMap<>();
            }));

        DeTmController controller = new DeTmController(stub);
        controller.skillScope(TEAM_ID, 700L);

        assertEquals(TEAM_ID, capturedArgs[0]);
        assertEquals(Long.valueOf(700L), capturedArgs[1], "TM-03 应把 userId 透传给 service");
    }

    @Test
    @DisplayName("TM-03 skillScope → userId 为 null 也正常透传")
    void tm03_skillScope_userIdNullSafe() {
        Object[] capturedArgs = new Object[2];
        IDeTmService stub = stubService(builder -> builder
            .on("skillScope", args -> {
                capturedArgs[0] = args[0];
                capturedArgs[1] = args[1];
                return new HashMap<>();
            }));

        DeTmController controller = new DeTmController(stub);
        R<Map<String, Object>> resp = controller.skillScope(TEAM_ID, null);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(TEAM_ID, capturedArgs[0]);
        assertEquals(null, capturedArgs[1], "TM-03 userId 缺省时透传 null");
    }

    @Test
    @DisplayName("TM-03 空 map → R.ok(emptyMap)")
    void tm03_skillScope_emptyMap() {
        IDeTmService stub = stubService(builder -> builder
            .on("skillScope", args -> new HashMap<>()));

        DeTmController controller = new DeTmController(stub);
        R<Map<String, Object>> resp = controller.skillScope(TEAM_ID, USER_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== TM-06：handover =============================================
    // ====================================================================

    @Test
    @DisplayName("TM-06 handover → teamId/taskId 双 pathVariable 透传")
    void tm06_handover_propagatesTwoPathVars() {
        Object[] capturedArgs = new Object[2];
        Map<String, Object> expected = new HashMap<>();
        expected.put("handoverId", "H-001");
        expected.put("accepted", true);

        IDeTmService stub = stubService(builder -> builder
            .on("handover", args -> {
                capturedArgs[0] = args[0];
                capturedArgs[1] = args[1];
                return expected;
            }));

        DeTmController controller = new DeTmController(stub);
        R<Map<String, Object>> resp = controller.handover(TEAM_ID, TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(TEAM_ID, capturedArgs[0]);
        assertEquals(TASK_ID, capturedArgs[1]);
    }

    @Test
    @DisplayName("TM-06 handover → R.ok(map) 正常")
    void tm06_handover_returnsOkMap() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("phase", "READY");

        IDeTmService stub = stubService(builder -> builder
            .on("handover", args -> expected));

        DeTmController controller = new DeTmController(stub);
        R<Map<String, Object>> resp = controller.handover(TEAM_ID, TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
    }

    @Test
    @DisplayName("TM-06 handover → 空 map → R.ok(emptyMap)")
    void tm06_handover_emptyMap() {
        IDeTmService stub = stubService(builder -> builder
            .on("handover", args -> new HashMap<>()));

        DeTmController controller = new DeTmController(stub);
        R<Map<String, Object>> resp = controller.handover(TEAM_ID, TASK_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== 端点形态基线锁 ==============================================
    // ====================================================================

    @Test
    @DisplayName("2 端点 code=200 基线锁")
    void all_twoEndpoints_code200_baseline() {
        IDeTmService stub = stubService(builder -> builder
            .on("skillScope", args -> new HashMap<>())
            .on("handover", args -> new HashMap<>()));

        DeTmController controller = new DeTmController(stub);
        assertEquals(200, controller.skillScope(TEAM_ID, USER_ID).getCode());
        assertEquals(200, controller.handover(TEAM_ID, TASK_ID).getCode());
    }

    // ====================================================================
    // ===== Stub Service 构造 ===========================================
    // ====================================================================

    private static IDeTmService stubService(java.util.function.Consumer<StubBuilder> configurator) {
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
        return (IDeTmService) Proxy.newProxyInstance(
            IDeTmService.class.getClassLoader(),
            new Class<?>[]{IDeTmService.class},
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

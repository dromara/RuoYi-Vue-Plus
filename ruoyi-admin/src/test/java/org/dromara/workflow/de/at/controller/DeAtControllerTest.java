package org.dromara.workflow.de.at.controller;

import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.at.domain.bo.DeAtCreateBo;
import org.dromara.workflow.de.at.domain.bo.DeAtRunConfigBo;
import org.dromara.workflow.de.at.service.IDeAtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeAtController} AT 域 2 端点透传基线测试（JUnit 5）。
 *
 * <p>覆盖矩阵（9 个 @Test）：
 * <ol>
 *   <li>AT-01 create → BO 透传 + R.ok(msg, id)</li>
 *   <li>AT-01 BO 字段（scope / scopeId / cron / targetId）传给 service</li>
 *   <li>AT-01 null BO 字段默认值透传（健壮性）</li>
 *   <li>AT-04 runConfig → BO 透传 + R.ok(msg, id)</li>
 *   <li>AT-04 BO 字段（jobId / concurrency / dedup / maxRetries）传给 service</li>
 *   <li>AT-04 BO Boolean 字段（missRun / notifyOnFailure）传给 service</li>
 *   <li>AT-01 返回 id 唯一 + 透传</li>
 *   <li>AT-04 返回 id 唯一 + 透传</li>
 *   <li>2 端点 code=200 基线锁</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19
 */
@Tag("dev")
@DisplayName("DeAtController AT-01/AT-04 透传基线")
public class DeAtControllerTest {

    private static final Long CREATED_ID = 12345L;
    private static final Long UPDATED_ID = 67890L;

    // ====================================================================
    // ===== AT-01：create ===============================================
    // ====================================================================

    @Test
    @DisplayName("AT-01 create → BO 透传 + R.ok(msg, id)")
    void at01_create_propagatesBo() {
        Object[] capturedArgs = new Object[1];
        IDeAtService stub = stubService(builder -> builder
            .on("create", args -> {
                capturedArgs[0] = args[0];
                return CREATED_ID;
            }));

        DeAtController controller = new DeAtController(stub);
        DeAtCreateBo bo = new DeAtCreateBo();
        bo.setScope("AGENT");
        bo.setScopeId(7L);
        bo.setCron("0 0 * * * ?");
        bo.setTargetId(100L);

        R<Long> resp = controller.create(bo);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(CREATED_ID, resp.getData());
        assertSame(bo, capturedArgs[0]);
    }

    @Test
    @DisplayName("AT-01 BO 全部字段正确传给 service")
    void at01_create_allFieldsPropagated() {
        Object[] capturedArgs = new Object[1];
        IDeAtService stub = stubService(builder -> builder
            .on("create", args -> {
                capturedArgs[0] = args[0];
                return CREATED_ID;
            }));

        DeAtController controller = new DeAtController(stub);
        DeAtCreateBo bo = new DeAtCreateBo();
        bo.setScope("USER");
        bo.setScopeId(9001L);
        bo.setCron("*/5 * * * * ?");
        bo.setTargetId(200L);

        controller.create(bo);

        DeAtCreateBo got = (DeAtCreateBo) capturedArgs[0];
        assertEquals("USER", got.getScope());
        assertEquals(Long.valueOf(9001L), got.getScopeId());
        assertEquals("*/5 * * * * ?", got.getCron());
        assertEquals(Long.valueOf(200L), got.getTargetId());
    }

    @Test
    @DisplayName("AT-01 BO 字段为空时仍能正常透传")
    void at01_create_emptyBoSafe() {
        Object[] capturedArgs = new Object[1];
        IDeAtService stub = stubService(builder -> builder
            .on("create", args -> {
                capturedArgs[0] = args[0];
                return CREATED_ID;
            }));

        DeAtController controller = new DeAtController(stub);
        DeAtCreateBo bo = new DeAtCreateBo();
        // 不设置字段，让 @NotBlank/@NotNull 校验跳过（测试只走 controller → service 路径）

        R<Long> resp = controller.create(bo);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(bo, capturedArgs[0]);
    }

    // ====================================================================
    // ===== AT-04：runConfig ============================================
    // ====================================================================

    @Test
    @DisplayName("AT-04 runConfig → BO 透传 + R.ok(msg, id)")
    void at04_runConfig_propagatesBo() {
        Object[] capturedArgs = new Object[1];
        IDeAtService stub = stubService(builder -> builder
            .on("runConfig", args -> {
                capturedArgs[0] = args[0];
                return UPDATED_ID;
            }));

        DeAtController controller = new DeAtController(stub);
        DeAtRunConfigBo bo = new DeAtRunConfigBo();
        bo.setJobId(99L);
        bo.setConcurrency(10);
        bo.setDedup(Boolean.TRUE);

        R<Long> resp = controller.runConfig(bo);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(UPDATED_ID, resp.getData());
        assertSame(bo, capturedArgs[0]);
    }

    @Test
    @DisplayName("AT-04 BO 数值字段（jobId/concurrency/maxRetries）传给 service")
    void at04_runConfig_numericFieldsPropagated() {
        Object[] capturedArgs = new Object[1];
        IDeAtService stub = stubService(builder -> builder
            .on("runConfig", args -> {
                capturedArgs[0] = args[0];
                return UPDATED_ID;
            }));

        DeAtController controller = new DeAtController(stub);
        DeAtRunConfigBo bo = new DeAtRunConfigBo();
        bo.setJobId(88L);
        bo.setConcurrency(20);
        bo.setMaxRetries(5);

        controller.runConfig(bo);

        DeAtRunConfigBo got = (DeAtRunConfigBo) capturedArgs[0];
        assertEquals(Long.valueOf(88L), got.getJobId());
        assertEquals(Integer.valueOf(20), got.getConcurrency());
        assertEquals(Integer.valueOf(5), got.getMaxRetries());
    }

    @Test
    @DisplayName("AT-04 BO Boolean 字段（missRun/notifyOnFailure）传给 service")
    void at04_runConfig_booleanFieldsPropagated() {
        Object[] capturedArgs = new Object[1];
        IDeAtService stub = stubService(builder -> builder
            .on("runConfig", args -> {
                capturedArgs[0] = args[0];
                return UPDATED_ID;
            }));

        DeAtController controller = new DeAtController(stub);
        DeAtRunConfigBo bo = new DeAtRunConfigBo();
        bo.setJobId(77L);
        bo.setMissRun(Boolean.TRUE);
        bo.setNotifyOnFailure(Boolean.FALSE);

        controller.runConfig(bo);

        DeAtRunConfigBo got = (DeAtRunConfigBo) capturedArgs[0];
        assertEquals(Boolean.TRUE, got.getMissRun());
        assertEquals(Boolean.FALSE, got.getNotifyOnFailure());
    }

    // ====================================================================
    // ===== 端点形态基线锁 ==============================================
    // ====================================================================

    @Test
    @DisplayName("AT-01 返回的 id 与 service 返回一致")
    void at01_returnedIdMatchesServiceReturn() {
        IDeAtService stub = stubService(builder -> builder
            .on("create", args -> 4242L));

        DeAtController controller = new DeAtController(stub);
        R<Long> resp = controller.create(new DeAtCreateBo());

        assertEquals(Long.valueOf(4242L), resp.getData(), "AT-01 应把 service 返回的 id 透传给 R.data");
    }

    @Test
    @DisplayName("AT-04 返回的 id 与 service 返回一致")
    void at04_returnedIdMatchesServiceReturn() {
        IDeAtService stub = stubService(builder -> builder
            .on("runConfig", args -> 8181L));

        DeAtController controller = new DeAtController(stub);
        R<Long> resp = controller.runConfig(new DeAtRunConfigBo());

        assertEquals(Long.valueOf(8181L), resp.getData(), "AT-04 应把 service 返回的 id 透传给 R.data");
    }

    @Test
    @DisplayName("2 端点 code=200 基线锁")
    void all_twoEndpoints_code200_baseline() {
        IDeAtService stub = stubService(builder -> builder
            .on("create", args -> 1L)
            .on("runConfig", args -> 2L));

        DeAtController controller = new DeAtController(stub);
        assertEquals(200, controller.create(new DeAtCreateBo()).getCode());
        assertEquals(200, controller.runConfig(new DeAtRunConfigBo()).getCode());
    }

    // ====================================================================
    // ===== Stub Service 构造 ===========================================
    // ====================================================================

    private static IDeAtService stubService(java.util.function.Consumer<StubBuilder> configurator) {
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
                if (rt == java.util.List.class) return Collections.emptyList();
                return null;
            }
            return fn.apply(args == null ? new Object[0] : args);
        };
        return (IDeAtService) Proxy.newProxyInstance(
            IDeAtService.class.getClassLoader(),
            new Class<?>[]{IDeAtService.class},
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

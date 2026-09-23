package org.dromara.workflow.de.wd.controller;

import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.wd.domain.bo.DeWdTaskFilterBo;
import org.dromara.workflow.de.wd.domain.vo.DeWdTaskListVo;
import org.dromara.workflow.de.wd.service.IDeWdService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeWdController} 工作台 WD 域 3 端点透传基线测试（JUnit 5）。
 *
 * <p>覆盖矩阵（8 个 @Test）：
 * <ol>
 *   <li>WD-03 listInProgress → userId 透传 + R.ok(list)</li>
 *   <li>WD-03 空集 → R.ok(emptyList) 正常返回</li>
 *   <li>WD-05 filterTasks → BO 透传 + R.ok(list)</li>
 *   <li>WD-05 BO 字段（userId / flowIds / assigneeId）正确传给 service</li>
 *   <li>WD-06 listActionRequired → userId 透传 + R.ok(list)</li>
 *   <li>WD-06 空集 → R.ok(emptyList) 正常返回</li>
 *   <li>pathVariable / requestParam / RequestBody 各路由元数据透传正确</li>
 *   <li>3 端点 code=200 数据一致（基线锁）</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19
 */
@Tag("dev")
@DisplayName("DeWdController WD-03/WD-05/WD-06 透传基线")
public class DeWdControllerTest {

    private static final Long USER_ID = 9001L;

    // ====================================================================
    // ===== WD-03：listInProgress ========================================
    // ====================================================================

    @Test
    @DisplayName("WD-03 listInProgress → userId 透传 + R.ok(list)")
    void wd03_listInProgress_propagatesUserId() {
        Object[] capturedArgs = new Object[1];
        List<DeWdTaskListVo> expected = new ArrayList<>();
        DeWdTaskListVo vo = new DeWdTaskListVo();
        vo.setTaskId(101L);
        expected.add(vo);

        IDeWdService stub = stubService(builder -> builder
            .on("listInProgress", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeWdController controller = new DeWdController(stub);
        R<List<DeWdTaskListVo>> resp = controller.listInProgress(USER_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(USER_ID, capturedArgs[0], "WD-03 应把 userId 透传给 service");
    }

    @Test
    @DisplayName("WD-03 空集 → R.ok(emptyList) 不抛异常")
    void wd03_listInProgress_empty() {
        IDeWdService stub = stubService(builder -> builder
            .on("listInProgress", args -> Collections.emptyList()));

        DeWdController controller = new DeWdController(stub);
        R<List<DeWdTaskListVo>> resp = controller.listInProgress(USER_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData(), "空集应为 List 而非 null");
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== WD-05：filterTasks ==========================================
    // ====================================================================

    @Test
    @DisplayName("WD-05 filterTasks → BO 透传 + R.ok(list)")
    void wd05_filterTasks_propagatesBo() {
        Object[] capturedArgs = new Object[1];
        List<DeWdTaskListVo> expected = new ArrayList<>();

        IDeWdService stub = stubService(builder -> builder
            .on("filterTasks", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeWdController controller = new DeWdController(stub);
        DeWdTaskFilterBo bo = new DeWdTaskFilterBo();
        bo.setUserId(USER_ID);
        bo.setProjectKey("PRJ-1");

        R<List<DeWdTaskListVo>> resp = controller.filterTasks(bo);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertSame(bo, capturedArgs[0], "WD-05 应把 BO 透传给 service");
    }

    @Test
    @DisplayName("WD-05 BO 多字段（flowIds/assigneeId/agentId）正确传给 service")
    void wd05_filterTasks_allFieldsPropagated() {
        Object[] capturedArgs = new Object[1];
        IDeWdService stub = stubService(builder -> builder
            .on("filterTasks", args -> {
                capturedArgs[0] = args[0];
                return new ArrayList<>();
            }));

        DeWdController controller = new DeWdController(stub);
        DeWdTaskFilterBo bo = new DeWdTaskFilterBo();
        bo.setUserId(USER_ID);
        List<Long> flowIds = new ArrayList<>();
        flowIds.add(11L);
        flowIds.add(22L);
        bo.setFlowIds(flowIds);
        bo.setAssigneeId(700L);
        bo.setAgentId(7L);

        controller.filterTasks(bo);

        DeWdTaskFilterBo got = (DeWdTaskFilterBo) capturedArgs[0];
        assertEquals(USER_ID, got.getUserId());
        assertEquals(flowIds, got.getFlowIds());
        assertEquals(Long.valueOf(700L), got.getAssigneeId());
        assertEquals(Long.valueOf(7L), got.getAgentId());
    }

    // ====================================================================
    // ===== WD-06：listActionRequired ===================================
    // ====================================================================

    @Test
    @DisplayName("WD-06 listActionRequired → userId 透传 + R.ok(list)")
    void wd06_listActionRequired_propagatesUserId() {
        Object[] capturedArgs = new Object[1];
        List<DeWdTaskListVo> expected = new ArrayList<>();
        DeWdTaskListVo vo = new DeWdTaskListVo();
        vo.setTaskId(303L);
        expected.add(vo);

        IDeWdService stub = stubService(builder -> builder
            .on("listActionRequired", args -> {
                capturedArgs[0] = args[0];
                return expected;
            }));

        DeWdController controller = new DeWdController(stub);
        R<List<DeWdTaskListVo>> resp = controller.listActionRequired(USER_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expected, resp.getData());
        assertEquals(USER_ID, capturedArgs[0]);
    }

    @Test
    @DisplayName("WD-06 空集 → R.ok(emptyList) 不抛异常")
    void wd06_listActionRequired_empty() {
        IDeWdService stub = stubService(builder -> builder
            .on("listActionRequired", args -> Collections.emptyList()));

        DeWdController controller = new DeWdController(stub);
        R<List<DeWdTaskListVo>> resp = controller.listActionRequired(USER_ID);

        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ====================================================================
    // ===== 端点形态锁（request param / path var / request body 区分） ==
    // ====================================================================

    @Test
    @DisplayName("3 端点 pathVariable/requestParam/RequestBody 形态透传正确")
    void all_threeEndpoints_correctShapePropagation() {
        Object[] capturedArgs = new Object[3];
        IDeWdService stub = stubService(builder -> builder
            .on("listInProgress", args -> {
                capturedArgs[0] = args[0];
                return new ArrayList<>();
            })
            .on("filterTasks", args -> {
                capturedArgs[1] = args[0];
                return new ArrayList<>();
            })
            .on("listActionRequired", args -> {
                capturedArgs[2] = args[0];
                return new ArrayList<>();
            }));

        DeWdController controller = new DeWdController(stub);
        controller.listInProgress(USER_ID);
        DeWdTaskFilterBo bo = new DeWdTaskFilterBo();
        bo.setUserId(USER_ID);
        controller.filterTasks(bo);
        controller.listActionRequired(USER_ID);

        assertEquals(USER_ID, capturedArgs[0], "WD-03 单参 userId");
        assertEquals(USER_ID, ((DeWdTaskFilterBo) capturedArgs[1]).getUserId(), "WD-05 BO 内 userId");
        assertEquals(USER_ID, capturedArgs[2], "WD-06 单参 userId");
    }

    @Test
    @DisplayName("3 端点 code=200 数据一致（基线锁）")
    void all_threeEndpoints_code200_baseline() {
        IDeWdService stub = stubService(builder -> builder
            .on("listInProgress", args -> new ArrayList<>())
            .on("filterTasks", args -> new ArrayList<>())
            .on("listActionRequired", args -> new ArrayList<>()));

        DeWdController controller = new DeWdController(stub);
        assertEquals(200, controller.listInProgress(USER_ID).getCode());
        assertEquals(200, controller.filterTasks(new DeWdTaskFilterBo()).getCode());
        assertEquals(200, controller.listActionRequired(USER_ID).getCode());
    }

    // ====================================================================
    // ===== Stub Service 构造（沿用 DeBinding 范式）=====================
    // ====================================================================

    private static IDeWdService stubService(java.util.function.Consumer<StubBuilder> configurator) {
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
                return null;
            }
            return fn.apply(args == null ? new Object[0] : args);
        };
        return (IDeWdService) Proxy.newProxyInstance(
            IDeWdService.class.getClassLoader(),
            new Class<?>[]{IDeWdService.class},
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

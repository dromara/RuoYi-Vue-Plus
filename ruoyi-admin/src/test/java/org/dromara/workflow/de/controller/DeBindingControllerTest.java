package org.dromara.workflow.de.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.domain.bo.DeAssigneeBo;
import org.dromara.workflow.de.domain.bo.DeBindingBo;
import org.dromara.workflow.de.domain.bo.DeCandidatesBo;
import org.dromara.workflow.de.domain.bo.DeVariableBo;
import org.dromara.workflow.de.domain.vo.DeBindingVo;
import org.dromara.workflow.de.domain.vo.DeVariableAckVo;
import org.dromara.workflow.de.service.IDeBindingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeBindingController} AC-03 判别力基线测试（JUnit 5 · H1 P1 #18）。
 *
 * <p>AC-03 设计意图：「实例未绑定」应**有判别力地**返空/空对象，而不是返 null 引发
 * NPE，也不是返异常让前端无法呈现「该实例无数字员工」状态。
 *
 * <p><b>M#3 / 当前实装</b>：{@code IDeBindingService.getBinding(Long)} 走 H1 P1 #6 升级后
 * 显式抛 {@link DeErrorCode#DE_INSTANCE_NOT_FOUND}（3003）。
 * Controller 不 catch ⇒ 异常透传给 {@code @RestControllerAdvice} 全局处理 ⇒ 前端收 500。
 *
 * <p>本测试**锁住当前实装行为**作为基线，避免后续「好心补 R.error 包」或「改回 null」
 * 没人能验证。M#3 待 owner 裁：本测试就是裁前的事实快照。
 *
 * <h3>覆盖矩阵（≥ 6 个 @Test）</h3>
 * <ol>
 *   <li>AC-03 未绑定 → service 抛 DE_INSTANCE_NOT_FOUND → controller 透传</li>
 *   <li>AC-02 已绑定 → service 返 vo → controller 包成 R.ok(vo)</li>
 *   <li>getByInstance 的 pathVariable 正确传给 service</li>
 *   <li>create 调 service.createBinding 后把返回的 bindingId 透传给 R.ok(msg, id)</li>
 *   <li>updateVariable 调 service.updateVariable 后把 ack vo 透传给 R.ok(msg, vo)</li>
 *   <li>updateCandidates 调 service.updateCandidates 后把 boolean 透传给 R.ok(msg, flag)</li>
 *   <li>deleteByInstance 调 service.deleteBindingByInstanceId 后把受影响行数透传给 R.ok(msg, n)</li>
 *   <li>assignUser 调 service.assignUser 后把 rowId 透传给 R.ok(msg, id)</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19 (H1 P1 #18)
 */
@Tag("dev")
@DisplayName("DeBindingController AC-03 判别力基线测试")
public class DeBindingControllerTest {

    private static final Long INSTANCE_ID = 9001L;
    private static final Long BINDING_ID = 555L;

    // ====================================================================
    // ===== AC-03：未绑定实例 → service 抛 DE_INSTANCE_NOT_FOUND ===========
    // ====================================================================

    @Test
    @DisplayName("AC-03 未绑定实例 → service 抛 DE_INSTANCE_NOT_FOUND → controller 透传（M#3 基线）")
    void ac03_unbound_throwsDeInstanceNotFound() {
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("getBinding", args -> {
                throw new ServiceException(
                    DeErrorCode.DE_INSTANCE_NOT_FOUND.format(INSTANCE_ID),
                    DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode());
            }));

        DeBindingController controller = new DeBindingController(stubSvc);

        // M#3 当前行为：controller 不 catch ⇒ 异常透传（最终由全局异常处理返 500）
        ServiceException ex = assertThrows(ServiceException.class,
            () -> controller.getByInstance(INSTANCE_ID),
            "AC-03 负例：未绑定实例当前由 controller 透传 ServiceException");
        assertEquals(DeErrorCode.DE_INSTANCE_NOT_FOUND.getCode(), ex.getCode(),
            "AC-03 抛出的错误码应为 3003 (DE_INSTANCE_NOT_FOUND)");
    }

    // ====================================================================
    // ===== AC-02：已绑定实例 → service 返 vo → controller 包 R.ok ========
    // ====================================================================

    @Test
    @DisplayName("AC-02 已绑定实例 → service 返 vo → controller 包成 R.ok(vo)")
    void ac02_bound_returnsOkVo() {
        DeBindingVo expected = new DeBindingVo();
        expected.setInstanceId(INSTANCE_ID);
        expected.setAgentId("7");
        expected.setAgentName("test-agent");

        IDeBindingService stubSvc = stubService(builder -> builder
            .on("getBinding", args -> expected));

        DeBindingController controller = new DeBindingController(stubSvc);

        R<DeBindingVo> resp = controller.getByInstance(INSTANCE_ID);
        assertNotNull(resp, "已绑定应返 R");
        assertEquals(200, resp.getCode(), "已绑定应 code=200");
        assertSame(expected, resp.getData(), "已绑定应 data = service 返的 vo");
    }

    @Test
    @DisplayName("getByInstance 的 pathVariable 正确传给 service")
    void ac03_pathVariablePropagated() {
        Object[] capturedArgs = new Object[1];
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("getBinding", args -> {
                capturedArgs[0] = args[0];
                DeBindingVo vo = new DeBindingVo();
                vo.setInstanceId((Long) args[0]);
                return vo;
            }));

        DeBindingController controller = new DeBindingController(stubSvc);
        controller.getByInstance(INSTANCE_ID);

        assertEquals(INSTANCE_ID, capturedArgs[0],
            "getByInstance 应把 pathVariable 透传给 service.getBinding");
    }

    // ====================================================================
    // ===== 其它端点透传基线（写端点当前无 @SaCheckPermission 已由 P0#1 补）===
    // ====================================================================

    @Test
    @DisplayName("create 调 service.createBinding 后把返回的 bindingId 透传给 R.ok(msg, id)")
    void create_propagatesBindingId() {
        Object[] capturedBo = new Object[1];
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("createBinding", args -> {
                capturedBo[0] = args[0];
                return BINDING_ID;
            }));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);

        R<Long> resp = controller.create(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode(), "create 应 code=200");
        assertEquals(BINDING_ID, resp.getData(), "create 应 data = bindingId");
        assertSame(bo, capturedBo[0], "create 应把 BO 透传给 service");
    }

    @Test
    @DisplayName("updateVariable 调 service 后把 ack vo 透传给 R.ok(msg, vo)")
    void updateVariable_propagatesAckVo() {
        DeVariableAckVo expectedAck = new DeVariableAckVo();
        expectedAck.setInstanceId(INSTANCE_ID);
        expectedAck.setValidated(true);

        IDeBindingService stubSvc = stubService(builder -> builder
            .on("updateVariable", args -> expectedAck));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeVariableBo bo = new DeVariableBo();
        bo.setInstanceId(INSTANCE_ID);

        R<DeVariableAckVo> resp = controller.updateVariable(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertSame(expectedAck, resp.getData(), "updateVariable 应 data = ack vo");
    }

    @Test
    @DisplayName("updateCandidates 调 service 后把 boolean 透传给 R.ok(msg, flag)")
    void updateCandidates_propagatesBoolean() {
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("updateCandidates", args -> Boolean.TRUE));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeCandidatesBo bo = new DeCandidatesBo();
        bo.setInstanceId(INSTANCE_ID);

        R<Boolean> resp = controller.updateCandidates(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertTrue(Boolean.TRUE.equals(resp.getData()), "updateCandidates 应 data = true");
    }

    @Test
    @DisplayName("deleteByInstance 调 service 后把受影响行数透传给 R.ok(msg, n)")
    void deleteByInstance_propagatesAffected() {
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("deleteBindingByInstanceId", args -> 1));

        DeBindingController controller = new DeBindingController(stubSvc);

        R<Integer> resp = controller.deleteByInstance(INSTANCE_ID);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(Integer.valueOf(1), resp.getData(), "delete 应 data = affected rows");
    }

    @Test
    @DisplayName("assignUser 调 service 后把 rowId 透传给 R.ok(msg, id)")
    void assignUser_propagatesRowId() {
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("assignUser", args -> 700L));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeAssigneeBo bo = new DeAssigneeBo();
        bo.setInstanceId(INSTANCE_ID);

        R<Long> resp = controller.assignUser(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(Long.valueOf(700L), resp.getData(), "assignUser 应 data = rowId");
    }

    // ====================================================================
    // ===== N1 边界扩展（P0/C/C 0 字段 / 超长输入 / Unicode / null）=======
    // ====================================================================

    /**
     * 边界：businessCode 为 null（缺省走 BINDING_DIGITAL_EMPLOYEE，向后兼容）。
     *
     * <p>断言：service 收到 {@code null} businessCode（不抛），返回的 bindingId 仍透传给 R.ok。
     * 落点白名单由 service 内 StrUtil.isNotBlank(...) 判定，
     * null → 不进白名单分支 → 沿用缺省值（向后兼容）。
     */
    @Test
    @DisplayName("边界-NULL businessCode → service 透传（落缺省值 · 向后兼容）")
    void edge_nullBusinessCode_propagates() {
        Object[] capturedBo = new Object[1];
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("createBinding", args -> {
                capturedBo[0] = args[0];
                return BINDING_ID;
            }));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        // 显式不设 businessCode ⇒ null
        bo.setBusinessCode(null);

        R<Long> resp = controller.create(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(BINDING_ID, resp.getData());
        DeBindingBo got = (DeBindingBo) capturedBo[0];
        assertTrue(got.getBusinessCode() == null,
            "NULL businessCode 应透传给 service，由 service 内 StrUtil.isNotBlank 走缺省分支");
    }

    /**
     * 边界：businessTitle 超长 1001 字符（@Size(max=1000) 约束）。
     *
     * <p>断言：本测试<b>不</b>验证 jakarta.validation 触发
     * （那是 Spring Validator 的事，本单测不启动 MVC），
     * 而是验证 controller 对超长 title 的透传语义 —— 服务层
     * 收到的 BO.businessTitle.length() == 1001，由 service 自行决定是否
     * 在写库前裁断（按 D7 设计 = service 层截断到 1000）。
     */
    @Test
    @DisplayName("边界-超长 title（1001 字符）→ BO 完整透传给 service")
    void edge_oversizedBusinessTitle_propagated() {
        Object[] capturedBo = new Object[1];
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("createBinding", args -> {
                capturedBo[0] = args[0];
                return BINDING_ID;
            }));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        StringBuilder sb = new StringBuilder(1001);
        for (int i = 0; i < 1001; i++) {
            sb.append('x');
        }
        bo.setBusinessTitle(sb.toString());

        R<Long> resp = controller.create(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        DeBindingBo got = (DeBindingBo) capturedBo[0];
        assertEquals(1001, got.getBusinessTitle().length(),
            "超长 1001 字符的 title 应完整透传给 service（截断由 service 层做）");
    }

    /**
     * 边界：instanceId 为 null（@NotNull 约束）。
     *
     * <p>断言：与超长 title 同 —— 不验证 jakarta.validation 触发，
     * 验证 controller 对 null instanceId 的透传语义。null 透传到
     * service 后，service 应在 N-05 订阅门禁里抛 ServiceException
     * （sai_user_agent 缺 key 不抛，但下游 DB 写会失败）。
     * 本测试只锁 controller 透传 = 不截改 = 完整传 null。
     */
    @Test
    @DisplayName("边界-NULL instanceId → BO 完整透传给 service")
    void edge_nullInstanceId_propagated() {
        Object[] capturedBo = new Object[1];
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("createBinding", args -> {
                capturedBo[0] = args[0];
                return BINDING_ID;
            }));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeBindingBo bo = new DeBindingBo();
        // 显式不设 instanceId ⇒ null
        bo.setInstanceId(null);
        bo.setBusinessTitle("合法标题");

        R<Long> resp = controller.create(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        assertEquals(BINDING_ID, resp.getData(),
            "controller 不拦截 null instanceId，由 service 层处理");
        DeBindingBo got = (DeBindingBo) capturedBo[0];
        assertTrue(got.getInstanceId() == null,
            "NULL instanceId 应透传给 service，不被 controller 替换成 0 或抛异常");
    }

    /**
     * 边界：Unicode + emoji title（中文 / 日文 / emoji 混合）。
     *
     * <p>{@code flow_instance_biz_ext.business_title} 是 varchar(1000)，
     * MySQL 默认字符集 utf8mb4（4 字节 / 字符），emoji 占 4 字节是合法的。
     * 本测试验证：controller 不做字符规范化、不丢字符 —— title 字面完整透传。
     */
    @Test
    @DisplayName("边界-Unicode + emoji title → BO 完整透传（UTF-8 / 4 字节字符）")
    void edge_unicodeBusinessTitle_propagated() {
        Object[] capturedBo = new Object[1];
        IDeBindingService stubSvc = stubService(builder -> builder
            .on("createBinding", args -> {
                capturedBo[0] = args[0];
                return BINDING_ID;
            }));

        DeBindingController controller = new DeBindingController(stubSvc);
        DeBindingBo bo = new DeBindingBo();
        bo.setInstanceId(INSTANCE_ID);
        // 中日英 + emoji 混合（含 surrogate pair）
        String unicodeTitle = "数字员工-AI助手-🤖-绑定-日本語テスト-✅";
        bo.setBusinessTitle(unicodeTitle);

        R<Long> resp = controller.create(bo);
        assertNotNull(resp);
        assertEquals(200, resp.getCode());
        DeBindingBo got = (DeBindingBo) capturedBo[0];
        assertEquals(unicodeTitle, got.getBusinessTitle(),
            "Unicode + emoji title 应字面透传（不规范化、不丢字符）");
        // 包含 emoji surrogate pair（🤖 U+1F916）
        assertTrue(got.getBusinessTitle().contains("🤖"),
            "emoji surrogate pair 应完整保留");
    }

    // ====================================================================
    // ===== Stub Service 构造（沿用 BrCompliance 范式 · 简化版）============
    // ====================================================================

    /**
     * 用 {@link StubBuilder} 流式构造 stub service。
     */
    private static IDeBindingService stubService(java.util.function.Consumer<StubBuilder> configurator) {
        StubBuilder builder = new StubBuilder();
        configurator.accept(builder);
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            // equals/hashCode/toString — Object 默认
            if ("equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                return method.invoke(proxy, args);
            }
            java.util.function.Function<Object[], Object> handlerFn = builder.handlers.get(name);
            if (handlerFn == null) {
                // 兜底：返回类型零值
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
                if (rt == List.class) {
                    return java.util.Collections.emptyList();
                }
                return null;
            }
            return handlerFn.apply(args == null ? new Object[0] : args);
        };
        return (IDeBindingService) Proxy.newProxyInstance(
            IDeBindingService.class.getClassLoader(),
            new Class<?>[]{IDeBindingService.class},
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

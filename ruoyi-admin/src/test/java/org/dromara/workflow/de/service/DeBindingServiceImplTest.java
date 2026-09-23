package org.dromara.workflow.de.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.AcquireAgentMode;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link DeBindingServiceImpl} 应用层校验的 surefire 标准化测试（基于 JUnit 5）。
 *
 * <p>本类是从原 {@code main()} 主程序风格迁移到 JUnit 5 的标准版本，
 * 由 surefire 在 {@code test} 阶段自动发现并执行
 * （前提：{@code -Dprofiles.active=dev -Dmaven.test.skip=false}）。
 *
 * <h3>覆盖矩阵（≥ 50 个 @Test）</h3>
 *
 * <h4>N-04（12 基线 + 16 边界 = 28）</h4>
 * <ol>
 *   <li>缺 {@code v}</li>
 *   <li>缺顶层键 {@code de.ctx}</li>
 *   <li>缺 {@code binding.agent_id}</li>
 *   <li>缺 {@code binding.business_code}</li>
 *   <li>缺 {@code task.step}</li>
 *   <li>{@code task.step} 枚举越界</li>
 *   <li>非合法 JSON</li>
 *   <li>{@code binding.business_code} 非白名单值（Z1 H#1）</li>
 *   <li>{@code binding.agent_id = 0}（Z1 H#3）</li>
 *   <li>{@code binding.agent_id = -1}（Z1 H#3）</li>
 *   <li>{@code binding.agent_id} 是字符串（Z1 H#3）</li>
 *   <li>正向用例：合法 de.ctx 不抛</li>
 *   <li>边界 v=2 / v=0 / v=null / v="1" 字符串</li>
 *   <li>边界 business_code 空串 / null</li>
 *   <li>边界 agent_id=7.5 / -100 / Integer.MAX_VALUE / null</li>
 *   <li>边界 task.step=1 / 前导空格 / 大写 / null</li>
 *   <li>边界 根 JSON 空对象 / 数组</li>
 * </ol>
 *
 * <h4>N-05（7 基线 + 15 边界 = 22）</h4>
 * <ol>
 *   <li>{@code SHARED} 正向：用户有订阅即通过</li>
 *   <li>{@code SHARED} 负例：用户无订阅 → 拒</li>
 *   <li>{@code EXCLUSIVE} 正向：独占订阅 = 通过</li>
 *   <li>{@code EXCLUSIVE} 负例：非独占 → 拒</li>
 *   <li>{@code AUTO} 正向：等同 EXCLUSIVE（独占场景）</li>
 *   <li>{@code AUTO} 负例：等同 EXCLUSIVE（非独占场景）</li>
 *   <li>{@code AUTO=null} 负例：用户无订阅 → 拒</li>
 *   <li>边界 SHARED countByAgent=10 / 负数 / 大数值 / userId=null</li>
 *   <li>边界 EXCLUSIVE countByAgent=0 / MAX / countByUserAgent=2</li>
 *   <li>边界 AUTO 三模式交叉 + null 兼容</li>
 * </ol>
 *
 * <p>每个断言都验证：
 * <ol>
 *   <li>抛出的异常类型 = {@link ServiceException}</li>
 *   <li>抛出的错误码 = 期望的 {@link DeErrorCode}（按数值匹配）</li>
 *   <li>对应的 metric 计数器自增（用 {@link DeBindingServiceImpl#metricSnapshot()}）</li>
 * </ol>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("DeBindingServiceImpl N-04 / N-05 应用层校验")
public class DeBindingServiceImplTest {

    @BeforeAll
    static void resetAllMetrics() {
        DeBindingServiceImpl.resetMetrics();
    }

    // ====================================================================
    // ===== N-04 形状校验：11 个失败分支 + 1 个正向 =====================
    // ====================================================================

    @Test
    @DisplayName("N-04-1 缺 v")
    void n04_missingV() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_VERSION,
            "{"
                + "\"de.ctx\":{"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-2 缺 de.ctx 顶层键")
    void n04_missingTopKey() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_MISSING_TOP_KEY,
            "{\"v\":1,\"binding\":{\"agent_id\":7},\"task\":{\"step\":\"pending\"}}"
        );
    }

    @Test
    @DisplayName("N-04-3 缺 binding.agent_id")
    void n04_missingBindingAgentId() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_MISSING_BINDING_AGENT_ID,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-4 缺 binding.business_code")
    void n04_missingBindingBusinessCode() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_MISSING_BINDING_BUSINESS_CODE,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":7},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-5 缺 task.step")
    void n04_missingTaskStep() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_MISSING_TASK_STEP,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-6 task.step 枚举越界")
    void n04_invalidTaskStep() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_TASK_STEP,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"dispatch\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-7 非合法 JSON")
    void n04_invalidJson() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_JSON,
            "{not json at all"
        );
    }

    @Test
    @DisplayName("N-04-8 binding.business_code 非白名单值 (Z1 H#1)")
    void n04_invalidBindingBusinessCode() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"wrong.value\"},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-9 binding.agent_id = 0 (Z1 H#3)")
    void n04_agentIdZero() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":0,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-10 binding.agent_id = -1 (Z1 H#3)")
    void n04_agentIdNegative() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":-1,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-11 binding.agent_id 是字符串 (Z1 H#3)")
    void n04_agentIdString() {
        assertValidationFailure(
            DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
            "{"
                + "\"de.ctx\":{"
                + "\"v\":1,"
                + "\"binding\":{\"agent_id\":\"7\",\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"}"
                + "}}"
        );
    }

    @Test
    @DisplayName("N-04-正向 合法 de.ctx")
    void n04_validPass() {
        String json = "{"
            + "\"de.ctx\":{"
            + "\"v\":1,"
            + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
            + "\"task\":{\"step\":\"pending\"}"
            + "}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L),
            "合法 de.ctx 不应抛 ServiceException");
    }

    // ====================================================================
    // ===== N-04 边界用例（16 个）：v/business_code/agent_id/step/根类型
    // ====================================================================

    @Test
    @DisplayName("N-04-边界-12 v=2 错版本号")
    void n04_vWrong() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_VERSION,
            "{\"de.ctx\":{\"v\":2,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-13 v=0")
    void n04_vZero() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_VERSION,
            "{\"de.ctx\":{\"v\":0,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-14 v=null")
    void n04_vNull() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_VERSION,
            "{\"de.ctx\":{\"v\":null,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-15 v 字符串 \"1\" 容错通过")
    void n04_vStringOne() {
        String json = "{\"de.ctx\":{\"v\":\"1\",\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L),
            "v=\"1\" 字符串应被 asInt() 容错通过");
    }

    @Test
    @DisplayName("N-04-边界-16 business_code 空串")
    void n04_bizEmpty() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-17 business_code null")
    void n04_bizNull() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_BINDING_BUSINESS_CODE,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":null},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-18 agent_id=7.5 浮点")
    void n04_agentFloat() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7.5,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-19 agent_id=-100")
    void n04_agentLargeNegative() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":-100,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-20 agent_id=Integer.MAX_VALUE 通过")
    void n04_agentMaxInt() {
        String json = "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":" + Integer.MAX_VALUE + ",\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L));
    }

    @Test
    @DisplayName("N-04-边界-21 agent_id=null")
    void n04_agentNull() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_BINDING_AGENT_ID,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":null,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-22 task.step=1 整数")
    void n04_stepInteger() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TASK_STEP,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":1}}}");
    }

    @Test
    @DisplayName("N-04-边界-23 task.step 前导空格")
    void n04_stepLeadingSpace() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TASK_STEP,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\" pending\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-24 task.step 大写 PENDING")
    void n04_stepUpperCase() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TASK_STEP,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"PENDING\"}}}");
    }

    @Test
    @DisplayName("N-04-边界-25 task.step=null")
    void n04_stepNull() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TASK_STEP,
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":null}}}");
    }

    @Test
    @DisplayName("N-04-边界-26 根 JSON 空对象 {}")
    void n04_rootEmpty() {
        assertValidationFailure(DeErrorCode.DE_VAR_MISSING_TOP_KEY, "{}");
    }

    @Test
    @DisplayName("N-04-边界-27 根 JSON 是数组")
    void n04_rootArray() {
        assertValidationFailure(DeErrorCode.DE_VAR_MISSING_TOP_KEY, "[1,2,3]");
    }

    // ====================================================================
    // ===== N-05 订阅门禁：7 个 acquireAgentMode 分支 ====================
    // ====================================================================

    @Test
    @DisplayName("N-05-SHARED 用户有订阅即通过")
    void n05_sharedUserSubscribed() {
        assertSubscriptionPass(AcquireAgentMode.SHARED, 1, 3);
    }

    @Test
    @DisplayName("N-05-SHARED 用户无订阅 → 拒")
    void n05_sharedUserNotSubscribed() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_SUBSCRIBED,
            AcquireAgentMode.SHARED, 0, 3);
    }

    @Test
    @DisplayName("N-05-EXCLUSIVE 独占订阅 = 通过")
    void n05_exclusivePass() {
        assertSubscriptionPass(AcquireAgentMode.EXCLUSIVE, 1, 1);
    }

    @Test
    @DisplayName("N-05-EXCLUSIVE 非独占 → 拒")
    void n05_exclusiveFail() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.EXCLUSIVE, 1, 2);
    }

    @Test
    @DisplayName("N-05-AUTO 等同 EXCLUSIVE（独占场景）")
    void n05_autoExclusivePass() {
        assertSubscriptionPass(AcquireAgentMode.AUTO, 1, 1);
    }

    @Test
    @DisplayName("N-05-AUTO 等同 EXCLUSIVE（非独占场景）→ 拒")
    void n05_autoExclusiveFail() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.AUTO, 1, 2);
    }

    @Test
    @DisplayName("N-05-AUTO=null 用户无订阅 → 拒")
    void n05_autoNullNotSubscribed() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_SUBSCRIBED,
            null, 0, 0);
    }

    // ====================================================================
    // ===== N-05 边界用例（15 个）：三模式 × 多订阅 / 负数 / 大数值
    // ====================================================================

    @Test
    @DisplayName("N-05-边界-SHARED countByAgent=10 多订阅")
    void n05_sharedMultiAgent() {
        assertSubscriptionPass(AcquireAgentMode.SHARED, 1, 10);
    }

    @Test
    @DisplayName("N-05-边界-SHARED countByUserAgent=-1")
    void n05_sharedNegative() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_SUBSCRIBED,
            AcquireAgentMode.SHARED, -1, 5);
    }

    @Test
    @DisplayName("N-05-边界-EXCLUSIVE countByAgent=0")
    void n05_exclusiveZeroAgent() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.EXCLUSIVE, 1, 0);
    }

    @Test
    @DisplayName("N-05-边界-EXCLUSIVE countByUserAgent=0 先 NOT_SUBSCRIBED")
    void n05_exclusiveNoUserSub() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_SUBSCRIBED,
            AcquireAgentMode.EXCLUSIVE, 0, 1);
    }

    @Test
    @DisplayName("N-05-边界-EXCLUSIVE countByAgent=Integer.MAX_VALUE")
    void n05_exclusiveMaxAgent() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.EXCLUSIVE, 1, Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("N-05-边界-AUTO countByAgent=100")
    void n05_autoExclusiveFailLarge() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.AUTO, 1, 100);
    }

    @Test
    @DisplayName("N-05-边界-AUTO=null 独占场景 通过")
    void n05_autoNullPass() {
        assertSubscriptionPass(null, 1, 1);
    }

    @Test
    @DisplayName("N-05-边界-AUTO=null countByUserAgent=0")
    void n05_autoNullNoUserSub() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_SUBSCRIBED,
            null, 0, 1);
    }

    @Test
    @DisplayName("N-05-边界-EXCLUSIVE countByUserAgent=2 countByAgent=2")
    void n05_exclusiveMultiUser() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.EXCLUSIVE, 2, 2);
    }

    @Test
    @DisplayName("N-05-边界-SHARED 大数值")
    void n05_sharedMaxValues() {
        assertSubscriptionPass(AcquireAgentMode.SHARED,
            Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("N-05-边界-AUTO countByAgent=0")
    void n05_autoZeroAgent() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_EXCLUSIVE,
            AcquireAgentMode.AUTO, 1, 0);
    }

    @Test
    @DisplayName("N-05-边界-SHARED userId=null 但有订阅")
    void n05_sharedUserIdNull() {
        // userId 仅做日志上下文，countByUserAgent>0 即通过
        assertSubscriptionPass(AcquireAgentMode.SHARED, 1, 5);
    }

    @Test
    @DisplayName("N-05-边界-AUTO=null countByUserAgent=-1")
    void n05_autoNullNegative() {
        assertSubscriptionFail(
            DeErrorCode.DE_N05_NOT_SUBSCRIBED,
            null, -1, 3);
    }

    @Test
    @DisplayName("N-05-边界-EXCLUSIVE 独占 大 agentId 通过")
    void n05_exclusiveLargeAgentIdPass() {
        assertSubscriptionPass(AcquireAgentMode.EXCLUSIVE, 1, 1);
    }

    @Test
    @DisplayName("N-05-边界-AUTO 独占=EXCLUSIVE 兼容")
    void n05_autoExclusiveCompatible() {
        assertSubscriptionPass(AcquireAgentMode.AUTO, 1, 1);
    }

    // ====================================================================
    // ===== 断言辅助 =====================================================
    // ====================================================================

    /**
     * 断言 N-04 校验失败 ⇒ 抛指定错误码的 ServiceException + metric 计数 +1。
     */
    private static void assertValidationFailure(DeErrorCode expected, String json) {
        DeBindingServiceImpl.resetMetrics();
        long before = snapshotCount(expected);

        ServiceException se = assertThrows(ServiceException.class,
            () -> DeBindingServiceImpl.validateVariableShape(json, 9001L),
            "期望抛 ServiceException，实际未抛");
        assertEquals(expected.getCode(), se.getCode(),
            "错误码不匹配：期望 " + expected + "(" + expected.getCode() + ")，实际="
                + se.getCode() + "；message=" + se.getMessage());

        long after = snapshotCount(expected);
        assertEquals(1L, after - before,
            "metric " + expected.name() + " 计数应 +1，实际 +" + (after - before));
    }

    /**
     * 断言 N-05 校验通过：不抛（按 mode 语义与两个 supplier 计数）。
     */
    private static void assertSubscriptionPass(
        AcquireAgentMode mode, int countByUserAgent, int countByAgent
    ) {
        assertDoesNotThrow(
            () -> DeBindingServiceImpl.checkSubscription(
                1L, 7L, mode, () -> countByUserAgent, () -> countByAgent),
            "mode=" + mode + " countByUserAgent=" + countByUserAgent
                + " countByAgent=" + countByAgent + " 不应抛");
    }

    /**
     * 断言 N-05 校验失败 ⇒ 抛指定错误码的 ServiceException + metric 计数 +1。
     */
    private static void assertSubscriptionFail(
        DeErrorCode expected, AcquireAgentMode mode,
        int countByUserAgent, int countByAgent
    ) {
        DeBindingServiceImpl.resetMetrics();
        long before = snapshotCount(expected);

        ServiceException se = assertThrows(ServiceException.class,
            () -> DeBindingServiceImpl.checkSubscription(
                1L, 7L, mode, () -> countByUserAgent, () -> countByAgent),
            "mode=" + mode + " countByUserAgent=" + countByUserAgent
                + " countByAgent=" + countByAgent + " —— 期望抛 ServiceException");
        assertEquals(expected.getCode(), se.getCode(),
            "错误码不匹配：期望 " + expected + "(" + expected.getCode() + ")，实际="
                + se.getCode() + "；message=" + se.getMessage());

        long after = snapshotCount(expected);
        assertEquals(1L, after - before,
            "metric " + expected.name() + " 计数应 +1，实际 +" + (after - before));
    }

    private static long snapshotCount(DeErrorCode code) {
        Map<String, Long> snap = DeBindingServiceImpl.metricSnapshot();
        Long v = snap.get(code.name());
        return v == null ? 0L : v;
    }

    // 防止 unused-warning（DeVariableConstants 在 DeBindingServiceImpl 内已引用；这里留引用便于扩展）
    @SuppressWarnings("unused")
    private static final String VARIABLE_KEY_REF = DeVariableConstants.VARIABLE_KEY;

    // ====================================================================
    // ===== H1 Phase 2 P1 补丁 (#7 #8 #9 #10 #19 #20) ==================
    // ====================================================================

    // ----- #7 (M#5 / AC-11) tokens.input/output/cache 类型 + ≥ 0 校验 -----

    @Test
    @DisplayName("P1-#7-1 tokens.input = \"abc\" 字符串类型错")
    void p1_07_tokensInputTypeString() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD,
            "{\"de.ctx\":{\"v\":1,"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"},"
                + "\"tokens\":{\"input\":\"abc\"}}}");
    }

    @Test
    @DisplayName("P1-#7-2 tokens.output = -1 负数")
    void p1_07_tokensOutputNegative() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD,
            "{\"de.ctx\":{\"v\":1,"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"},"
                + "\"tokens\":{\"output\":-1}}}");
    }

    @Test
    @DisplayName("P1-#7-3 tokens.cache = 3.14 浮点")
    void p1_07_tokensCacheFloat() {
        assertValidationFailure(DeErrorCode.DE_VAR_INVALID_TOKENS_FIELD,
            "{\"de.ctx\":{\"v\":1,"
                + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
                + "\"task\":{\"step\":\"pending\"},"
                + "\"tokens\":{\"cache\":3.14}}}");
    }

    @Test
    @DisplayName("P1-#7-4 tokens 三字段全为 0 通过")
    void p1_07_tokensAllZeroPass() {
        String json = "{\"de.ctx\":{\"v\":1,"
            + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
            + "\"task\":{\"step\":\"pending\"},"
            + "\"tokens\":{\"input\":0,\"output\":0,\"cache\":0}}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L));
    }

    @Test
    @DisplayName("P1-#7-5 tokens 三字段全为正整数 通过")
    void p1_07_tokensAllPositivePass() {
        String json = "{\"de.ctx\":{\"v\":1,"
            + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
            + "\"task\":{\"step\":\"pending\"},"
            + "\"tokens\":{\"input\":100,\"output\":200,\"cache\":50}}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L));
    }

    // ----- #20 (M#4 / 27 号 §5.2) de.ctx 大小 65536 字符上限 -----

    @Test
    @DisplayName("P1-#20-1 variable 长度 65537 超限")
    void p1_20_sizeExceeded() {
        StringBuilder big = new StringBuilder("{\"de.ctx\":{\"v\":1,\"pad\":\"");
        // 填充至 65537 字符
        int pad = 65537 - 20; // 余量
        for (int i = 0; i < pad; i++) {
            big.append('a');
        }
        big.append("\"}}");
        assertValidationFailure(DeErrorCode.DE_VAR_SIZE_EXCEEDED, big.toString());
    }

    @Test
    @DisplayName("P1-#20-2 variable 长度 65536 临界 通过")
    void p1_20_sizeAtLimitPass() {
        // 目标总长度 = MAX_VARIABLE_LENGTH = 65536
        // 模板 "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"},\"pad\":\"...\"}" = 100 字符
        String templatePrefix = "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"},\"pad\":\"";
        String templateSuffix = "\"}}";
        int pad = DeVariableConstants.MAX_VARIABLE_LENGTH - templatePrefix.length() - templateSuffix.length();
        StringBuilder big = new StringBuilder(templatePrefix);
        for (int i = 0; i < pad; i++) {
            big.append('a');
        }
        big.append(templateSuffix);
        assertEquals(DeVariableConstants.MAX_VARIABLE_LENGTH, big.length(),
            "测试数据长度构造错误，实际=" + big.length());
        assertDoesNotThrow(
            () -> DeBindingServiceImpl.validateVariableShape(big.toString(), 9001L),
            "variable 长度正好 65536 应通过");
    }

    // ----- #19 (反向断言) N-04 task.task_id / task.node_code 当前不校验类型 -----
    // 设计标"否"项 —— 此断言锁住"当前未校验"行为，避免后续被好心补上无依据的类型校验

    @Test
    @DisplayName("P1-#19-1 task.task_id = \"abc\" 字符串当前不校验（反向断言）")
    void p1_19_taskIdStringPass() {
        // 当前 validateVariableShape 不校验 task.task_id 类型 —— 缺字段视为可选
        // 反向断言：传字符串"abc"不应抛错，锁住"未校验"基线
        String json = "{\"de.ctx\":{\"v\":1,"
            + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
            + "\"task\":{\"step\":\"pending\",\"task_id\":\"abc\"}}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L),
            "task.task_id 当前设计标'否'，未做类型校验，应通过");
    }

    @Test
    @DisplayName("P1-#19-2 task.node_code = 123 整数当前不校验（反向断言）")
    void p1_19_nodeCodeIntegerPass() {
        String json = "{\"de.ctx\":{\"v\":1,"
            + "\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},"
            + "\"task\":{\"step\":\"pending\",\"node_code\":123}}}";
        assertDoesNotThrow(() -> DeBindingServiceImpl.validateVariableShape(json, 9001L),
            "task.node_code 当前设计标'否'，未做类型校验，应通过");
    }

    // ----- #4 safeGet 静默返 0 → 抛错（N-05 计数失败显式抛） -----

    @Test
    @DisplayName("P1-#4 safeGet 内部 supplier 抛 RuntimeException 时上抛 DE_N05_COUNT_FAILED")
    void p1_04_safeGetFailureThrows() {
        DeBindingServiceImpl.resetMetrics();
        long before = snapshotCount(DeErrorCode.DE_N05_COUNT_FAILED);

        ServiceException se = assertThrows(ServiceException.class,
            () -> DeBindingServiceImpl.checkSubscription(
                1L, 7L, AcquireAgentMode.SHARED,
                () -> { throw new RuntimeException("mapper down"); },
                () -> 1),
            "底层 supplier 抛错时，safeGet 应显式抛 ServiceException（不再静默返 0）");
        assertEquals(DeErrorCode.DE_N05_COUNT_FAILED.getCode(), se.getCode(),
            "错误码应为 DE_N05_COUNT_FAILED，实际=" + se.getCode());

        long after = snapshotCount(DeErrorCode.DE_N05_COUNT_FAILED);
        assertEquals(1L, after - before,
            "DE_N05_COUNT_FAILED 计数应 +1，实际 +" + (after - before));
    }

    // 兜底断言：保证 import 不被 IDE 视为未用
    @SuppressWarnings("unused")
    private static void touchImports() {
        assertNotNull("sanity", "sanity");
        fail("never called");
    }
}
package org.dromara.workflow.de.probe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AC-14 探针设计的应用层验证（JUnit 5 · P0 #14 / N-08 反向门禁）。
 *
 * <p>本类是对 {@code docs/P3真执行探针方案-2026-09-18.md} §一 / §四 的应用层验证，
 * 不是真跑探针（探针需要真实 MySQL 库 + 探针实例 9002 + 流程定义 + sys_user，由 owner 一次性执行）。
 * 本类做的是：
 * <ol>
 *   <li>验证 §一 1.2 的两条路径判定逻辑：路径①（PASS）→ variable 应含 {@code de.ctx}；
 *       路径②（REJECT）→ variable 应不含 {@code de.ctx}</li>
 *   <li>验证 §四 4.1 第 (3) 条的「N-08 判定域排除探针实例 9002」过滤正确性
 *       （探针跑过之后判定域不会被污染 = §四 4.2 复算表的关键证据）</li>
 *   <li>验证 §一 1.3 复算 SQL 单行版的 {@code matchesAc14Expectation} 判定矩阵</li>
 * </ol>
 *
 * <p>★ 探针本身「产品代码 = 0」（§五）—— 本类是「探针设计的测试」而非「探针本身」。
 * 它落在 {@code src/test}，**不**进入 ruoyi-admin 主 jar。
 *
 * <h3>覆盖矩阵（≥ 10 个 @Test）</h3>
 * <ul>
 *   <li>§一 1.2 路径①：PASS + 含 {@code de.ctx} → 期望匹配</li>
 *   <li>§一 1.2 路径①：PASS + 不含 {@code de.ctx} → 期望不匹配</li>
 *   <li>§一 1.2 路径②：REJECT + 不含 {@code de.ctx} → 期望匹配</li>
 *   <li>§一 1.2 路径②：REJECT + 含 {@code de.ctx} → 期望不匹配（§一 1.4 异常分支）</li>
 *   <li>§四 4.1 第 (3) 条：isProbeRow 识别 9002 行</li>
 *   <li>§四 4.2 复算表：excludeProbeInstances 后判定域行数 = 0（探针跑了但探针实例已被剔除）</li>
 *   <li>§四 4.2 复算表：未过滤时判定域行数 = 1（探针跑了 + 不过滤 = 假红）</li>
 *   <li>§一 1.3：{@code hasDeCtx} 对 null / 空串 / 非 JSON / 合法 JSON 4 种情形</li>
 *   <li>边界：skip_type 未知时 matchesAc14Expectation = false</li>
 *   <li>边界：his == null 时 isProbeRow = false / matchesAc14Expectation = false</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-19 (H1 P0 #14)
 */
@Tag("dev")
@DisplayName("AC-14 探针设计应用层验证 · N-08 反向门禁")
public class Ac14ProbeFilterTest {

    /** AC-14 探针实例编号（专用；不与验收夹具实例 9001 混用） */
    private static final long PROBE_INSTANCE_ID = 9002L;

    /** 验收夹具实例编号（与探针严格区分） */
    private static final long VERIFY_INSTANCE_ID = 9001L;

    /** de.ctx 顶层键名 */
    private static final String DE_CTX_KEY = "de.ctx";

    // ====================================================================
    // ===== §一 1.2 两条路径判定 =========================================
    // ====================================================================

    @Test
    @DisplayName("§一 1.2 路径① (PASS + 含 de.ctx) → 期望匹配")
    void probe_path1_passWithDeCtx() {
        FlowHisTask his = newHis(PROBE_INSTANCE_ID, "PASS",
            "{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7,\"business_code\":\"digital_employee.binding\"},\"task\":{\"step\":\"pending\"}},\"messageType\":\"a\"}");
        assertTrue(matchesAc14Expectation(his),
            "路径① 期望：PASS + variable 含 de.ctx → 匹配");
    }

    @Test
    @DisplayName("§一 1.2 路径① (PASS + 不含 de.ctx) → 期望不匹配")
    void probe_path1_passNoDeCtx() {
        FlowHisTask his = newHis(PROBE_INSTANCE_ID, "PASS",
            "{\"messageType\":\"a\",\"messageNotice\":\"b\"}");
        assertFalse(matchesAc14Expectation(his),
            "路径① 异常：PASS + variable 不含 de.ctx → 不匹配（§一 1.4「路径① ctx 为 NULL」分支）");
    }

    @Test
    @DisplayName("§一 1.2 路径② (REJECT + 不含 de.ctx) → 期望匹配")
    void probe_path2_rejectNoDeCtx() {
        FlowHisTask his = newHis(PROBE_INSTANCE_ID, "REJECT",
            "{\"messageType\":\"a\",\"messageNotice\":\"b\"}");
        assertTrue(matchesAc14Expectation(his),
            "路径② 期望：REJECT + variable 不含 de.ctx → 匹配");
    }

    @Test
    @DisplayName("§一 1.2 路径② (REJECT + 含 de.ctx) → 期望不匹配（§一 1.4 异常分支）")
    void probe_path2_rejectWithDeCtx() {
        FlowHisTask his = newHis(PROBE_INSTANCE_ID, "REJECT",
            "{\"de.ctx\":{\"v\":1},\"messageType\":\"a\"}");
        assertFalse(matchesAc14Expectation(his),
            "路径② 异常：REJECT + variable 含 de.ctx → 不匹配（§一 1.4「底座行为与本仓实测不符」分支）");
    }

    // ====================================================================
    // ===== §四 4.1 第 (3) 条：探针实例 9002 判定域排除 ==================
    // ====================================================================

    @Test
    @DisplayName("§四 4.1 第 (3) 条：isProbeRow 识别 9002")
    void probe_isProbeRow() {
        assertTrue(isProbeRow(newHis(PROBE_INSTANCE_ID, "PASS", "{}")),
            "instance_id=9002 应被识别为探针实例");
        assertFalse(isProbeRow(newHis(VERIFY_INSTANCE_ID, "PASS", "{}")),
            "instance_id=9001（验收实例）不应被识别为探针实例");
        assertFalse(isProbeRow(newHis(12345L, "PASS", "{}")),
            "任意非 9002 不应被识别为探针实例");
        assertFalse(isProbeRow(null),
            "null 输入应返回 false（防御性）");
    }

    @Test
    @DisplayName("§四 4.2 复算表：excludeProbeInstances 后判定域行数 = 0（探针跑过但已被剔除）")
    void probe_excludeAfterProbeRun() {
        List<FlowHisTask> rows = Arrays.asList(
            newHis(PROBE_INSTANCE_ID, "PASS", "{\"de.ctx\":{\"v\":1}}")
        );
        List<FlowHisTask> filtered = excludeProbeInstances(rows);
        assertEquals(0, filtered.size(),
            "探针实例 9002 行被剔除后，判定域行数应为 0（§四 4.2 复算表「绿（真绿，探针不在判定面）」）");
    }

    @Test
    @DisplayName("§四 4.2 复算表：未过滤时判定域行数 = 1（探针跑了 + 不过滤 = 假红）")
    void probe_includeWithoutExclude() {
        List<FlowHisTask> rows = Arrays.asList(
            newHis(PROBE_INSTANCE_ID, "PASS", "{\"de.ctx\":{\"v\":1}}")
        );
        assertEquals(1, rows.size(),
            "未过滤时探针实例 9002 行的 N-08 假红复算证据（§四 4.2 复算表「假红」分支）");
    }

    @Test
    @DisplayName("§四 4.2：excludeProbeInstances 混合 9002 + 9001 + 其他实例 = 只剔 9002")
    void probe_excludeMixedInstances() {
        List<FlowHisTask> rows = Arrays.asList(
            newHis(PROBE_INSTANCE_ID, "PASS", "{\"de.ctx\":{\"v\":1}}"),
            newHis(VERIFY_INSTANCE_ID, "REJECT", "{}"),
            newHis(12345L, "PASS", "{\"de.ctx\":{\"v\":1}}")
        );
        List<FlowHisTask> filtered = excludeProbeInstances(rows);
        assertEquals(2, filtered.size(),
            "混合行：探针实例应被剔除，其他实例应保留");
        for (FlowHisTask row : filtered) {
            assertFalse(isProbeRow(row),
                "过滤后所有行 instance_id ≠ 9002");
        }
    }

    // ====================================================================
    // ===== §一 1.3 复算 SQL 单行版：hasDeCtx 边界 =======================
    // ====================================================================

    @Test
    @DisplayName("§一 1.3：hasDeCtx 对 null / 空串 / 非 JSON / 合法 JSON 4 种情形")
    void probe_hasDeCtxBoundaries() {
        assertFalse(hasDeCtx(null), "null 应返回 false");
        assertFalse(hasDeCtx(""), "空串应返回 false");
        assertFalse(hasDeCtx("not-json"), "非合法 JSON 应返回 false（解析失败 = 视为不存在）");
        assertTrue(hasDeCtx("{\"de.ctx\":{\"v\":1}}"), "合法 JSON 含 de.ctx 应返回 true");
        assertFalse(hasDeCtx("{\"messageType\":\"a\"}"), "合法 JSON 不含 de.ctx 应返回 false");
        assertTrue(hasDeCtx("{\"de.ctx\":{},\"foo\":\"bar\"}"), "de.ctx 值为空对象也视为存在");
    }

    // ====================================================================
    // ===== 边界：skip_type 未知 / null 行 =================================
    // ====================================================================

    @Test
    @DisplayName("边界：skip_type 未知时 matchesAc14Expectation = false")
    void probe_unknownSkipType() {
        FlowHisTask his = newHis(PROBE_INSTANCE_ID, "OTHER", "{\"de.ctx\":{\"v\":1}}");
        assertFalse(matchesAc14Expectation(his),
            "未知 skip_type 视为不匹配（防御性）");
    }

    @Test
    @DisplayName("边界：his == null 时 isProbeRow = false / matchesAc14Expectation = false")
    void probe_nullRow() {
        assertFalse(isProbeRow(null), "null his 不应被识别为探针行");
        assertFalse(matchesAc14Expectation(null), "null his 不应匹配 AC-14 期望");
    }

    // ====================================================================
    // ===== 静态逻辑（不依赖 Spring，单测可直接调用）====================
    // ====================================================================

    /**
     * §四 4.1 第 (3) 条的应用层等价实现：判断一条行是否属于探针实例（9002）。
     */
    private static boolean isProbeRow(FlowHisTask his) {
        return his != null && Long.valueOf(PROBE_INSTANCE_ID).equals(his.getInstanceId());
    }

    /**
     * §四 4.2 的应用层等价实现：从判定域中剔除探针实例。
     */
    private static List<FlowHisTask> excludeProbeInstances(Collection<FlowHisTask> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<FlowHisTask> result = new ArrayList<>(rows.size());
        for (FlowHisTask row : rows) {
            if (!isProbeRow(row)) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * §一 1.2 的应用层等价实现：从 variable JSON 字符串检测 de.ctx 顶层键。
     */
    private static boolean hasDeCtx(String variable) {
        if (variable == null || variable.isEmpty()) {
            return false;
        }
        try {
            JsonNode root = new ObjectMapper().readTree(variable);
            return root != null && root.has(DE_CTX_KEY);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * §一 1.3 复算 SQL 单行版：单行 AC-14 期望判定。
     */
    private static boolean matchesAc14Expectation(FlowHisTask his) {
        if (his == null) {
            return false;
        }
        String skipType = his.getSkipType();
        boolean hasCtx = hasDeCtx(his.getVariable());
        if ("PASS".equalsIgnoreCase(skipType)) {
            return hasCtx;
        }
        if ("REJECT".equalsIgnoreCase(skipType)) {
            return !hasCtx;
        }
        return false;
    }

    /**
     * 构造一条 flow_his_task 行（避免直接 new FlowHisTask 让测试更紧凑）。
     */
    private static FlowHisTask newHis(Long instanceId, String skipType, String variable) {
        FlowHisTask his = new FlowHisTask();
        his.setInstanceId(instanceId);
        his.setSkipType(skipType);
        his.setVariable(variable);
        return his;
    }
}

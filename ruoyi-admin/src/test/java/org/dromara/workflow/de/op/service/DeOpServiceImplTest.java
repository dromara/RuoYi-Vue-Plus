package org.dromara.workflow.de.op.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.workflow.de.domain.SaiAgentUsageStat;
import org.dromara.workflow.de.mapper.SaiAgentUsageStatMapper;
import org.dromara.workflow.de.op.domain.vo.DeOpAlertVo;
import org.dromara.workflow.de.op.domain.vo.DeOpConfigVo;
import org.dromara.workflow.de.op.domain.vo.DeOpMetricsVo;
import org.dromara.workflow.de.op.domain.vo.DeOpMetricsVo.DeOpAgentUsageVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OP 域 {@link DeOpServiceImpl} 单测（纯逻辑，不启 Spring、零 DB）。
 *
 * <p><b>范式对齐</b>：与 {@code DeSyLogServiceImplTest}/{@code DeSyConfigServiceImplTest}/
 * {@code DeHrServiceImplTest} 同款——Proxy 捕获型 stub mapper 驱动数据访问分支。
 *
 * <p><b>与样例的必要差异（因目标类 extends ServiceImpl）</b>：
 * {@link DeOpServiceImpl} 继承 {@code ServiceImpl<SaiAgentUsageStatMapper, SaiAgentUsageStat>}，
 * 无构造注入点，{@code metrics/alerts/config} 通过基类的 {@code list()/count()/getOne()} 访问数据，
 * 这些方法最终委派到 {@code getBaseMapper()}（= {@code CrudRepository.baseMapper} 字段）。
 * 因此本类：
 * <ol>
 *   <li>用反射把 Proxy stub 注入 {@code baseMapper} 字段（{@code getBaseMapper()} 仅做
 *       {@code Assert.notNull} 后直接返回该字段——已用 javap 核实）；</li>
 *   <li>手工初始化 {@link SaiAgentUsageStat} 的 TableInfo/lambda 缓存
 *       （{@code TableInfoHelper.initTableInfo}），使服务内 {@code LambdaQueryWrapper.ge(SaiAgentUsageStat::getStatDate,...)}
 *       等在无 MyBatis 容器环境下也能即时解析列名（否则抛 can not find lambda cache）。</li>
 * </ol>
 *
 * <p>钉死的业务不变量：
 * <ul>
 *   <li>metrics：窗口聚合总量 + 按 Agent 分解（TreeMap 升序），null 计数按 0 处理，空数据零值空分解；</li>
 *   <li>metrics：days 归一（null/&le;0&rarr;7，&gt;365&rarr;365）；</li>
 *   <li>alerts：usage-drop（prev&gt;0 且 curr 零/缺，high）先于 conversation-stalled（curr msg&gt;0 且 conv==0，medium）；</li>
 *   <li>config：defaultWindowDays=7、维度常量、totalRows 透传 count、min/max 缺行为 null。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("OP 域 DeOpServiceImpl：metrics 聚合 / alerts 环比 / config 读（Proxy stub + 反射注入 baseMapper）")
class DeOpServiceImplTest {

    private static final long DAY_MS = 86_400_000L;

    /** 一次性安装 lambda 列缓存（进程内静态，幂等）。 */
    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiAgentUsageStat.class);
        return true;
    }

    // ════════════════════════════ metrics ════════════════════════════

    @Test
    @DisplayName("metrics：窗口聚合总量 + 按 Agent 升序分解（同 Agent 多行合并）")
    void metrics_aggregatesByAgentSorted() {
        Harness h = new Harness();
        h.enqueue(List.of(
            row(2L, 100L, 3, 1, daysAgo(1)),
            row(1L, 100L, 7, 2, daysAgo(1)),
            row(1L, 200L, 1, 4, daysAgo(2))));
        DeOpMetricsVo vo = h.service.metrics(null, null, 5);

        assertEquals(5, vo.getWindowDays());
        assertEquals(11L, vo.getTotalMessages(), "3+7+1");
        assertEquals(7L, vo.getTotalConversations(), "1+2+4");
        assertEquals(2, vo.getAgentCount(), "两个不同 agentId");
        assertEquals(1, h.selectListCalls, "metrics 必须恰好一次 selectList");
        // TreeMap 升序：agent1 先于 agent2
        List<DeOpAgentUsageVo> byAgent = vo.getByAgent();
        assertEquals(2, byAgent.size());
        assertEquals(1L, byAgent.get(0).getAgentId());
        assertEquals(8, byAgent.get(0).getMessageCount(), "同 Agent 两行合并 7+1");
        assertEquals(6, byAgent.get(0).getConversationCount(), "2+4");
        assertEquals(2L, byAgent.get(1).getAgentId());
        assertEquals(3, byAgent.get(1).getMessageCount());
    }

    @Test
    @DisplayName("★metrics：空数据明确无数据（OP-04）—— 零值 + 空分解，不以运行数冒充")
    void metrics_emptyRows_zeroValues() {
        Harness h = new Harness();
        h.enqueue(List.of());
        DeOpMetricsVo vo = h.service.metrics(null, null, 7);

        assertEquals(0, vo.getAgentCount());
        assertEquals(0L, vo.getTotalMessages());
        assertEquals(0L, vo.getTotalConversations());
        assertTrue(vo.getByAgent().isEmpty(), "空数据必须空分解");
        assertNotNull(vo.getWindowStart());
    }

    @Test
    @DisplayName("metrics：null message_count / conversation_count 按 0 计入")
    void metrics_nullCountsTreatedAsZero() {
        Harness h = new Harness();
        h.enqueue(List.of(row(5L, 1L, null, null, daysAgo(1))));
        DeOpMetricsVo vo = h.service.metrics(5L, null, 3);

        assertEquals(1, vo.getAgentCount(), "行存在即计入 agent 数");
        assertEquals(0L, vo.getTotalMessages(), "null 计数必须按 0");
        assertEquals(0L, vo.getTotalConversations());
        assertEquals(0, vo.getByAgent().get(0).getMessageCount());
    }

    @Test
    @DisplayName("★metrics：days 归一边界（null/0/负&rarr;7；&gt;365&rarr;365；区间内保留）")
    void metrics_daysNormalization() {
        assertEquals(7, windowOf(null), "null days -> 7");
        assertEquals(7, windowOf(0), "0 days -> 7");
        assertEquals(7, windowOf(-3), "负 days -> 7");
        assertEquals(30, windowOf(30), "区间内原样");
        assertEquals(365, windowOf(1000), "上限 365");
        assertEquals(365, windowOf(365), "边界 365");
    }

    @Test
    @DisplayName("metrics：windowStart ≈ now - windowDays（容差 ±1 天）")
    void metrics_windowStartReflectsDays() {
        Harness h = new Harness();
        h.enqueue(List.of());
        long now = System.currentTimeMillis();
        DeOpMetricsVo vo = h.service.metrics(null, null, 10);
        long ws = vo.getWindowStart().getTime();
        assertTrue(ws >= now - 11 * DAY_MS && ws <= now - 9 * DAY_MS,
            "windowStart 应落在 now-10d 附近，实测偏差=" + ((now - ws) / DAY_MS) + "d");
    }

    // ════════════════════════════ alerts ════════════════════════════

    @Test
    @DisplayName("★alerts：usage-drop(high) 先产出，conversation-stalled(medium) 后产出，prev/curr 值正确")
    void alerts_usageDropThenConversationStalled() {
        Harness h = new Harness();
        // 第一次 selectList = 当前窗口 [currStart, now)
        h.enqueue(List.of(
            row(1L, 1L, 10, 0, daysAgo(1)),   // 有消息、零对话 -> stalled
            row(2L, 1L, 5, 3, daysAgo(1))));   // 健康
        // 第二次 selectList = 上一窗口 [prevStart, currStart)
        h.enqueue(List.of(
            row(1L, 1L, 4, 1, daysAgo(8)),
            row(2L, 1L, 8, 2, daysAgo(8)),
            row(3L, 1L, 6, 1, daysAgo(8))));   // agent3 当前窗口缺席 -> usage-drop
        List<DeOpAlertVo> out = h.service.alerts(7);

        assertEquals(2, h.selectListCalls, "alerts 必须两次窗口查询");
        assertEquals(2, out.size(), "一条 usage-drop + 一条 conversation-stalled");

        DeOpAlertVo drop = out.get(0);
        assertEquals(3L, drop.getAgentId());
        assertEquals("usage-drop", drop.getAlertType());
        assertEquals("high", drop.getSeverity());
        assertEquals(6, drop.getPrevWindowMessages());
        assertEquals(0, drop.getCurrWindowMessages(), "当前窗口缺失记 0");

        DeOpAlertVo stalled = out.get(1);
        assertEquals(1L, stalled.getAgentId());
        assertEquals("conversation-stalled", stalled.getAlertType());
        assertEquals("medium", stalled.getSeverity());
        assertEquals(4, stalled.getPrevWindowMessages());
        assertEquals(10, stalled.getCurrWindowMessages());
    }

    @Test
    @DisplayName("alerts：健康窗口（有消息且有对话、上一窗口也无落差）-> 零告警")
    void alerts_healthy_noAlerts() {
        Harness h = new Harness();
        h.enqueue(List.of(row(1L, 1L, 10, 5, daysAgo(1))));    // curr 有消息有对话
        h.enqueue(List.of(row(1L, 1L, 12, 6, daysAgo(8))));    // prev 有消息，curr 非零 -> 非 drop
        List<DeOpAlertVo> out = h.service.alerts(7);
        assertTrue(out.isEmpty(), "健康窗口不得产生任何告警");
    }

    @Test
    @DisplayName("★alerts：两窗口皆空 -> 空列表（零告警，非伪造）")
    void alerts_emptyWindows_returnsEmpty() {
        Harness h = new Harness();
        h.enqueue(List.of());
        h.enqueue(List.of());
        List<DeOpAlertVo> out = h.service.alerts(null);
        assertTrue(out.isEmpty(), "空数据必须空告警列表");
        assertEquals(2, h.selectListCalls);
    }

    @Test
    @DisplayName("alerts：prev 消息为 0 的行即便 curr 缺失也不产生 usage-drop（drop 需 prev>0）")
    void alerts_prevZeroMessages_noDrop() {
        Harness h = new Harness();
        h.enqueue(List.of());                       // curr 空
        h.enqueue(List.of(row(9L, 1L, 0, 0, daysAgo(8)))); // prev 存在但消息=0
        List<DeOpAlertVo> out = h.service.alerts(7);
        assertTrue(out.isEmpty(), "prev 消息为 0 不构成 usage-drop");
    }

    // ════════════════════════════ config ════════════════════════════

    @Test
    @DisplayName("config：口径常量 + totalRows 透传 count + min/max 取自首末行")
    void config_readsConstantsAndWindow() {
        Harness h = new Harness();
        Date statDate = daysAgo(3);
        h.selectCountResult = 42L;
        h.selectOneResult = row(1L, 1L, 1, 1, statDate);

        DeOpConfigVo vo = h.service.config();

        assertEquals(7, vo.getDefaultWindowDays(), "默认窗口口径固定 7");
        assertEquals(List.of("agent", "user", "stat_date"), vo.getMetricDimensions());
        assertEquals(42L, vo.getTotalRows(), "必须透传 count() 结果");
        assertEquals(statDate, vo.getMinStatDate());
        assertEquals(statDate, vo.getMaxStatDate());
        assertEquals(1, h.selectCountCalls, "config 必须恰好一次 count");
        assertEquals(2, h.selectOneCalls, "config 必须两次 getOne（min + max）");
    }

    @Test
    @DisplayName("★config：空表（count=0 且 getOne 返回 null）-> totalRows=0、min/max 为 null，不得伪造日期")
    void config_emptyTable_nullBounds() {
        Harness h = new Harness();
        h.selectCountResult = 0L;
        h.selectOneResult = null;

        DeOpConfigVo vo = h.service.config();

        assertEquals(0L, vo.getTotalRows());
        assertNull(vo.getMinStatDate(), "空表 min 必须 null");
        assertNull(vo.getMaxStatDate(), "空表 max 必须 null");
        assertEquals(7, vo.getDefaultWindowDays());
    }

    // ════════════════════════════ helpers ════════════════════════════

    private static int windowOf(Integer days) {
        Harness h = new Harness();
        h.enqueue(List.of());
        return h.service.metrics(null, null, days).getWindowDays();
    }

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - days * DAY_MS);
    }

    private static SaiAgentUsageStat row(Long agentId, Long userId, Integer msg, Integer conv, Date statDate) {
        SaiAgentUsageStat r = new SaiAgentUsageStat();
        r.setAgentId(agentId);
        r.setUserId(userId);
        r.setMessageCount(msg);
        r.setConversationCount(conv);
        r.setStatDate(statDate);
        return r;
    }

    /**
     * 捕获型夹具：反射注入 Proxy stub 到 baseMapper 字段；selectList 结果可编程队列，
     * selectCount / selectOne 可编程返回值，并记录各方法调用次数。
     */
    private static final class Harness {
        final DeOpServiceImpl service;
        final Deque<List<SaiAgentUsageStat>> selectListQueue = new ArrayDeque<>();
        final List<Object[]> selectListArgs = new ArrayList<>();
        int selectListCalls;
        int selectCountCalls;
        int selectOneCalls;
        Long selectCountResult = 0L;
        SaiAgentUsageStat selectOneResult;

        Harness() {
            // 触发静态列缓存初始化
            if (!CACHE_READY) {
                throw new IllegalStateException("lambda cache init failed");
            }
            this.service = new DeOpServiceImpl();
            injectBaseMapper(this.service, stub());
        }

        void enqueue(List<SaiAgentUsageStat> rows) {
            selectListQueue.add(rows);
        }

        private SaiAgentUsageStatMapper stub() {
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                switch (name) {
                    case "selectList" -> {
                        selectListCalls++;
                        selectListArgs.add(args);
                        List<SaiAgentUsageStat> next = selectListQueue.poll();
                        return next != null ? next : new ArrayList<SaiAgentUsageStat>();
                    }
                    case "selectCount" -> {
                        selectCountCalls++;
                        return selectCountResult;
                    }
                    case "selectOne" -> {
                        selectOneCalls++;
                        return selectOneResult;
                    }
                    case "toString" -> {
                        return "SaiAgentUsageStatMapperStub";
                    }
                    case "hashCode" -> {
                        return System.identityHashCode(proxy);
                    }
                    case "equals" -> {
                        return proxy == (args == null ? null : args[0]);
                    }
                    default -> {
                        return defaultValue(method.getReturnType());
                    }
                }
            };
            ClassLoader cl = SaiAgentUsageStatMapper.class.getClassLoader();
            return (SaiAgentUsageStatMapper) Proxy.newProxyInstance(
                cl, new Class<?>[] {SaiAgentUsageStatMapper.class}, handler);
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class || type == Boolean.class) {
                return false;
            }
            if (type == int.class || type == Integer.class) {
                return 0;
            }
            if (type == long.class || type == Long.class) {
                return 0L;
            }
            if (List.class.isAssignableFrom(type)) {
                return new ArrayList<>();
            }
            return null;
        }
    }

    /** 反射沿类层次查找并设置 baseMapper 字段（声明于 CrudRepository）。 */
    private static void injectBaseMapper(Object target, Object mapper) {
        try {
            Class<?> c = target.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("baseMapper");
                    f.setAccessible(true);
                    f.set(target, mapper);
                    return;
                } catch (NoSuchFieldException ignore) {
                    c = c.getSuperclass();
                }
            }
            throw new IllegalStateException("baseMapper field not found in hierarchy of " + target.getClass());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("inject baseMapper failed", e);
        }
    }
}

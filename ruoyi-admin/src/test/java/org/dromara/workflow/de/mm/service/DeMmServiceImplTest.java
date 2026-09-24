package org.dromara.workflow.de.mm.service;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mm.domain.vo.DeMmConfigVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmConversationDetailVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmMemoryVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MM 域 {@link DeMmServiceImpl} 纯逻辑单测（T1-W2，不启 Spring、零 DB）。
 *
 * <p>范式与 W1（DeSyConfigServiceImplTest）一致：Proxy 捕获型 stub mapper + 构造注入直调。
 * 本类差异点：被测服务 extends MyBatis-Plus {@code ServiceImpl}，其 {@code list(wrapper)}
 * 经 {@code getBaseMapper().selectList(wrapper)} 路由（3.5.17 字节码已核实），
 * 故需反射注入父类 {@code CrudRepository#baseMapper} 字段。
 *
 * <p>钉死的业务不变量：
 * <ul>
 *   <li>listMemories：userId 必填 fail-closed；可选过滤条件（agentId/conversationId/role/keyword）
 *       仅在非空白时进入查询参数；记录→VO 全字段映射；</li>
 *   <li>conversationDetail：会话不存在即抛（空结果不放行）；角色/状态分布统计、
 *       metadata 计数、first/last 取数口径（列表首尾）、preview 200 字符截断；</li>
 *   <li>memoryConfig：数字员工不存在即抛；memory_enabled 三态映射（1→true，0/null→false）；
 *       会话数/用户数按去重统计且 null 不计。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("MM 域 DeMmServiceImpl：记忆列表 / 会话详情 / 记忆配置（纯逻辑）")
class DeMmServiceImplTest {

    private static final Long AGENT_ID = 3L;
    private static final Long USER_ID = 7L;
    private static final String CONV_ID = "conv-1";

    // ════════════════ listMemories ════════════════

    @Test
    @DisplayName("listMemories：userId 为 null 必须 fail-closed 抛异常，且不触达 mapper")
    void listMemories_userIdNull_throws() {
        Harness h = new Harness();
        ServiceException ex = assertThrows(ServiceException.class,
            () -> h.service.listMemories(null, null, null, null, null));
        assertTrue(ex.getMessage().contains("userId"));
        assertEquals(0, h.selectListCalls, "异常路径不得查库");
    }

    @Test
    @DisplayName("listMemories：空结果返回空列表（非 null）")
    void listMemories_emptyResult_returnsEmpty() {
        Harness h = new Harness();
        h.selectListResult = List.of();
        List<DeMmMemoryVo> out = h.service.listMemories(USER_ID, null, null, null, null);
        assertEquals(1, h.selectListCalls);
        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("listMemories：记录→VO 八字段全量映射")
    void listMemories_mapsAllFieldsToVo() {
        Harness h = new Harness();
        h.selectListResult = List.of(rec(11L, AGENT_ID, USER_ID, CONV_ID, "user", "你好", "{\"a\":1}", 1));
        List<DeMmMemoryVo> out = h.service.listMemories(USER_ID, null, null, null, null);
        assertEquals(1, out.size());
        DeMmMemoryVo vo = out.get(0);
        assertEquals(11L, vo.getRecordId());
        assertEquals(AGENT_ID, vo.getAgentId());
        assertEquals(USER_ID, vo.getUserId());
        assertEquals(CONV_ID, vo.getConversationId());
        assertEquals("user", vo.getRole());
        assertEquals("你好", vo.getContent());
        assertEquals("{\"a\":1}", vo.getMetadata());
        assertEquals(1, vo.getStatus());
    }

    @Test
    @DisplayName("listMemories：全部可选过滤条件非空白时必须进入查询参数")
    void listMemories_allFilters_presentInWrapperParams() {
        Harness h = new Harness();
        h.selectListResult = List.of();
        h.service.listMemories(USER_ID, AGENT_ID, CONV_ID, "user", "kw");
        AbstractWrapper<?, ?, ?> qw = h.capturedWrappers.get(0);
        // MP 3.5.17 normal 段结构：首条件 3 段（列/关键字/值），后续每条件 4 段（多一个 AND）；
        // orderBy 不占 normal。5 个条件 = 3 + 4×4 = 19
        List<ISqlSegment> normal = qw.getExpression().getNormal();
        assertEquals(19, normal.size(), "userId+agentId+conversationId+role+keyword 五个条件缺一不可");
        // 末段为 keyword 的 like 值段（惰性 formatParam），materialize 后落入参数表
        normal.get(normal.size() - 1).getSqlSegment();
        Collection<Object> values = qw.getParamNameValuePairs().values();
        assertTrue(values.stream().anyMatch(v -> String.valueOf(v).contains("kw")),
            "keyword 必须以 like（含 % 包裹）形式进参");
    }

    @Test
    @DisplayName("listMemories：空白可选过滤条件必须被忽略，只留 userId 一个条件")
    void listMemories_blankOptionalFilters_ignored() {
        Harness h = new Harness();
        h.selectListResult = List.of();
        h.service.listMemories(USER_ID, null, "  ", "", "   ");
        AbstractWrapper<?, ?, ?> qw = h.capturedWrappers.get(0);
        // 仅 userId 一个条件（3 段）；agentId=null 与三个空白串全部被忽略
        assertEquals(3, qw.getExpression().getNormal().size(),
            "空白/null 过滤条件不得进入查询表达式");
    }

    // ════════════════ conversationDetail ════════════════

    @Test
    @DisplayName("conversationDetail：conversationId null/空白均 fail-closed")
    void conversationDetail_blankThrows() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.conversationDetail(null));
        assertThrows(ServiceException.class, () -> h.service.conversationDetail("  "));
        assertEquals(0, h.selectListCalls, "异常路径不得查库");
    }

    @Test
    @DisplayName("conversationDetail：会话无记录必须抛『不存在』，不放行空 VO")
    void conversationDetail_notFound_throws() {
        Harness h = new Harness();
        h.selectListResult = List.of();
        ServiceException ex = assertThrows(ServiceException.class,
            () -> h.service.conversationDetail(CONV_ID));
        assertTrue(ex.getMessage().contains(CONV_ID));
    }

    @Test
    @DisplayName("conversationDetail：角色/状态分布计数 + metadata 非空白计数 + 首尾取数口径")
    void conversationDetail_countsAndDistributions() {
        Harness h = new Harness();
        // 顺序即 selectList 返回序：first=第 0 条，last=末条
        h.selectListResult = List.of(
            rec(1L, AGENT_ID, USER_ID, CONV_ID, "user", "第一问", "meta-a", 1),
            rec(2L, AGENT_ID, USER_ID, CONV_ID, "assistant", "答", "", 1),
            rec(3L, AGENT_ID, 8L, CONV_ID, "user", "第二问", null, 2));
        DeMmConversationDetailVo vo = h.service.conversationDetail(CONV_ID);

        assertEquals(CONV_ID, vo.getConversationId());
        assertEquals(3, vo.getRecordCount());
        assertEquals(Map.of("user", 2L, "assistant", 1L), vo.getRoleDistribution());
        assertEquals(Map.of(1, 2L, 2, 1L), vo.getStatusDistribution());
        assertEquals(1L, vo.getRecordsWithMetadata(), "空串与 null metadata 均不得计数");
        assertEquals(AGENT_ID, vo.getAgentId(), "agentId 取首条");
        assertEquals(USER_ID, vo.getUserId(), "userId 取首条");
        assertEquals("第一问", vo.getFirstContent());
        assertEquals("第二问", vo.getLastContent());
        assertEquals("user", vo.getLastRole(), "lastRole 取末条");
        assertEquals(3, vo.getRecords().size(), "明细须全量映射");
    }

    @Test
    @DisplayName("★conversationDetail：preview 超过 200 字符必须截断，短文本原样")
    void conversationDetail_previewTruncation() {
        Harness h = new Harness();
        String long300 = "L".repeat(300);
        h.selectListResult = List.of(
            rec(1L, AGENT_ID, USER_ID, CONV_ID, "user", long300, null, 1),
            rec(2L, AGENT_ID, USER_ID, CONV_ID, "assistant", "短", null, 1));
        DeMmConversationDetailVo vo = h.service.conversationDetail(CONV_ID);
        assertEquals("L".repeat(200), vo.getFirstContent(), "超长内容须恰好截为 200 字符");
        assertEquals("短", vo.getLastContent(), "200 以内内容不得改动");
    }

    @Test
    @DisplayName("conversationDetail：content 为 null 时 preview 返回 null 不抛 NPE")
    void conversationDetail_nullContent_previewNull() {
        Harness h = new Harness();
        h.selectListResult = List.of(
            rec(1L, AGENT_ID, USER_ID, CONV_ID, "user", null, null, 1));
        DeMmConversationDetailVo vo = h.service.conversationDetail(CONV_ID);
        assertNull(vo.getFirstContent());
        assertNull(vo.getLastContent());
    }

    // ════════════════ memoryConfig ════════════════

    @Test
    @DisplayName("memoryConfig：agentId 为 null fail-closed，不触达任何 mapper")
    void memoryConfig_agentIdNull_throws() {
        Harness h = new Harness();
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.memoryConfig(null));
        assertTrue(ex.getMessage().contains("agentId"));
        assertEquals(0, h.selectListCalls);
        assertEquals(0, h.agentSelectByIdCalls);
    }

    @Test
    @DisplayName("memoryConfig：数字员工不存在必须抛『数字员工不存在』")
    void memoryConfig_agentMissing_throws() {
        Harness h = new Harness();
        h.agent = null;
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.memoryConfig(AGENT_ID));
        assertTrue(ex.getMessage().contains("数字员工不存在"));
        assertEquals(0, h.selectListCalls, "agent 校验失败后不得再查记录");
    }

    @Test
    @DisplayName("memoryConfig：memory_enabled 三态映射 1→true、0→false、null→false")
    void memoryConfig_memoryEnabledTristate() {
        assertEquals(true, memoryEnabledOf(1));
        assertEquals(false, memoryEnabledOf(0));
        assertEquals(false, memoryEnabledOf(null));
    }

    private static boolean memoryEnabledOf(Integer memoryEnabled) {
        Harness h = new Harness();
        h.agent = agent(memoryEnabled);
        h.selectListResult = List.of();
        return h.service.memoryConfig(AGENT_ID).getMemoryEnabled();
    }

    @Test
    @DisplayName("memoryConfig：总数/会话去重/用户去重/metadata 计数（null 标识符不计）")
    void memoryConfig_statsWithDedup() {
        Harness h = new Harness();
        h.agent = agent(1);
        h.selectListResult = List.of(
            rec(1L, AGENT_ID, USER_ID, "c1", "user", "a", "m", 1),      // c1 + u7 + meta
            rec(2L, AGENT_ID, USER_ID, "c1", "user", "b", null, 1),     // 重复 c1/u7
            rec(3L, AGENT_ID, 8L, "c2", "assistant", "c", "  ", 1),     // c2 + u8，metadata 空白不计
            rec(4L, AGENT_ID, null, null, "user", "d", "m", 1));        // null 会话/用户不计
        DeMmConfigVo vo = h.service.memoryConfig(AGENT_ID);
        assertEquals(AGENT_ID, vo.getAgentId());
        assertTrue(vo.getMemoryEnabled());
        assertEquals(4, vo.getTotalRecords());
        assertEquals(2L, vo.getRecordsWithMetadata(), "空串 metadata 不得计数");
        assertEquals(2, vo.getConversationCount(), "conversationId 去重且 null 不计");
        assertEquals(2, vo.getUserCount(), "userId 去重且 null 不计");
    }

    @Test
    @DisplayName("memoryConfig：无任何记录时各计数为 0 而非抛错")
    void memoryConfig_emptyRecords_zeroStats() {
        Harness h = new Harness();
        h.agent = agent(0);
        h.selectListResult = List.of();
        DeMmConfigVo vo = h.service.memoryConfig(AGENT_ID);
        assertFalse(vo.getMemoryEnabled());
        assertEquals(0, vo.getTotalRecords());
        assertEquals(0, vo.getConversationCount());
        assertEquals(0, vo.getUserCount());
        assertEquals(0L, vo.getRecordsWithMetadata());
    }

    // ════════════════ fixtures ════════════════

    private static SaiAgentConversationRecord rec(Long id, Long agentId, Long userId, String convId,
                                                  String role, String content, String metadata, Integer status) {
        SaiAgentConversationRecord r = new SaiAgentConversationRecord();
        r.setId(id);
        r.setAgentId(agentId);
        r.setUserId(userId);
        r.setConversationId(convId);
        r.setRole(role);
        r.setContent(content);
        r.setMetadata(metadata);
        r.setStatus(status);
        return r;
    }

    private static SaiAgent agent(Integer memoryEnabled) {
        SaiAgent a = new SaiAgent();
        a.setId(AGENT_ID);
        a.setMemoryEnabled(memoryEnabled);
        return a;
    }

    /**
     * 捕获型夹具：selectList 可编程返回并捕获 wrapper；selectById(agent) 可编程返回。
     * baseMapper 为 MP 父类 CrudRepository 的 protected 字段，反射注入（字节码已核实
     * getBaseMapper() 仅在字段为 null 时抛错，注入后直通）。
     */
    private static final class Harness {
        final DeMmServiceImpl service;
        final List<AbstractWrapper<?, ?, ?>> capturedWrappers = new ArrayList<>();
        List<SaiAgentConversationRecord> selectListResult = new ArrayList<>();
        SaiAgent agent = agent(1);
        int selectListCalls;
        int agentSelectByIdCalls;

        Harness() {
            SaiAgentConversationRecordMapper recordMapper = stub(SaiAgentConversationRecordMapper.class,
                (method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        selectListCalls++;
                        if (args.length == 1 && args[0] instanceof AbstractWrapper<?, ?, ?> qw) {
                            capturedWrappers.add(qw);
                        }
                        return selectListResult;
                    }
                    return defaultValue(method.getReturnType());
                });
            SaiAgentMapper agentMapper = stub(SaiAgentMapper.class, (method, args) -> {
                if ("selectById".equals(method.getName())) {
                    agentSelectByIdCalls++;
                    return agent;
                }
                return defaultValue(method.getReturnType());
            });
            this.service = new DeMmServiceImpl(agentMapper);
            try {
                Field baseMapper = Class.forName("com.baomidou.mybatisplus.spring.repository.CrudRepository")
                    .getDeclaredField("baseMapper");
                baseMapper.setAccessible(true);
                baseMapper.set(this.service, recordMapper);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("注入 CrudRepository#baseMapper 失败，范式前提已变", e);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T stub(Class<T> iface, Handler handler) {
            return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
                (proxy, method, args) -> {
                    Object r = handler.handle(method, args == null ? new Object[0] : args);
                    if (r != null) {
                        return r;
                    }
                    return defaultValue(method.getReturnType());
                });
        }

        private interface Handler {
            Object handle(java.lang.reflect.Method method, Object[] args) throws Exception;
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
                return List.of();
            }
            return null;
        }
    }
}

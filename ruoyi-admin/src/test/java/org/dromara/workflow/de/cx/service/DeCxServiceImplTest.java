package org.dromara.workflow.de.cx.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.cx.domain.vo.DeCxValidateVo;
import org.dromara.workflow.de.domain.SysUserRole;
import org.dromara.workflow.de.mapper.SysUserRoleMapper;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CX-10 上下文校验（授权 / 版本 / 有效期）surefire 标准化测试（JUnit 5）。
 *
 * <p>由 surefire 自动发现并执行（前提：{@code -Dprofiles.active=dev -Dmaven.test.skip=false}）。
 *
 * <h3>本类要钉死的一件事</h3>
 * <p>CX-10 验收原句：「<b>版本标识不是简单按字典序判断新旧</b>」。
 * 这是本类里 {@code versionSemantics_*} 一组用例存在的唯一理由 ——
 * 它们把「字典序会判错、数值比较才判对」摆成可执行的断言，
 * 而不是一句写在注释里的承诺。
 *
 * <p>DB 读取用 {@link java.lang.reflect.Proxy} 构造 stub mapper（不启 Spring 上下文），
 * 与 {@code BrComplianceServiceTest} 同一手法。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Tag("dev")
@DisplayName("DeCxServiceImpl CX-10 授权 / 版本 / 有效期校验")
public class DeCxServiceImplTest {

    private static final Long INSTANCE_ID = 9001L;
    private static final Long USER_ID = 1761100000000000001L;
    private static final String USERNAME = "admin";

    // ========================================================================
    // ===== ★ 版本比较语义（CX-10 验收要点：不是字典序）=======================
    // ========================================================================

    @Test
    @DisplayName("★版本语义 数值比较：v=10 不比 v=9 旧（字典序会判反）")
    void versionSemantics_tenIsNewerThanNine() {
        // 数值语义（本实装）：10 > 9 ⇒ 不是旧版
        assertFalse(DeCxServiceImpl.isOlderRevision(10, 9),
            "数值比较下 v=10 应判为新版（不比 9 旧）");

        // 字典序（本实装要防的错）：'1' < '9' ⇒ "10" 排在 "9" 之前 ⇒ 会被误判成旧版
        assertTrue("10".compareTo("9") < 0,
            "字典序反例应成立 —— 若此断言失败，说明本测试的前提假设本身错了");
    }

    @Test
    @DisplayName("★版本语义 数值比较：v=9 比 v=10 旧")
    void versionSemantics_nineIsOlderThanTen() {
        assertTrue(DeCxServiceImpl.isOlderRevision(9, 10));
        assertFalse(DeCxServiceImpl.isOlderRevision(10, 10), "同版本不算旧");
        assertFalse(DeCxServiceImpl.isOlderRevision(11, 10));
    }

    @Test
    @DisplayName("★版本语义 跨位数整段：2 比 10 旧（字典序同位数下同号，此处仍以数值为准）")
    void versionSemantics_singleVsDoubleDigit() {
        assertTrue(DeCxServiceImpl.isOlderRevision(2, 10));
        assertFalse(DeCxServiceImpl.isOlderRevision(100, 99));
        assertTrue(DeCxServiceImpl.isOlderRevision(99, 100));
    }

    @Test
    @DisplayName("★版本语义 文本形态的版本也转成数值再比 —— 不留字符串入口")
    void versionSemantics_textualVersionIsParsedToInt() {
        ObjectMapper om = new ObjectMapper();
        // JSON 整数
        assertEquals(1, DeCxServiceImpl.parseRevision(om.valueToTree(1)));
        // 纯数字文本 ⇒ 转数值（不是留着字符串）
        Integer ten = DeCxServiceImpl.parseRevision(om.valueToTree("10"));
        assertEquals(10, ten);
        // 拿到数值后，比较走整数路径
        assertFalse(DeCxServiceImpl.isOlderRevision(ten, 9));
    }

    @Test
    @DisplayName("★版本语义 不可解析一律 null（由调用方 fail-closed）")
    void versionSemantics_unparseableIsNull() {
        ObjectMapper om = new ObjectMapper();
        assertNull(DeCxServiceImpl.parseRevision(null), "null 节点");
        assertNull(DeCxServiceImpl.parseRevision(om.valueToTree("abc")), "非数字文本");
        assertNull(DeCxServiceImpl.parseRevision(om.valueToTree(1.5)), "小数不是整数版本");
        assertNull(DeCxServiceImpl.parseRevision(om.valueToTree("")), "空串");
        assertNull(DeCxServiceImpl.parseRevision(om.valueToTree(true)), "布尔");
        assertNull(DeCxServiceImpl.parseRevision(om.valueToTree(new ObjectMapper().createObjectNode())),
            "对象");
    }

    // ========================================================================
    // ===== action 归一化 ====================================================
    // ========================================================================

    @Test
    @DisplayName("action 空 = read；大小写不敏感")
    void actionNormalization() {
        assertEquals(DeCxServiceImpl.ACTION_READ, DeCxServiceImpl.normalizeAction(null));
        assertEquals(DeCxServiceImpl.ACTION_READ, DeCxServiceImpl.normalizeAction("  "));
        assertEquals(DeCxServiceImpl.ACTION_READ, DeCxServiceImpl.normalizeAction("read"));
        assertEquals(DeCxServiceImpl.ACTION_WRITE, DeCxServiceImpl.normalizeAction("write"));
        assertEquals(DeCxServiceImpl.ACTION_WRITE, DeCxServiceImpl.normalizeAction("WRITE"));
    }

    @Test
    @DisplayName("action 未知取值抛错 —— 不静默降级成 read")
    void actionUnknownRejected() {
        assertThrows(ServiceException.class, () -> DeCxServiceImpl.normalizeAction("nuke"));
    }

    // ========================================================================
    // ===== 三条校验的正例 / 反例 ============================================
    // ========================================================================

    @Test
    @DisplayName("正例 授权 + 版本 + 有效期全过 ⇒ valid=true / reason=OK")
    void pass_allThreeChecks() {
        Fixture f = Fixture.healthy();
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);

        assertTrue(vo.getValid());
        assertEquals(DeCxServiceImpl.REASON_OK, vo.getReason());
        assertEquals(1, vo.getCurrentVersion());
        assertEquals(DeCxServiceImpl.SERVER_MIN_VERSION, vo.getMinVersion());
        assertEquals(List.of(7L), vo.getRoleIds());
        assertNotNull(vo.getCheckedAt());
    }

    @Test
    @DisplayName("★最低版本来自服务端常量，不由请求体决定")
    void minVersionIsServerSideFact() {
        Fixture f = Fixture.healthy();
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertEquals(DeCxServiceImpl.SERVER_MIN_VERSION, vo.getMinVersion());
        assertEquals(1, newSvc(f).serverMinVersion(), "服务端最低版本 = de.ctx schema 版本真值");
    }

    @Test
    @DisplayName("实例不存在 ⇒ INSTANCE_NOT_FOUND")
    void instanceNotFound() {
        Fixture f = Fixture.healthy();
        f.instance = null;
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertFalse(vo.getValid());
        assertEquals(DeCxServiceImpl.REASON_INSTANCE_NOT_FOUND, vo.getReason());
    }

    @Test
    @DisplayName("★撤权后不再放行：sys_user_role 空集 ⇒ NO_ROLE（同一夹具先过后拒）")
    void revocationStopsAccess() {
        Fixture f = Fixture.healthy();
        DeCxServiceImpl svc = newSvc(f);

        // 撤权前：放行
        assertTrue(svc.contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getValid());

        // 撤权：sys_user_role 里该用户的角色被清空（实时读库 ⇒ 下一次校验即生效）
        f.roles = new ArrayList<>();
        DeCxValidateVo after = svc.contextValidate(INSTANCE_ID, "write", USER_ID, USERNAME);
        assertFalse(after.getValid());
        assertEquals(DeCxServiceImpl.REASON_NO_ROLE, after.getReason());
    }

    @Test
    @DisplayName("有角色但 flow_user 未命中且非发起人 ⇒ NO_TASK_RELATION")
    void noTaskRelation() {
        Fixture f = Fixture.healthy();
        f.flowUserCount = 0L;
        f.hisTaskCount = 0L;
        f.instance.setCreateBy("someone-else");
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertFalse(vo.getValid());
        assertEquals(DeCxServiceImpl.REASON_NO_TASK_RELATION, vo.getReason());
    }

    @Test
    @DisplayName("已办分支：flow_his_task.approver 命中即放行")
    void relationViaHisTask() {
        Fixture f = Fixture.healthy();
        f.flowUserCount = 0L;
        f.hisTaskCount = 1L;
        f.instance.setCreateBy("someone-else");
        assertTrue(newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getValid());
    }

    @Test
    @DisplayName("发起分支：flow_instance.create_by 命中即放行")
    void relationViaCreateBy() {
        Fixture f = Fixture.healthy();
        f.flowUserCount = 0L;
        f.hisTaskCount = 0L;
        f.instance.setCreateBy(USERNAME);
        assertTrue(newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getValid());
    }

    @Test
    @DisplayName("上下文缺失 / 不可解析 ⇒ CONTEXT_MISSING / CONTEXT_UNPARSEABLE")
    void contextMissingAndUnparseable() {
        Fixture f = Fixture.healthy();
        f.instance.setVariable(null);
        assertEquals(DeCxServiceImpl.REASON_CONTEXT_MISSING,
            newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getReason());

        f.instance.setVariable("{not json");
        assertEquals(DeCxServiceImpl.REASON_CONTEXT_UNPARSEABLE,
            newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getReason());

        f.instance.setVariable("{\"other\":{}}");
        assertEquals(DeCxServiceImpl.REASON_CONTEXT_MISSING,
            newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getReason());

        // 合法 JSON 但根不是对象 ⇒ 形状不对，归 UNPARSEABLE 而不是「缺键」
        f.instance.setVariable("123");
        assertEquals(DeCxServiceImpl.REASON_CONTEXT_UNPARSEABLE,
            newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getReason());
        f.instance.setVariable("[1,2]");
        assertEquals(DeCxServiceImpl.REASON_CONTEXT_UNPARSEABLE,
            newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getReason());
    }

    @Test
    @DisplayName("版本缺失 / 非数字 ⇒ VERSION_MISSING / VERSION_UNPARSEABLE（fail-closed）")
    void versionMissingAndUnparseable() {
        Fixture f = Fixture.healthy();
        f.instance.setVariable("{\"de.ctx\":{\"binding\":{\"agent_id\":7}}}");
        DeCxValidateVo missing = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertFalse(missing.getValid());
        assertEquals(DeCxServiceImpl.REASON_VERSION_MISSING, missing.getReason());
        assertNull(missing.getCurrentVersion());

        f.instance.setVariable("{\"de.ctx\":{\"v\":\"abc\"}}");
        DeCxValidateVo bad = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertFalse(bad.getValid());
        assertEquals(DeCxServiceImpl.REASON_VERSION_UNPARSEABLE, bad.getReason());
    }

    @Test
    @DisplayName("版本低于服务端最低 ⇒ STALE")
    void staleVersion() {
        Fixture f = Fixture.healthy();
        f.instance.setVariable("{\"de.ctx\":{\"v\":0}}");
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "write", USER_ID, USERNAME);
        assertFalse(vo.getValid());
        assertEquals(DeCxServiceImpl.REASON_STALE, vo.getReason());
        assertEquals(0, vo.getCurrentVersion());
        assertEquals(DeCxServiceImpl.SERVER_MIN_VERSION, vo.getMinVersion());
    }

    @Test
    @DisplayName("★过期后不再放行：expires_at 已过 ⇒ EXPIRED（同一夹具先过后拒）")
    void expiredStopsAccess() {
        Fixture f = Fixture.healthy();
        f.instance.setVariable("{\"de.ctx\":{\"v\":1,\"expires_at\":\"" + pastIso() + "\"}}");
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertFalse(vo.getValid());
        assertEquals(DeCxServiceImpl.REASON_EXPIRED, vo.getReason());
        assertNotNull(vo.getExpiresAt());
    }

    @Test
    @DisplayName("未过期 ⇒ 放行；epoch 毫秒形态也认")
    void notExpiredPasses() {
        Fixture f = Fixture.healthy();
        f.instance.setVariable("{\"de.ctx\":{\"v\":1,\"expires_at\":\"" + futureIso() + "\"}}");
        assertTrue(newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getValid());

        f.instance.setVariable("{\"de.ctx\":{\"v\":1,\"expires_at\":"
            + (System.currentTimeMillis() + 3_600_000L) + "}}");
        assertTrue(newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME).getValid());
    }

    @Test
    @DisplayName("expires_at 不可解析 ⇒ EXPIRY_UNPARSEABLE（fail-closed，不放行）")
    void expiryUnparseableIsClosed() {
        Fixture f = Fixture.healthy();
        f.instance.setVariable("{\"de.ctx\":{\"v\":1,\"expires_at\":\"not-a-time\"}}");
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertFalse(vo.getValid());
        assertEquals(DeCxServiceImpl.REASON_EXPIRY_UNPARSEABLE, vo.getReason());
    }

    @Test
    @DisplayName("入参缺失 ⇒ 抛 ServiceException（instanceId / userId）")
    void missingArgsRejected() {
        Fixture f = Fixture.healthy();
        DeCxServiceImpl svc = newSvc(f);
        assertThrows(ServiceException.class, () -> svc.contextValidate(null, "read", USER_ID, USERNAME));
        assertThrows(ServiceException.class, () -> svc.contextValidate(INSTANCE_ID, "read", null, USERNAME));
    }

    @Test
    @DisplayName("★授权判定先于版本 —— 无权调用方读不到实例上下文版本")
    void authorizationBeforeVersion() {
        Fixture f = Fixture.healthy();
        f.roles = new ArrayList<>();
        f.instance.setVariable("{\"de.ctx\":{\"v\":7}}");
        DeCxValidateVo vo = newSvc(f).contextValidate(INSTANCE_ID, "read", USER_ID, USERNAME);
        assertEquals(DeCxServiceImpl.REASON_NO_ROLE, vo.getReason());
        assertNull(vo.getCurrentVersion(), "拒因是授权时不应回带上下文版本");
    }

    // ========================================================================
    // ===== 夹具 ============================================================
    // ========================================================================

    /** 一组健康的桩数据（三条校验全过）。 */
    private static final class Fixture {
        FlowInstance instance;
        long flowUserCount;
        long hisTaskCount;
        List<SysUserRole> roles;

        static Fixture healthy() {
            Fixture f = new Fixture();
            FlowInstance inst = new FlowInstance();
            inst.setId(INSTANCE_ID);
            inst.setCreateBy("someone-else");
            inst.setVariable("{\"de.ctx\":{\"v\":1,\"binding\":{\"agent_id\":7},"
                + "\"task\":{\"step\":\"running\"}}}");
            f.instance = inst;

            FlowTask task = new FlowTask();
            task.setId(300L);
            task.setInstanceId(INSTANCE_ID);

            f.flowUserCount = 1L;   // flow_user 命中（本需求点名的分支）
            f.hisTaskCount = 0L;

            SysUserRole role = new SysUserRole();
            role.setUserId(USER_ID);
            role.setRoleId(7L);
            f.roles = new ArrayList<>(List.of(role));
            return f;
        }
    }

    private static DeCxServiceImpl newSvc(Fixture f) {
        FlowTask task = new FlowTask();
        task.setId(300L);
        task.setInstanceId(INSTANCE_ID);

        return new DeCxServiceImpl(
            stubMapper(FlwInstanceMapper.class, id -> f.instance),
            stubMapper(FlwTaskMapper.class, id -> null, null, () -> new ArrayList<>(List.of(task))),
            stubMapper(FlowUserMapper.class, id -> null, () -> f.flowUserCount),
            stubMapper(FlwHisTaskMapper.class, id -> null, () -> f.hisTaskCount),
            stubMapper(SysUserRoleMapper.class, id -> null, null, () -> new ArrayList<>(f.roles)),
            new ObjectMapper()
        );
    }

    private static String pastIso() {
        return java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            .minusDays(1).withNano(0).toString();
    }

    private static String futureIso() {
        return java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            .plusDays(1).withNano(0).toString();
    }

    // ========================================================================
    // ===== stub mapper（动态代理）===========================================
    // ========================================================================

    private static <M> M stubMapper(Class<M> iface, Function<Object, Object> byIdResolver) {
        return stubMapper(iface, byIdResolver, null, null);
    }

    private static <M> M stubMapper(Class<M> iface,
                                    Function<Object, Object> byIdResolver,
                                    Supplier<Object> countSupplier) {
        return stubMapper(iface, byIdResolver, countSupplier, null);
    }

    /**
     * CX-10 实装只调用 {@code selectById} / {@code selectList} / {@code selectCount}
     * 三个方法；其余 MyBatis-Plus 默认方法不被触发，不实现。
     */
    @SuppressWarnings("unchecked")
    private static <M> M stubMapper(Class<M> iface,
                                    Function<Object, Object> byIdResolver,
                                    Supplier<Object> countSupplier,
                                    Supplier<Object> listSupplier) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectById".equals(name) && method.getParameterCount() == 1) {
                return byIdResolver == null ? null : byIdResolver.apply(args[0]);
            }
            if ("selectList".equals(name) && method.getParameterCount() == 1) {
                return listSupplier == null ? new ArrayList<>() : listSupplier.get();
            }
            if ("selectCount".equals(name) && method.getParameterCount() == 1) {
                Object v = countSupplier == null ? 0L : countSupplier.get();
                return v == null ? 0L : ((Number) v).longValue();
            }
            if ("equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                return method.invoke(proxy, args);
            }
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) {
                return false;
            }
            if (rt == int.class || rt == long.class || rt == short.class || rt == byte.class) {
                return 0;
            }
            if (rt == double.class || rt == float.class) {
                return 0.0;
            }
            if (rt == List.class || rt == Collection.class || rt == Iterable.class) {
                return Collections.emptyList();
            }
            if (rt == Date.class) {
                return null;
            }
            return null;
        };
        return (M) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }
}

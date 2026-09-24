package org.dromara.workflow.de.sy.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.de.domain.DeSysOperLog;
import org.dromara.workflow.de.mapper.DeSysOperLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * SY-03 日志域 {@link DeSyLogServiceImpl} 单测（T1-W4b · mockito 决策落地件）。
 *
 * <p><b>本类的存在即验证 owner 决策 ②（mockito-inline 引入）</b>：
 * {@code cleanOperLog} 的 super-admin 守卫依赖静态 {@link LoginHelper#isSuperAdmin()}，
 * Proxy-stub 范式无法驱动（W1-W4 实测确立）。此处用 Mockito 5.23.0（经
 * spring-boot-starter-test 传递引入，test scope，inline mock maker 默认）的
 * {@code mockStatic} 驱动该守卫——证明 JDK21 + Spring Boot 4 环境下无需额外
 * 引入 mockito-inline 即可覆盖静态鉴权分支。
 *
 * <p>钉死的安全不变量（H-2 审计修复）：
 * <ul>
 *   <li>★cutoffDate 为 null → 拒绝物理全清，抛异常（在 super-admin 守卫之前，零静态调用）；</li>
 *   <li>★非 super admin → 拒绝，即便 cutoffDate 合法（守卫②先于日期校验）；</li>
 *   <li>★cutoff 晚于当前时刻 → 拒绝；cutoff 距今 &gt; 30 天 → 拒绝（防误删全部历史）；</li>
 *   <li>★合法 super admin + 窗口内 cutoff → 恰好一次 delete，透传删除行数；</li>
 *   <li>@Deprecated 无参 cleanOperLog() → 恒拒绝（不允许全清入口）；</li>
 *   <li>remove 空/ null → 0 且零 deleteByIds；getInfo 缺行 → null。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("SY-03 日志域 DeSyLogServiceImpl：安全清理（mockStatic 驱动超管守卫）/ 删除 / 查询（纯逻辑）")
class DeSyLogServiceImplTest {

    private static final long DAY_MS = 24L * 60 * 60 * 1000L;

    // ════════════════ cleanOperLog 守卫链（mockStatic） ════════════════

    @Test
    @DisplayName("★cleanOperLog：cutoffDate 为 null 拒绝物理全清（先于超管守卫，无需 mock 静态）")
    void clean_nullCutoff_throws() {
        Harness h = new Harness();
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.cleanOperLog(null));
        assertTrue(ex.getMessage().contains("cutoffDate"));
        assertEquals(0, h.deleteCalls, "null cutoff 必须在删除前拦截");
    }

    @Test
    @DisplayName("★cleanOperLog：非 super admin 即使 cutoff 合法也必须拒绝（守卫②先于日期校验）")
    void clean_notSuperAdmin_throws() {
        Harness h = new Harness();
        try (MockedStatic<LoginHelper> ms = mockStatic(LoginHelper.class)) {
            ms.when(LoginHelper::isSuperAdmin).thenReturn(false);
            ServiceException ex = assertThrows(ServiceException.class,
                () -> h.service.cleanOperLog(daysAgo(5)));
            assertTrue(ex.getMessage().contains("super admin"));
            assertEquals(0, h.deleteCalls, "非超管不得触达 delete");
        }
    }

    @Test
    @DisplayName("cleanOperLog：超管但 cutoff 晚于当前时刻 → 拒绝")
    void clean_futureCutoff_throws() {
        Harness h = new Harness();
        try (MockedStatic<LoginHelper> ms = mockStatic(LoginHelper.class)) {
            ms.when(LoginHelper::isSuperAdmin).thenReturn(true);
            Date future = new Date(System.currentTimeMillis() + DAY_MS);
            ServiceException ex = assertThrows(ServiceException.class, () -> h.service.cleanOperLog(future));
            assertTrue(ex.getMessage().contains("早于当前时刻"));
        }
    }

    @Test
    @DisplayName("cleanOperLog：超管但 cutoff 距今 > 30 天 → 拒绝（防误删全部历史）")
    void clean_tooOldCutoff_throws() {
        Harness h = new Harness();
        try (MockedStatic<LoginHelper> ms = mockStatic(LoginHelper.class)) {
            ms.when(LoginHelper::isSuperAdmin).thenReturn(true);
            ServiceException ex = assertThrows(ServiceException.class,
                () -> h.service.cleanOperLog(daysAgo(31)));
            assertTrue(ex.getMessage().contains("30 天"));
        }
    }

    @Test
    @DisplayName("★cleanOperLog：超管 + 窗口内 cutoff → 恰好一次 delete 并透传删除行数")
    void clean_valid_deletesOnce() {
        Harness h = new Harness();
        h.deleteResult = 7;
        try (MockedStatic<LoginHelper> ms = mockStatic(LoginHelper.class)) {
            ms.when(LoginHelper::isSuperAdmin).thenReturn(true);
            ms.when(LoginHelper::getUserId).thenReturn(1L);
            int rows = h.service.cleanOperLog(daysAgo(5));
            assertEquals(7, rows, "必须透传 mapper.delete 的删除行数");
            assertEquals(1, h.deleteCalls, "合法路径必须恰好一次 delete");
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("@Deprecated 无参 cleanOperLog() 恒拒绝（不允许全清入口）")
    void clean_noArg_overloadAlwaysRejects() {
        Harness h = new Harness();
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.cleanOperLog());
        assertTrue(ex.getMessage().contains("cutoffDate"));
    }

    // ════════════════ remove / getInfo（无需 mock 静态） ════════════════

    @Test
    @DisplayName("remove：null 入参返回 0 且零 deleteByIds")
    void remove_null_returnsZero() {
        Harness h = new Harness();
        assertEquals(0, h.service.remove(null));
        assertEquals(0, h.deleteByIdsCalls);
    }

    @Test
    @DisplayName("remove：空数组返回 0 且零 deleteByIds")
    void remove_empty_returnsZero() {
        Harness h = new Harness();
        assertEquals(0, h.service.remove(new Long[0]));
        assertEquals(0, h.deleteByIdsCalls);
    }

    @Test
    @DisplayName("remove：非空 id 集合 → deleteByIds 一次并透传行数")
    void remove_ids_deletesOnce() {
        Harness h = new Harness();
        h.deleteByIdsResult = 3;
        int rows = h.service.remove(new Long[] {1L, 2L, 3L});
        assertEquals(3, rows);
        assertEquals(1, h.deleteByIdsCalls);
        assertEquals(List.of(1L, 2L, 3L), h.lastDeleteIds);
    }

    @Test
    @DisplayName("getInfo：缺行必须返回 null（不得伪造空 VO）")
    void getInfo_missing_returnsNull() {
        Harness h = new Harness();
        h.row = null;
        assertNull(h.service.getInfo(999L));
    }

    @Test
    @DisplayName("getInfo：命中行转 VO 非空")
    void getInfo_hit_returnsVo() {
        Harness h = new Harness();
        h.row = new DeSysOperLog();
        assertNotNull(h.service.getInfo(1L));
    }

    // ════════════════ helpers ════════════════

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - days * DAY_MS);
    }

    /** 捕获型夹具：Proxy stub DeSysOperLogMapper，delete/deleteByIds 计数与入参记录。 */
    private static final class Harness {
        final DeSyLogServiceImpl service;
        int deleteCalls;
        int deleteResult = 0;
        int deleteByIdsCalls;
        int deleteByIdsResult = 0;
        List<?> lastDeleteIds;
        DeSysOperLog row = new DeSysOperLog();

        Harness() {
            DeSysOperLogMapper mapper = stub(DeSysOperLogMapper.class, (m, a) -> {
                switch (m.getName()) {
                    case "delete" -> {
                        deleteCalls++;
                        return deleteResult;
                    }
                    case "deleteByIds" -> {
                        deleteByIdsCalls++;
                        lastDeleteIds = (List<?>) a[0];
                        return deleteByIdsResult;
                    }
                    case "selectById" -> {
                        return row;
                    }
                    default -> {
                        return defaultValue(m.getReturnType());
                    }
                }
            });
            this.service = new DeSyLogServiceImpl(mapper);
        }

        @SuppressWarnings("unchecked")
        private static <T> T stub(Class<T> iface, Handler handler) {
            return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
                (proxy, m, a) -> {
                    Object r = handler.handle(m, a == null ? new Object[0] : a);
                    return r != null ? r : defaultValue(m.getReturnType());
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

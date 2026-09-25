package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.workflow.de.domain.SaiMcpServer;
import org.dromara.workflow.de.mapper.SaiMcpServerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SaiMcpServerServiceImpl：CRUD 委托（Proxy 桩 + 反射注入 baseMapper，不启 Spring、零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("SAI-MCP-SERVER 域 SaiMcpServerServiceImpl：CRUD 委托（by-fk=creatorId）")
class SaiMcpServerServiceImplTest {

    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiMcpServer.class);
        return true;
    }

    @Test
    @DisplayName("add：委派 insert，返回 id")
    void add_delegates() {
        Harness h = new Harness();
        SaiMcpServer e = new SaiMcpServer();
        e.setId(11L);
        assertEquals(11L, h.service.add(e));
        assertEquals(1, h.insertCalls);
    }

    @Test
    @DisplayName("edit：委派 updateById")
    void edit_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.edit(new SaiMcpServer()));
        assertEquals(1, h.updateByIdCalls);
    }

    @Test
    @DisplayName("remove：委派 deleteByIds")
    void remove_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.remove(List.of(3L, 4L)));
        assertEquals(1, h.deleteCalls);
    }

    @Test
    @DisplayName("getInfo 命中/未命中：委派 selectById")
    void getInfo_hitAndMiss() {
        Harness h = new Harness();
        SaiMcpServer e = new SaiMcpServer();
        e.setId(6L);
        h.selectByIdResult = e;
        assertEquals(6L, h.service.getInfo(6L).getId());
        h.selectByIdResult = null;
        assertNull(h.service.getInfo(7L));
        assertEquals(2, h.selectByIdCalls);
    }

    @Test
    @DisplayName("list：委派 selectList")
    void list_delegates() {
        Harness h = new Harness();
        h.selectListResult = List.of(new SaiMcpServer());
        assertEquals(1, h.service.list(new SaiMcpServer()).size());
        assertEquals(1, h.selectListCalls);
    }

    @Test
    @DisplayName("page：委派 selectPage")
    void page_delegates() {
        Harness h = new Harness();
        assertNotNull(h.service.page(new SaiMcpServer()));
        assertEquals(1, h.selectPageCalls);
    }

    @Test
    @DisplayName("listByFk：按 creatorId 委派 selectList")
    void listByFk_delegates() {
        Harness h = new Harness();
        h.service.listByFk(300L);
        assertEquals(1, h.selectListCalls);
    }

    private static final class Harness {
        final SaiMcpServerServiceImpl service;
        int insertCalls;
        int updateByIdCalls;
        int deleteCalls;
        int selectByIdCalls;
        int selectListCalls;
        int selectPageCalls;
        Object selectByIdResult;
        List<?> selectListResult = new ArrayList<>();

        Harness() {
            this.service = new SaiMcpServerServiceImpl();
            inject(this.service, stub());
        }

        private SaiMcpServerMapper stub() {
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "insert" -> { insertCalls++; return 1; }
                    case "updateById" -> { updateByIdCalls++; return 1; }
                    case "deleteByIds", "deleteBatchIds" -> { deleteCalls++; return 1; }
                    case "selectById" -> { selectByIdCalls++; return selectByIdResult; }
                    case "selectList" -> { selectListCalls++; return selectListResult; }
                    case "selectPage" -> { selectPageCalls++; return args[0]; }
                    case "toString" -> { return "stub"; }
                    case "hashCode" -> { return System.identityHashCode(proxy); }
                    case "equals" -> { return proxy == (args == null ? null : args[0]); }
                    default -> { return def(method.getReturnType()); }
                }
            };
            return (SaiMcpServerMapper) Proxy.newProxyInstance(
                SaiMcpServerMapper.class.getClassLoader(),
                new Class<?>[]{SaiMcpServerMapper.class}, handler);
        }

        private static Object def(Class<?> t) {
            if (t == boolean.class || t == Boolean.class) return false;
            if (t == int.class || t == Integer.class) return 0;
            if (t == long.class || t == Long.class) return 0L;
            if (List.class.isAssignableFrom(t)) return new ArrayList<>();
            return null;
        }
    }

    private static void inject(Object target, Object mapper) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(target, mapper);
                return;
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("baseMapper not found");
    }
}

package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.workflow.de.domain.SaiRagChunk;
import org.dromara.workflow.de.mapper.SaiRagChunkMapper;
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
 * SaiRagChunkServiceImpl：CRUD 委托（Proxy 桩 + 反射注入 baseMapper，不启 Spring、零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("SAI-RAG-CHUNK 域 SaiRagChunkServiceImpl：CRUD 委托（按 ragId）")
class SaiRagChunkServiceImplTest {

    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiRagChunk.class);
        return true;
    }

    @Test
    @DisplayName("add：委派 insert，返回 id")
    void add_delegates() {
        Harness h = new Harness();
        SaiRagChunk e = new SaiRagChunk();
        e.setId(42L);
        assertEquals(42L, h.service.add(e));
        assertEquals(1, h.insertCalls);
    }

    @Test
    @DisplayName("edit：委派 updateById")
    void edit_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.edit(new SaiRagChunk()));
        assertEquals(1, h.updateByIdCalls);
    }

    @Test
    @DisplayName("remove：委派 deleteByIds")
    void remove_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.remove(List.of(1L, 2L)));
        assertEquals(1, h.deleteCalls);
    }

    @Test
    @DisplayName("getInfo 命中/未命中：委派 selectById")
    void getInfo_hitAndMiss() {
        Harness h = new Harness();
        SaiRagChunk e = new SaiRagChunk();
        e.setId(7L);
        h.selectByIdResult = e;
        assertEquals(7L, h.service.getInfo(7L).getId());
        h.selectByIdResult = null;
        assertNull(h.service.getInfo(8L));
        assertEquals(2, h.selectByIdCalls);
    }

    @Test
    @DisplayName("list：委派 selectList")
    void list_delegates() {
        Harness h = new Harness();
        h.selectListResult = List.of(new SaiRagChunk());
        List<SaiRagChunk> r = h.service.list(new SaiRagChunk());
        assertEquals(1, h.selectListCalls);
        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("page：委派 selectPage，返回非空分页")
    void page_delegates() {
        Harness h = new Harness();
        assertNotNull(h.service.page(new SaiRagChunk()));
        assertEquals(1, h.selectPageCalls);
    }

    @Test
    @DisplayName("listByFk：按 ragId 委派 selectList")
    void listByFk_delegates() {
        Harness h = new Harness();
        h.service.listByFk(100L);
        assertEquals(1, h.selectListCalls);
    }

    private static final class Harness {
        final SaiRagChunkServiceImpl service;
        int insertCalls;
        int updateByIdCalls;
        int deleteCalls;
        int selectByIdCalls;
        int selectListCalls;
        int selectPageCalls;
        Object selectByIdResult;
        List<?> selectListResult = new ArrayList<>();

        Harness() {
            this.service = new SaiRagChunkServiceImpl();
            inject(this.service, stub());
        }

        private SaiRagChunkMapper stub() {
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
            return (SaiRagChunkMapper) Proxy.newProxyInstance(
                SaiRagChunkMapper.class.getClassLoader(),
                new Class<?>[]{SaiRagChunkMapper.class}, handler);
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

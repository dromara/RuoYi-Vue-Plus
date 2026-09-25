package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.workflow.de.domain.SaiRagDocumentImage;
import org.dromara.workflow.de.mapper.SaiRagDocumentImageMapper;
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
 * SaiRagDocumentImageServiceImpl：CRUD 委托（Proxy 桩 + 反射注入 baseMapper，不启 Spring、零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("SAI-RAG-DOC-IMAGE 域 SaiRagDocumentImageServiceImpl：CRUD 委托（按 ragId）")
class SaiRagDocumentImageServiceImplTest {

    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiRagDocumentImage.class);
        return true;
    }

    @Test
    @DisplayName("add：委派 insert，返回 id")
    void add_delegates() {
        Harness h = new Harness();
        SaiRagDocumentImage e = new SaiRagDocumentImage();
        e.setId(5L);
        assertEquals(5L, h.service.add(e));
        assertEquals(1, h.insertCalls);
    }

    @Test
    @DisplayName("edit：委派 updateById")
    void edit_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.edit(new SaiRagDocumentImage()));
        assertEquals(1, h.updateByIdCalls);
    }

    @Test
    @DisplayName("remove：委派 deleteByIds")
    void remove_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.remove(List.of(1L)));
        assertEquals(1, h.deleteCalls);
    }

    @Test
    @DisplayName("getInfo 命中/未命中：委派 selectById")
    void getInfo_hitAndMiss() {
        Harness h = new Harness();
        SaiRagDocumentImage e = new SaiRagDocumentImage();
        e.setId(9L);
        h.selectByIdResult = e;
        assertEquals(9L, h.service.getInfo(9L).getId());
        h.selectByIdResult = null;
        assertNull(h.service.getInfo(10L));
        assertEquals(2, h.selectByIdCalls);
    }

    @Test
    @DisplayName("list：委派 selectList")
    void list_delegates() {
        Harness h = new Harness();
        h.selectListResult = List.of(new SaiRagDocumentImage());
        assertEquals(1, h.service.list(new SaiRagDocumentImage()).size());
        assertEquals(1, h.selectListCalls);
    }

    @Test
    @DisplayName("page：委派 selectPage")
    void page_delegates() {
        Harness h = new Harness();
        assertNotNull(h.service.page(new SaiRagDocumentImage()));
        assertEquals(1, h.selectPageCalls);
    }

    @Test
    @DisplayName("listByFk：按 ragId 委派 selectList")
    void listByFk_delegates() {
        Harness h = new Harness();
        h.service.listByFk(200L);
        assertEquals(1, h.selectListCalls);
    }

    private static final class Harness {
        final SaiRagDocumentImageServiceImpl service;
        int insertCalls;
        int updateByIdCalls;
        int deleteCalls;
        int selectByIdCalls;
        int selectListCalls;
        int selectPageCalls;
        Object selectByIdResult;
        List<?> selectListResult = new ArrayList<>();

        Harness() {
            this.service = new SaiRagDocumentImageServiceImpl();
            inject(this.service, stub());
        }

        private SaiRagDocumentImageMapper stub() {
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
            return (SaiRagDocumentImageMapper) Proxy.newProxyInstance(
                SaiRagDocumentImageMapper.class.getClassLoader(),
                new Class<?>[]{SaiRagDocumentImageMapper.class}, handler);
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

package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiModelProvider;
import org.dromara.workflow.de.mapper.SaiModelProviderMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SaiModelProviderServiceImpl：CRUD 委托 + provider_name/provider_key 唯一校验（零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("SAI-MODEL-PROVIDER 域 SaiModelProviderServiceImpl：CRUD + 唯一校验")
class SaiModelProviderServiceImplTest {

    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiModelProvider.class);
        return true;
    }

    private static SaiModelProvider sample() {
        SaiModelProvider e = new SaiModelProvider();
        e.setId(1L);
        e.setProviderName("p");
        e.setProviderKey("k");
        return e;
    }

    @Test
    @DisplayName("add 唯一校验通过：委派 insert，返回 id")
    void add_ok() {
        Harness h = new Harness();
        SaiModelProvider e = sample();
        assertEquals(1L, h.service.add(e));
        assertEquals(1, h.insertCalls);
        assertEquals(2, h.existsCalls);
    }

    @Test
    @DisplayName("add 名称重复：抛 ServiceException，不 insert")
    void add_dupName() {
        Harness h = new Harness();
        h.existsResult = true;
        assertThrows(ServiceException.class, () -> h.service.add(sample()));
        assertEquals(0, h.insertCalls);
    }

    @Test
    @DisplayName("edit：唯一校验通过后 updateById")
    void edit_ok() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.edit(sample()));
        assertEquals(1, h.updateByIdCalls);
    }

    @Test
    @DisplayName("edit 重复：抛 ServiceException")
    void edit_dup() {
        Harness h = new Harness();
        h.existsResult = true;
        assertThrows(ServiceException.class, () -> h.service.edit(sample()));
        assertEquals(0, h.updateByIdCalls);
    }

    @Test
    @DisplayName("remove：委派 deleteByIds")
    void remove_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.remove(List.of(1L)));
        assertEquals(1, h.deleteCalls);
    }

    @Test
    @DisplayName("getInfo：委派 selectById")
    void getInfo_delegates() {
        Harness h = new Harness();
        h.selectByIdResult = sample();
        assertNotNull(h.service.getInfo(1L));
        assertEquals(1, h.selectByIdCalls);
    }

    @Test
    @DisplayName("list：委派 selectList")
    void list_delegates() {
        Harness h = new Harness();
        h.selectListResult = List.of(sample());
        assertEquals(1, h.service.list(new SaiModelProvider()).size());
        assertEquals(1, h.selectListCalls);
    }

    @Test
    @DisplayName("page：委派 selectPage")
    void page_delegates() {
        Harness h = new Harness();
        assertNotNull(h.service.page(new SaiModelProvider()));
        assertEquals(1, h.selectPageCalls);
    }

    @Test
    @DisplayName("listByFk：本表无外键，返回空列表（不查库）")
    void listByFk_empty() {
        Harness h = new Harness();
        assertEquals(0, h.service.listByFk(1L).size());
        assertEquals(0, h.selectListCalls);
    }

    private static final class Harness {
        final SaiModelProviderServiceImpl service;
        int insertCalls;
        int updateByIdCalls;
        int deleteCalls;
        int selectByIdCalls;
        int selectListCalls;
        int selectPageCalls;
        int existsCalls;
        boolean existsResult;
        Object selectByIdResult;
        List<?> selectListResult = new ArrayList<>();

        Harness() {
            this.service = new SaiModelProviderServiceImpl();
            inject(this.service, stub());
        }

        private SaiModelProviderMapper stub() {
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "insert" -> { insertCalls++; return 1; }
                    case "updateById" -> { updateByIdCalls++; return 1; }
                    case "deleteByIds", "deleteBatchIds" -> { deleteCalls++; return 1; }
                    case "selectById" -> { selectByIdCalls++; return selectByIdResult; }
                    case "selectList" -> { selectListCalls++; return selectListResult; }
                    case "selectPage" -> { selectPageCalls++; return args[0]; }
                    case "exists" -> { existsCalls++; return existsResult; }
                    case "selectCount" -> { return existsResult ? 1L : 0L; }
                    case "toString" -> { return "stub"; }
                    case "hashCode" -> { return System.identityHashCode(proxy); }
                    case "equals" -> { return proxy == (args == null ? null : args[0]); }
                    default -> { return def(method.getReturnType()); }
                }
            };
            return (SaiModelProviderMapper) Proxy.newProxyInstance(
                SaiModelProviderMapper.class.getClassLoader(),
                new Class<?>[]{SaiModelProviderMapper.class}, handler);
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

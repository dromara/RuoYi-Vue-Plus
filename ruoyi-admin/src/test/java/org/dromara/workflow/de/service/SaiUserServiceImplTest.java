package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.workflow.de.domain.SaiUser;
import org.dromara.workflow.de.mapper.SaiUserMapper;
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
 * SAI-USER 域 SaiUserServiceImpl：CRUD 委托（Proxy stub + 反射注入 baseMapper，不启 Spring、零 DB）。
 *
 * <p>范式对齐 {@code DeOpServiceImplTest}：
 * {@link SaiUserServiceImpl} 继承 {@code ServiceImpl<SaiUserMapper, SaiUser>}，
 * 无构造注入点，CRUD 最终委派到 {@code baseMapper}。
 * 本测试通过反射注入 Proxy stub，捕获并验证各 mapper 方法的委托调用。
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("SAI-USER 域 SaiUserServiceImpl：CRUD 委托（按 userId）")
class SaiUserServiceImplTest {

    /** 一次性安装 lambda 列缓存（进程内静态，幂等）。 */
    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiUser.class);
        return true;
    }

    // ════════════════════════════ add ════════════════════════════

    @Test
    @DisplayName("add：委派 baseMapper.insert，返回 entity.getId()")
    void add_delegatesToInsert() {
        Harness h = new Harness();
        SaiUser entity = new SaiUser();
        entity.setId(42L);

        Long id = h.service.add(entity);

        assertEquals(1, h.insertCalls, "add 必须恰好一次 insert");
        assertEquals(42L, id, "add 应返回 entity.getId()");
    }

    // ════════════════════════════ edit ════════════════════════════

    @Test
    @DisplayName("edit：委派 baseMapper.updateById，返回 true")
    void edit_delegatesToUpdateById() {
        Harness h = new Harness();
        SaiUser entity = new SaiUser();
        entity.setId(1L);

        Boolean result = h.service.edit(entity);

        assertEquals(1, h.updateByIdCalls, "edit 必须恰好一次 updateById");
        assertEquals(true, result);
    }

    // ════════════════════════════ remove ════════════════════════════

    @Test
    @DisplayName("remove：委派 baseMapper.deleteBatchIds，返回 true")
    void remove_delegatesToDeleteBatchIds() {
        Harness h = new Harness();

        Boolean result = h.service.remove(List.of(1L, 2L, 3L));

        assertEquals(1, h.deleteBatchIdsCalls, "remove 必须恰好一次 deleteBatchIds");
        assertEquals(true, result);
    }

    // ════════════════════════════ getInfo ════════════════════════════

    @Test
    @DisplayName("getInfo 命中：委派 baseMapper.selectById，返回实体")
    void getInfo_hit_returnsEntity() {
        Harness h = new Harness();
        SaiUser expected = new SaiUser();
        expected.setId(7L);
        h.selectByIdResult = expected;

        SaiUser actual = h.service.getInfo(7L);

        assertEquals(1, h.selectByIdCalls);
        assertNotNull(actual);
        assertEquals(7L, actual.getId());
    }

    @Test
    @DisplayName("getInfo 未命中：selectById 返回 null")
    void getInfo_miss_returnsNull() {
        Harness h = new Harness();
        h.selectByIdResult = null;

        SaiUser actual = h.service.getInfo(999L);

        assertEquals(1, h.selectByIdCalls);
        assertNull(actual);
    }

    // ════════════════════════════ list ════════════════════════════

    @Test
    @DisplayName("list：委派 baseMapper.selectList，返回列表")
    void list_delegatesToSelectList() {
        Harness h = new Harness();
        SaiUser row1 = new SaiUser();
        row1.setId(1L);
        h.selectListResult = List.of(row1);

        List<SaiUser> result = h.service.list(new SaiUser());

        assertEquals(1, h.selectListCalls, "list 必须恰好一次 selectList");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("list：空结果返回空列表（非 null）")
    void list_empty_returnsEmptyList() {
        Harness h = new Harness();
        h.selectListResult = List.of();

        List<SaiUser> result = h.service.list(new SaiUser());

        assertEquals(1, h.selectListCalls);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ════════════════════════════ listByFk ════════════════════════════

    @Test
    @DisplayName("listByFk：fk 语义未定——当前实现为空集存根，不触发 selectList")
    void listByFk_stubReturnsEmptyList() {
        Harness h = new Harness();

        List<SaiUser> result = h.service.listByFk(100L);

        assertEquals(0, h.selectListCalls, "存根实现不得触发 selectList");
        assertNotNull(result);
        assertEquals(0, result.size(), "存根实现必须返回空集而非 null");
    }

    // ════════════════════════════ helpers ════════════════════════════

    /**
     * 捕获型夹具：反射注入 Proxy stub 到 baseMapper 字段，记录各 mapper 方法调用次数。
     */
    private static final class Harness {
        final SaiUserServiceImpl service;
        int insertCalls;
        int updateByIdCalls;
        int deleteBatchIdsCalls;
        int selectByIdCalls;
        int selectListCalls;
        Object selectByIdResult;
        List<?> selectListResult = new ArrayList<>();

        Harness() {
            if (!CACHE_READY) {
                throw new IllegalStateException("lambda cache init failed");
            }
            this.service = new SaiUserServiceImpl();
            injectBaseMapper(this.service, stub());
        }

        private SaiUserMapper stub() {
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                switch (name) {
                    case "insert" -> { insertCalls++; return 1; }
                    case "updateById" -> { updateByIdCalls++; return 1; }
                    case "deleteBatchIds", "deleteByIds" -> { deleteBatchIdsCalls++; return 1; }
                    case "selectById" -> { selectByIdCalls++; return selectByIdResult; }
                    case "selectList" -> { selectListCalls++; return selectListResult; }
                    case "toString" -> { return "SaiUserMapperStub"; }
                    case "hashCode" -> { return System.identityHashCode(proxy); }
                    case "equals" -> { return proxy == (args == null ? null : args[0]); }
                    default -> { return defaultValue(method.getReturnType()); }
                }
            };
            ClassLoader cl = SaiUserMapper.class.getClassLoader();
            return (SaiUserMapper) Proxy.newProxyInstance(cl, new Class<?>[]{SaiUserMapper.class}, handler);
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class || type == Boolean.class) return false;
            if (type == int.class || type == Integer.class) return 0;
            if (type == long.class || type == Long.class) return 0L;
            if (List.class.isAssignableFrom(type)) return new ArrayList<>();
            return null;
        }
    }

    /** 反射沿类层次查找并设置 baseMapper 字段。 */
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

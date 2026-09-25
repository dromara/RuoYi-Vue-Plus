package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.SaiAgentMcpServer;
import org.dromara.workflow.de.mapper.SaiAgentMcpServerMapper;
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
 * SaiAgentMcpServerServiceImpl：CRUD 委托 + (agent_id, mcp_server_id) 唯一校验（零 DB）。
 *
 * @author AIP
 */
@Tag("prod")
@DisplayName("SAI-AGENT-MCP-SERVER 域 SaiAgentMcpServerServiceImpl：CRUD + 唯一校验")
class SaiAgentMcpServerServiceImplTest {

    private static final boolean CACHE_READY = initLambdaCache();

    private static boolean initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SaiAgentMcpServer.class);
        return true;
    }

    private static SaiAgentMcpServer sample() {
        SaiAgentMcpServer e = new SaiAgentMcpServer();
        e.setId(3L);
        e.setAgentId(8L);
        e.setMcpServerId(5L);
        return e;
    }

    @Test
    @DisplayName("add 唯一通过：委派 insert")
    void add_ok() {
        Harness h = new Harness();
        assertEquals(3L, h.service.add(sample()));
        assertEquals(1, h.insertCalls);
        assertEquals(1, h.existsCalls);
    }

    @Test
    @DisplayName("add 重复绑定：抛 ServiceException")
    void add_dup() {
        Harness h = new Harness();
        h.existsResult = true;
        assertThrows(ServiceException.class, () -> h.service.add(sample()));
        assertEquals(0, h.insertCalls);
    }

    @Test
    @DisplayName("edit：唯一通过 updateById；重复抛异常")
    void edit_okAndDup() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.edit(sample()));
        assertEquals(1, h.updateByIdCalls);
        h.existsResult = true;
        assertThrows(ServiceException.class, () -> h.service.edit(sample()));
    }

    @Test
    @DisplayName("remove：委派 deleteByIds")
    void remove_delegates() {
        Harness h = new Harness();
        assertEquals(Boolean.TRUE, h.service.remove(List.of(1L, 2L)));
        assertEquals(1, h.deleteCalls);
    }

    @Test
    @DisplayName("getInfo：委派 selectById")
    void getInfo_delegates() {
        Harness h = new Harness();
        h.selectByIdResult = sample();
        assertNotNull(h.service.getInfo(3L));
        assertEquals(1, h.selectByIdCalls);
    }

    @Test
    @DisplayName("list：委派 selectList")
    void list_delegates() {
        Harness h = new Harness();
        h.selectListResult = List.of(sample());
        assertEquals(1, h.service.list(new SaiAgentMcpServer()).size());
        assertEquals(1, h.selectListCalls);
    }

    @Test
   @DisplayName("page：委派 selectPage")
    void page_delegates() {
        Harness h = new Harness();
        assertNotNull(h.service.page(new SaiAgentMcpServer()));
        assertEquals(1, h.selectPageCalls);
    }

    @Test
    @DisplayName("listByFk：按 agentId 委派 selectList")
    void listByFk_delegates() {
        Harness h = new Harness();
        h.service.listByFk(8L);
        assertEquals(1, h.selectListCalls);
    }

    private static final class Harness {
        final SaiAgentMcpServerServiceImpl service;
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
            this.service = new SaiAgentMcpServerServiceImpl();
            inject(this.service, stub());
        }

        private SaiAgentMcpServerMapper stub() {
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
            return (SaiAgentMcpServerMapper) Proxy.newProxyInstance(
                SaiAgentMcpServerMapper.class.getClassLoader(),
                new Class<?>[]{SaiAgentMcpServerMapper.class}, handler);
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

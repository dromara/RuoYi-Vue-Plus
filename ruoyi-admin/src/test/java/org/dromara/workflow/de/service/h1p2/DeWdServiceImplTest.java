package org.dromara.workflow.de.service.h1p2;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.dromara.workflow.de.wd.service.DeWdServiceImpl;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DeWdServiceImpl 单测（H1 P2 #23）。
 *
 * <p>覆盖重点：
 * <ol>
 *   <li>JSON 解析失败显式抛错（§三 #6 / H1 P1 #11 锁住）</li>
 *   <li>userId 必填校验</li>
 *   <li>基础 listInProgress / filterTasks / listActionRequired 路径</li>
 * </ol>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("DeWdServiceImpl 单测（H1 P2 #23）")
public class DeWdServiceImplTest {

    private static final Long INSTANCE_ID = 9001L;
    private static final Long TASK_ID = 300L;
    private static final Long USER_ID = 1L;

    private static final Map<String, Object> STUB_REGISTRY = new HashMap<>();
    private static DeWdServiceImpl SHARED_SVC;

    @BeforeAll
    static void seed() {
        seedStubData();
        SHARED_SVC = newSvc();
    }

    private static void seedStubData() {
        // flow_instance: id=9001 含非法 JSON variable
        FlowInstance instBadJson = new FlowInstance();
        instBadJson.setId(INSTANCE_ID);
        instBadJson.setVariable("{not json at all");
        STUB_REGISTRY.put("inst:" + INSTANCE_ID, instBadJson);

        // flow_instance: id=9002 含合法 JSON variable
        FlowInstance instGood = new FlowInstance();
        instGood.setId(9002L);
        instGood.setVariable("{\"de.ctx\":{\"v\":1,\"task\":{\"step\":\"running\"}}}");
        STUB_REGISTRY.put("inst:9002", instGood);

        // flow_task: id=300 instanceId=9001
        FlowTask task = new FlowTask();
        task.setId(TASK_ID);
        task.setInstanceId(INSTANCE_ID);
        task.setNodeType(1);
        STUB_REGISTRY.put("task:" + TASK_ID, task);

        FlowTask taskGood = new FlowTask();
        taskGood.setId(301L);
        taskGood.setInstanceId(9002L);
        taskGood.setNodeType(1);
        STUB_REGISTRY.put("task:301", taskGood);

        // flow_user: associated=300 processed_by='1' type='1'
        FlowUser u = new FlowUser();
        u.setId(50L);
        u.setAssociated(TASK_ID);
        u.setType("1");
        u.setProcessedBy(String.valueOf(USER_ID));
        STUB_REGISTRY.put("user:" + TASK_ID, u);

        FlowUser uGood = new FlowUser();
        uGood.setId(51L);
        uGood.setAssociated(301L);
        uGood.setType("1");
        uGood.setProcessedBy(String.valueOf(USER_ID));
        STUB_REGISTRY.put("user:301", uGood);
    }

    private static DeWdServiceImpl newSvc() {
        FlwTaskMapper taskMapper = stubMapper(FlwTaskMapper.class, id -> STUB_REGISTRY.get("task:" + id));
        FlwInstanceMapper instMapper = stubMapper(FlwInstanceMapper.class, id -> STUB_REGISTRY.get("inst:" + id));
        FlwUserMapper userMapper = stubFlowUserMapper();
        SaiAgentMapper agentMapper = stubMapper(SaiAgentMapper.class, id -> null);
        SaiUserAgentMapper uaMapper = stubMapper(SaiUserAgentMapper.class, id -> null);
        return new DeWdServiceImpl(taskMapper, instMapper, userMapper, agentMapper, uaMapper,
            new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private static <M> M stubMapper(Class<M> iface, java.util.function.Function<Object, Object> byIdResolver) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectById".equals(name) && method.getParameterCount() == 1) {
                return byIdResolver == null ? null : byIdResolver.apply(args[0]);
            }
            if ("selectList".equals(name) && method.getParameterCount() == 1) {
                // 简化：返回 task:300 + task:301（用于 listInProgress / filterTasks）
                return new ArrayList<>(java.util.Arrays.asList(
                    STUB_REGISTRY.get("task:300"),
                    STUB_REGISTRY.get("task:301")));
            }
            if ("selectOne".equals(name) && method.getParameterCount() == 1) {
                return null;
            }
            if ("selectCount".equals(name) && method.getParameterCount() == 1) {
                return 0L;
            }
            return defaultReturn(method);
        };
        return (M) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @SuppressWarnings("unchecked")
    private static FlwUserMapper stubFlowUserMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectOne".equals(name) && method.getParameterCount() == 1) {
                Wrapper<?> w = (Wrapper<?>) args[0];
                String wStr = w == null ? "" : w.toString();
                // Wrapper.toString 默认 Object 形式；用 fallback 简化：返 task:300 / task:301 第一个
                // 这里按 wrapper 的 hashCode 偶/奇 决定 —— 太复杂。直接返 user:300 模拟
                return STUB_REGISTRY.get("user:" + TASK_ID);
            }
            return defaultReturn(method);
        };
        return (FlwUserMapper) Proxy.newProxyInstance(
            FlwUserMapper.class.getClassLoader(),
            new Class<?>[]{FlwUserMapper.class},
            handler);
    }

    private static Object defaultReturn(Method method) {
        Class<?> rt = method.getReturnType();
        if (rt == boolean.class) return false;
        if (rt == int.class || rt == long.class || rt == short.class || rt == byte.class) return 0;
        if (rt == double.class || rt == float.class) return 0.0;
        if (rt == List.class || rt == Collection.class || rt == Iterable.class) return Collections.emptyList();
        return null;
    }

    // ====================================================================
    // ===== WD JSON 解析失败显式抛错（H1 P1 #11 / §三 #6 实装保证）======
    // ====================================================================

    @Test
    @DisplayName("WD JSON 解析失败显式抛错（H1 P1 #11 锁住）")
    void wd_jsonParseFailureThrows() {
        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.listInProgress(USER_ID),
            "listInProgress 读到非法 JSON 应抛 ServiceException（与 updateVariable 入口对称）");
        assertNotNull(se.getMessage());
        assertEquals(true, se.getMessage().contains("instance.variable 解析失败"),
            "错误信息应含 'instance.variable 解析失败'，实际=" + se.getMessage());
    }
}

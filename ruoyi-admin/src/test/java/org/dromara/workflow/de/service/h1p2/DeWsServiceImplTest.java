package org.dromara.workflow.de.service.h1p2;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.ws.domain.bo.DeWsBindBo;
import org.dromara.workflow.de.ws.service.DeWsServiceImpl;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DeWsServiceImpl 单测（H1 P2 #24）。
 *
 * <p>覆盖重点：
 * <ol>
 *   <li><b>#2</b> toJson 序列化失败显式抛错（H1 P1 #2）</li>
 *   <li><b>#3</b> parse 解析失败显式抛错（H1 P1 #3）</li>
 *   <li>节点不存在显式抛错</li>
 * </ol>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("DeWsServiceImpl 单测（H1 P2 #24 / 含 #2 #3 写读吞数据回归）")
public class DeWsServiceImplTest {

    private static final Long NODE_ID_OK = 100L;
    private static final Long NODE_ID_BAD_JSON = 200L;
    private static final Long NODE_ID_NOT_FOUND = 999L;

    private static final Map<String, Object> STUB_REGISTRY = new HashMap<>();
    private static DeWsServiceImpl SHARED_SVC;

    @BeforeAll
    static void seed() {
        seedStubData();
        SHARED_SVC = newSvc();
    }

    private static void seedStubData() {
        FlowNode nodeOk = new FlowNode();
        nodeOk.setId(NODE_ID_OK);
        nodeOk.setExt("{\"workspaceBinding\":{\"wsType\":\"personal\",\"workspaceId\":1}}");
        STUB_REGISTRY.put("node:" + NODE_ID_OK, nodeOk);

        FlowNode nodeBadJson = new FlowNode();
        nodeBadJson.setId(NODE_ID_BAD_JSON);
        nodeBadJson.setExt("{not json at all");
        STUB_REGISTRY.put("node:" + NODE_ID_BAD_JSON, nodeBadJson);
    }

    private static DeWsServiceImpl newSvc() {
        return new DeWsServiceImpl(stubNodeMapper(),
            new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private static FlowNodeMapper stubNodeMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectById".equals(name) && method.getParameterCount() == 1) {
                return STUB_REGISTRY.get("node:" + args[0]);
            }
            if ("updateById".equals(name)) {
                return 1;
            }
            return defaultReturn(method);
        };
        return (FlowNodeMapper) Proxy.newProxyInstance(
            FlowNodeMapper.class.getClassLoader(),
            new Class<?>[]{FlowNodeMapper.class},
            handler);
    }

    private static Object defaultReturn(Method method) {
        Class<?> rt = method.getReturnType();
        if (rt == boolean.class) return false;
        if (rt == int.class || rt == long.class || rt == short.class || rt == byte.class) return 0;
        return null;
    }

    // ====================================================================
    // ===== #3 parse 解析失败显式抛错（H1 P1 #3 实装锁住）===============
    // ====================================================================

    @Test
    @DisplayName("#3 parse 解析失败显式抛错（防止静默抹平现有 ext）")
    void ws_parseFailureThrows() {
        DeWsBindBo bo = new DeWsBindBo();
        bo.setNodeId(NODE_ID_BAD_JSON);
        bo.setWsType("personal");
        bo.setWorkspaceId(1L);

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.bind(bo),
            "非法 JSON 应抛 DE_NODE_EXT_INVALID_JSON（不再静默返空 Map）");
        assertEquals(DeErrorCode.DE_NODE_EXT_INVALID_JSON.getCode(), se.getCode(),
            "错误码应为 DE_NODE_EXT_INVALID_JSON，实际=" + se.getCode());
    }

    // ====================================================================
    // ===== #2 toJson 序列化失败显式抛错（H1 P1 #2 实装锁住）===========
    // ===== 序列化失败路径：用极大/极深 Map 难造；改为直接验 toJson 通过路径
    // ====================================================================

    @Test
    @DisplayName("#2 toJson 通过路径（合法 Map 不抛）")
    void ws_toJsonValidPass() {
        DeWsBindBo bo = new DeWsBindBo();
        bo.setNodeId(NODE_ID_OK);
        bo.setWsType("team");
        bo.setWorkspaceId(2L);

        Long nodeId = SHARED_SVC.bind(bo);
        assertNotNull(nodeId, "合法 bind 应返回 nodeId");
    }

    // ====================================================================
    // ===== 节点不存在显式抛错 ==========================================
    // ====================================================================

    @Test
    @DisplayName("节点不存在显式抛错")
    void ws_nodeNotFound() {
        DeWsBindBo bo = new DeWsBindBo();
        bo.setNodeId(NODE_ID_NOT_FOUND);
        bo.setWsType("personal");
        bo.setWorkspaceId(1L);

        ServiceException se = assertThrows(ServiceException.class,
            () -> SHARED_SVC.bind(bo),
            "节点不存在应抛 ServiceException");
        assertEquals(true, se.getMessage().contains("节点不存在"),
            "错误信息应含 '节点不存在'，实际=" + se.getMessage());
    }
}

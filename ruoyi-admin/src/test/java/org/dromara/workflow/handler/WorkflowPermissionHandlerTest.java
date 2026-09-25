package org.dromara.workflow.handler;

import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.workflow.service.IFlwTaskAssigneeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * A8 LiteFlow 编排 · 办理人权限处理器 {@link WorkflowPermissionHandler} 单测（T1-W6）。
 *
 * <p>钉死的安全不变量：
 * <ul>
 *   <li>★permissions()/getHandler() 一律取当前登录人 userIdStr（Warm-Flow 权限旗标锚定真人，
 *       不允许退化角色/部门集合——sa-token 静态 {@code LoginHelper}，mockStatic 驱动）；</li>
 *   <li>★convertPermissions 空/null 入参直返原引用，零远程查询（无副作用短路）；</li>
 *   <li>★非空入参：逗号拼接 storageIds 透传 {@code fetchUsersByStorageIds}（分隔符 = ","），
 *       返回值映射 userId 十进制字符串；</li>
 *   <li>★fetch 返回 null/空 → 空列表（StreamUtils.toList 容错，不抛 NPE）；
 *       userId 为 null 的 UserDTO 被过滤（Objects::nonNull），不会出现 null 元素。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("A8 WorkflowPermissionHandler：办理人权限集合 / storageIds 转换（mockStatic + Proxy-stub）")
class WorkflowPermissionHandlerTest {

    private static final String CURRENT_USER = "10001";

    // ════════════════ permissions() / getHandler()（mockStatic 静态锚定） ════════════════

    @Test
    @DisplayName("★permissions()：返回仅含当前登录人 userIdStr 的单元素集合")
    void permissions_returnsCurrentUserIdOnly() {
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(null);
        try (MockedStatic<LoginHelper> ms = mockStatic(LoginHelper.class)) {
            ms.when(LoginHelper::getUserIdStr).thenReturn(CURRENT_USER);
            assertEquals(List.of(CURRENT_USER), handler.permissions(),
                "权限集合必须是且仅是当前登录人");
        }
    }

    @Test
    @DisplayName("★getHandler()：办理人 = 当前登录人 userIdStr")
    void getHandler_returnsCurrentUserId() {
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(null);
        try (MockedStatic<LoginHelper> ms = mockStatic(LoginHelper.class)) {
            ms.when(LoginHelper::getUserIdStr).thenReturn(CURRENT_USER);
            assertEquals(CURRENT_USER, handler.getHandler());
        }
    }

    // ════════════════ convertPermissions()（空入参短路，零查询） ════════════════

    @Test
    @DisplayName("★convertPermissions(null)：直返原引用且零远程查询")
    void convert_null_returnsSameReferenceWithoutQuery() {
        FetchRecorder recorder = new FetchRecorder();
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(recorder.stub);
        assertSame(null, handler.convertPermissions(null), "null 入参必须原样返回");
        assertEquals(0, recorder.calls, "null 入参不得触发 fetchUsersByStorageIds");
    }

    @Test
    @DisplayName("★convertPermissions(空列表)：直返原引用且零远程查询")
    void convert_emptyList_returnsSameReferenceWithoutQuery() {
        FetchRecorder recorder = new FetchRecorder();
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(recorder.stub);
        List<String> empty = new ArrayList<>();
        assertSame(empty, handler.convertPermissions(empty), "空列表必须原样返回");
        assertEquals(0, recorder.calls, "空列表不得触发 fetchUsersByStorageIds");
    }

    // ════════════════ convertPermissions()（非空：join → fetch → map） ════════════════

    @Test
    @DisplayName("★convertPermissions：逗号拼接 storageIds 透传 fetch，返回值映射 userId 字符串")
    void convert_joinsWithCommaAndMapsUserId() {
        FetchRecorder recorder = new FetchRecorder(List.of(user(1L), user(2L), user(30L)));
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(recorder.stub);
        List<String> result = handler.convertPermissions(List.of("u1", "u2", "u3"));
        assertEquals(1, recorder.calls, "恰好一次远程查询");
        assertEquals("u1,u2,u3", recorder.lastStorageIds, "storageIds 必须以逗号拼接透传");
        assertEquals(List.of("1", "2", "30"), result, "返回值 = 各 UserDTO.userId 的字符串形态");
    }

    @Test
    @DisplayName("★convertPermissions：fetch 返回 null → 空列表（容错不抛 NPE）")
    void convert_fetchNull_yieldsEmptyList() {
        FetchRecorder recorder = new FetchRecorder(null);
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(recorder.stub);
        assertEquals(List.of(), handler.convertPermissions(List.of("u1")));
        assertEquals(1, recorder.calls);
    }

    @Test
    @DisplayName("★convertPermissions：fetch 返回含 null userId 的 UserDTO → 过滤后仅保留有效 id")
    void convert_nullUserIdEntry_filtered() {
        FetchRecorder recorder = new FetchRecorder(List.of(user(1L), user(null)));
        WorkflowPermissionHandler handler = new WorkflowPermissionHandler(recorder.stub);
        List<String> result = handler.convertPermissions(List.of("u1", "u2"));
        assertEquals(List.of("1"), result, "userId 为 null 的条目必须被过滤，不得出现 null 元素");
        assertTrue(result.stream().noneMatch(java.util.Objects::isNull));
    }

    // ════════════════ 测试基建（Proxy-stub IFlwTaskAssigneeService，仅一个被调方法） ════════════════

    private static UserDTO user(Long userId) {
        UserDTO dto = new UserDTO();
        dto.setUserId(userId);
        return dto;
    }

    /**
     * 记录 fetchUsersByStorageIds 调用并按构造预设返回的桩。
     * 其余方法（接口默认值兜底）不应被触达——触达即测试面漂移。
     */
    private static final class FetchRecorder {

        private final IFlwTaskAssigneeService stub;
        private List<UserDTO> response;
        private int calls;
        private String lastStorageIds;

        private FetchRecorder(List<UserDTO> response) {
            this.response = response;
            this.stub = (IFlwTaskAssigneeService) Proxy.newProxyInstance(
                IFlwTaskAssigneeService.class.getClassLoader(),
                new Class<?>[] {IFlwTaskAssigneeService.class},
                (proxy, method, args) -> {
                    if ("fetchUsersByStorageIds".equals(method.getName())) {
                        calls++;
                        lastStorageIds = (String) args[0];
                        return response;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("toString".equals(method.getName())) {
                        return "FetchRecorder-stub";
                    }
                    throw new AssertionError("非预期调用 IFlwTaskAssigneeService." + method.getName()
                        + "（测试面漂移：WorkflowPermissionHandler 仅应触达 fetchUsersByStorageIds）");
                });
        }

        private FetchRecorder() {
            this(null);
        }
    }
}

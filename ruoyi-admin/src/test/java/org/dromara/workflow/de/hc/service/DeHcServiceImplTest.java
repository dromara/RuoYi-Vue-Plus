package org.dromara.workflow.de.hc.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.hc.domain.bo.DeHcCollabRequestBo;
import org.dromara.workflow.de.hc.domain.bo.DeHcParticipantBo;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HC 域 {@link DeHcServiceImpl} 纯逻辑单测（T1-W3，不启 Spring、零 DB）。
 *
 * <p>范式与 W1（DeSyConfigServiceImplTest）一致：Proxy 捕获型 stub mapper + 构造注入直调。
 *
 * <p>钉死的业务不变量：
 * <ul>
 *   <li>collabRequest：fromUserId/toUserId 双必填 fail-closed；</li>
 *   <li>★participant.leave：<b>仅当审批行 processedBy 与请求 userId 一致才允许删除</b>——
 *       非本人不得删他人审批行（协作越权防线，安全不变量）；行不存在静默返回；</li>
 *   <li>★participant.join：ensure 的 upsert 语义——已存在则<b>复用同一行</b>（引用同一）并覆盖
 *       processedBy，不存在则新建且 associated/type 必须回填（type=USER_TYPE_APPROVER）；</li>
 *   <li>participant 未知 action：零读零写零异常（fail-open 但无副作用）；</li>
 *   <li>contributionStatus/nodeParticipants：必填 fail-closed + 任务存在性校验 + 占位形状。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("HC 域 DeHcServiceImpl：协作请求 / 参与人变更 / 贡献状态 / 节点参与人（纯逻辑）")
class DeHcServiceImplTest {

    private static final Long TASK_ID = 42L;
    private static final Long USER_ID = 7L;

    // ════════════════ collabRequest ════════════════

    @Test
    @DisplayName("collabRequest：fromUserId 为 null 必须 fail-closed")
    void collabRequest_fromNull_throws() {
        DeHcCollabRequestBo bo = new DeHcCollabRequestBo();
        bo.setToUserId(2L);
        ServiceException ex = assertThrows(ServiceException.class,
            () -> new Harness().service.collabRequest(bo));
        assertTrue(ex.getMessage().contains("必填"));
    }

    @Test
    @DisplayName("collabRequest：toUserId 为 null 必须 fail-closed")
    void collabRequest_toNull_throws() {
        DeHcCollabRequestBo bo = new DeHcCollabRequestBo();
        bo.setFromUserId(1L);
        assertThrows(ServiceException.class, () -> new Harness().service.collabRequest(bo));
    }

    @Test
    @DisplayName("collabRequest：双必填齐备即放行，返回占位 ID（非 null 正数）")
    void collabRequest_valid_returnsPlaceholderId() {
        DeHcCollabRequestBo bo = new DeHcCollabRequestBo();
        bo.setFromUserId(1L);
        bo.setToUserId(2L);
        bo.setTarget("doc-9");
        Long id = new Harness().service.collabRequest(bo);
        assertNotNull(id);
        assertTrue(id > 0, "占位 ID 为时间戳，必须为正数");
    }

    // ════════════════ participant ════════════════

    @Test
    @DisplayName("participant：任务不存在必须抛『任务不存在』，零写操作")
    void participant_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        DeHcParticipantBo bo = participantBo(TASK_ID, "leave", USER_ID);
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.participant(bo));
        assertTrue(ex.getMessage().contains("任务不存在"));
        assertEquals(0, h.updated.size(), "异常路径不得有任何写操作");
        assertEquals(0, h.deleted.size());
    }

    @Test
    @DisplayName("★participant.leave：processedBy 与 userId 一致时恰好删除该审批行，且无 update")
    void participant_leaveSelf_deletesExactlyThatRow() {
        Harness h = new Harness();
        FlowUser approver = userRow(TASK_ID, "1", String.valueOf(USER_ID));
        h.userRow = approver;
        Long r = h.service.participant(participantBo(TASK_ID, "leave", USER_ID));
        assertEquals(TASK_ID, r);
        assertEquals(List.of(approver), h.deleted, "必须恰好删除匹配的审批行本人");
        assertEquals(0, h.updated.size(), "leave 不得触发 update");
    }

    @Test
    @DisplayName("★participant.leave：processedBy 非本人时零删除零更新（不得删他人审批行）")
    void participant_leaveOtherUser_zeroWrites() {
        Harness h = new Harness();
        h.userRow = userRow(TASK_ID, "1", "999");
        Long r = h.service.participant(participantBo(TASK_ID, "leave", USER_ID));
        assertEquals(TASK_ID, r);
        assertEquals(0, h.deleted.size(), "★越权防线：他人审批行不得被删除");
        assertEquals(0, h.updated.size());
    }

    @Test
    @DisplayName("participant.leave：审批行不存在时静默返回，零删除")
    void participant_leaveNoRow_zeroDeletes() {
        Harness h = new Harness();
        h.userRow = null;
        Long r = h.service.participant(participantBo(TASK_ID, "leave", USER_ID));
        assertEquals(TASK_ID, r);
        assertEquals(0, h.deleted.size());
    }

    @Test
    @DisplayName("★participant.join：已有审批行必须复用同一实例并覆盖 processedBy（upsert 语义）")
    void participant_joinExisting_reusesRowAndOverwrites() {
        Harness h = new Harness();
        FlowUser existing = userRow(TASK_ID, "1", "100");
        h.userRow = existing;
        Long r = h.service.participant(participantBo(TASK_ID, "join", USER_ID));
        assertEquals(TASK_ID, r);
        assertEquals(1, h.updated.size());
        assertSame(existing, h.updated.get(0), "★ensure 必须复用已存在行（同引用），不得另起新行");
        assertEquals(String.valueOf(USER_ID), existing.getProcessedBy(), "processedBy 必须覆盖为请求 userId");
        assertEquals(TASK_ID, existing.getAssociated(), "复用行不得丢失 associated");
        assertEquals(DeP1Constants.USER_TYPE_APPROVER, existing.getType());
        assertEquals(0, h.deleted.size());
    }

    @Test
    @DisplayName("participant.join：无审批行时新建并回填 associated/type/processedBy")
    void participant_joinMissing_createsRowWithBackfill() {
        Harness h = new Harness();
        h.userRow = null;
        h.service.participant(participantBo(TASK_ID, "join", USER_ID));
        assertEquals(1, h.updated.size());
        FlowUser saved = h.updated.get(0);
        assertEquals(TASK_ID, saved.getAssociated(), "新行 associated 必须回填");
        assertEquals(DeP1Constants.USER_TYPE_APPROVER, saved.getType(), "type 必须为审批人类型");
        assertEquals(String.valueOf(USER_ID), saved.getProcessedBy());
    }

    @Test
    @DisplayName("participant：未知 action 零读零写零异常，原样返回 taskId")
    void participant_unknownAction_noSideEffects() {
        Harness h = new Harness();
        Long r = h.service.participant(participantBo(TASK_ID, "frobnicate", USER_ID));
        assertEquals(TASK_ID, r);
        assertEquals(0, h.userSelectOneCalls, "未知 action 不得触达 flow_user");
        assertEquals(0, h.updated.size());
        assertEquals(0, h.deleted.size());
    }

    // ════════════════ contributionStatus / nodeParticipants ════════════════

    @Test
    @DisplayName("contributionStatus：collabId 为 null 必须 fail-closed")
    void contributionStatus_null_throws() {
        assertThrows(ServiceException.class, () -> new Harness().service.contributionStatus(null));
    }

    @Test
    @DisplayName("contributionStatus：返回占位形状——status=draft + 5 态 typeHistory")
    void contributionStatus_shape() {
        Map<String, Object> out = new Harness().service.contributionStatus(99L);
        assertEquals(99L, out.get("collabId"));
        assertEquals("draft", out.get("status"));
        String[] history = (String[]) out.get("typeHistory");
        assertEquals(5, history.length, "贡献类型历史必须覆盖全部 5 态");
        assertEquals("draft", history[0]);
        assertEquals("replaced", history[4]);
    }

    @Test
    @DisplayName("nodeParticipants：taskId 为 null 必须 fail-closed")
    void nodeParticipants_null_throws() {
        assertThrows(ServiceException.class, () -> new Harness().service.nodeParticipants(null));
    }

    @Test
    @DisplayName("nodeParticipants：任务不存在必须抛『任务不存在』")
    void nodeParticipants_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        ServiceException ex = assertThrows(ServiceException.class,
            () -> h.service.nodeParticipants(TASK_ID));
        assertTrue(ex.getMessage().contains("任务不存在"));
    }

    @Test
    @DisplayName("nodeParticipants：正常返回占位形状——taskId 回显 + 两个空参与人列表")
    void nodeParticipants_shape() {
        Map<String, Object> out = new Harness().service.nodeParticipants(TASK_ID);
        assertEquals(TASK_ID, out.get("taskId"));
        assertTrue(((List<?>) out.get("participants")).isEmpty());
        assertTrue(((List<?>) out.get("agentAuthorizations")).isEmpty());
    }

    // ════════════════ fixtures ════════════════

    private static DeHcParticipantBo participantBo(Long taskId, String action, Long userId) {
        DeHcParticipantBo bo = new DeHcParticipantBo();
        bo.setTaskId(taskId);
        bo.setAction(action);
        bo.setUserId(userId);
        return bo;
    }

    private static FlowUser userRow(Long taskId, String type, String processedBy) {
        FlowUser u = new FlowUser();
        u.setAssociated(taskId);
        u.setType(type);
        u.setProcessedBy(processedBy);
        return u;
    }

    /** 捕获型夹具：task 可编程返回；userRow 为 selectOne 可编程返回；写操作全量记录。 */
    private static final class Harness {
        final DeHcServiceImpl service;
        final List<FlowUser> updated = new ArrayList<>();
        final List<FlowUser> deleted = new ArrayList<>();
        FlowTask task = task(TASK_ID);
        FlowUser userRow;
        int userSelectOneCalls;

        Harness() {
            FlwTaskMapper taskMapper = stub(FlwTaskMapper.class, (method, args) -> {
                if ("selectById".equals(method.getName())) {
                    return task;
                }
                return defaultValue(method.getReturnType());
            });
            FlwUserMapper userMapper = stub(FlwUserMapper.class, (method, args) -> {
                switch (method.getName()) {
                    case "selectOne" -> {
                        userSelectOneCalls++;
                        return userRow;
                    }
                    case "updateById" -> {
                        updated.add((FlowUser) args[0]);
                        return 1;
                    }
                    case "deleteById" -> {
                        deleted.add((FlowUser) args[0]);
                        return 1;
                    }
                    default -> { return defaultValue(method.getReturnType()); }
                }
            });
            this.service = new DeHcServiceImpl(taskMapper, userMapper);
        }

        private static FlowTask task(Long id) {
            FlowTask t = new FlowTask();
            t.setId(id);
            return t;
        }

        @SuppressWarnings("unchecked")
        private static <T> T stub(Class<T> iface, Handler handler) {
            return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
                (proxy, method, args) -> {
                    Object r = handler.handle(method, args == null ? new Object[0] : args);
                    return r != null ? r : defaultValue(method.getReturnType());
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

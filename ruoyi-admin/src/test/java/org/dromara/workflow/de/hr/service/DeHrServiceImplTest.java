package org.dromara.workflow.de.hr.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.hr.domain.bo.DeHrAssignBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrHandoverBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrTaskActionBo;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DeHrServiceImpl} 应用层校验的 surefire 标准化测试（JUnit 5 · H1 P1 #22）。
 *
 * <p>HR 域此前 0 单测（DeBindingServiceImpl 19 + BrComplianceService 22 都覆盖了别的域）。
 * 本类填补 HR-03 / HR-05 / HR-06 三方法的最小覆盖集，stub mapper 沿用 BrCompliance 范式。
 *
 * <h3>覆盖矩阵（≥ 12 个 @Test）</h3>
 *
 * <h4>HR-03 assign（2 个）</h4>
 * <ol>
 *   <li>缺 taskId / instanceId → 抛 ServiceException</li>
 *   <li>task 不存在或 instanceId 不匹配 → 抛 ServiceException</li>
 * </ol>
 *
 * <h4>HR-05 taskAction（2 个）</h4>
 * <ol>
 *   <li>action 空白 → 抛 ServiceException</li>
 *   <li>reject action 且 approver 存在 → 删除 approver 行（affected 行通过 flwUserMapper.deleteById 计数）</li>
 * </ol>
 *
 * <h4>HR-06 handover（8 个）</h4>
 * <ol>
 *   <li>fromUserId / toUserId null → 抛 ServiceException</li>
 *   <li><b>fromUserId == toUserId 零效果负例（H1 §四 #10）</b>：affected = 0，不报错</li>
 *   <li>taskIds = null → affected = 0</li>
 *   <li>taskIds = [] → affected = 0</li>
 *   <li>fromUserId 与 processed_by 不匹配 → affected = 0（continue）</li>
 *   <li>fromUserId 与 processed_by 匹配 + 有 task → 转办 + affected = 1</li>
 *   <li>approver 为 null（任务无审批人行） → affected = 0（continue）</li>
 *   <li>多 task 部分匹配：3 个 task 中 1 个 processed_by 匹配 → affected = 1</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-19 (H1 P1 #22)
 */
@Tag("dev")
@DisplayName("DeHrServiceImpl HR-03 / HR-05 / HR-06 应用层")
public class DeHrServiceImplTest {

    private static final Long INSTANCE_ID = 9001L;
    private static final Long TASK_ID = 500L;
    private static final Long TASK_ID_2 = 501L;
    private static final Long TASK_ID_3 = 502L;
    private static final Long TASK_ID_MISSING = 99999L;
    private static final Long FROM_USER = 100L;
    private static final Long TO_USER = 200L;
    private static final Long POST_ID = 300L;

    /** stub 容器：模拟 flow_task + flow_user 表 */
    private static final Map<String, Object> STUB_REGISTRY = new HashMap<>();
    /** 记录 stub mapper 的写操作次数（用于 affected 计数） */
    private static int deleteByIdCallCount;
    private static int updateByIdCallCount;
    private static int insertCallCount;
    /** selectOne 当前应返回的审批人（每次 handover 循环查一次） */
    private static FlowUser currentApprover;

    // ====================================================================
    // ===== HR-03 assign ==============================================
    // ====================================================================

    @Test
    @DisplayName("HR-03 assign 缺 taskId → 抛 ServiceException")
    void hr03_assign_missingTaskId() {
        DeHrServiceImpl svc = newSvc();
        DeHrAssignBo bo = new DeHrAssignBo();
        bo.setInstanceId(INSTANCE_ID);
        // bo.setTaskId(null) — 故意留空
        assertThrows(ServiceException.class, () -> svc.assign(bo),
            "缺 taskId 应抛 ServiceException");
    }

    @Test
    @DisplayName("HR-03 assign task.instanceId 与 BO.instanceId 不匹配 → 抛 ServiceException")
    void hr03_assign_instanceMismatch() {
        DeHrServiceImpl svc = newSvc();
        // task 存在但 instanceId 是 1L，不匹配 BO.instanceId = 9001
        FlowTask task = taskWithInstance(500L, 1L);
        STUB_REGISTRY.put("task:" + TASK_ID, task);

        DeHrAssignBo bo = new DeHrAssignBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setTaskId(TASK_ID);
        bo.setPostId(POST_ID);
        assertThrows(ServiceException.class, () -> svc.assign(bo),
            "task.instanceId 不匹配 BO.instanceId 应抛 ServiceException");
    }

    // ====================================================================
    // ===== HR-05 taskAction ==========================================
    // ====================================================================

    @Test
    @DisplayName("HR-05 taskAction action 空白 → 抛 ServiceException")
    void hr05_actionBlank() {
        DeHrServiceImpl svc = newSvc();
        DeHrTaskActionBo bo = new DeHrTaskActionBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setTaskId(TASK_ID);
        bo.setAction("");
        assertThrows(ServiceException.class, () -> svc.taskAction(bo),
            "action 空白应抛 ServiceException");
    }

    @Test
    @DisplayName("HR-05 taskAction reject action → 调 flwUserMapper.deleteById 一次")
    void hr05_actionReject_deletesApprover() {
        resetCounters();
        // 准备：task 存在 + approver 行存在（id 非 null ⇒ 走 deleteById 分支）
        STUB_REGISTRY.put("task:" + TASK_ID, taskWithInstance(TASK_ID, INSTANCE_ID));
        FlowUser approver = new FlowUser();
        approver.setId(700L);
        approver.setAssociated(TASK_ID);
        STUB_REGISTRY.put("user:" + TASK_ID, approver);
        currentApprover = approver; // ensureRow 调 selectOne 返 currentApprover

        DeHrServiceImpl svc = newSvc();
        DeHrTaskActionBo bo = new DeHrTaskActionBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setTaskId(TASK_ID);
        bo.setAction("reject");
        Long result = svc.taskAction(bo);

        assertEquals(TASK_ID, result, "reject action 应返回 taskId");
        assertEquals(1, deleteByIdCallCount, "reject action 应调 flwUserMapper.deleteById 一次");
    }

    // ====================================================================
    // ===== HR-06 handover（核心）=======================================
    // ====================================================================

    @Test
    @DisplayName("HR-06 handover fromUserId null → 抛 ServiceException")
    void hr06_handoverNullFrom() {
        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(null);
        bo.setToUserId(TO_USER);
        assertThrows(ServiceException.class, () -> svc.handover(bo),
            "fromUserId null 应抛 ServiceException");
    }

    @Test
    @DisplayName("HR-06 handover toUserId null → 抛 ServiceException")
    void hr06_handoverNullTo() {
        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(null);
        assertThrows(ServiceException.class, () -> svc.handover(bo),
            "toUserId null 应抛 ServiceException");
    }

    @Test
    @DisplayName("HR-06 handover fromUserId == toUserId 零效果负例（H1 §四 #10）")
    void hr06_handoverZeroEffect_sameUser() {
        resetCounters();
        // 准备：task + approver 都匹配
        STUB_REGISTRY.put("task:" + TASK_ID, taskWithInstance(TASK_ID, INSTANCE_ID));
        FlowUser approver = new FlowUser();
        approver.setId(700L);
        approver.setAssociated(TASK_ID);
        approver.setProcessedBy(String.valueOf(FROM_USER));
        STUB_REGISTRY.put("user:" + TASK_ID, approver);
        currentApprover = approver;

        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(FROM_USER); // 与 fromUserId 同值
        bo.setTaskIds(Arrays.asList(TASK_ID));

        int affected = assertDoesNotThrow(() -> svc.handover(bo),
            "零效果负例不抛 ServiceException");
        assertEquals(0, affected,
            "fromUserId == toUserId 应零效果早返 affected = 0（§四 #10 / 28 号 AC-06 边界）");
        assertEquals(0, updateByIdCallCount,
            "零效果早返后不应调 flwUserMapper.updateById");
    }

    @Test
    @DisplayName("HR-06 handover taskIds = null → affected = 0，不报错")
    void hr06_handoverNullTaskIds() {
        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(TO_USER);
        bo.setTaskIds(null);
        int affected = svc.handover(bo);
        assertEquals(0, affected, "taskIds null 应 affected = 0");
    }

    @Test
    @DisplayName("HR-06 handover taskIds = [] → affected = 0，不报错")
    void hr06_handoverEmptyTaskIds() {
        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(TO_USER);
        bo.setTaskIds(new ArrayList<>());
        int affected = svc.handover(bo);
        assertEquals(0, affected, "taskIds 空列表应 affected = 0");
    }

    @Test
    @DisplayName("HR-06 handover fromUserId 不匹配 approver.processed_by → affected = 0")
    void hr06_handoverFromMismatch() {
        resetCounters();
        // 准备：task 存在 + approver.processed_by = "999"（不等于 FROM_USER）
        STUB_REGISTRY.put("task:" + TASK_ID, taskWithInstance(TASK_ID, INSTANCE_ID));
        FlowUser approver = new FlowUser();
        approver.setId(700L);
        approver.setAssociated(TASK_ID);
        approver.setProcessedBy("999");
        STUB_REGISTRY.put("user:" + TASK_ID, approver);
        currentApprover = approver;

        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(TO_USER);
        bo.setTaskIds(Arrays.asList(TASK_ID));

        int affected = svc.handover(bo);
        assertEquals(0, affected, "processed_by 不匹配应 affected = 0（continue）");
    }

    @Test
    @DisplayName("HR-06 handover 正常匹配 → 转办 + affected = 1")
    void hr06_handoverNormal() {
        resetCounters();
        // 准备：task 存在 + approver.processed_by = FROM_USER（匹配）
        STUB_REGISTRY.put("task:" + TASK_ID, taskWithInstance(TASK_ID, INSTANCE_ID));
        FlowUser approver = new FlowUser();
        approver.setId(700L);
        approver.setAssociated(TASK_ID);
        approver.setProcessedBy(String.valueOf(FROM_USER));
        STUB_REGISTRY.put("user:" + TASK_ID, approver);
        currentApprover = approver;

        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(TO_USER);
        bo.setTaskIds(Arrays.asList(TASK_ID));

        int affected = svc.handover(bo);
        assertEquals(1, affected, "正常匹配应 affected = 1");
        assertTrue(updateByIdCallCount >= 1, "转办后应调 updateById 至少 1 次");
    }

    @Test
    @DisplayName("HR-06 handover approver 为 null（任务无审批人行） → affected = 0")
    void hr06_handoverApproverMissing() {
        resetCounters();
        STUB_REGISTRY.put("task:" + TASK_ID, taskWithInstance(TASK_ID, INSTANCE_ID));
        // 不放 user:TASK_ID 且 currentApprover = null ⇒ stub mapper 返 null
        currentApprover = null;

        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(TO_USER);
        bo.setTaskIds(Arrays.asList(TASK_ID));

        int affected = svc.handover(bo);
        assertEquals(0, affected, "approver 缺失应 continue ⇒ affected = 0");
    }

    @Test
    @DisplayName("HR-06 handover 多 task：第 1 个匹配 → 转办；之后 processed_by 被改 → 不再匹配 → affected = 1")
    void hr06_handoverMultiTaskPartial() {
        resetCounters();
        // 准备 3 个 task：每个 task selectOne 都返同一 approver（stub 简化）
        STUB_REGISTRY.put("task:" + TASK_ID, taskWithInstance(TASK_ID, INSTANCE_ID));
        STUB_REGISTRY.put("task:" + TASK_ID_2, taskWithInstance(TASK_ID_2, INSTANCE_ID));
        STUB_REGISTRY.put("task:" + TASK_ID_3, taskWithInstance(TASK_ID_3, INSTANCE_ID));

        FlowUser approverMatch = new FlowUser();
        approverMatch.setId(701L);
        approverMatch.setAssociated(TASK_ID);
        approverMatch.setProcessedBy(String.valueOf(FROM_USER)); // 初始 = FROM_USER（匹配）
        STUB_REGISTRY.put("user:" + TASK_ID, approverMatch);
        currentApprover = approverMatch;

        DeHrServiceImpl svc = newSvc();
        DeHrHandoverBo bo = new DeHrHandoverBo();
        bo.setFromUserId(FROM_USER);
        bo.setToUserId(TO_USER);
        bo.setTaskIds(Arrays.asList(TASK_ID, TASK_ID_2, TASK_ID_3));

        // handover 循环：
        //   iter 1: selectOne 返 approverMatch，processed_by = FROM_USER（匹配）⇒ 转办 + processed_by 改为 TO_USER + affected++
        //   iter 2: selectOne 返同一对象，processed_by = TO_USER（不匹配）⇒ continue
        //   iter 3: 同上 ⇒ continue
        // ⇒ affected = 1（这正是设计预期：首匹配后状态变更，后续不再匹配）
        int affected = svc.handover(bo);
        assertEquals(1, affected,
            "3 task 循环：第 1 次匹配转办后 processed_by 改为 toUserId，后续两次不再匹配 ⇒ affected = 1");
    }

    // ====================================================================
    // ===== 桩数据 + Mapper 构造（沿用 BrCompliance 范式）==============
    // ====================================================================

    private static FlowTask taskWithInstance(Long taskId, Long instanceId) {
        FlowTask task = new FlowTask();
        task.setId(taskId);
        task.setInstanceId(instanceId);
        return task;
    }

    private static void resetCounters() {
        deleteByIdCallCount = 0;
        updateByIdCallCount = 0;
        insertCallCount = 0;
        STUB_REGISTRY.clear();
        currentApprover = null;
    }

    @SuppressWarnings("unchecked")
    private static <M> M stubMapper(Class<M> iface, java.util.function.Function<Object, Object> byIdResolver) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            int pc = method.getParameterCount();
            if ("selectById".equals(name) && pc == 1) {
                return byIdResolver == null ? null : byIdResolver.apply(args[0]);
            }
            if ("selectOne".equals(name) && pc == 1) {
                // HR-06 handover 调 selectOne(LambdaQueryWrapper) —— 走 currentApprover 桩
                return currentApprover;
            }
            if ("updateById".equals(name) && pc == 1) {
                updateByIdCallCount++;
                return 1;
            }
            if ("deleteById".equals(name) && pc == 1) {
                deleteByIdCallCount++;
                return 1;
            }
            if ("insert".equals(name) && pc == 1) {
                insertCallCount++;
                return 1;
            }
            if ("equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                return method.invoke(proxy, args);
            }
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) {
                return false;
            }
            if (rt == int.class || rt == Integer.class) {
                return 0;
            }
            if (rt == long.class || rt == Long.class) {
                return 0L;
            }
            return null;
        };
        return (M) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    private static DeHrServiceImpl newSvc() {
        // 构造参数顺序：DeHrServiceImpl(FlwUserMapper, FlwTaskMapper)
        return new DeHrServiceImpl(
            stubMapper(FlwUserMapper.class, id -> STUB_REGISTRY.get("user:" + id)),
            stubMapper(FlwTaskMapper.class, id -> STUB_REGISTRY.get("task:" + id))
        );
    }
}

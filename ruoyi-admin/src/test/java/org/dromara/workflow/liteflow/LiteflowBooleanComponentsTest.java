package org.dromara.workflow.liteflow;

import org.dromara.workflow.domain.bo.TaskOperationBo;
import org.dromara.workflow.domain.context.CompleteTaskContext;
import org.dromara.workflow.domain.context.InstanceDeleteContext;
import org.dromara.workflow.domain.context.StartProcessContext;
import org.dromara.workflow.domain.context.TaskOperationContext;
import org.dromara.workflow.liteflow.complete.CompleteNeedAutoPassComponent;
import org.dromara.workflow.liteflow.instance.InstanceDeleteExistsComponent;
import org.dromara.workflow.liteflow.operation.TaskOpNeedNotifyComponent;
import org.dromara.workflow.liteflow.start.StartExistsComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A8 LiteFlow 编排 · 四布尔分支组件单测（T1-W6）。
 *
 * <p>四组件是四条链（startProcessChain / completeTaskChain / taskOperationChain /
 * deleteInstanceChain）的分支阀门，判定必须钉死（走错分支 = 续提交覆盖他人实例 /
 * 误进自动审批 / 漏发通知 / 空删）。驱动方式：匿名子类覆写
 * {@code getContextBean(Class)} 返回固定 Context，直接调 {@code processBoolean()}，
 * 不拉起 LiteFlow 运行时（对齐 T1 波次零容器范式）。
 *
 * <p>钉死的判定真值表：
 * <ul>
 *   <li>★startExists：existingInstance 非 null → true（续提交）；null → false；</li>
 *   <li>★instanceDeleteExists：flowInstances 非空集合 → true；null / 空列表 → false；</li>
 *   <li>★completeNeedAutoPass：仅 Boolean.TRUE → true；null / FALSE → false（三态严格）；</li>
 *   <li>★taskOpNeedNotify：result 且 messageType 非空 → true；任一不满足 → false。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("A8 LiteFlow 四布尔分支组件：链路分支阀门真值表（匿名子类覆写 getContextBean）")
class LiteflowBooleanComponentsTest {

    // ════════════════ startExists（startProcessChain：已有实例续提交分支） ════════════════

    @Test
    @DisplayName("★startExists：existingInstance 非 null → true（走续提交，防重复发起新实例）")
    void startExists_presentInstance_true() {
        StartProcessContext ctx = new StartProcessContext(null);
        ctx.setExistingInstance(new org.dromara.warm.flow.orm.entity.FlowInstance());
        assertTrue(driveStart(ctx), "已有实例必须走续提交分支");
    }

    @Test
    @DisplayName("★startExists：existingInstance 为 null → false（正常发起分支）")
    void startExists_absentInstance_false() {
        StartProcessContext ctx = new StartProcessContext(null);
        assertFalse(driveStart(ctx), "无已有实例必须走正常发起分支");
    }

    // ════════════════ instanceDeleteExists（deleteInstanceChain：存在可删数据分支） ════════════════

    @Test
    @DisplayName("★instanceDeleteExists：flowInstances 未加载（null）→ false（跳过删除，防空删）")
    void instanceDeleteExists_nullInstances_false() {
        InstanceDeleteContext ctx = InstanceDeleteContext.byBusinessIds(List.of("b1"));
        assertFalse(driveInstanceDelete(ctx), "实例未加载不得进入删除分支");
    }

    @Test
    @DisplayName("★instanceDeleteExists：flowInstances 为空列表 → false")
    void instanceDeleteExists_emptyInstances_false() {
        InstanceDeleteContext ctx = InstanceDeleteContext.byInstanceIds(List.of(1L));
        ctx.setFlowInstances(Collections.emptyList());
        assertFalse(driveInstanceDelete(ctx));
    }

    @Test
    @DisplayName("★instanceDeleteExists：flowInstances 非空 → true（进入级联删除）")
    void instanceDeleteExists_nonEmptyInstances_true() {
        InstanceDeleteContext ctx = InstanceDeleteContext.byInstanceIds(List.of(1L, 2L));
        ctx.setFlowInstances(List.of(new org.dromara.warm.flow.orm.entity.FlowInstance()));
        assertTrue(driveInstanceDelete(ctx));
    }

    // ════════════════ completeNeedAutoPass（completeTaskChain：自动审批分支） ════════════════

    @Test
    @DisplayName("★completeNeedAutoPass：autoPass = Boolean.TRUE → true（进入自动审批）")
    void completeNeedAutoPass_trueValue_true() {
        CompleteTaskContext ctx = new CompleteTaskContext(null);
        ctx.setAutoPass(Boolean.TRUE);
        assertTrue(driveComplete(ctx));
    }

    @Test
    @DisplayName("★completeNeedAutoPass：autoPass = null（未解析出开关）→ false（三态严格，null 不等同 true）")
    void completeNeedAutoPass_nullValue_false() {
        CompleteTaskContext ctx = new CompleteTaskContext(null);
        assertFalse(driveComplete(ctx), "null 不得触发自动审批");
    }

    @Test
    @DisplayName("★completeNeedAutoPass：autoPass = Boolean.FALSE → false")
    void completeNeedAutoPass_falseValue_false() {
        CompleteTaskContext ctx = new CompleteTaskContext(null);
        ctx.setAutoPass(Boolean.FALSE);
        assertFalse(driveComplete(ctx));
    }

    // ════════════════ taskOpNeedNotify（taskOperationChain：消息通知分支） ════════════════

    @Test
    @DisplayName("★taskOpNeedNotify：result=true 且 messageType 非空 → true（发送通知）")
    void taskOpNeedNotify_successWithMessageTypes_true() {
        TaskOperationContext ctx = taskOpContext(true, List.of("2", "3"));
        assertTrue(driveTaskOp(ctx));
    }

    @Test
    @DisplayName("★taskOpNeedNotify：result=false（操作失败）即使配置了 messageType → false（失败不发通知）")
    void taskOpNeedNotify_failedEvenWithMessageTypes_false() {
        TaskOperationContext ctx = taskOpContext(false, List.of("2"));
        assertFalse(driveTaskOp(ctx), "操作失败不得发通知");
    }

    @Test
    @DisplayName("★taskOpNeedNotify：result=true 但 messageType=null（未配置通知）→ false")
    void taskOpNeedNotify_successNullMessageTypes_false() {
        TaskOperationContext ctx = taskOpContext(true, null);
        assertFalse(driveTaskOp(ctx));
    }

    @Test
    @DisplayName("★taskOpNeedNotify：result=true 但 messageType=空列表 → false")
    void taskOpNeedNotify_successEmptyMessageTypes_false() {
        TaskOperationContext ctx = taskOpContext(true, Collections.emptyList());
        assertFalse(driveTaskOp(ctx));
    }

    // ════════════════ 驱动器（匿名子类覆写 getContextBean，绕开 LiteFlow slot 运行时） ════════════════

    private static boolean driveStart(StartProcessContext ctx) {
        return new StartExistsComponent() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getContextBean(Class<T> clazz) {
                return (T) ctx;
            }
        }.processBoolean();
    }

    private static boolean driveInstanceDelete(InstanceDeleteContext ctx) {
        return new InstanceDeleteExistsComponent() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getContextBean(Class<T> clazz) {
                return (T) ctx;
            }
        }.processBoolean();
    }

    private static boolean driveComplete(CompleteTaskContext ctx) {
        return new CompleteNeedAutoPassComponent() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getContextBean(Class<T> clazz) {
                return (T) ctx;
            }
        }.processBoolean();
    }

    private static boolean driveTaskOp(TaskOperationContext ctx) {
        return new TaskOpNeedNotifyComponent() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getContextBean(Class<T> clazz) {
                return (T) ctx;
            }
        }.processBoolean();
    }

    private static TaskOperationContext taskOpContext(boolean result, List<String> messageTypes) {
        TaskOperationBo bo = new TaskOperationBo();
        bo.setMessageType(messageTypes);
        TaskOperationContext ctx = new TaskOperationContext(bo, "1");
        ctx.setResult(result);
        return ctx;
    }
}

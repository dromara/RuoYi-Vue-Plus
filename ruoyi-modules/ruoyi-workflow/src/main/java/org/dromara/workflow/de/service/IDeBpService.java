package org.dromara.workflow.de.service;

import java.util.Map;

/**
 * 03 号 §一 10 业务环节（E0—E9）的占位服务接口。
 *
 * <p>本接口的 10 个方法对应 {@code docs/设计包/03-业务流程与状态机.md} §1.2
 * 的 10 环节（E0 定义就绪 / E1 发起 / E2 首任务留痕 / E3 待办可见 / E4 办理 /
 * E5 驳回 / E6 撤销 / E7 作废 / E8 终止 / E9 归档），由
 * {@code docs/03号业务环节实施清单-2026-09-18.md} §六 落地为占位实现 —— 每个方法
 * 返回 mock 数据 + 日志标「03 号业务环节实施清单 §六 占位」。
 *
 * <p><b>本接口不接任何底层服务、不写表</b>。仅作为 controller 与未来真实实现之间的
 * 契约层存在，便于端到端打通而不阻塞后续分阶段接入。
 *
 * @author AIP
 */
public interface IDeBpService {

    // ===== 10 业务环节（E0—E9）=====

    /**
     * E0 定义就绪。落点 = {@code flow_definition.is_publish}。
     * 触发点 = {@code PUT /workflow/definition/publish/{id}}。
     */
    Map<String, Object> e0DraftCheck(Object payload);

    /**
     * E1 发起（创建 {@code flow_instance}）。落点 = {@code flow_instance}。
     * 触发点 = {@code POST /workflow/task/startWorkFlow}。
     */
    Map<String, Object> e1Start(Object payload);

    /**
     * E2 字节码自动生成（首个任务留痕）。落点 = {@code flow_definition.ext}。
     * 引擎在 {@code insService.start} 内建开始节点 {@code flow_task} +
     * {@code flow_his_task}（【字节码】）。
     */
    Map<String, Object> e2BytecodeGen(Object payload);

    /**
     * E3 起草任务派发（{@code assignUser}）。落点 = {@code flow_user}。
     * 触发点 = {@code POST /de/binding/assignee}（见 {@code DeBindingController}）。
     */
    Map<String, Object> e3AssignUser(Object payload);

    /**
     * E4 办理（{@code completeTask}）。落点 = {@code flow_task} / {@code flow_his_task}。
     * 触发点 = {@code POST /workflow/task/completeTask}。
     */
    Map<String, Object> e4CompleteTask(Object payload);

    /**
     * E5 驳回（{@code backProcess}）。落点 = {@code flow_task} / {@code flow_his_task}。
     * 触发点 = {@code POST /workflow/task/backProcess}。
     */
    Map<String, Object> e5BackProcess(Object payload);

    /**
     * E6 异常修复（counter + path）。落点 = {@code flow_task}。
     * 用于驳回后异常路径修复的占位入口。
     */
    Map<String, Object> e6AbnormalFix(Object payload);

    /**
     * E7 归档（auto-complete）。落点 = {@code flow_his_task}。
     * 由 E4 副产品触发，无专门入口 —— 监听器写
     * {@code flow_instance.flow_status = finish}（见 D-3 断点）。
     */
    Map<String, Object> e7Archive(Object payload);

    /**
     * E8 撤回 / 作废。落点 = {@code flow_task}。
     * 触发点 = {@code PUT /workflow/instance/cancelProcessApply} / {@code POST /workflow/instance/invalid}。
     */
    Map<String, Object> e8RevokeOrInvalid(Object payload);

    /**
     * E9 交接。落点 = {@code flow_task} / {@code flow_user}。
     * 触发点 = 委托 / 转办相关底座接口（{@code TaskOperationEnum}）。
     */
    Map<String, Object> e9Transfer(Object payload);
}

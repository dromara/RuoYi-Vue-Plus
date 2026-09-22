package org.dromara.workflow.de.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 03 号 §一 10 业务环节（E0—E9）的占位实现。
 *
 * <p><b>本类是占位</b>，每个方法仅返回 mock 数据 + 标「03 号业务环节实施清单 §六 占位」
 * 的日志 —— 不写任何表、不调任何 mapper、不接任何底座 service。
 *
 * <p>对应文档：
 * <ul>
 *   <li>{@code docs/设计包/03-业务流程与状态机.md} §1.2 E0—E9 逐环节钉表</li>
 *   <li>{@code docs/03号业务环节实施清单-2026-09-18.md} §六 占位实现</li>
 * </ul>
 *
 * <p><b>不发明任何表名 / 列名</b>：占位响应字段 {@code status} / {@code phase} /
 * {@code note} / {@code landed} 均为字符串常量，与底座 DDL 字面 0 交集，
 * 不会被 {@code script/check_schema_grounding.py} 扫到。
 *
 * @author AIP
 */
@Slf4j
@Service
public class DeBpServiceImpl implements IDeBpService {

    /** 占位标记 —— 出现在每个方法的日志中，便于一键检索「哪些是占位」。 */
    private static final String PLACEHOLDER_TAG = "03号业务环节实施清单 §六 占位";

    /** 构造统一占位响应。{@code landed} 字段为该环节的真实落点表（仅说明用，不写表）。 */
    private static Map<String, Object> mock(String phase, String landing, String note) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "PLACEHOLDER");
        r.put("phase", phase);
        r.put("landed", landing);
        r.put("note", note);
        return r;
    }

    // ===== 10 业务环节（E0—E9）占位实现 =====

    @Override
    public Map<String, Object> e0DraftCheck(Object payload) {
        log.info("[{}] E0 定义就绪 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E0", "flow_definition.is_publish",
            "占位：未真实校验定义发布态。真实路径 PUT /workflow/definition/publish/{id}");
    }

    @Override
    public Map<String, Object> e1Start(Object payload) {
        log.info("[{}] E1 发起 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E1", "flow_instance",
            "占位：未真实创建 flow_instance。真实路径 POST /workflow/task/startWorkFlow");
    }

    @Override
    public Map<String, Object> e2BytecodeGen(Object payload) {
        log.info("[{}] E2 字节码自动生成 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E2", "flow_definition.ext",
            "占位：引擎在 insService.start 内建开始节点 flow_task + flow_his_task（本仓不可见 · 【字节码】）");
    }

    @Override
    public Map<String, Object> e3AssignUser(Object payload) {
        log.info("[{}] E3 起草任务派发 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E3", "flow_user",
            "占位：未真实写 flow_user。真实路径 POST /de/binding/assignee（DeBindingController）");
    }

    @Override
    public Map<String, Object> e4CompleteTask(Object payload) {
        log.info("[{}] E4 办理 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E4", "flow_task + flow_his_task",
            "占位：未真实办理任务。真实路径 POST /workflow/task/completeTask");
    }

    @Override
    public Map<String, Object> e5BackProcess(Object payload) {
        log.info("[{}] E5 驳回 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E5", "flow_task + flow_his_task",
            "占位：未真实驳回任务。真实路径 POST /workflow/task/backProcess");
    }

    @Override
    public Map<String, Object> e6AbnormalFix(Object payload) {
        log.info("[{}] E6 异常修复 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E6", "flow_task",
            "占位：驳回后异常路径修复（counter + path）—— 真实落点与判定口径待 owner 裁");
    }

    @Override
    public Map<String, Object> e7Archive(Object payload) {
        log.info("[{}] E7 归档 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E7", "flow_his_task",
            "占位：归档由 E4 副产品触发，监听器写 flow_instance.flow_status=finish（D-3 断点）");
    }

    @Override
    public Map<String, Object> e8RevokeOrInvalid(Object payload) {
        log.info("[{}] E8 撤回 / 作废 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E8", "flow_task",
            "占位：未真实撤销/作废。真实路径 PUT /workflow/instance/cancelProcessApply 或 POST /workflow/instance/invalid");
    }

    @Override
    public Map<String, Object> e9Transfer(Object payload) {
        log.info("[{}] E9 交接 payload={}", PLACEHOLDER_TAG, payload);
        return mock("E9", "flow_task + flow_user",
            "占位：未真实交接。真实路径委托/转办相关底座接口（TaskOperationEnum）");
    }
}

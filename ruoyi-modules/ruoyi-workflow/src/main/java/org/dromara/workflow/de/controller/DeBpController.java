package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.service.IDeBpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 03 号 §一 10 业务环节（E0—E9）的占位 REST 端点（声明式占位）。
 *
 * <p>对应文档：
 * <ul>
 *   <li>{@code docs/设计包/03-业务流程与状态机.md} §1.2 E0—E9 逐环节钉表</li>
 *   <li>{@code docs/03号业务环节实施清单-2026-09-18.md} §六 占位实现</li>
 * </ul>
 *
 * <p><b>本 controller 是「声明式占位」，不接任何底层服务、不真写表</b>：
 * <ul>
 *   <li>每个端点统一前缀 <code>/de/bp-stage/</code>（bp-stage = 业务环节占位，
 *       与 {@code DeP1GapCoverController} 的 {@code /de/bp/} 前缀区分避免冲突）。</li>
 *   <li>每个端点响应字段固定包含：{@code status}（PLACEHOLDER）· {@code phase}（E0..E9）·
 *       {@code landed}（真实落点表.列说明）· {@code note}（一句话说明）。</li>
 *   <li>所有端点挂占位权限 {@code workflow:de:bp:query}（{@code @SaCheckPermission}），
 *       <b>不实际接</b>权限系统；接入由 owner 裁后再做。</li>
 * </ul>
 *
 * <p><b>endpoint 清单（10 个）</b>：
 *
 * <pre>
 *   GET  /de/bp-stage/e0-draft-check        E0  定义就绪
 *   POST /de/bp-stage/e1-start              E1  发起
 *   POST /de/bp-stage/e2-bytecode-gen       E2  字节码自动生成
 *   POST /de/bp-stage/e3-assign-user        E3  起草任务派发
 *   POST /de/bp-stage/e4-complete-task      E4  办理
 *   POST /de/bp-stage/e5-back-process       E5  驳回
 *   POST /de/bp-stage/e6-abnormal-fix       E6  异常修复
 *   POST /de/bp-stage/e7-archive            E7  归档
 *   POST /de/bp-stage/e8-revoke-or-invalid  E8  撤回 / 作废
 *   POST /de/bp-stage/e9-transfer           E9  交接
 * </pre>
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/bp-stage")
public class DeBpController extends BaseController {

    /** 占位权限（不实际接；owner 决议后再做）。 */
    private static final String PERM = "workflow:de:bp:query";

    private final IDeBpService deBpService;

    // ===== 10 业务环节（E0—E9）=====

    /**
     * E0 定义就绪。真实落点 = {@code flow_definition.is_publish}。
     */
    @SaCheckPermission(PERM)
    @GetMapping("/e0-draft-check")
    public R<Map<String, Object>> e0DraftCheck(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e0DraftCheck(payload));
    }

    /**
     * E1 发起。真实落点 = {@code flow_instance}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e1-start")
    public R<Map<String, Object>> e1Start(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e1Start(payload));
    }

    /**
     * E2 字节码自动生成（首任务留痕）。真实落点 = {@code flow_definition.ext}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e2-bytecode-gen")
    public R<Map<String, Object>> e2BytecodeGen(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e2BytecodeGen(payload));
    }

    /**
     * E3 起草任务派发（{@code assignUser}）。真实落点 = {@code flow_user}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e3-assign-user")
    public R<Map<String, Object>> e3AssignUser(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e3AssignUser(payload));
    }

    /**
     * E4 办理（{@code completeTask}）。真实落点 = {@code flow_task} / {@code flow_his_task}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e4-complete-task")
    public R<Map<String, Object>> e4CompleteTask(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e4CompleteTask(payload));
    }

    /**
     * E5 驳回（{@code backProcess}）。真实落点 = {@code flow_task} / {@code flow_his_task}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e5-back-process")
    public R<Map<String, Object>> e5BackProcess(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e5BackProcess(payload));
    }

    /**
     * E6 异常修复（counter + path）。真实落点 = {@code flow_task}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e6-abnormal-fix")
    public R<Map<String, Object>> e6AbnormalFix(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e6AbnormalFix(payload));
    }

    /**
     * E7 归档（auto-complete）。真实落点 = {@code flow_his_task}。
     * 由 E4 副产品触发，无专门入口（监听器写 {@code flow_instance.flow_status = finish}，D-3 断点）。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e7-archive")
    public R<Map<String, Object>> e7Archive(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e7Archive(payload));
    }

    /**
     * E8 撤回 / 作废。真实落点 = {@code flow_task}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e8-revoke-or-invalid")
    public R<Map<String, Object>> e8RevokeOrInvalid(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e8RevokeOrInvalid(payload));
    }

    /**
     * E9 交接。真实落点 = {@code flow_task} / {@code flow_user}。
     */
    @SaCheckPermission(PERM)
    @PostMapping("/e9-transfer")
    public R<Map<String, Object>> e9Transfer(@RequestBody(required = false) Object payload) {
        return R.ok(deBpService.e9Transfer(payload));
    }
}

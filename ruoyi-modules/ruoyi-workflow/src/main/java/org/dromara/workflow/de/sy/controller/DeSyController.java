package org.dromara.workflow.de.sy.controller;

import jakarta.validation.Valid;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.sy.domain.bo.DeSyCorrectionBo;
import org.dromara.workflow.de.sy.service.IDeSyService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SY 域 REST 端点（SY-05 / SY-06 / SY-07 / SY-08 / SY-09 共 5 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sy")
public class DeSyController extends BaseController {

    private final IDeSyService deSyService;

    /** SY-05 确认状态 */
    @SaCheckPermission("workflow:de:sy:query")
    @GetMapping("/{taskId}/ack-state")
    public R<Map<String, Object>> ackState(@PathVariable Long taskId) {
        return R.ok(deSyService.ackState(taskId));
    }

    /** SY-06 人类纠正产生新事实版本 */
    @SaCheckPermission("workflow:de:sy:edit")
    @PostMapping("/correction-version")
    public R<Long> correctionVersion(@Valid @RequestBody DeSyCorrectionBo bo) {
        return R.ok("correction version 已写入", deSyService.correctionVersion(bo));
    }

    /** SY-07 检查点恢复 */
    @SaCheckPermission("workflow:de:sy:edit")
    @PostMapping("/{taskId}/checkpoint-recover")
    public R<Map<String, Object>> checkpointRecover(@PathVariable Long taskId) {
        return R.ok(deSyService.checkpointRecover(taskId));
    }

    /** SY-08 迟到 / 撤回 / 已提交节点变更 */
    @SaCheckPermission("workflow:de:sy:query")
    @GetMapping("/{taskId}/late-arrivals")
    public R<Map<String, Object>> lateArrivals(@PathVariable Long taskId) {
        return R.ok(deSyService.lateArrivals(taskId));
    }

    /** SY-09 下游验证交接包 */
    @SaCheckPermission("workflow:de:sy:query")
    @GetMapping("/{taskId}/handover-validate")
    public R<Map<String, Object>> handoverPackageValidate(@PathVariable Long taskId) {
        return R.ok(deSyService.handoverPackageValidate(taskId));
    }
}
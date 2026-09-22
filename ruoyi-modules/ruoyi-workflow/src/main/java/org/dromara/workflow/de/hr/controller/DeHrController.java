package org.dromara.workflow.de.hr.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.hr.domain.bo.DeHrAssignBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrHandoverBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrTaskActionBo;
import org.dromara.workflow.de.hr.service.IDeHrService;
import org.springframework.web.bind.annotation.*;

/**
 * HR 域 REST 端点（HR-03 / HR-05 / HR-06 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/hr")
public class DeHrController extends BaseController {

    private final IDeHrService deHrService;

    /** HR-03 责任分派 */
    @PostMapping("/assign")
    public R<Long> assign(@Valid @RequestBody DeHrAssignBo bo) {
        return R.ok("分派成功", deHrService.assign(bo));
    }

    /** HR-05 催办 / 延期 / 请人 / 退回 / 升级 */
    @PostMapping("/task-action")
    public R<Long> taskAction(@Valid @RequestBody DeHrTaskActionBo bo) {
        return R.ok("动作已记录", deHrService.taskAction(bo));
    }

    /** HR-06 人员离岗 / 休假 / 停用 后交接 */
    @PostMapping("/handover")
    public R<Integer> handover(@Valid @RequestBody DeHrHandoverBo bo) {
        return R.ok("交接完成", deHrService.handover(bo));
    }
}
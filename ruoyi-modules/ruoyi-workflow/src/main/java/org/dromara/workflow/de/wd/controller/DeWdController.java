package org.dromara.workflow.de.wd.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.wd.domain.bo.DeWdTaskFilterBo;
import org.dromara.workflow.de.wd.domain.vo.DeWdTaskListVo;
import org.dromara.workflow.de.wd.service.IDeWdService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作台 WD 域 REST 端点（WD-03 / WD-05 / WD-06 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/wd")
public class DeWdController extends BaseController {

    private final IDeWdService deWdService;

    /** WD-03：列出「AI 执行中」任务。 GET /de/wd/in-progress?userId=xxx */
    @GetMapping("/in-progress")
    public R<List<DeWdTaskListVo>> listInProgress(@RequestParam Long userId) {
        return R.ok(deWdService.listInProgress(userId));
    }

    /** WD-05：工作台任务筛选。 POST /de/wd/filter */
    @PostMapping("/filter")
    public R<List<DeWdTaskListVo>> filterTasks(@Valid @RequestBody DeWdTaskFilterBo bo) {
        return R.ok(deWdService.filterTasks(bo));
    }

    /** WD-06：异常 / 求助 / 审核 / 资源失效 集中「需要我处理」。 GET /de/wd/action-required?userId=xxx */
    @GetMapping("/action-required")
    public R<List<DeWdTaskListVo>> listActionRequired(@RequestParam Long userId) {
        return R.ok(deWdService.listActionRequired(userId));
    }
}
package org.dromara.workflow.de.at.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.at.domain.bo.DeAtCreateBo;
import org.dromara.workflow.de.at.domain.bo.DeAtRunConfigBo;
import org.dromara.workflow.de.at.service.IDeAtService;
import org.springframework.web.bind.annotation.*;

/**
 * AT 域 REST 端点（AT-01 / AT-04 共 2 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/at")
public class DeAtController extends BaseController {

    private final IDeAtService deAtService;

    /** AT-01 自动化创建 */
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody DeAtCreateBo bo) {
        return R.ok("automation 创建已记录", deAtService.create(bo));
    }

    /** AT-04 自动化运行配置 */
    @PutMapping("/run-config")
    public R<Long> runConfig(@Valid @RequestBody DeAtRunConfigBo bo) {
        return R.ok("run-config 已更新", deAtService.runConfig(bo));
    }
}
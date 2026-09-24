package org.dromara.workflow.de.rs.controller;

import jakarta.validation.Valid;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.rs.domain.bo.DeRsConfirmBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsCrossCheckBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsDecideBo;
import org.dromara.workflow.de.rs.service.IDeRsService;
import org.springframework.web.bind.annotation.*;

/**
 * RS 域 REST 端点（RS-05 / RS-06 / RS-07 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/rs")
public class DeRsController extends BaseController {

    private final IDeRsService deRsService;

    /** RS-05 审核者决定 */
    @SaCheckPermission("workflow:de:rs:edit")
    @PostMapping("/decide")
    public R<Long> decide(@Valid @RequestBody DeRsDecideBo bo) {
        return R.ok("决定已写入历史", deRsService.decide(bo));
    }

    /** RS-06 交叉核验 */
    @SaCheckPermission("workflow:de:rs:edit")
    @PostMapping("/cross-check")
    public R<Boolean> crossCheck(@Valid @RequestBody DeRsCrossCheckBo bo) {
        return R.ok(deRsService.crossCheck(bo));
    }

    /** RS-07 接收确认 / 权限不足 / 业务取消 / 回退 / 补偿 */
    @SaCheckPermission("workflow:de:rs:edit")
    @PostMapping("/confirm")
    public R<Long> confirm(@Valid @RequestBody DeRsConfirmBo bo) {
        return R.ok("确认动作已记录", deRsService.confirm(bo));
    }
}
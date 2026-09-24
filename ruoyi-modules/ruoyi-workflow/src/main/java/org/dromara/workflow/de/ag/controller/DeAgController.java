package org.dromara.workflow.de.ag.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.ag.domain.vo.DeAgOverviewVo;
import org.dromara.workflow.de.ag.service.IDeAgService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AG 域 REST 端点（AG-04 / AG-05 共 2 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/ag")
public class DeAgController extends BaseController {

    private final IDeAgService deAgService;

    /** AG-04 数字员工详情页 */
    @SaCheckPermission("workflow:de:ag:query")
    @GetMapping("/{agentId}/overview")
    public R<DeAgOverviewVo> overview(@PathVariable Long agentId) {
        return R.ok(deAgService.overview(agentId));
    }

    /** AG-05 我创建的 / 共享给我的 / 团队使用 */
    @SaCheckPermission("workflow:de:ag:query")
    @GetMapping("/{agentId}/relations")
    public R<List<String>> relations(@PathVariable Long agentId, @RequestParam(required = false) Long userId) {
        return R.ok(deAgService.relations(agentId, userId));
    }
}
package org.dromara.workflow.de.tm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.tm.service.IDeTmService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TM 域 REST 端点（TM-03 / TM-06 共 2 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/tm")
public class DeTmController extends BaseController {

    private final IDeTmService deTmService;

    /** TM-03 团队共享技能 vs 私有能力 */
    @SaCheckPermission("workflow:de:tm:query")
    @GetMapping("/{teamId}/skill-scope")
    public R<Map<String, Object>> skillScope(@PathVariable Long teamId,
                                              @RequestParam(required = false) Long userId) {
        return R.ok(deTmService.skillScope(teamId, userId));
    }

    /** TM-06 团队执行输出对接节点完成检查与交接包 */
    @SaCheckPermission("workflow:de:tm:edit")
    @PostMapping("/{teamId}/handover/{taskId}")
    public R<Map<String, Object>> handover(@PathVariable Long teamId, @PathVariable Long taskId) {
        return R.ok(deTmService.handover(teamId, taskId));
    }
}
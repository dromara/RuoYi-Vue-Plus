package org.dromara.workflow.de.sk.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.sk.domain.vo.DeSkCardVo;
import org.dromara.workflow.de.sk.service.IDeSkService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SK 域 REST 端点（SK-01 / SK-02 / SK-06 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sk")
public class DeSkController extends BaseController {

    private final IDeSkService deSkService;

    /** SK-01 技能目录 */
    @GetMapping("/catalog")
    public R<List<Map<String, Object>>> catalog(@RequestParam(required = false) Long userId) {
        return R.ok(deSkService.catalog(userId));
    }

    /** SK-02 技能卡 */
    @GetMapping("/{skillId}/card")
    public R<DeSkCardVo> card(@PathVariable Long skillId) {
        return R.ok(deSkService.card(skillId));
    }

    /** SK-06 技能依赖 */
    @GetMapping("/{skillId}/dependencies")
    public R<Map<String, Object>> dependencies(@PathVariable Long skillId) {
        return R.ok(deSkService.dependencies(skillId));
    }
}
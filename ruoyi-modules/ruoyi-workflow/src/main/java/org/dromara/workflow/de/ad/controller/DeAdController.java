package org.dromara.workflow.de.ad.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.ad.domain.bo.DeAdHandoverBo;
import org.dromara.workflow.de.ad.service.IDeAdService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AD 域 REST 端点（AD-03 / AD-04 / AD-07 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/ad")
public class DeAdController extends BaseController {

    private final IDeAdService deAdService;

    /** AD-03 维度分开 */
    @SaCheckPermission("workflow:de:ad:query:dimension")
    @GetMapping("/dimension-separate")
    public R<Map<String, Object>> dimensionSeparate(@RequestParam Long userId) {
        return R.ok(deAdService.dimensionSeparate(userId));
    }

    /** AD-04 资源目录 */
    @SaCheckPermission("workflow:de:ad:query:catalog")
    @GetMapping("/resource-catalog")
    public R<List<Map<String, Object>>> resourceCatalog(@RequestParam Long userId) {
        return R.ok(deAdService.resourceCatalog(userId));
    }

    /** AD-07 资产交接一致性预览 */
    @SaCheckPermission("workflow:de:ad:edit:handover")
    @PostMapping("/asset-handover")
    public R<Map<String, Object>> assetHandover(@Valid @RequestBody DeAdHandoverBo bo) {
        return R.ok("交接预览已生成", deAdService.assetHandover(bo));
    }
}
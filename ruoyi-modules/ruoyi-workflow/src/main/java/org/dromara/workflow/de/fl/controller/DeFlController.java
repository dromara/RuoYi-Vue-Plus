package org.dromara.workflow.de.fl.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.fl.domain.bo.DeFlExecuteBo;
import org.dromara.workflow.de.fl.service.IDeFlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FL 域 REST 端点（FL-01 / FL-03 / FL-04 / FL-05 共 4 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/fl")
public class DeFlController extends BaseController {

    private final IDeFlService deFlService;

    /** FL-01 节点可选择的执行方案目录 */
    @GetMapping("/catalog")
    public R<List<Map<String, Object>>> catalog(@RequestParam Long nodeId) {
        return R.ok(deFlService.catalog(nodeId));
    }

    /** FL-03 结构化执行 */
    @PostMapping("/execute")
    public R<Long> execute(@Valid @RequestBody DeFlExecuteBo bo) {
        return R.ok("execute 已记录", deFlService.execute(bo));
    }

    /** FL-04 方案版本 */
    @GetMapping("/versions")
    public R<List<Map<String, Object>>> versions(@RequestParam Long nodeId) {
        return R.ok(deFlService.versions(nodeId));
    }

    /** FL-05 节点执行轨迹 */
    @GetMapping("/{taskId}/trace")
    public R<Map<String, Object>> trace(@PathVariable Long taskId) {
        return R.ok(deFlService.trace(taskId));
    }
}
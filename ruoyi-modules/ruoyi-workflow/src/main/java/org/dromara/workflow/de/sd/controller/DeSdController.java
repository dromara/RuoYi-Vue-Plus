package org.dromara.workflow.de.sd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.sd.service.IDeSdService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SD 域 REST 端点（SD-02 / SD-03 / SD-04 / SD-05 / SD-06 共 5 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sd")
public class DeSdController extends BaseController {

    private final IDeSdService deSdService;

    /** SD-02 属性区组织 */
    @SaCheckPermission("workflow:de:sd:query")
    @GetMapping("/{sopId}/attributes")
    public R<Map<String, Object>> attributes(@PathVariable Long sopId) {
        return R.ok(deSdService.attributes(sopId));
    }

    /** SD-03 资源规则 */
    @SaCheckPermission("workflow:de:sd:query")
    @GetMapping("/{sopId}/resource-rules")
    public R<Map<String, Object>> resourceRules(@PathVariable Long sopId) {
        return R.ok(deSdService.resourceRules(sopId));
    }

    /** SD-04 员工视角预览 */
    @SaCheckPermission("workflow:de:sd:query")
    @GetMapping("/{sopId}/preview")
    public R<Map<String, Object>> preview(@PathVariable Long sopId,
                                            @RequestParam(required = false) Long userId) {
        return R.ok(deSdService.preview(sopId, userId));
    }

    /** SD-05 流程图（分支 / 并行 / 等待 / 超时 / 返工 / 子流程 / 终止补偿） */
    @SaCheckPermission("workflow:de:sd:query")
    @GetMapping("/{sopId}/graph")
    public R<Map<String, Object>> graph(@PathVariable Long sopId) {
        return R.ok(deSdService.graph(sopId));
    }

    /** SD-06 版本 / Diff / 发布 / 回退 / 在途影响 */
    @SaCheckPermission("workflow:de:sd:query")
    @GetMapping("/{sopId}/versions")
    public R<Map<String, Object>> versions(@PathVariable Long sopId) {
        return R.ok(deSdService.versions(sopId));
    }
}
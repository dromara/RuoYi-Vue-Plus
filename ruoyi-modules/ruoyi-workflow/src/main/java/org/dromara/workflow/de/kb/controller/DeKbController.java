package org.dromara.workflow.de.kb.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.kb.domain.vo.DeKbWorkspaceVo;
import org.dromara.workflow.de.kb.service.IDeKbService;
import org.springframework.web.bind.annotation.*;

/**
 * KB 域 REST 端点（KB-01 共 1 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/kb")
public class DeKbController extends BaseController {

    private final IDeKbService deKbService;

    /** KB-01 知识详情工作区 */
    @SaCheckPermission("workflow:de:kb:query")
    @GetMapping("/{documentId}/workspace")
    public R<DeKbWorkspaceVo> workspace(@PathVariable Long documentId,
                                          @RequestParam(required = false) Long userId) {
        return R.ok(deKbService.workspace(documentId, userId));
    }
}
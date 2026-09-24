package org.dromara.workflow.de.ws.controller;

import jakarta.validation.Valid;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.ws.domain.bo.DeWsAttachmentBo;
import org.dromara.workflow.de.ws.domain.bo.DeWsBindBo;
import org.dromara.workflow.de.ws.domain.bo.DeWsPermissionsBo;
import org.dromara.workflow.de.ws.service.IDeWsService;
import org.springframework.web.bind.annotation.*;

/**
 * WS 域 REST 端点（WS-01 / WS-02 / WS-04 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/ws")
public class DeWsController extends BaseController {

    private final IDeWsService deWsService;

    /** WS-01 工作空间绑定节点 */
    @SaCheckPermission("workflow:de:ws:edit")
    @PostMapping("/bind")
    public R<Long> bind(@Valid @RequestBody DeWsBindBo bo) {
        return R.ok("workspace 已绑定", deWsService.bind(bo));
    }

    /** WS-02 节点附件 */
    @SaCheckPermission("workflow:de:ws:edit")
    @PostMapping("/attachment")
    public R<Long> attachment(@Valid @RequestBody DeWsAttachmentBo bo) {
        return R.ok("附件已引用", deWsService.attachment(bo));
    }

    /** WS-04 文件读写 / 网络 / 命令权限 */
    @SaCheckPermission("workflow:de:ws:edit")
    @PutMapping("/permissions")
    public R<Long> permissions(@Valid @RequestBody DeWsPermissionsBo bo) {
        return R.ok("exec 权限已写入", deWsService.permissions(bo));
    }
}
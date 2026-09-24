package org.dromara.workflow.de.im.controller;

import jakarta.validation.Valid;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.im.domain.bo.DeImExpertConsultBo;
import org.dromara.workflow.de.im.domain.bo.DeImRecoverBo;
import org.dromara.workflow.de.im.service.IDeImService;
import org.springframework.web.bind.annotation.*;

/**
 * IM 域 REST 端点（IM-04 / IM-06 共 2 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/im")
public class DeImController extends BaseController {

    private final IDeImService deImService;

    /** IM-04 真人专家咨询 */
    @SaCheckPermission("workflow:de:im:edit")
    @PostMapping("/expert-consult")
    public R<Long> expertConsult(@Valid @RequestBody DeImExpertConsultBo bo) {
        return R.ok("专家咨询已创建", deImService.expertConsult(bo));
    }

    /** IM-06 连接 / 成员 / 暂停 / 授权 / 投递恢复 */
    @SaCheckPermission("workflow:de:im:edit")
    @PostMapping("/recover")
    public R<Long> recover(@Valid @RequestBody DeImRecoverBo bo) {
        return R.ok("恢复动作已派发", deImService.recover(bo));
    }
}
package org.dromara.system.controller.system;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SysMessageBoxVo;
import org.dromara.system.service.ISysMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息记录控制器
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/message")
public class SysMessageController extends BaseController {

    private final ISysMessageService messageService;

    /**
     * 查询当前用户消息盒子数据
     *
     * @return 消息盒子数据
     */
    @GetMapping("/box")
    public R<SysMessageBoxVo> getBox() {
        return R.ok(messageService.queryMessageBox(LoginHelper.getUserId()));
    }
}

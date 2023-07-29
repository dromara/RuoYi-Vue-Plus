package org.dromara.system.controller.system;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.service.ISysSocialService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社会化关系
 *
 * @author thiszhc
 * @date 2023-06-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/social")
public class SysSocialController extends BaseController {

    private final ISysSocialService socialUserService;

    /**
     * 查询社会化关系列表
     */
    @GetMapping("/list")
    public R<List<SysSocialVo>> list() {
        return R.ok(socialUserService.queryListByUserId(LoginHelper.getUserId()));
    }


    /**
     * 获取社会化关系详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<SysSocialVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable String id) {
        return R.ok(socialUserService.queryById(id));
    }

}

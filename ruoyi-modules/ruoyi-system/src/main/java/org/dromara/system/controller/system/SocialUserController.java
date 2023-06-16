package org.dromara.system.controller.system;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SocialUserVo;
import org.dromara.system.service.ISocialUserService;
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
public class SocialUserController extends BaseController {

    private final ISocialUserService socialUserService;

    /**
     * 查询社会化关系列表
     */
//    这里改成用户默认的。只能查看自己的权限更好哦
//    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public R<List<SocialUserVo>> list() {
        return R.ok(socialUserService.queryList());
    }


    /**
     * 获取社会化关系详细信息
     *
     * @param id 主键
     */
//    这里改成用户默认的。只能查看自己的权限更好哦
//    @SaCheckPermission("system:user:query")
    @GetMapping("/{id}")
    public R<SocialUserVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable String id) {
        return R.ok(socialUserService.queryById(id));
    }



}

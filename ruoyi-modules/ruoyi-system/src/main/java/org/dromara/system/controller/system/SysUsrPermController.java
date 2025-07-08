package org.dromara.system.controller.system;


import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.bo.SysUsrPermBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.domain.vo.SysUsrPermVo;
import org.dromara.system.service.ISysUsrPermService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/system/usrPerm")
public class SysUsrPermController extends BaseController {
    private final ISysUsrPermService usrPerm;
    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public TableDataInfo<SysUsrPermVo> list(SysUsrPermBo user, PageQuery pageQuery) {
        return usrPerm.selectUsrPermList(user,pageQuery);
    }
}

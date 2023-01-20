package com.ruoyi.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.system.domain.SysMenu;
import com.ruoyi.system.domain.SysUser;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysLoginService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 登录验证
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
public class SysLoginController {

    private final SysLoginService loginService;
    private final ISysMenuService menuService;
    private final ISysUserService userService;

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginBody loginBody) {
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
            loginBody.getUuid());
        return R.ok(Map.of(Constants.TOKEN, token));
    }

    /**
     * 短信登录(示例)
     *
     * @param smsLoginBody 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/smsLogin")
    public R<Map<String, Object>> smsLogin(@Validated @RequestBody SmsLoginBody smsLoginBody) {
        // 生成令牌
        String token = loginService.smsLogin(smsLoginBody.getPhonenumber(), smsLoginBody.getSmsCode());
        return R.ok(Map.of(Constants.TOKEN, token));
    }

    /**
     * 小程序登录(示例)
     *
     * @param xcxCode 小程序code
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/xcxLogin")
    public R<Map<String, Object>> xcxLogin(@NotBlank(message = "{xcx.code.not.blank}") String xcxCode) {
        // 生成令牌
        String token = loginService.xcxLogin(xcxCode);
        return R.ok(Map.of(Constants.TOKEN, token));
    }

    /**
     * 退出登录
     */
    @SaIgnore
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public R<Map<String, Object>> getInfo() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        SysUser user = userService.selectUserById(loginUser.getUserId());
        return R.ok(Map.of(
            "user", user,
            "roles", loginUser.getRolePermission(),
            "permissions", loginUser.getMenuPermission()
        ));
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public R<List<RouterVo>> getRouters() {
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(LoginHelper.getUserId());
        return R.ok(menuService.buildMenus(menus));
    }
}

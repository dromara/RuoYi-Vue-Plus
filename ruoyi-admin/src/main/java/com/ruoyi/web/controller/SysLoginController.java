package com.ruoyi.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.core.utils.StreamUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.common.tenant.helper.TenantHelper;
import com.ruoyi.system.domain.SysMenu;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.domain.vo.SysUserVo;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.domain.vo.LoginVo;
import com.ruoyi.web.domain.vo.TenantListVo;
import com.ruoyi.web.domain.vo.UserInfoVo;
import com.ruoyi.web.service.SysLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.List;

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
    private final ISysTenantService tenantService;

    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/login")
    public R<LoginVo> login(@Validated @RequestBody LoginBody body) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.login(
                body.getTenantId(),
                body.getUsername(), body.getPassword(),
                body.getCode(), body.getUuid());
        loginVo.setToken(token);
        return R.ok(loginVo);
    }

    /**
     * 短信登录(示例)
     *
     * @param body 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/smsLogin")
    public R<LoginVo> smsLogin(@Validated @RequestBody SmsLoginBody body) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.smsLogin(body.getTenantId(), body.getPhonenumber(), body.getSmsCode());
        loginVo.setToken(token);
        return R.ok(loginVo);
    }

    /**
     * 小程序登录(示例)
     *
     * @param xcxCode 小程序code
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/xcxLogin")
    public R<LoginVo> xcxLogin(@NotBlank(message = "{xcx.code.not.blank}") String xcxCode) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.xcxLogin(xcxCode);
        loginVo.setToken(token);
        return R.ok(loginVo);
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
     * 登录页面租户下拉框
     *
     * @return 租户列表
     */
    @SaIgnore
    @GetMapping("/tenant/list")
    public R<List<TenantListVo>> tenantList(HttpServletRequest request) throws Exception {
        List<SysTenantVo> tenantList = tenantService.queryList(new SysTenantBo());
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        // 获取域名
        String host = new URL(request.getRequestURL().toString()).getHost();
        // 根据域名进行筛选
        List<TenantListVo> list = StreamUtils.filter(voList, vo -> StringUtils.equals(vo.getDomain(), host));
        return R.ok(CollUtil.isNotEmpty(list) ? list : voList);
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public R<UserInfoVo> getInfo() {
        UserInfoVo userInfoVo = new UserInfoVo();
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (TenantHelper.isEnable() && LoginHelper.isSuperAdmin()) {
            // 超级管理员 如果重新加载用户信息需清除动态租户
            TenantHelper.clearDynamic();
        }
        SysUserVo user = userService.selectUserById(loginUser.getUserId());
        userInfoVo.setUser(user);
        userInfoVo.setPermissions(loginUser.getMenuPermission());
        userInfoVo.setRoles(loginUser.getRolePermission());
        return R.ok(userInfoVo);
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

package com.ruoyi.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.common.core.utils.MapstructUtils;
import com.ruoyi.common.core.utils.StreamUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.web.domain.vo.LoginVo;
import com.ruoyi.web.domain.vo.TenantListVo;
import com.ruoyi.web.service.SysLoginService;
import com.ruoyi.web.service.SysRegisterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;

/**
 * 认证
 *
 * @author Lion Li
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysConfigService configService;
    private final ISysTenantService tenantService;

    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
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
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user) {
        if (!configService.selectRegisterEnabled(user.getTenantId())) {
            return R.fail("当前系统没有开启注册功能！");
        }
        registerService.register(user);
        return R.ok();
    }

    /**
     * 登录页面租户下拉框
     *
     * @return 租户列表
     */
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

}

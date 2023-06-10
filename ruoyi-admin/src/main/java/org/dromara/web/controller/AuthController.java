package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.dromara.common.auth.utils.AuthUtils;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.EmailLoginBody;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.core.domain.model.RegisterBody;
import org.dromara.common.core.domain.model.SmsLoginBody;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysTenantService;
import org.dromara.web.domain.vo.LoginTenantVo;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.domain.vo.TenantListVo;
import org.dromara.web.service.SysLoginService;
import org.dromara.web.service.SysRegisterService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private AuthStateCache authStateCache;
    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysConfigService configService;
    private final ISysTenantService tenantService;
    private final SysUserMapper userMapper;
    private final Map<String, String> auths = new HashMap<>();
    {
        auths.put("gitee", "{\"clientId\":\"38eaaa1b77b5e064313057a2f5745ce3a9f3e7686d9bd302c7df2f308ef6db81\",\"clientSecret\":\"2e633af8780cb9fe002c4c7291b722db944402e271efb99b062811f52d7da1ff\",\"redirectUri\":\"http://127.0.0.1:8888/social-login?source=gitee\"}");
        auths.put("github", "{\"clientId\":\"Iv1.1be0cdcd71aca63b\",\"clientSecret\":\"0d59d28b43152bc8906011624db37b0fed88d154\",\"redirectUri\":\"http://127.0.0.1:80/social-login?source=github\"}");
        authStateCache = AuthDefaultStateCache.INSTANCE;// 使用默认的缓存
    }

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
     * 短信登录
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/smsLogin")
    public R<LoginVo> smsLogin(@Validated @RequestBody SmsLoginBody body) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.smsLogin(
            body.getTenantId(),
            body.getPhonenumber(),
            body.getSmsCode());
        loginVo.setToken(token);
        return R.ok(loginVo);
    }

    /**
     * 邮件登录
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/emailLogin")
    public R<LoginVo> emailLogin(@Validated @RequestBody EmailLoginBody body) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.emailLogin(
            body.getTenantId(),
            body.getEmail(),
            body.getEmailCode());
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
     * 认证授权
     * @param source
     * @throws IOException
     */
    @GetMapping("/binding/{source}")
    @ResponseBody
    public R<LoginVo> authBinding(@PathVariable("source") String source, HttpServletRequest request){
        SysUserVo userLoding = new SysUserVo();
        if (ObjectUtil.isNull(userLoding)) {
            return R.fail("授权失败，请先登录再绑定");
        }
        if (userMapper.checkAuthUser(userLoding.getUserId(),source) > 0)
        {
            return R.fail(source + "平台账号已经绑定");
        }
        String obj = auths.get(source);
        if (StringUtils.isEmpty(obj))
        {
            return R.fail(source + "平台账号暂不支持");
        }
        JSONObject json = JSONUtil.parseObj(obj);
        AuthRequest authRequest = AuthUtils.getAuthRequest(source,
            json.getStr("clientId"),
            json.getStr("clientSecret"),
            json.getStr("redirectUri"), authStateCache);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return R.ok(authorizeUrl);
    }

    /**
     * @param source
     * @param callback
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/social-login/{source}")
    public R<String> socialLogin(@PathVariable("source") String source, AuthCallback callback, HttpServletRequest request) throws IOException {
        String obj = auths.get(source);
        if (StringUtils.isEmpty(obj))
        {
            return R.fail("第三方平台系统不支持或未提供来源");
        }
        JSONObject json = JSONUtil.parseObj(obj);
        AuthRequest authRequest = AuthUtils.getAuthRequest(source,
            json.getStr("clientId"),
            json.getStr("clientSecret"),
            json.getStr("redirectUri"), authStateCache);
        AuthResponse<AuthUser> response = authRequest.login(callback);
        return loginService.socialLogin(source, response, request);
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
    public R<LoginTenantVo> tenantList(HttpServletRequest request) throws Exception {
        List<SysTenantVo> tenantList = tenantService.queryList(new SysTenantBo());
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        // 获取域名
        String host;
        String referer = request.getHeader("referer");
        if (StringUtils.isNotBlank(referer)) {
            // 这里从referer中取值是为了本地使用hosts添加虚拟域名，方便本地环境调试
            host = referer.split("//")[1].split("/")[0];
        } else {
            host = new URL(request.getRequestURL().toString()).getHost();
        }
        // 根据域名进行筛选
        List<TenantListVo> list = StreamUtils.filter(voList, vo ->
            StringUtils.equals(vo.getDomain(), host));
        // 返回对象
        LoginTenantVo vo = new LoginTenantVo();
        vo.setVoList(CollUtil.isNotEmpty(list) ? list : voList);
        vo.setTenantEnabled(TenantHelper.isEnable());
        return R.ok(vo);
    }

}

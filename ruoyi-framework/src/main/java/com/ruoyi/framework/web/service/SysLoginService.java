package com.ruoyi.framework.web.service;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

	@Autowired
    private ISysUserService userService;

	@Autowired
	private ISysConfigService configService;

	@Autowired
	private AsyncService asyncService;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid)
    {
		HttpServletRequest request = ServletUtils.getRequest();
		boolean captchaOnOff = configService.selectCaptchaOnOff();
        // 验证码开关
        if (captchaOnOff)
        {
            validateCaptcha(username, code, uuid, request);
        }
        // 用户验证
        Authentication authentication = null;
        try
        {
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
				asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match"), request);
                throw new UserPasswordNotMatchException();
            }
            else
            {
				asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage(), request);
                throw new CustomException(e.getMessage());
            }
        }
		asyncService.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"), request);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUser());
        // 生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid, HttpServletRequest request) {
		String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
		String captcha = redisCache.getCacheObject(verifyKey);
		redisCache.deleteObject(verifyKey);
		if (captcha == null) {
			asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"), request);
			throw new CaptchaExpireException();
		}
		if (!code.equalsIgnoreCase(captcha)) {
			asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"), request);
			throw new CaptchaException();
		}
    }

    /**
     * 记录登录信息
     */
    public void recordLoginInfo(SysUser user)
    {
        user.setLoginIp(ServletUtils.getClientIP());
        user.setLoginDate(DateUtils.getNowDate());
		user.setUpdateBy(user.getUserName());
        userService.updateUserProfile(user);
    }
}

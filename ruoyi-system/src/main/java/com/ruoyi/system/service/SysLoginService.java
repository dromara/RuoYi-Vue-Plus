package com.ruoyi.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.service.LogininforService;
import com.ruoyi.common.enums.DeviceType;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserException;
import com.ruoyi.common.utils.*;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 登录校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginService {

    private final ISysUserService userService;
    private final ISysConfigService configService;
    private final LogininforService asyncService;
    private final SysPermissionService permissionService;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid) {
        HttpServletRequest request = ServletUtils.getRequest();
        boolean captchaOnOff = configService.selectCaptchaOnOff();
        // 验证码开关
        if (captchaOnOff) {
            validateCaptcha(username, code, uuid, request);
        }
        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(Constants.LOGIN_ERROR + username);
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(Constants.LOGIN_ERROR_NUMBER)) {
            asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.exceed", Constants.LOGIN_ERROR_LIMIT_TIME), request);
            throw new UserException("user.password.retry.limit.exceed", Constants.LOGIN_ERROR_LIMIT_TIME);
        }

        SysUser user = userService.selectUserByUserName(username);
        if (StringUtils.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UserException("user.not.exists", username);
        } else if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new UserException("user.password.delete", username);
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UserException("user.blocked", username);
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword())) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(Constants.LOGIN_ERROR_NUMBER)) {
                RedisUtils.setCacheObject(Constants.LOGIN_ERROR + username, errorNumber, Constants.LOGIN_ERROR_LIMIT_TIME, TimeUnit.MINUTES);
                asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.exceed", Constants.LOGIN_ERROR_LIMIT_TIME), request);
                throw new UserException("user.password.retry.limit.exceed", Constants.LOGIN_ERROR_LIMIT_TIME);
            } else {
                // 未达到规定错误次数 则递增
                RedisUtils.setCacheObject(Constants.LOGIN_ERROR + username, errorNumber);
                asyncService.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.count", errorNumber), request);
                throw new UserException("user.password.retry.limit.count", errorNumber);
            }
        }

        // 登录成功 清空错误次数
        RedisUtils.deleteObject(Constants.LOGIN_ERROR + username);
        asyncService.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"), request);
        recordLoginInfo(user.getUserId(), username);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setMenuPermission(permissionService.getMenuPermission(user));
        loginUser.setRolePermission(permissionService.getRolePermission(user));

        // 生成token
        LoginUtils.loginByDevice(loginUser, UserType.SYS_USER, DeviceType.PC);
        return StpUtil.getTokenValue();
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid, HttpServletRequest request) {
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
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
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId, String username) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(ServletUtils.getClientIP());
        sysUser.setLoginDate(DateUtils.getNowDate());
        sysUser.setUpdateBy(username);
        userService.updateUserProfile(sysUser);
    }
}

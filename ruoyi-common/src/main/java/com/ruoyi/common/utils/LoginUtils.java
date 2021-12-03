package com.ruoyi.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.DeviceType;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.exception.UtilException;

/**
 * 登录鉴权工具
 * 为适配多端登录而封装
 *
 * @author Lion Li
 */
public class LoginUtils {

    private final static String LOGIN_USER_KEY = "loginUser";

    /**
     * 登录系统
     * 针对两套用户体系
     * @param loginUser 登录用户信息
     */
    public static void login(LoginUser loginUser, UserType userType) {
        StpUtil.login(userType.getUserType() + loginUser.getUserId());
        setLoginUser(loginUser);
    }

    /**
     * 登录系统 基于 设备类型
     * 针对一套用户体系
     * @param loginUser 登录用户信息
     */
    public static void loginByDevice(LoginUser loginUser, UserType userType, DeviceType deviceType) {
        StpUtil.login(userType.getUserType() + loginUser.getUserId(), deviceType.getDevice());
        setLoginUser(loginUser);
    }

    /**
     * 设置用户数据
     */
    public static void setLoginUser(LoginUser loginUser) {
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取用户
     **/
    public static LoginUser getLoginUser() {
        return (LoginUser) StpUtil.getTokenSession().get(LOGIN_USER_KEY);
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNull(loginUser)) {
            String loginId = StpUtil.getLoginIdAsString();
            String userId;
            String replace = "";
            if (StringUtils.contains(loginId, UserType.SYS_USER.getUserType())) {
                userId = StringUtils.replace(loginId, UserType.SYS_USER.getUserType(), replace);
            } else if (StringUtils.contains(loginId, UserType.APP_USER.getUserType())){
                userId = StringUtils.replace(loginId, UserType.APP_USER.getUserType(), replace);
            } else {
                throw new UtilException("登录用户: LoginId异常 => " + loginId);
            }
            return Long.parseLong(userId);
        }
        return loginUser.getUserId();
    }

    /**
     * 获取部门ID
     **/
    public static Long getDeptId() {
        return getLoginUser().getDeptId();
    }

    /**
     * 获取用户账户
     **/
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取用户类型
     */
    public static UserType getUserType() {
        String loginId = StpUtil.getLoginIdAsString();
        return getUserType(loginId);
    }

    public static UserType getUserType(Object loginId) {
        if (StringUtils.contains(loginId.toString(), UserType.SYS_USER.getUserType())) {
            return UserType.SYS_USER;
        } else if (StringUtils.contains(loginId.toString(), UserType.APP_USER.getUserType())){
            return UserType.APP_USER;
        } else {
            throw new UtilException("登录用户: LoginId异常 => " + loginId);
        }
    }

}

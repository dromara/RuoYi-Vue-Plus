package com.ruoyi.common.utils;

import cn.dev33.satoken.stp.StpUtil;
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

    /**
     * 登录系统
     * 针对两套用户体系
     * @param userId 用户id
     */
    public static void login(Long userId, UserType userType) {
        StpUtil.login(userType.getUserType() + userId);
    }

    /**
     * 登录系统 基于 设备类型
     * 针对一套用户体系
     * @param userId 用户id
     */
    public static void loginByDevice(Long userId, UserType userType, DeviceType deviceType) {
        StpUtil.login(userType.getUserType() + userId, deviceType.getDevice());
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
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

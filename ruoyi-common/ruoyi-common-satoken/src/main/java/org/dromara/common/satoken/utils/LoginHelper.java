package org.dromara.common.satoken.utils;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.convert.Convert;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ip.AddressUtils;


/**
 * 登录鉴权助手
 * <p>
 * user_type 为 用户类型 同一个用户表 可以有多种用户类型 例如 pc,app
 * deivce 为 设备类型 同一个用户类型 可以有 多种设备类型 例如 web,ios
 * 可以组成 用户类型与设备类型多对多的 权限灵活控制
 * <p>
 * 多用户体系 针对 多种用户类型 但权限控制不一致
 * 可以组成 多用户类型表与多设备类型 分别控制权限
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String USER_KEY = "userId";
    public static final String USER_NAME_KEY = "userName";
    public static final String DEPT_KEY = "deptId";
    public static final String DEPT_NAME_KEY = "deptName";
    public static final String DEPT_CATEGORY_KEY = "deptCategory";
    public static final String CLIENT_KEY = "clientid";

    /**
     * 登录系统 基于 设备类型
     * 针对相同用户体系不同设备
     *
     * @param loginUser 登录用户信息
     * @param model     配置参数
     */
    public static void login(LoginUser loginUser, SaLoginParameter model) {
        model = ObjectUtil.defaultIfNull(model, new SaLoginParameter());
        fillRequestContext(loginUser, model);
        StpUtil.login(loginUser.getLoginId(),
            model.setExtra(USER_KEY, loginUser.getUserId())
                .setExtra(USER_NAME_KEY, loginUser.getUsername())
                .setExtra(DEPT_KEY, loginUser.getDeptId())
                .setExtra(DEPT_NAME_KEY, loginUser.getDeptName())
                .setExtra(DEPT_CATEGORY_KEY, loginUser.getDeptCategory())
        );
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 在登录时补充当前请求上下文，避免登录态中的终端信息缺失。
     *
     * @param loginUser 登录用户
     * @param model 登录参数
     */
    private static void fillRequestContext(LoginUser loginUser, SaLoginParameter model) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (ObjectUtil.isNull(request)) {
            return;
        }
        String ip = ServletUtils.getClientIP(request);
        if (StringUtils.isBlank(loginUser.getIpaddr())) {
            loginUser.setIpaddr(ip);
        }
        if (StringUtils.isBlank(loginUser.getLoginLocation()) && StringUtils.isNotBlank(ip)) {
            loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        }
        UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        if (StringUtils.isBlank(loginUser.getBrowser())) {
            loginUser.setBrowser(userAgent.getBrowser().getName());
        }
        if (StringUtils.isBlank(loginUser.getOs())) {
            loginUser.setOs(userAgent.getOs().getName());
        }
        if (StringUtils.isBlank(loginUser.getDeviceType()) && StringUtils.isNotBlank(model.getDeviceType())) {
            loginUser.setDeviceType(model.getDeviceType());
        }
    }

    /**
     * 获取当前 Token 对应的登录用户信息。
     *
     * @return 登录用户
     */
    @SuppressWarnings("unchecked")
    public static <T extends LoginUser> T getLoginUser() {
        SaSession session = StpUtil.getTokenSession();
        if (ObjectUtil.isNull(session)) {
            return null;
        }
        return (T) session.get(LOGIN_USER_KEY);
    }

    /**
     * 根据指定 Token 获取登录用户信息。
     *
     * @param token Token
     * @return 登录用户
     */
    @SuppressWarnings("unchecked")
    public static <T extends LoginUser> T getLoginUser(String token) {
        SaSession session = StpUtil.getTokenSessionByToken(token);
        if (ObjectUtil.isNull(session)) {
            return null;
        }
        return (T) session.get(LOGIN_USER_KEY);
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        return Convert.toLong(getExtra(USER_KEY));
    }

    /**
     * 获取当前登录用户 ID 字符串。
     *
     * @return 用户 ID 字符串
     */
    public static String getUserIdStr() {
        return Convert.toStr(getExtra(USER_KEY));
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 用户名
     */
    public static String getUsername() {
        return Convert.toStr(getExtra(USER_NAME_KEY));
    }

    /**
     * 获取当前登录用户部门 ID。
     *
     * @return 部门 ID
     */
    public static Long getDeptId() {
        return Convert.toLong(getExtra(DEPT_KEY));
    }

    /**
     * 获取当前登录用户部门名称。
     *
     * @return 部门名称
     */
    public static String getDeptName() {
        return Convert.toStr(getExtra(DEPT_NAME_KEY));
    }

    /**
     * 获取当前登录用户部门类别编码。
     *
     * @return 部门类别编码
     */
    public static String getDeptCategory() {
        return Convert.toStr(getExtra(DEPT_CATEGORY_KEY));
    }

    /**
     * 获取当前 Token 的扩展信息
     *
     * @param key 键值
     * @return 对应的扩展数据
     */
    private static Object getExtra(String key) {
        try {
            return StpUtil.getExtra(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前登录用户类型。
     *
     * @return 用户类型
     */
    public static UserType getUserType() {
        String loginType = StpUtil.getLoginIdAsString();
        return UserType.getUserType(loginType);
    }

    /**
     * 是否为超级管理员
     *
     * @param userId 用户ID
     * @return 是否为超级管理员
     */
    public static boolean isSuperAdmin(Long userId) {
        return SystemConstants.SUPER_ADMIN_USER_ID.equals(userId);
    }

    /**
     * 是否为超级管理员
     *
     * @return 是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        return isSuperAdmin(getUserId());
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isLogin() {
        try {
            StpUtil.checkLogin();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

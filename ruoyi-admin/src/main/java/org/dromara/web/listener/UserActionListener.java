package org.dromara.web.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.web.event.UserLoginSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 用户行为监听器，用于同步在线状态和登录日志。
 *
 * @author Lion Li
 */
@Component
@Slf4j
public class UserActionListener implements SaTokenListener {

    /**
     * 登录成功后记录在线信息并写入登录日志。
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginParameter) {
        SpringUtils.context().publishEvent(new UserLoginSuccessEvent(loginId, tokenValue, loginParameter));
    }

    /**
     * 注销时清理在线缓存。
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogout, userId:{}, token:***{}", loginId, StringUtils.right(tokenValue, 8));
    }

    /**
     * 被踢下线时清理在线缓存。
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doKickout, userId:{}, token:***{}", loginId, StringUtils.right(tokenValue, 8));
    }

    /**
     * 被顶下线时清理在线缓存。
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doReplaced, userId:{}, token:***{}", loginId, StringUtils.right(tokenValue, 8));
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
    }

    /**
     * 每次打开二级认证时触发
     */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
    }

    /**
     * 每次Token续期时触发
     */
    @Override
    public void doRenewTimeout(String loginType, Object loginId, String tokenValue, long timeout) {
    }
}

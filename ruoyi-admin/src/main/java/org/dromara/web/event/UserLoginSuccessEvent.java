package org.dromara.web.event;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;

/**
 * 用户登录成功事件。
 *
 * @param loginId        登录标识
 * @param tokenValue     token 值
 * @param loginParameter 登录参数
 */
public record UserLoginSuccessEvent(Object loginId, String tokenValue, SaLoginParameter loginParameter) {
}

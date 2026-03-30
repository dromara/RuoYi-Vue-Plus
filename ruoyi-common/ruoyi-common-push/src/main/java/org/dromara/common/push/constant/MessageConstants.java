package org.dromara.common.push.constant;

/**
 * 模块通用消息常量定义。
 *
 * @author Lion Li
 */
public interface MessageConstants {

    /**
     * 登录用户信息
     */
    String LOGIN_USER_KEY = "loginUser";

    /**
     * 登录令牌
     */
    String LOGIN_TOKEN_KEY = "token";

    /**
     * 全局消息订阅主题
     */
    String MESSAGE_TOPIC = "global:message";

    /**
     * 心跳请求标识
     */
    String PING = "ping";

    /**
     * 心跳响应标识
     */
    String PONG = "pong";
}

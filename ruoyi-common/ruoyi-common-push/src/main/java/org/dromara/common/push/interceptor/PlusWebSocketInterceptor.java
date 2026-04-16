package org.dromara.common.push.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.push.constant.MessageConstants;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.model.LoginUser;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器。
 *
 * @author Lion Li
 */
@Slf4j
public class PlusWebSocketInterceptor implements HandshakeInterceptor {

    /**
     * 握手前拦截（核心认证逻辑）
     * 校验登录状态、Token、客户端ID，认证通过才允许建立 WebSocket 连接
     *
     * @param attributes 用于传递到 WebSocketSession 的属性集合
     * @return 是否允许握手（true=允许，false=拒绝）
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            // 1. 获取当前登录用户与 Token
            LoginUser loginUser = LoginHelper.getLoginUser();
            String tokenValue = StpUtil.getTokenValue();

            // 2. 未登录直接拒绝握手
            if (loginUser == null || StringUtils.isBlank(tokenValue)) {
                return false;
            }

            // 3. 校验客户端ID（防止多端冒用）
            String headerCid = ServletUtils.getRequest().getHeader(LoginHelper.CLIENT_KEY);
            String paramCid = ServletUtils.getParameter(LoginHelper.CLIENT_KEY);
            String clientId = StpUtil.getExtra(LoginHelper.CLIENT_KEY).toString();

            // 客户端ID必须与请求头/参数中的一致，否则拒绝连接
            if (!StringUtils.equalsAny(clientId, headerCid, paramCid)) {
                throw NotLoginException.newInstance(StpUtil.getLoginType(),
                    "-100", "客户端ID与Token不匹配",
                    StpUtil.getTokenValue());
            }

            // 4. 认证通过，将用户信息存入会话属性，供后续 WebSocketHandler 使用
            attributes.put(MessageConstants.LOGIN_USER_KEY, loginUser);
            attributes.put(MessageConstants.LOGIN_TOKEN_KEY, tokenValue);
            return true;
        } catch (NotLoginException e) {
            // 认证失败，记录日志并拒绝连接
            log.error("WebSocket 认证失败'{}',无法访问系统资源", e.getMessage());
            return false;
        }
    }

    /**
     * 握手完成后触发
     * 此处无需处理，留空即可
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}

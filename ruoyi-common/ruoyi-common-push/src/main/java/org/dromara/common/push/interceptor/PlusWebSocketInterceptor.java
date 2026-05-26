package org.dromara.common.push.interceptor;

import cn.dev33.satoken.stp.StpUtil;
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
public class PlusWebSocketInterceptor implements HandshakeInterceptor {

    /**
     * 握手前提取统一鉴权后的用户信息。
     *
     * @param attributes 用于传递到 WebSocketSession 的属性集合
     * @return 是否允许握手（true=允许，false=拒绝）
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        String tokenValue = StpUtil.getTokenValue();
        attributes.put(MessageConstants.LOGIN_USER_KEY, loginUser);
        attributes.put(MessageConstants.LOGIN_TOKEN_KEY, tokenValue);
        return true;
    }

    /**
     * 握手完成后触发
     * 此处无需处理，留空即可
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}

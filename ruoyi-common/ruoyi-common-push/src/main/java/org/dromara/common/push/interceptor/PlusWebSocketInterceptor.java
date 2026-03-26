package org.dromara.common.push.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.push.constant.MessageConstants;
import org.dromara.common.satoken.utils.LoginHelper;
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

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            String tokenValue = StpUtil.getTokenValue();
            if (loginUser == null || StringUtils.isBlank(tokenValue)) {
                return false;
            }

            String headerCid = ServletUtils.getRequest().getHeader(LoginHelper.CLIENT_KEY);
            String paramCid = ServletUtils.getParameter(LoginHelper.CLIENT_KEY);
            String clientId = StpUtil.getExtra(LoginHelper.CLIENT_KEY).toString();
            if (!StringUtils.equalsAny(clientId, headerCid, paramCid)) {
                throw NotLoginException.newInstance(StpUtil.getLoginType(),
                    "-100", "客户端ID与Token不匹配",
                    StpUtil.getTokenValue());
            }

            attributes.put(MessageConstants.LOGIN_USER_KEY, loginUser);
            attributes.put(MessageConstants.LOGIN_TOKEN_KEY, tokenValue);
            return true;
        } catch (NotLoginException e) {
            log.error("WebSocket 认证失败'{}',无法访问系统资源", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }
}

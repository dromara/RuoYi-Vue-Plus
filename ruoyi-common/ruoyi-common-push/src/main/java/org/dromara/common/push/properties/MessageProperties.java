package org.dromara.common.push.properties;

import lombok.Data;
import org.dromara.common.push.enums.MessageTransportEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一消息推送配置。
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties("message")
public class MessageProperties {

    /**
     * 是否启用消息推送。
     */
    private Boolean enabled = true;

    /**
     * 传输方式：sse / websocket。
     */
    private String transport = MessageTransportEnum.SSE.getCode();

    /**
     * 统一访问路径。
     */
    private String path = "/resource/message";

    /**
     * WebSocket 允许的跨域来源。
     */
    private String allowedOrigins = "*";
}

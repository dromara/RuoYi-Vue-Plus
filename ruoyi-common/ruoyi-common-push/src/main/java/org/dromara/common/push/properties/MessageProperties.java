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
    private String[] allowedOrigins = {"*"};

    /**
     * SSE 连接超时时间，单位毫秒。
     */
    private long sseTimeout = 86_400_000L;

    /**
     * 本地连接心跳检测间隔，单位秒。
     */
    private long heartbeatInterval = 60L;

    /**
     * WebSocket 单次发送超时时间，单位毫秒。
     */
    private int webSocketSendTimeLimit = 10_000;

    /**
     * WebSocket 发送缓冲区大小。
     */
    private int webSocketBufferSizeLimit = 64_000;
}

package org.dromara.common.mqtt.listener;

import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.codec.message.MqttPublishMessage;
import org.dromara.mica.mqtt.core.client.IMqttClientGlobalMessageListener;
import org.tio.core.ChannelContext;

import java.nio.charset.StandardCharsets;

/**
 * 全局消息监听，可以监听到所有订阅消息
 *
 * @author Lion Li
 */
@Slf4j
public class MqttClientGlobalMessageListener implements IMqttClientGlobalMessageListener {

    @Override
    public void onMessage(ChannelContext context, String topic, MqttPublishMessage message, byte[] payload) {
        log.info("MqttGlobalMessageEvent => topic: {}, msg: {}", topic, new String(payload, StandardCharsets.UTF_8));
    }

}

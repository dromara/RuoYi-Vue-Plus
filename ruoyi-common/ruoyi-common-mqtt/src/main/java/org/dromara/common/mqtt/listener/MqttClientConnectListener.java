package org.dromara.common.mqtt.listener;

import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.client.IMqttClientConnectListener;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;
import org.tio.core.ChannelContext;

/**
 * 客户端连接状态监听
 *
 * @author Lion Li
 */
@Slf4j
public class MqttClientConnectListener implements IMqttClientConnectListener {
    //
    private final MqttClientCreator mqttClientCreator;

    public MqttClientConnectListener(MqttClientCreator mqttClientCreator) {
        this.mqttClientCreator = mqttClientCreator;
    }

    @Override
    public void onConnected(ChannelContext context, boolean isReconnect) {
        // 创建连接
        log.info("MqttConnectedEvent:{}", context);
    }

    @Override
    public void onDisconnect(ChannelContext context, Throwable throwable, String remark, boolean isRemove) {
        // 离线时更新重连
        log.info("MqttDisconnectEvent:{}", context, throwable);
        // 在断线时更新 clientId、username、password
//        mqttClientCreator.clientId("newClient" + System.currentTimeMillis())
//            .username("newUserName")
//            .password("newPassword");
    }
}

package org.dromara.common.mqtt.listener;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.core.client.IMqttClientConnectListener;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;

/**
 * 客户端连接状态监听
 *
 * @author Lion Li
 */
@Slf4j
public class MqttClientConnectListener implements IMqttClientConnectListener {

    private final MqttClientCreator mqttClientCreator;

    /**
     * 创建 MQTT 客户端连接监听器。
     *
     * @param mqttClientCreator MQTT 客户端创建器
     */
    public MqttClientConnectListener(MqttClientCreator mqttClientCreator) {
        this.mqttClientCreator = mqttClientCreator;
    }

    /**
     * 客户端连接成功回调。
     *
     * @param context     通道上下文
     * @param isReconnect 是否重连
     */
    @Override
    public void onConnected(ChannelContext context, boolean isReconnect) {
        // 创建连接
        log.info("MqttConnectedEvent:{}", context);
    }

    /**
     * 客户端断开连接回调。
     *
     * @param context   通道上下文
     * @param throwable 断开异常
     * @param remark    断开说明
     * @param isRemove  是否移除连接
     */
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

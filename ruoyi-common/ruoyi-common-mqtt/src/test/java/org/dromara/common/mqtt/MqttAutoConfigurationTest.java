package org.dromara.common.mqtt;

import org.dromara.common.mqtt.config.MqttAutoConfiguration;
import org.dromara.common.mqtt.listener.MqttClientConnectListener;
import org.dromara.common.mqtt.listener.MqttClientGlobalMessageListener;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@DisplayName("common-mqtt 功能单元测试")
class MqttAutoConfigurationTest {

    /**
     * 验证 MQTT 自动配置能够创建连接与全局消息监听器，且不需要建立真实网络连接。
     */
    @Test
    @DisplayName("创建 MQTT 监听器")
    void shouldCreateMqttListeners() {
        MqttAutoConfiguration configuration = new MqttAutoConfiguration();

        MqttClientConnectListener connectListener =
            configuration.mqttClientConnectListener(mock(MqttClientCreator.class));
        MqttClientGlobalMessageListener messageListener = configuration.mqttClientGlobalMessageListener();

        assertNotNull(connectListener);
        assertNotNull(messageListener);
    }

    /**
     * 验证 MQTT 自定义器 Bean 可以独立创建，避免自动配置方法意外依赖运行时连接状态。
     */
    @Test
    @DisplayName("创建 MQTT 客户端自定义器")
    void shouldCreateMqttClientCustomizer() {
        assertNotNull(new MqttAutoConfiguration().mqttClientCustomizer());
    }
}

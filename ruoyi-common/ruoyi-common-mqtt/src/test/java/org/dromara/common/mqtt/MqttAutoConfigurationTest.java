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
     * 验证工作、分组和业务任务均由虚拟线程执行。
     */
    @Test
    @DisplayName("三个 MQTT 执行器均使用虚拟线程")
    void shouldUseVirtualThreadsForAllClientExecutors() throws Exception {
        var creator = new MqttClientCreator();
        new MqttAutoConfiguration().mqttClientCustomizer().customize(creator);
        try (var worker = creator.getTioExecutor(); var group = creator.getGroupExecutor();
             var business = creator.getMqttExecutor()) {
            for (var executor : java.util.List.of(worker, group, business)) {
                org.junit.jupiter.api.Assertions.assertTrue(executor.submit(() -> Thread.currentThread().isVirtual())
                    .get(5, java.util.concurrent.TimeUnit.SECONDS));
            }
        }
    }
}

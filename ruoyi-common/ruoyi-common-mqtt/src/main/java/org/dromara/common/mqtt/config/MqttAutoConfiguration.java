package org.dromara.common.mqtt.config;

import org.dromara.common.mqtt.listener.MqttClientConnectListener;
import org.dromara.common.mqtt.listener.MqttClientGlobalMessageListener;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * mqtt客户端配置初始化
 * <p>
 * 用法文档 <a href="https://mica-mqtt.dreamlu.net/guide/spring/client.html">...</a>
 * 测试server搭建:
 * 可执行下载其他mqtt服务端搭建
 * 也可使用 mica自带的server搭建 <a href="https://mica-mqtt.dreamlu.net/guide/spring/server.html">...</a>
 *
 * @author Lion Li
 */
@AutoConfiguration
@ConditionalOnProperty(value = "mqtt.client.enabled", havingValue = "true")
public class MqttAutoConfiguration {

    @Bean
    public MqttClientConnectListener mqttClientConnectListener(MqttClientCreator mqttClientCreator) {
        return new MqttClientConnectListener(mqttClientCreator);
    }

    @Bean
    public MqttClientGlobalMessageListener mqttClientGlobalMessageListener() {
        return new MqttClientGlobalMessageListener();
    }

}

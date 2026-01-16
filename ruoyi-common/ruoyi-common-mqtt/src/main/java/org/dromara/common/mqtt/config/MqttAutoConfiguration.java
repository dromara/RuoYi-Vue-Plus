package org.dromara.common.mqtt.config;

import org.dromara.common.mqtt.listener.MqttClientConnectListener;
import org.dromara.common.mqtt.listener.MqttClientGlobalMessageListener;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * mqtt客户端配置初始化
 * <p>
 * 用法文档 <a href="https://gitee.com/dromara/mica-mqtt/blob/master/starter/mica-mqtt-client-spring-boot-starter/README.md">...</a>
 *
 * @author Lion Li
 */
@AutoConfiguration
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

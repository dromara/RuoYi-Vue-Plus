package org.dromara.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.demo.domain.TestDemo;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.codec.message.MqttPublishMessage;
import org.dromara.mica.mqtt.core.annotation.MqttClientSubscribe;
import org.dromara.mica.mqtt.core.deserialize.MqttJsonDeserializer;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * mqtt 演示案例
 * <p>
 * 用法文档 <a href="https://mica-mqtt.dreamlu.net/guide/spring/client.html">...</a>
 * 测试server搭建:
 * 可执行下载其他mqtt服务端搭建
 * 也可使用 mica自带的server搭建 <a href="https://mica-mqtt.dreamlu.net/guide/spring/server.html">...</a>
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/mqtt")
@Slf4j
public class MqttController {

    @Lazy
    @Autowired
    private MqttClientTemplate client;

    @GetMapping("/send")
    public boolean send() {
        client.publish("/test/client", "测试测试".getBytes(StandardCharsets.UTF_8));
        return true;
    }

    @MqttClientSubscribe("/test/#")
    public void subQos0(String topic, byte[] payload) {
        log.info("topic:{} payload:{}", topic, new String(payload, StandardCharsets.UTF_8));
    }

    @MqttClientSubscribe(value = "/qos1/#", qos = MqttQoS.QOS1)
    public void subQos1(String topic, byte[] payload) {
        log.info("topic:{} payload:{}", topic, new String(payload, StandardCharsets.UTF_8));
    }

    @MqttClientSubscribe("/sys/${productKey}/${deviceName}/thing/sub/register")
    public void thingSubRegister(String topic, byte[] payload) {
        // 1.3.8 开始支持，@MqttClientSubscribe 注解支持 ${} 变量替换，会默认替换成 +
        // 注意：mica-mqtt 会先从 Spring boot 配置中替换参数 ${}，如果存在配置会优先被替换。
        log.info("topic:{} payload:{}", topic, new String(payload, StandardCharsets.UTF_8));
    }

    @MqttClientSubscribe(
        value = "/test/json",
        deserialize = MqttJsonDeserializer.class // 2.4.5 开始支持 自定义序列化，默认 json 序列化
    )
    public void testJson(String topic, MqttPublishMessage message, TestDemo data) {
        // 2.4.5 开始支持，支持 2 到 3 个参数，字段类型映射规则如下
        // String 字符串会默认映射到 topic，
        // MqttPublishMessage 会默认映射到 原始的消息，可以拿到 mqtt5 的 props 参数
        // byte[] 会映射到 mqtt 消息内容 payload
        // ByteBuffer 会映射到 mqtt 消息内容 payload
        // 其他类型会走序列化，确保消息能够序列化，默认为 json 序列化
        log.info("topic:{} json data:{}", topic, data);
    }

}

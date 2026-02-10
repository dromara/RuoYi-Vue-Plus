package org.dromara.common.mqtt.config;

import org.dromara.common.mqtt.listener.MqttClientConnectListener;
import org.dromara.common.mqtt.listener.MqttClientGlobalMessageListener;
import org.dromara.mica.mqtt.core.client.MqttClientCreator;
import org.dromara.mica.mqtt.core.client.MqttClientCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.tio.utils.thread.ThreadUtils;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;
import org.tio.utils.thread.pool.TioCallerRunsPolicy;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    /**
     * 客户端使用虚拟线程配置
     */
    @Bean
    public MqttClientCustomizer mqttClientCustomizer() {
        return creator -> {
            // 这个数不重要 已经使用虚拟线程 就是填一下防止报错
            int corePoolSize = ThreadUtils.CORE_POOL_SIZE;

            ThreadFactory factory = new VirtualThreadTaskExecutor("tio-worker-virtual").getVirtualThreadFactory();
            SynThreadPoolExecutor tioExecutor = new SynThreadPoolExecutor(corePoolSize, corePoolSize,
                0L, new LinkedBlockingQueue<>(), factory, new TioCallerRunsPolicy());
            tioExecutor.prestartCoreThread();
            creator.tioExecutor(tioExecutor);

            ThreadFactory factory1 = new VirtualThreadTaskExecutor("tio-group-virtual").getVirtualThreadFactory();
            ThreadPoolExecutor groupExecutor = new ThreadPoolExecutor(corePoolSize, corePoolSize,
                0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory1, new TioCallerRunsPolicy());
            groupExecutor.prestartCoreThread();
            creator.groupExecutor(groupExecutor);

            ThreadFactory factory2 = new VirtualThreadTaskExecutor("biz-worker-virtual").getVirtualThreadFactory();
            ThreadPoolExecutor mqttExecutor = new ThreadPoolExecutor(corePoolSize, corePoolSize,
                0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory2, new TioCallerRunsPolicy());
            mqttExecutor.prestartCoreThread();
            creator.mqttExecutor(mqttExecutor);
        };
    }

}

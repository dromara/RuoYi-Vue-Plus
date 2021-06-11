package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    /**
     * 是否开启线程池
     */
    private boolean enabled;

    /**
     * 核心线程池大小
     */
    private int corePoolSize;

    /**
     * 最大可创建的线程数
     */
    private int maxPoolSize;

    /**
     * 队列最大长度
     */
    private int queueCapacity;

    /**
     * 线程池维护线程所允许的空闲时间
     */
    private int keepAliveSeconds;

    /**
     * 线程池对拒绝任务(无线程可用)的处理策略
     */
    private String rejectedExecutionHandler;

}

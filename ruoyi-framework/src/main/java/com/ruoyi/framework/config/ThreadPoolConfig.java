package com.ruoyi.framework.config;

import com.ruoyi.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author Lion Li
 **/
@Configuration
public class ThreadPoolConfig {

    // 核心线程池大小
    @Value("${threadPoolConfig.corePoolSize}")
    private int corePoolSize;

    // 最大可创建的线程数
    @Value("${threadPoolConfig.maxPoolSize}")
    private int maxPoolSize;

    // 队列最大长度
    @Value("${threadPoolConfig.queueCapacity}")
    private int queueCapacity;

    // 线程池维护线程所允许的空闲时间
    @Value("${threadPoolConfig.keepAliveSeconds}")
    private int keepAliveSeconds;

    // 线程池对拒绝任务(无线程可用)的处理策略
    @Value("${threadPoolConfig.rejectedExecutionHandler}")
    private String rejectedExecutionHandler;

    @Bean(name = "threadPoolTaskExecutor")
    @ConditionalOnProperty(prefix = "threadPoolTaskExecutor", name = "enabled", havingValue = "true")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        RejectedExecutionHandler handler;
        if (rejectedExecutionHandler.equals("CallerRunsPolicy")) {
            handler = new ThreadPoolExecutor.CallerRunsPolicy();
        } else if (rejectedExecutionHandler.equals("DiscardOldestPolicy")) {
            handler = new ThreadPoolExecutor.DiscardOldestPolicy();
        } else if (rejectedExecutionHandler.equals("DiscardPolicy")) {
            handler = new ThreadPoolExecutor.DiscardPolicy();
        } else {
            handler = new ThreadPoolExecutor.AbortPolicy();
        }
        executor.setRejectedExecutionHandler(handler);
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }
}

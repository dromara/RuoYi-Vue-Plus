package org.dromara.common.job.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import tech.powerjob.worker.PowerJobWorker;

/**
 * 启动定时任务
 * @author yhan219
 * @since 2023/6/2
 */
@AutoConfiguration
@ConditionalOnBean(PowerJobWorker.class)
@ConditionalOnProperty(prefix = "powerjob.worker", name = "enabled", havingValue = "true")
@EnableScheduling
public class PowerJobConfig {


}

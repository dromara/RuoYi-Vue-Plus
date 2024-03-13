package org.dromara.common.job.config;

import com.aizuda.easy.retry.client.starter.EnableEasyRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动定时任务
 *
 * @author dhb52
 * @since 2024/3/12
 */
@Configuration
@ConditionalOnProperty(prefix = "easy-retry", name = "enabled", havingValue = "true")
@EnableScheduling
@EnableEasyRetry(group = "${easy-retry.group-name}")
public class EasyRetryConfig {
}

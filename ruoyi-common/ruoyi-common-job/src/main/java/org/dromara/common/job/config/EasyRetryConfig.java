package org.dromara.common.job.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.aizuda.easy.retry.client.common.appender.EasyRetryLogbackAppender;
import com.aizuda.easy.retry.client.common.event.EasyRetryStartingEvent;
import com.aizuda.easy.retry.client.starter.EnableEasyRetry;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动定时任务
 *
 * @author dhb52
 * @since 2024/3/12
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "easy-retry", name = "enabled", havingValue = "true")
@EnableScheduling
@EnableEasyRetry(group = "${easy-retry.group-name}")
public class EasyRetryConfig {

    @EventListener(EasyRetryStartingEvent.class)
    public void onStarting(EasyRetryStartingEvent event) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        EasyRetryLogbackAppender<ILoggingEvent> ca = new EasyRetryLogbackAppender<>();
        ca.setName("easy_log_appender");
        ca.start();
        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(ca);
    }

}

package org.dromara.common.job;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

import org.dromara.common.job.config.SnailJobConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("common-job 功能单元测试")
class SnailJobConfigTest {

    @Test
    void shouldAttachRemoteLoggerOnlyOnceAcrossStartupEvents() {
        var root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        var config = new SnailJobConfig();
        try {
            config.onStarting(null);
            config.onStarting(null);
            long count = 0;
            var appenders = root.iteratorForAppenders();
            while (appenders.hasNext()) {
                if ("snail_log_appender".equals(appenders.next().getName())) {
                    count++;
                }
            }
            assertEquals(1, count);
        } finally {
            Appender<ILoggingEvent> appender;
            while ((appender = root.getAppender("snail_log_appender")) != null) {
                root.detachAppender(appender);
                appender.stop();
            }
        }
    }

    /**
     * 验证定时任务配置仅在显式启用 Snail Job 时加载，并同时开启 Spring 调度能力。
     */
    @Test
    @DisplayName("声明 Snail Job 启用条件")
    void shouldDeclareSnailJobEnablementCondition() {
        ConditionalOnProperty condition = SnailJobConfig.class.getAnnotation(ConditionalOnProperty.class);

        assertNotNull(condition);
        assertEquals("snail-job", condition.prefix());
        assertArrayEquals(new String[]{"enabled"}, condition.name());
        assertEquals("true", condition.havingValue());
        assertNotNull(SnailJobConfig.class.getAnnotation(EnableScheduling.class));
    }
}

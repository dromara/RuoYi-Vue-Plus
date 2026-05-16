package org.dromara.common.mail.config;

import cn.hutool.extra.mail.MailAccount;
import org.dromara.common.mail.config.properties.MailProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * JavaMail 配置
 *
 * @author Michelle.Chung
 */
@AutoConfiguration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @ConditionalOnProperty(value = "mail.enabled", havingValue = "true")
    public MailAccount mailAccount(MailProperties mailProperties) {
        return mailProperties.toMailAccount();
    }

}

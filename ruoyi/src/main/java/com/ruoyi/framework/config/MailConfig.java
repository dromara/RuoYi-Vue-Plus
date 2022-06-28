package com.ruoyi.framework.config;

import cn.hutool.extra.mail.MailAccount;
import com.ruoyi.framework.config.properties.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JavaMail 配置
 *
 * @author Michelle.Chung
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(value = "mail.enabled", havingValue = "true")
    public MailAccount mailAccount(MailProperties mailProperties) {
        MailAccount account = new MailAccount();
        account.setHost(mailProperties.getHost());
        account.setPort(mailProperties.getPort());
        account.setAuth(mailProperties.getAuth());
        account.setFrom(mailProperties.getFrom());
        account.setUser(mailProperties.getUser());
        account.setPass(mailProperties.getPass());
        account.setSocketFactoryPort(mailProperties.getPort());
        account.setStarttlsEnable(mailProperties.getStarttlsEnable());
        account.setSslEnable(mailProperties.getSslEnable());
        account.setTimeout(mailProperties.getTimeout());
        account.setConnectionTimeout(mailProperties.getConnectionTimeout());
        return account;
    }

}

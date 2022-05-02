package com.ruoyi.framework.config;

import cn.hutool.extra.mail.MailAccount;
import com.ruoyi.framework.config.properties.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * JavaMail 配置
 *
 * @author Michelle.Chung
 */
@Configuration
public class MailConfig {

    @Resource
    private MailProperties mailProperties;

    /**
     * 初始化 JavaMailSender
     */
    @Bean
    @ConditionalOnProperty(value = "spring.mail.enabled", havingValue = "true")
    public MailAccount mailAccount() {
        MailAccount account = new MailAccount();
        account.setFrom(mailProperties.getUsername());
        account.setUser(mailProperties.getUsername());
        account.setPass(mailProperties.getPassword());
        account.setPort(mailProperties.getPort());
        account.setAuth(mailProperties.getAuth());
        account.setDebug(mailProperties.getDebug());
        account.setStarttlsEnable(mailProperties.getStarttlsEnable());
        return account;
    }

}

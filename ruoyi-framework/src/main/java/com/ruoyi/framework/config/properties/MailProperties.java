package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JavaMail 配置属性
 *
 * @author Michelle.Chung
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    /**
     * 过滤开关
     */
    private String enabled;

    /**
     * 邮件服务地址
     */
    private String host;

    /**
     * 用户名
     */
    private String username;

    /**
     * 授权码 (设置 - 账户 - POP3/SMTP服务)
     */
    private String password;

    /**
     * 邮箱加密端口，不同邮箱的端口不一样
     */
    private Integer port;

    /**
     * 是否需要用户认证
     */
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private Boolean auth;

    /**
     * 是否启用TLS加密
     */
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private Boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust}")
    private String sslTrust;

    private Boolean debug;

    /**
     * 传输协议 starttls.enable = true 时为 smtps
     */
    private String protocol;

}

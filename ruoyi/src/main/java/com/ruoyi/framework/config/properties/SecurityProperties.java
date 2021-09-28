package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Security 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 退出登录url
     */
    private String logoutUrl;

    /**
     * 匿名放行路径
     */
    private String[] anonymous;

    /**
     * 用户任意访问放行路径
     */
    private String[] permitAll;

}

package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * token 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "token")
public class TokenProperties {

    /**
     * 令牌自定义标识
     */
    private String header;

    /**
     * 令牌秘钥
     */
    private String secret;

    /**
     * 令牌有效期（默认30分钟）
     */
    private int expireTime;
}

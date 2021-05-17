package com.ruoyi.framework.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {

    /**
     * 验证码类型
     */
    private Boolean enabled;
    /**
     * 验证码类别
     */
    private String title;
    /**
     * 数字验证码位数
     */
    private String description;
    /**
     * 字符验证码长度
     */
    private String version;

}

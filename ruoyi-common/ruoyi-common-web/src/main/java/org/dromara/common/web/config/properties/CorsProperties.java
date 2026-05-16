package org.dromara.common.web.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置属性。
 */
@Data
@ConfigurationProperties(prefix = "web.cors")
public class CorsProperties {

    /**
     * 是否允许携带凭证。
     */
    private Boolean allowCredentials = true;

    /**
     * 允许的来源匹配规则。
     */
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

    /**
     * 允许的请求头。
     */
    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    /**
     * 允许的请求方法。
     */
    private List<String> allowedMethods = new ArrayList<>(List.of("*"));

    /**
     * 预检请求缓存时间，单位秒。
     */
    private Long maxAge = 1800L;

}

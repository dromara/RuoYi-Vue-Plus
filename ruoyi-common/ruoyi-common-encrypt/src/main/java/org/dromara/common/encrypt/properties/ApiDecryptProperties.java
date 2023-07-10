package org.dromara.common.encrypt.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * api解密属性配置类
 * @author wdhcr
 */
@Data
@ConfigurationProperties(prefix = "api-decrypt")
public class ApiDecryptProperties {

    /**
     * 加密开关
     */
    private Boolean enabled;

    /**
     * 头部标识
     */
    private String headerFlag;


    /**
     * 公钥
     */
    private String publicKey;

    /**
     * 私钥
     */
    private String privateKey;
}

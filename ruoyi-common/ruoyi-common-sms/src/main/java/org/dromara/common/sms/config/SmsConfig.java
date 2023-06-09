package org.dromara.common.sms.config;

import org.dromara.common.sms.config.properties.SmsProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 短信配置类
 *
 * @author Lion Li
 * @version 4.2.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {

}

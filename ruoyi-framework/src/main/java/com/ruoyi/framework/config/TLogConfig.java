package com.ruoyi.framework.config;

import com.yomahub.tlog.springboot.TLogWebAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 整合 TLog 框架配置
 *
 * @author Lion Li
 * @since 3.3.0
 */
@Configuration
// 排除 web 自动配置 自定义实现
@EnableAutoConfiguration(exclude = TLogWebAutoConfiguration.class)
public class TLogConfig {

}

package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 重复提交 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "repeat-submit")
public class RepeatSubmitProperties {

    /**
     * 间隔时间(毫秒)
     */
    private int interval;

}

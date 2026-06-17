package org.dromara.common.mybatis.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SQL 日志配置。
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties(prefix = "mybatis-plus.sql-log")
public class SqlLogProperties {

    /**
     * 是否开启完整 SQL 输出。
     */
    private Boolean enabled = false;

    /**
     * 输出方式，可选 console、log。
     */
    private String output = "console";

}

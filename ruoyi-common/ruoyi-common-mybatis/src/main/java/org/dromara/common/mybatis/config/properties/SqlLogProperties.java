package org.dromara.common.mybatis.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 不输出成功执行日志的表名；执行异常仍会输出。
     */
    private List<String> ignoreTables = new ArrayList<>();

}

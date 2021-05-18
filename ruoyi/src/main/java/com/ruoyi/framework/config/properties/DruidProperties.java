package com.ruoyi.framework.config.properties;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * druid 配置属性
 *
 * @author Lion Li
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.druid")
public class DruidProperties {

    /** 初始连接数 */
    private int initialSize;
    /** 最小连接池数量 */
    private int minIdle;
    /** 最大连接池数量 */
    private int maxActive;
    /** 配置获取连接等待超时的时间 */
    private int maxWait;
    /** 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 */
    private int timeBetweenEvictionRunsMillis;
    /** 配置一个连接在池中最小生存的时间，单位是毫秒 */
    private int minEvictableIdleTimeMillis;
    /** 配置一个连接在池中最大生存的时间，单位是毫秒 */
    private int maxEvictableIdleTimeMillis;
    /** 配置检测连接是否有效 */
    private String validationQuery;
    /** 初始连接数 */
    private boolean testWhileIdle;
    /** 初始连接数 */
    private boolean testOnBorrow;
    /** 初始连接数 */
    private boolean testOnReturn;

    public DruidDataSource dataSource(DruidDataSource datasource) {
        datasource.setInitialSize(initialSize);
        datasource.setMaxActive(maxActive);
        datasource.setMinIdle(minIdle);
        datasource.setMaxWait(maxWait);
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setMaxEvictableIdleTimeMillis(maxEvictableIdleTimeMillis);
        datasource.setValidationQuery(validationQuery);
        datasource.setTestWhileIdle(testWhileIdle);
        datasource.setTestOnBorrow(testOnBorrow);
        datasource.setTestOnReturn(testOnReturn);
        return datasource;
    }
}

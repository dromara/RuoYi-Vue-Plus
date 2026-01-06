package org.dromara.common.tenant.config;

import cn.dev33.satoken.dao.SaTokenDao;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.dromara.common.redis.config.RedisConfig;
import org.dromara.common.redis.config.properties.RedissonProperties;
import org.dromara.common.tenant.core.TenantSaTokenDao;
import org.dromara.common.tenant.handle.PlusTenantLineHandler;
import org.dromara.common.tenant.handle.TenantKeyPrefixHandler;
import org.dromara.common.tenant.manager.TenantSpringCacheManager;
import org.dromara.common.tenant.properties.TenantProperties;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 租户配置类
 *
 * @author Lion Li
 */
@EnableConfigurationProperties(TenantProperties.class)
@AutoConfiguration(after = {RedisConfig.class})
@ConditionalOnProperty(value = "tenant.enable", havingValue = "true")
public class TenantConfig {

    @ConditionalOnClass(TenantLineInnerInterceptor.class)
    @AutoConfiguration
    static class MybatisPlusConfiguration {

        /**
         * 多租户插件
         */
        @Bean
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties tenantProperties) {
            return new TenantLineInnerInterceptor(new PlusTenantLineHandler(tenantProperties));
        }

    }

    @Bean
    public RedissonAutoConfigurationCustomizer tenantRedissonCustomizer(RedissonProperties redissonProperties) {
        return config -> {
            // 设置多租户 redis key前缀
            config.setNameMapper(new TenantKeyPrefixHandler(redissonProperties.getKeyPrefix()));
        };
    }

    /**
     * 多租户缓存管理器
     */
    @Primary
    @Bean
    public CacheManager tenantCacheManager() {
        return new TenantSpringCacheManager();
    }

    /**
     * 多租户鉴权dao实现
     */
    @Primary
    @Bean
    public SaTokenDao tenantSaTokenDao() {
        return new TenantSaTokenDao();
    }

}

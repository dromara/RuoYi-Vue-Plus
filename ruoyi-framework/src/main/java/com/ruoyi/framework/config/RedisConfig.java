package com.ruoyi.framework.config;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.RedissonProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * redis配置
 *
 * @author Lion Li
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

	private static final String REDIS_PROTOCOL_PREFIX = "redis://";
	private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

	@Autowired
	private RedisProperties redisProperties;

	@Autowired
	private RedissonProperties redissonProperties;

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(RedissonClient.class)
	public RedissonClient redisson() throws IOException {
		String prefix = REDIS_PROTOCOL_PREFIX;
		if (redisProperties.isSsl()) {
			prefix = REDISS_PROTOCOL_PREFIX;
		}
		Config config = new Config();
		config.setThreads(redissonProperties.getThreads())
			.setNettyThreads(redissonProperties.getNettyThreads())
			.setCodec(JsonJacksonCodec.INSTANCE)
			.setTransportMode(redissonProperties.getTransportMode());

		RedissonProperties.SingleServerConfig singleServerConfig = redissonProperties.getSingleServerConfig();
		// 使用单机模式
		config.useSingleServer()
			.setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
			.setConnectTimeout(((Long) redisProperties.getTimeout().toMillis()).intValue())
			.setDatabase(redisProperties.getDatabase())
			.setPassword(StringUtils.isNotBlank(redisProperties.getPassword()) ? redisProperties.getPassword() : null)
			.setTimeout(singleServerConfig.getTimeout())
			.setRetryAttempts(singleServerConfig.getRetryAttempts())
			.setRetryInterval(singleServerConfig.getRetryInterval())
			.setSubscriptionsPerConnection(singleServerConfig.getSubscriptionsPerConnection())
			.setClientName(singleServerConfig.getClientName())
			.setIdleConnectionTimeout(singleServerConfig.getIdleConnectionTimeout())
			.setSubscriptionConnectionMinimumIdleSize(singleServerConfig.getSubscriptionConnectionMinimumIdleSize())
			.setSubscriptionConnectionPoolSize(singleServerConfig.getSubscriptionConnectionPoolSize())
			.setConnectionMinimumIdleSize(singleServerConfig.getConnectionMinimumIdleSize())
			.setConnectionPoolSize(singleServerConfig.getConnectionPoolSize())
			.setDnsMonitoringInterval(singleServerConfig.getDnsMonitoringInterval());
		RedissonClient redissonClient = Redisson.create(config);
		log.info("初始化 redis 配置");
		return redissonClient;
	}

	/**
	 * 整合spring-cache
	 */
	@Bean
	public CacheManager cacheManager(RedissonClient redissonClient) {
		List<RedissonProperties.CacheGroup> cacheGroup = redissonProperties.getCacheGroup();
		Map<String, CacheConfig> config = new HashMap<>();
		for (RedissonProperties.CacheGroup group : cacheGroup) {
			CacheConfig cacheConfig = new CacheConfig(group.getTtl(), group.getMaxIdleTime());
			cacheConfig.setMaxSize(group.getMaxSize());
			config.put(group.getGroupId(), cacheConfig);
		}
		return new RedissonSpringCacheManager(redissonClient, config, JsonJacksonCodec.INSTANCE);
	}

	@Bean
	public DefaultRedisScript<Long> limitScript() {
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(limitScriptText());
		redisScript.setResultType(Long.class);
		return redisScript;
	}

	/**
	 * 限流脚本
	 */
	private String limitScriptText() {
		return StrUtil.builder()
			.append("local key = KEYS[1]\n")
			.append("local count = tonumber(ARGV[1])\n")
			.append("local time = tonumber(ARGV[2])\n")
			.append("local current = redis.call('get', key);\n")
			.append("if current and tonumber(current) > count then\n")
			.append("    return current;\n")
			.append("end\n")
			.append("current = redis.call('incr', key)\n")
			.append("if tonumber(current) == 1 then\n")
			.append("    redis.call('expire', key, time)\n")
			.append("end\n")
			.append("return current;")
			.toString();
	}

}

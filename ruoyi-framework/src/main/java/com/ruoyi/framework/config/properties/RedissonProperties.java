package com.ruoyi.framework.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.config.TransportMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redisson 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {

	/**
	 * 线程池数量,默认值 = 当前处理核数量 * 2
	 */
	private int threads;

	/**
	 * Netty线程池数量,默认值 = 当前处理核数量 * 2
	 */
	private int nettyThreads;

	/**
	 * 传输模式
	 */
	private TransportMode transportMode;

	/**
	 * 单机服务配置
	 */
	private SingleServerConfig singleServerConfig;

	/**
	 * 缓存组
	 */
	private List<CacheGroup> cacheGroup;

	@Data
	@NoArgsConstructor
	public static class SingleServerConfig {

		/**
		 * 客户端名称
		 */
		private String clientName;

		/**
		 * 最小空闲连接数
		 */
		private int connectionMinimumIdleSize;

		/**
		 * 连接池大小
		 */
		private int connectionPoolSize;

		/**
		 * 连接空闲超时，单位：毫秒
		 */
		private int idleConnectionTimeout;

		/**
		 * 命令等待超时，单位：毫秒
		 */
		private int timeout;

		/**
		 * 如果尝试在此限制之内发送成功，则开始启用 timeout 计时。
		 */
		private int retryAttempts;

		/**
		 * 命令重试发送时间间隔，单位：毫秒
		 */
		private int retryInterval;

		/**
		 * 发布和订阅连接的最小空闲连接数
		 */
		private int subscriptionConnectionMinimumIdleSize;

		/**
		 * 发布和订阅连接池大小
		 */
		private int subscriptionConnectionPoolSize;

		/**
		 * 单个连接最大订阅数量
		 */
		private int subscriptionsPerConnection;

		/**
		 * DNS监测时间间隔，单位：毫秒
		 */
		private int dnsMonitoringInterval;

	}

	@Data
	@NoArgsConstructor
	public static class CacheGroup {

		/**
		 * 组id
		 */
		private String groupId;

		/**
		 * 组过期时间
		 */
		private long ttl;

		/**
		 * 组最大空闲时间
		 */
		private long maxIdleTime;

		/**
		 * 组最大长度
		 */
		private int maxSize;

	}

}

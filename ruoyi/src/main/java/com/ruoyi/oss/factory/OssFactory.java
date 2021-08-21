package com.ruoyi.oss.factory;

import cn.hutool.core.convert.Convert;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.reflect.ReflectUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.oss.constant.CloudConstant;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.service.ICloudStorageStrategy;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传Factory
 *
 * @author Lion Li
 */
public class OssFactory {

	private static RedisCache redisCache;

	static {
		OssFactory.redisCache = SpringUtils.getBean(RedisCache.class);
	}

	/**
	 * 服务实例缓存
	 */
	private static final Map<String, ICloudStorageStrategy> SERVICES = new ConcurrentHashMap<>();

	/**
	 * 服务配置更新时间缓存
	 */
	private static final Map<String, Date> SERVICES_UPDATE_TIME = new ConcurrentHashMap<>();

	/**
	 * 获取默认实例
	 */
	public static ICloudStorageStrategy instance() {
		// 获取redis 默认类型
		String type = Convert.toStr(redisCache.getCacheObject(CloudConstant.CACHE_CONFIG_KEY));
		if (StringUtils.isEmpty(type)) {
			throw new OssException("文件存储服务类型无法找到!");
		}
		return instance(type);
	}

	/**
	 * 根据类型获取实例
	 */
	public static ICloudStorageStrategy instance(String type) {
		ICloudStorageStrategy service = SERVICES.get(type);
		Date oldDate = SERVICES_UPDATE_TIME.get(type);
		Object json = redisCache.getCacheObject(CloudConstant.SYS_OSS_KEY + type);
		CloudStorageProperties properties = JsonUtils.parseObject(json.toString(), CloudStorageProperties.class);
		if (properties == null) {
			throw new OssException("系统异常, '" + type + "'配置信息不存在!");
		}
		Date nowDate = properties.getUpdateTime();
		// 服务存在并更新时间相同则返回(使用更新时间确保配置最终一致性)
		if (service != null && oldDate.equals(nowDate)) {
			return service;
		}
		// 获取redis配置信息 创建对象 并缓存
		service = (ICloudStorageStrategy) ReflectUtils.newInstance(CloudServiceEnumd.getServiceClass(type));
		((AbstractCloudStorageStrategy)service).init(properties);
		SERVICES.put(type, service);
		SERVICES_UPDATE_TIME.put(type, nowDate);
		return service;
	}

}

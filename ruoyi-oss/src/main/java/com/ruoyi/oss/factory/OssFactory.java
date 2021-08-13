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

	private static final Map<String, ICloudStorageStrategy> SERVICES = new ConcurrentHashMap<>();

	public static ICloudStorageStrategy instance() {
		String type = Convert.toStr(redisCache.getCacheObject(CloudConstant.CACHE_CONFIG_KEY));
		if (StringUtils.isEmpty(type)) {
			throw new OssException("文件存储服务类型无法找到!");
		}
		return instance(type);
	}

	public static ICloudStorageStrategy instance(String type) {
		ICloudStorageStrategy service = SERVICES.get(type);
		if (service == null) {
			Object json = redisCache.getCacheObject(CloudConstant.SYS_OSS_KEY + type);
			CloudStorageProperties properties = JsonUtils.parseObject(json.toString(), CloudStorageProperties.class);
			String beanName = CloudServiceEnumd.getServiceName(type);
			ICloudStorageStrategy bean = (ICloudStorageStrategy) ReflectUtils.newInstance(CloudServiceEnumd.getServiceClass(type), properties);
			SpringUtils.registerBean(beanName, bean);
			service = SpringUtils.getBean(beanName);
			SERVICES.put(type, bean);
		}
		return service;
	}

	public static void destroy(String type) {
		ICloudStorageStrategy service = SERVICES.get(type);
		if (service == null) {
			return;
		}
		SpringUtils.unregisterBean(CloudServiceEnumd.getServiceName(type));
		SERVICES.remove(type);
	}

}

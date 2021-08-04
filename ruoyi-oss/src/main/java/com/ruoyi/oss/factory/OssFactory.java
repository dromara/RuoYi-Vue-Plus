package com.ruoyi.oss.factory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.oss.constant.CloudConstant;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.service.ICloudStorageService;

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

	private static final Map<String, ICloudStorageService> SERVICES = new ConcurrentHashMap<>();

	public static ICloudStorageService instance() {
		String type = Convert.toStr(redisCache.getCacheObject(Constants.SYS_CONFIG_KEY + CloudConstant.CLOUD_STORAGE_CONFIG_KEY));
		if (StringUtils.isEmpty(type)) {
			throw new OssException("文件存储服务类型无法找到!");
		}
		return instance(type);
	}

	public static ICloudStorageService instance(String type) {
		ICloudStorageService service = SERVICES.get(type);
		if (service == null) {
			service = (ICloudStorageService) SpringUtils.getBean(CloudServiceEnumd.getServiceClass(type));
		}
		return service;
	}

	public static void register(String type, ICloudStorageService iCloudStorageService) {
		Assert.notNull(type, "type can't be null");
		SERVICES.put(type, iCloudStorageService);
	}
}

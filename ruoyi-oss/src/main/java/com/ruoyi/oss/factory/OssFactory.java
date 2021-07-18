package com.ruoyi.oss.factory;

import cn.hutool.core.lang.Assert;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.oss.constant.CloudConstant;
import com.ruoyi.oss.service.ICloudStorageService;
import com.ruoyi.system.service.ISysConfigService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传Factory
 *
 * @author Lion Li
 */
public class OssFactory {

    private static ISysConfigService sysConfigService;

	static {
		OssFactory.sysConfigService = SpringUtils.getBean(ISysConfigService.class);
	}

	private static final Map<String, ICloudStorageService> SERVICES = new ConcurrentHashMap<>();

	public static ICloudStorageService instance() {
		String type = sysConfigService.selectConfigByKey(CloudConstant.CLOUD_STORAGE_CONFIG_KEY);
		return SERVICES.get(type);
	}

	public static ICloudStorageService instance(String type) {
		return SERVICES.get(type);
	}

	public static void register(String type, ICloudStorageService iCloudStorageService) {
		Assert.notNull(type, "type can't be null");
		SERVICES.put(type, iCloudStorageService);
	}
}

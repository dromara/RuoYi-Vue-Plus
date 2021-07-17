package com.ruoyi.oss.service.abstractd;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.oss.config.CloudStorageConfig;
import com.ruoyi.oss.service.ICloudStorageService;
import com.ruoyi.oss.utils.DateUtils;

import java.util.UUID;

/**
 * 云存储(支持七牛、阿里云、腾讯云、minio)
 */
public abstract class AbstractCloudStorageService implements ICloudStorageService {

	/**
	 * 云存储配置信息
	 */
	protected CloudStorageConfig config;

	public int getServiceType() {
		return config.getType();
	}

	@Override
	public String getPath(String prefix, String suffix) {
		// 生成uuid
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		// 文件路径
		String path = DateUtils.dateTime() + "/" + uuid;
		if (StrUtil.isNotBlank(prefix)) {
			path = prefix + "/" + path;
		}
		return path + suffix;
	}

}

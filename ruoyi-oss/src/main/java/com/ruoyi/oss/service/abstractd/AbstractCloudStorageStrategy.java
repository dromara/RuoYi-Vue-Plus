package com.ruoyi.oss.service.abstractd;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.service.ICloudStorageStrategy;

import java.io.InputStream;

/**
 * 云存储策略(支持七牛、阿里云、腾讯云、minio)
 *
 * @author Lion Li
 */
public abstract class AbstractCloudStorageStrategy implements ICloudStorageStrategy {

	protected CloudStorageProperties properties;

	public abstract void init(CloudStorageProperties properties);

	@Override
	public abstract void createBucket();

	@Override
	public abstract String getServiceType();

	@Override
	public String getPath(String prefix, String suffix) {
		// 生成uuid
		String uuid = IdUtil.fastSimpleUUID();
		// 文件路径
		String path = DateUtils.datePath() + "/" + uuid;
		if (StringUtils.isNotBlank(prefix)) {
			path = prefix + "/" + path;
		}
		return path + suffix;
	}

	@Override
	public abstract UploadResult upload(byte[] data, String path, String contentType);

	@Override
	public abstract void delete(String path);

	@Override
	public UploadResult upload(InputStream inputStream, String path, String contentType) {
		byte[] data = IoUtil.readBytes(inputStream);
		return this.upload(data, path, contentType);
	}

	@Override
	public abstract UploadResult uploadSuffix(byte[] data, String suffix, String contentType);

	@Override
	public abstract UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType);

	@Override
	public abstract String getEndpointLink();
}

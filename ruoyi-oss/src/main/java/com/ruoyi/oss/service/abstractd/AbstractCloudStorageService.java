package com.ruoyi.oss.service.abstractd;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.service.ICloudStorageService;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.util.Date;

/**
 * 云存储(支持七牛、阿里云、腾讯云、minio)
 *
 * @author Lion Li
 */
public abstract class AbstractCloudStorageService implements ICloudStorageService, InitializingBean {

	@Override
	public abstract void createBucket();

	@Override
	public abstract String getServiceType();

	@Override
	public String getPath(String prefix, String suffix) {
		// 生成uuid
		String uuid = IdUtil.fastSimpleUUID();
		// 文件路径
		String path = DateUtil.format(new Date(), "yyyyMMdd") + "/" + uuid;
		if (StrUtil.isNotBlank(prefix)) {
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
	public abstract void afterPropertiesSet() throws Exception;

	@Override
	public abstract String getEndpointLink();
}

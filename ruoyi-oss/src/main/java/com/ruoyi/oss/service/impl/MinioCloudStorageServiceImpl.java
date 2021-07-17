package com.ruoyi.oss.service.impl;

import cn.hutool.core.io.IoUtil;
import com.ruoyi.oss.config.CloudStorageConfig;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import io.minio.MinioClient;

import java.io.InputStream;

/**
 * minio存储
 */
public class MinioCloudStorageServiceImpl extends AbstractCloudStorageService {

	private MinioClient minioClient;

	public MinioCloudStorageServiceImpl(CloudStorageConfig config) {
		this.config = config;
		// 初始化
		init();
	}

	private void init() {
		minioClient = MinioClient.builder()
			.endpoint(config.getDomain())
			.credentials(config.getAccessKey(), config.getSecretKey())
			.build();
	}

	@Override
	public String upload(byte[] data, String path) {
		try {

		} catch (Exception e) {
			throw new OssException("上传文件失败，请核对Minio配置信息");
		}
		return config.getDomain() + "/" + path;
	}

	@Override
	public void delete(String path) {
		try {

		} catch (Exception e) {
			throw new OssException(e.getMessage());
		}
	}

	@Override
	public String upload(InputStream inputStream, String path) {
		byte[] data = IoUtil.readBytes(inputStream);
		return this.upload(data, path);
	}

	@Override
	public String uploadSuffix(byte[] data, String suffix) {
		return upload(data, getPath(config.getPrefix(), suffix));
	}

	@Override
	public String uploadSuffix(InputStream inputStream, String suffix) {
		return upload(inputStream, getPath(config.getPrefix(), suffix));
	}

}

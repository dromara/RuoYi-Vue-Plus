package com.ruoyi.oss.service.impl;

import com.aliyun.oss.OSSClient;
import com.ruoyi.oss.config.CloudStorageConfig;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 阿里云存储
 */
public class AliyunCloudStorageServiceImpl extends AbstractCloudStorageService {

	private OSSClient client;

	public AliyunCloudStorageServiceImpl(CloudStorageConfig config) {
		this.config = config;
		// 初始化
		init();
	}

	private void init() {
		client = new OSSClient(config.getDomain(), config.getAccessKey(), config.getSecretKey());
	}

	@Override
	public String upload(byte[] data, String path) {
		return upload(new ByteArrayInputStream(data), path);
	}

	@Override
	public String upload(InputStream inputStream, String path) {
		try {
			client.putObject(config.getBucketName(), path, inputStream);
		} catch (Exception e) {
			throw new OssException("上传文件失败，请检查配置信息");
		}
		return config.getDomain() + "/" + path;
	}

	@Override
	public void delete(String path) {
		path = path.replace(config.getDomain() + "/", "");
		try {
			client.deleteObject(config.getBucketName(), path);
		} catch (Exception e) {
			throw new OssException("上传文件失败，请检查配置信息");
		}
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

package com.ruoyi.oss.service.impl;

import cn.hutool.core.io.IoUtil;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.ruoyi.oss.config.CloudStorageConfig;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;

import java.io.InputStream;

/**
 * 七牛云存储
 */
public class QiniuCloudStorageServiceImpl extends AbstractCloudStorageService {

	private UploadManager uploadManager;
	private BucketManager bucketManager;
	private String token;

	public QiniuCloudStorageServiceImpl(CloudStorageConfig config) {
		this.config = config;
		// 初始化
		init();
	}

	private void init() {
		// z0 z1 z2
		Configuration config = new Configuration(Region.autoRegion());
		// 默认不使用https
		config.useHttpsDomains = false;
		uploadManager = new UploadManager(config);
		Auth auth = Auth.create(this.config.getAccessKey(), this.config.getSecretKey());
		token = auth.uploadToken(this.config.getBucketName());
		bucketManager = new BucketManager(auth, config);
	}

	@Override
	public String upload(byte[] data, String path) {
		try {
			Response res = uploadManager.put(data, path, token);
			if (!res.isOK()) {
				throw new RuntimeException("上传七牛出错：" + res.toString());
			}
		} catch (Exception e) {
			throw new OssException("上传文件失败，请核对七牛配置信息");
		}
		return config.getDomain() + "/" + path;
	}

	@Override
	public void delete(String path) {
		try {
			path = path.replace(config.getDomain() + "/", "");
			Response res = bucketManager.delete(config.getBucketName(), path);
			if (!res.isOK()) {
				throw new RuntimeException("删除七牛文件出错：" + res.toString());
			}
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

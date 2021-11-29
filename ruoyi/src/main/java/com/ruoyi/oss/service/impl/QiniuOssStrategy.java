package com.ruoyi.oss.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.OssEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.properties.OssProperties;
import com.ruoyi.oss.service.abstractd.AbstractOssStrategy;

import java.io.InputStream;

/**
 * 七牛云存储策略
 *
 * @author Lion Li
 */
public class QiniuOssStrategy extends AbstractOssStrategy {

	private UploadManager uploadManager;
	private BucketManager bucketManager;
	private Auth auth;

	@Override
	public void init(OssProperties cloudStorageProperties) {
		properties = cloudStorageProperties;
		try {
			Configuration config = new Configuration(getRegion(properties.getRegion()));
			// https设置
			config.useHttpsDomains = false;
			config.useHttpsDomains = "Y".equals(properties.getIsHttps());
			uploadManager = new UploadManager(config);
			auth = Auth.create(properties.getAccessKey(), properties.getSecretKey());
			String bucketName = properties.getBucketName();
			bucketManager = new BucketManager(auth, config);

			if (!ArrayUtil.contains(bucketManager.buckets(), bucketName)) {
				bucketManager.createBucket(bucketName, properties.getRegion());
			}
		} catch (Exception e) {
			throw new OssException("七牛云存储配置错误! 请检查系统配置:[" + e.getMessage() + "]");
		}
	}

	@Override
	public void createBucket() {
		try {
			String bucketName = properties.getBucketName();
			if (ArrayUtil.contains(bucketManager.buckets(), bucketName)) {
				return;
			}
			bucketManager.createBucket(bucketName, properties.getRegion());
		} catch (Exception e) {
			throw new OssException("创建Bucket失败, 请核对七牛云配置信息:[" + e.getMessage() + "]");
		}
	}

	@Override
	public String getServiceType() {
		return OssEnumd.QINIU.getValue();
	}

	@Override
	public UploadResult upload(byte[] data, String path, String contentType) {
		try {
            String token = auth.uploadToken(properties.getBucketName());
            Response res = uploadManager.put(data, path, token, null, contentType, false);
			if (!res.isOK()) {
				throw new RuntimeException("上传七牛出错：" + res.error);
			}
		} catch (Exception e) {
			throw new OssException("上传文件失败，请核对七牛配置信息:[" + e.getMessage() + "]");
		}
		return new UploadResult().setUrl(getEndpointLink() + "/" + path).setFilename(path);
	}

	@Override
	public void delete(String path) {
		try {
			path = path.replace(getEndpointLink() + "/", "");
			Response res = bucketManager.delete(properties.getBucketName(), path);
			if (!res.isOK()) {
				throw new RuntimeException("删除七牛文件出错：" + res.error);
			}
		} catch (Exception e) {
			throw new OssException(e.getMessage());
		}
	}

	@Override
	public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
		return upload(data, getPath(properties.getPrefix(), suffix), contentType);
	}

	@Override
	public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
		return upload(inputStream, getPath(properties.getPrefix(), suffix), contentType);
	}

	@Override
	public String getEndpointLink() {
		return properties.getEndpoint();
	}

	private Region getRegion(String region) {
		switch (region) {
			case "z0":
				return Region.region0();
			case "z1":
				return Region.region1();
			case "z2":
				return Region.region2();
			case "na0":
				return Region.regionNa0();
			case "as0":
				return Region.regionAs0();
			default:
				return Region.autoRegion();
		}
	}

}

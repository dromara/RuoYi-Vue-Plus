package com.ruoyi.oss.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.properties.CloudStorageProperties.QiniuProperties;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 七牛云存储
 *
 * @author Lion Li
 */
@Lazy
@Service
public class QiniuCloudStorageServiceImpl extends AbstractCloudStorageService implements InitializingBean {

	private final UploadManager uploadManager;
	private final BucketManager bucketManager;
	private final String token;
	private final QiniuProperties properties;

	@Autowired
	public QiniuCloudStorageServiceImpl(CloudStorageProperties properties) {
		this.properties = properties.getQiniu();
		try {
			Configuration config = new Configuration(getRegion(this.properties.getRegion()));
			// https设置
			config.useHttpsDomains = false;
			if (this.properties.getIsHttps() != null) {
				config.useHttpsDomains = this.properties.getIsHttps();
			}
			uploadManager = new UploadManager(config);
			Auth auth = Auth.create(
				this.properties.getAccessKey(),
				this.properties.getSecretKey());
			String bucketName = this.properties.getBucketName();
			token = auth.uploadToken(bucketName);
			bucketManager = new BucketManager(auth, config);

			if (!ArrayUtil.contains(bucketManager.buckets(), bucketName)) {
				bucketManager.createBucket(bucketName, this.properties.getRegion());
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("七牛云存储配置错误! 请检查系统配置!");
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
			throw new OssException("创建Bucket失败, 请核对七牛云配置信息");
		}
	}

	@Override
	public String getServiceType() {
		return CloudServiceEnumd.QINIU.getValue();
	}

	@Override
	public UploadResult upload(byte[] data, String path, String contentType) {
		try {
			Response res = uploadManager.put(data, path, token, null, contentType, false);
			if (!res.isOK()) {
				throw new RuntimeException("上传七牛出错：" + res.toString());
			}
		} catch (Exception e) {
			throw new OssException("上传文件失败，请核对七牛配置信息");
		}
		return new UploadResult().setUrl(getEndpointLink() + "/" + path).setFilename(path);
	}

	@Override
	public void delete(String path) {
		try {
			path = path.replace(getEndpointLink() + "/", "");
			Response res = bucketManager.delete(properties.getBucketName(), path);
			if (!res.isOK()) {
				throw new RuntimeException("删除七牛文件出错：" + res.toString());
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
	public void afterPropertiesSet() throws Exception {
		OssFactory.register(getServiceType(),this);
	}

	@Override
	public String getEndpointLink() {
		return properties.getDomain();
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

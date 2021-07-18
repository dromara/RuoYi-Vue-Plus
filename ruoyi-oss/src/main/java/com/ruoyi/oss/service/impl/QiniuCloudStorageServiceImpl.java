package com.ruoyi.oss.service.impl;

import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.properties.QiniuProperties;
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
		// z0 z1 z2
		Configuration config = new Configuration(Region.autoRegion());
		// 默认不使用https
		config.useHttpsDomains = false;
		uploadManager = new UploadManager(config);
		Auth auth = Auth.create(
			this.properties.getAccessKey(),
			this.properties.getSecretKey());
		token = auth.uploadToken(this.properties.getBucketName());
		bucketManager = new BucketManager(auth, config);
	}


	@Override
	public String getServiceType() {
		return CloudServiceEnumd.QINIU.getValue();
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
		return this.properties.getDomain() + "/" + path;
	}

	@Override
	public void delete(String path) {
		try {
			path = path.replace(this.properties.getDomain() + "/", "");
			Response res = bucketManager.delete(this.properties.getBucketName(), path);
			if (!res.isOK()) {
				throw new RuntimeException("删除七牛文件出错：" + res.toString());
			}
		} catch (Exception e) {
			throw new OssException(e.getMessage());
		}
	}

	@Override
	public String uploadSuffix(byte[] data, String suffix) {
		return upload(data, getPath(this.properties.getPrefix(), suffix));
	}

	@Override
	public String uploadSuffix(InputStream inputStream, String suffix) {
		return upload(inputStream, getPath(this.properties.getPrefix(), suffix));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		OssFactory.register(getServiceType(),this);
	}

}

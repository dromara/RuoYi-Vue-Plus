package com.ruoyi.oss.service.impl;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.AliyunProperties;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 阿里云存储
 *
 * @author Lion Li
 */
@Lazy
@Service
public class AliyunCloudStorageServiceImpl extends AbstractCloudStorageService implements InitializingBean {

	private final OSSClient client;
	private final AliyunProperties properties;

	@Autowired
	public AliyunCloudStorageServiceImpl(CloudStorageProperties properties) {
		this.properties = properties.getAliyun();
		ClientConfiguration configuration = new ClientConfiguration();
		DefaultCredentialProvider credentialProvider = new DefaultCredentialProvider(
			this.properties.getAccessKeyId(),
			this.properties.getAccessKeySecret());
		client = new OSSClient(this.properties.getEndpoint(), credentialProvider, configuration);
	}

	@Override
	public String getServiceType() {
		return CloudServiceEnumd.ALIYUN.getValue();
	}

	@Override
	public String upload(byte[] data, String path) {
		return upload(new ByteArrayInputStream(data), path);
	}

	@Override
	public String upload(InputStream inputStream, String path) {
		try {
			client.putObject(this.properties.getBucketName(), path, inputStream);
		} catch (Exception e) {
			throw new OssException("上传文件失败，请检查配置信息");
		}
		return this.properties.getEndpoint() + "/" + path;
	}

	@Override
	public void delete(String path) {
		path = path.replace(this.properties.getEndpoint() + "/", "");
		try {
			client.deleteObject(this.properties.getBucketName(), path);
		} catch (Exception e) {
			throw new OssException("上传文件失败，请检查配置信息");
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

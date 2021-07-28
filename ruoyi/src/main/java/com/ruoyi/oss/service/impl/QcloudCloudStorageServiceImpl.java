package com.ruoyi.oss.service.impl;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.properties.CloudStorageProperties.QcloudProperties;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 腾讯云存储
 *
 * @author Lion Li
 */
@Lazy
@Service
public class QcloudCloudStorageServiceImpl extends AbstractCloudStorageService implements InitializingBean {

	private final COSClient client;
	private final QcloudProperties properties;

	@Autowired
	public QcloudCloudStorageServiceImpl(CloudStorageProperties properties) {
		this.properties = properties.getQcloud();
		try {
			COSCredentials credentials = new BasicCOSCredentials(
				this.properties.getSecretId(),
				this.properties.getSecretKey());
			// 初始化客户端配置
			ClientConfig clientConfig = new ClientConfig();
			// 设置bucket所在的区域，华南：gz 华北：tj 华东：sh
			clientConfig.setRegion(new Region(this.properties.getRegion()));
			if (this.properties.getIsHttps()) {
				clientConfig.setHttpProtocol(HttpProtocol.https);
			} else {
				clientConfig.setHttpProtocol(HttpProtocol.http);
			}
			client = new COSClient(credentials, clientConfig);
			createBucket();
		} catch (Exception e) {
			throw new IllegalArgumentException("腾讯云存储配置错误! 请检查系统配置!");
		}
	}

	@Override
	public void createBucket() {
		try {
			String bucketName = properties.getBucketName();
			if (client.doesBucketExist(bucketName)) {
				return;
			}
			CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
			createBucketRequest.setCannedAcl(CannedAccessControlList.PublicRead);
			client.createBucket(createBucketRequest);
		} catch (Exception e) {
			throw new OssException("创建Bucket失败, 请核对腾讯云配置信息");
		}
	}

	@Override
	public String getServiceType() {
		return CloudServiceEnumd.QCLOUD.getValue();
	}

	@Override
	public UploadResult upload(byte[] data, String path, String contentType) {
		return upload(new ByteArrayInputStream(data), path, contentType);
	}

	@Override
	public UploadResult upload(InputStream inputStream, String path, String contentType) {
		try {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(contentType);
			client.putObject(new PutObjectRequest(properties.getBucketName(), path, inputStream, metadata));
		} catch (Exception e) {
			throw new OssException("上传文件失败，请检查腾讯云配置信息");
		}
		return new UploadResult().setUrl(getEndpointLink() + "/" + path).setFilename(path);
	}

	@Override
	public void delete(String path) {
		path = path.replace(getEndpointLink() + "/", "");
		try {
			client.deleteObject(new DeleteObjectRequest(properties.getBucketName(), path));
		} catch (Exception e) {
			throw new OssException("上传文件失败，请检腾讯云查配置信息");
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
		String endpoint = properties.getEndpoint();
		StringBuilder sb = new StringBuilder(endpoint);
		if (StrUtil.containsAnyIgnoreCase(endpoint, "http://")) {
			sb.insert(7, properties.getBucketName() + ".");
		} else if (StrUtil.containsAnyIgnoreCase(endpoint, "https://")) {
			sb.insert(8, properties.getBucketName() + ".");
		} else {
			throw new OssException("Endpoint配置错误");
		}
		return sb.toString();
	}
}

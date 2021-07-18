package com.ruoyi.oss.service.impl;

import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.properties.CloudStorageProperties.MinioProperties;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import io.minio.MinioClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * minio存储
 *
 * @author Lion Li
 */
@Lazy
@Service
public class MinioCloudStorageServiceImpl extends AbstractCloudStorageService implements InitializingBean {

	private final MinioClient minioClient;
	private final MinioProperties properties;

	@Autowired
	public MinioCloudStorageServiceImpl(CloudStorageProperties properties) {
		this.properties = properties.getMinio();
		try {
			minioClient = MinioClient.builder()
				.endpoint(this.properties.getEndpoint())
				.credentials(this.properties.getAccessKey(), this.properties.getSecretKey())
				.build();
		} catch (Exception e) {
			throw new IllegalArgumentException("Minio存储配置错误! 请检查系统配置!");
		}
	}

	@Override
	public String getServiceType() {
		return CloudServiceEnumd.MINIO.getValue();
	}

	@Override
	public String upload(byte[] data, String path) {
		try {

		} catch (Exception e) {
			throw new OssException("上传文件失败，请核对Minio配置信息");
		}
		return this.properties.getEndpoint() + "/" + path;
	}

	@Override
	public void delete(String path) {
		try {

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

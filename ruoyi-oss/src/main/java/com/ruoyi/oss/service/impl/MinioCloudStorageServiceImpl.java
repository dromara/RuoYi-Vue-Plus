package com.ruoyi.oss.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.enumd.PolicyType;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.properties.CloudStorageProperties.MinioProperties;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import io.minio.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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
			createBucket();
		} catch (Exception e) {
			throw new IllegalArgumentException("Minio存储配置错误! 请检查系统配置!");
		}
	}

	@Override
	public void createBucket() {
		try {
			String bucketName = properties.getBucketName();
			boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
			if (exists) {
				return;
			}
			// 不存在就创建桶
			minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
			minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
				.bucket(bucketName)
				.config(getPolicy(bucketName, PolicyType.READ))
				.build());
		} catch (Exception e) {
			throw new OssException("创建Bucket失败, 请核对Minio配置信息");
		}
	}

	@Override
	public String getServiceType() {
		return CloudServiceEnumd.MINIO.getValue();
	}

	@Override
	public UploadResult upload(byte[] data, String path, String contentType) {
		return upload(new ByteArrayInputStream(data), path, contentType);
	}

	@Override
	public UploadResult upload(InputStream inputStream, String path, String contentType) {
		try {
			minioClient.putObject(PutObjectArgs.builder()
				.bucket(properties.getBucketName())
				.object(path)
				.contentType(StrUtil.blankToDefault(contentType, MediaType.APPLICATION_OCTET_STREAM_VALUE))
				.stream(inputStream, inputStream.available(), -1)
				.build());
		} catch (Exception e) {
			throw new OssException("上传文件失败，请核对Minio配置信息");
		}
		return new UploadResult().setUrl(getEndpointLink() + "/" + path).setFilename(path);
	}

	@Override
	public void delete(String path) {
		path = path.replace(getEndpointLink() + "/", "");
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
				.bucket(properties.getBucketName())
				.object(path)
				.build());
		} catch (Exception e) {
			throw new OssException(e.getMessage());
		}
	}

	@Override
	public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
		return upload(data, getPath("", suffix), contentType);
	}

	@Override
	public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
		return upload(inputStream, getPath("", suffix), contentType);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		OssFactory.register(getServiceType(), this);
	}

	@Override
	public String getEndpointLink() {
		return properties.getEndpoint() + "/" + properties.getBucketName();
	}

	private String getPolicy(String bucketName, PolicyType policyType) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("    \"Statement\": [\n");
		builder.append("        {\n");
		builder.append("            \"Action\": [\n");
		if (policyType == PolicyType.WRITE) {
			builder.append("                \"s3:GetBucketLocation\",\n");
			builder.append("                \"s3:ListBucketMultipartUploads\"\n");
		} else if (policyType == PolicyType.READ_WRITE) {
			builder.append("                \"s3:GetBucketLocation\",\n");
			builder.append("                \"s3:ListBucket\",\n");
			builder.append("                \"s3:ListBucketMultipartUploads\"\n");
		} else {
			builder.append("                \"s3:GetBucketLocation\"\n");
		}
		builder.append("            ],\n");
		builder.append("            \"Effect\": \"Allow\",\n");
		builder.append("            \"Principal\": \"*\",\n");
		builder.append("            \"Resource\": \"arn:aws:s3:::");
		builder.append(bucketName);
		builder.append("\"\n");
		builder.append("        },\n");
		if (PolicyType.READ.equals(policyType)) {
			builder.append("        {\n");
			builder.append("            \"Action\": [\n");
			builder.append("                \"s3:ListBucket\"\n");
			builder.append("            ],\n");
			builder.append("            \"Effect\": \"Deny\",\n");
			builder.append("            \"Principal\": \"*\",\n");
			builder.append("            \"Resource\": \"arn:aws:s3:::");
			builder.append(bucketName);
			builder.append("\"\n");
			builder.append("        },\n");
		}
		builder.append("        {\n");
		builder.append("            \"Action\": ");
		switch (policyType) {
			case WRITE:
				builder.append("[\n");
				builder.append("                \"s3:AbortMultipartUpload\",\n");
				builder.append("                \"s3:DeleteObject\",\n");
				builder.append("                \"s3:ListMultipartUploadParts\",\n");
				builder.append("                \"s3:PutObject\"\n");
				builder.append("            ],\n");
				break;
			case READ_WRITE:
				builder.append("[\n");
				builder.append("                \"s3:AbortMultipartUpload\",\n");
				builder.append("                \"s3:DeleteObject\",\n");
				builder.append("                \"s3:GetObject\",\n");
				builder.append("                \"s3:ListMultipartUploadParts\",\n");
				builder.append("                \"s3:PutObject\"\n");
				builder.append("            ],\n");
				break;
			default:
				builder.append("\"s3:GetObject\",\n");
				break;
		}
		builder.append("            \"Effect\": \"Allow\",\n");
		builder.append("            \"Principal\": \"*\",\n");
		builder.append("            \"Resource\": \"arn:aws:s3:::");
		builder.append(bucketName);
		builder.append("/*\"\n");
		builder.append("        }\n");
		builder.append("    ],\n");
		builder.append("    \"Version\": \"2012-10-17\"\n");
		builder.append("}\n");
		return builder.toString();
	}
}

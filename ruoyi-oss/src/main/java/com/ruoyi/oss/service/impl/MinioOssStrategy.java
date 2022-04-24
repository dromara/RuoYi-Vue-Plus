package com.ruoyi.oss.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oss.constant.OssConstant;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.OssEnumd;
import com.ruoyi.oss.enumd.PolicyType;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.properties.OssProperties;
import com.ruoyi.oss.service.abstractd.AbstractOssStrategy;
import io.minio.*;
import io.minio.http.HttpUtils;
import okhttp3.HttpUrl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * minio存储策略
 *
 * @author Lion Li
 */
@Component
public class MinioOssStrategy extends AbstractOssStrategy {

    private MinioClient minioClient;

    @Override
    public void init(OssProperties ossProperties) {
        super.init(ossProperties);
        try {
            MinioClient.Builder builder = MinioClient.builder();
            if (OssConstant.IS_HTTPS.equals(ossProperties.getIsHttps())) {
                HttpUrl url = HttpUtils.getBaseUrl(properties.getEndpoint())
                    .newBuilder().scheme("https").build();
                builder.endpoint(url);
            } else {
                builder.endpoint(properties.getEndpoint());
            }
            minioClient = builder.credentials(properties.getAccessKey(), properties.getSecretKey()).build();
            createBucket();
        } catch (Exception e) {
            throw new OssException("Minio存储配置错误! 请检查系统配置:[" + e.getMessage() + "]");
        }
        isInit = true;
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
            throw new OssException("创建Bucket失败, 请核对Minio配置信息:[" + e.getMessage() + "]");
        }
    }

    @Override
    public OssEnumd getServiceType() {
        return OssEnumd.MINIO;
    }

    @Override
    public UploadResult upload(byte[] data, String path, String contentType) {
        return upload(new ByteArrayInputStream(data), path, contentType);
    }

    @Override
    public UploadResult upload(InputStream inputStream, String path, String contentType) {
        try {
            // 解决 inputStream.available() 再 socket 下传输延迟问题 导致获取数值不精确
            Thread.sleep(1000);
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(path)
                .contentType(StringUtils.blankToDefault(contentType, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .stream(inputStream, inputStream.available(), -1)
                .build());
        } catch (Exception e) {
            throw new OssException("上传文件失败，请核对Minio配置信息:[" + e.getMessage() + "]");
        }
        return UploadResult.builder().url(getEndpointLink() + "/" + path).filename(path).build();
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
        return upload(data, getPath(properties.getPrefix(), suffix), contentType);
    }

    @Override
    public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
        return upload(inputStream, getPath(properties.getPrefix(), suffix), contentType);
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

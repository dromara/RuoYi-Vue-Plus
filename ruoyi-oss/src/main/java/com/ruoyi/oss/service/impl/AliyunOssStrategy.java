package com.ruoyi.oss.service.impl;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.OssEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.properties.OssProperties;
import com.ruoyi.oss.service.abstractd.AbstractOssStrategy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 阿里云存储策略
 *
 * @author Lion Li
 */
@Component
public class AliyunOssStrategy extends AbstractOssStrategy {

    private OSSClient client;

    @Override
    public void init(OssProperties ossProperties) {
        super.init(ossProperties);
        try {
            ClientConfiguration configuration = new ClientConfiguration();
            DefaultCredentialProvider credentialProvider = new DefaultCredentialProvider(
                properties.getAccessKey(), properties.getSecretKey());
            client = new OSSClient(properties.getEndpoint(), credentialProvider, configuration);
            createBucket();
        } catch (Exception e) {
            throw new OssException("阿里云存储配置错误! 请检查系统配置:[" + e.getMessage() + "]");
        }
        isInit = true;
    }

    @Override
    public void createBucket() {
        try {
            String bucketName = properties.getBucketName();
            if (client.doesBucketExist(bucketName)) {
                return;
            }
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
            client.createBucket(createBucketRequest);
        } catch (Exception e) {
            throw new OssException("创建Bucket失败, 请核对阿里云配置信息:[" + e.getMessage() + "]");
        }
    }

    @Override
    public OssEnumd getServiceType() {
        return OssEnumd.ALIYUN;
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
            throw new OssException("上传文件失败，请检查阿里云配置信息:[" + e.getMessage() + "]");
        }
        return UploadResult.builder().url(getEndpointLink() + "/" + path).filename(path).build();    }

    @Override
    public void delete(String path) {
        path = path.replace(getEndpointLink() + "/", "");
        try {
            client.deleteObject(properties.getBucketName(), path);
        } catch (Exception e) {
            throw new OssException("上传文件失败，请检查阿里云配置信息:[" + e.getMessage() + "]");
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
        String endpoint = properties.getEndpoint();
        StringBuilder sb = new StringBuilder(endpoint);
        if (StringUtils.containsAnyIgnoreCase(endpoint, "http://")) {
            sb.insert(7, properties.getBucketName() + ".");
        } else if (StringUtils.containsAnyIgnoreCase(endpoint, "https://")) {
            sb.insert(8, properties.getBucketName() + ".");
        } else {
            throw new OssException("Endpoint配置错误");
        }
        return sb.toString();
    }
}

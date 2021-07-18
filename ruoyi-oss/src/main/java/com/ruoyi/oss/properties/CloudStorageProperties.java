package com.ruoyi.oss.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS云存储 配置属性
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloud-storage")
public class CloudStorageProperties {

	private QiniuProperties qiniu;

	private AliyunProperties aliyun;

	private QcloudProperties qcloud;

	private MinioProperties minio;

}

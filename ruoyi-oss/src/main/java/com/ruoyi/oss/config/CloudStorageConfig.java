package com.ruoyi.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * 云存储配置信息
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cloud-storage")
public class CloudStorageConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 类型 1：七牛 2：阿里云 3：腾讯云 4: minio
	 */
	private Integer type;

	/**
	 * 七牛绑定的域名
	 */
	private String domain;

	/**
	 * 七牛路径前缀
	 */
	private String prefix;

	/**
	 * 七牛ACCESS_KEY
	 */
	private String accessKey;

	/**
	 * 七牛SECRET_KEY
	 */
	private String secretKey;

	/**
	 * 七牛存储空间名
	 */
	private String bucketName;

	/**
	 * 腾讯云AppId
	 */
	private Integer qcloudAppId;

	/**
	 * 腾讯云SecretId
	 */
	private String qcloudSecretId;

	/**
	 * 腾讯云SecretKey
	 */
	private String qcloudSecretKey;

	/**
	 * 腾讯云COS所属地区
	 */
	private String qcloudRegion;

}

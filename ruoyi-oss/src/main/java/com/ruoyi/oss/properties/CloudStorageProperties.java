package com.ruoyi.oss.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
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

	private Boolean previewListImage;

	private QiniuProperties qiniu;

	private AliyunProperties aliyun;

	private QcloudProperties qcloud;

	private MinioProperties minio;

	/**
	 * 阿里云 配置属性
	 *
	 * @author Lion Li
	 */
	@Data
	@NoArgsConstructor
	public static class AliyunProperties {

		/**
		 * 阿里云绑定的域名
		 */
		private String endpoint;

		/**
		 * 阿里云路径前缀
		 */
		private String prefix;

		/**
		 * 阿里云AccessKeyId
		 */
		private String accessKeyId;

		/**
		 * 阿里云AccessKeySecret
		 */
		private String accessKeySecret;

		/**
		 * 阿里云BucketName
		 */
		private String bucketName;

	}

	/**
	 * Minio 配置属性
	 *
	 * @author Lion Li
	 */
	@Data
	@NoArgsConstructor
	public static class MinioProperties {

		/**
		 * minio域名
		 */
		private String endpoint;

		/**
		 * minio ACCESS_KEY
		 */
		private String accessKey;

		/**
		 * minio SECRET_KEY
		 */
		private String secretKey;

		/**
		 * minio 存储空间名
		 */
		private String bucketName;

	}

	/**
	 * 腾讯云COS 配置属性
	 *
	 * @author Lion Li
	 */
	@Data
	@NoArgsConstructor
	public static class QcloudProperties {

		/**
		 * 腾讯云绑定的域名
		 */
		private String endpoint;

		/**
		 * 腾讯云路径前缀
		 */
		private String prefix;

		/**
		 * 腾讯云SecretId
		 */
		private String secretId;

		/**
		 * 腾讯云SecretKey
		 */
		private String secretKey;

		/**
		 * 腾讯云BucketName
		 */
		private String bucketName;

		/**
		 * 七牛是否使用https
		 */
		private Boolean isHttps;

		/**
		 * 腾讯云COS所属地区
		 */
		private String region;

	}

	/**
	 * 七牛云 配置属性
	 *
	 * @author Lion Li
	 */
	@Data
	@NoArgsConstructor
	public static class QiniuProperties {

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
		 * 七牛存储区域
		 */
		private String region;

		/**
		 * 七牛是否使用https
		 */
		private Boolean isHttps;

	}

}

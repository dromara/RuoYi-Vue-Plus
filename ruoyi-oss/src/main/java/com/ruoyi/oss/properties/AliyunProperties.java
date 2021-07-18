package com.ruoyi.oss.properties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阿里云 配置属性
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class AliyunProperties {

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

package com.ruoyi.oss.properties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 腾讯云COS 配置属性
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class QcloudProperties {

	/**
	 * 腾讯云绑定的域名
	 */
	private String domain;

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
	 * 腾讯云COS所属地区
	 */
	private String region;

}

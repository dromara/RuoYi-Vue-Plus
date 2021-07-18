package com.ruoyi.oss.enumd;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 云存储服务商枚举
 *
 * @author LionLi
 */
@Getter
@AllArgsConstructor
public enum CloudServiceEnumd {

	/**
	 * 七牛云
	 */
	QINIU("qiniu"),

	/**
	 * 阿里云
	 */
	ALIYUN("aliyun"),

	/**
	 * 腾讯云
	 */
	QCLOUD("qcloud"),

	/**
	 * minio
	 */
	MINIO("minio");

	private final String value;
}

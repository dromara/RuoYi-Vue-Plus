package com.ruoyi.oss.properties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minio 配置属性
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class MinioProperties {

	/**
	 * 七牛绑定的域名
	 */
	private String endpoint;

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

}

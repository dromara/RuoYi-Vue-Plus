package com.ruoyi.oss.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


public class CloudConstant {

	/**
	 * 云存储配置KEY
	 */
	public final static String CLOUD_STORAGE_CONFIG_KEY = "sys.oss.cloud-storage";

	/**
	 * 云服务商
	 */
	@Getter
	@AllArgsConstructor
	public enum CloudService {

		/**
		 * 七牛云
		 */
		QINIU(1),

		/**
		 * 阿里云
		 */
		ALIYUN(2),

		/**
		 * 腾讯云
		 */
		QCLOUD(3),

		/**
		 * minio
		 */
		MINIO(4);

		private final int value;
	}
}

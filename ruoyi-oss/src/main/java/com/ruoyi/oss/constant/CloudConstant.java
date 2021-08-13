package com.ruoyi.oss.constant;

import com.ruoyi.oss.properties.CloudStorageProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 云存储常量
 *
 * @author Lion Li
 */
public class CloudConstant {

	/**
	 * OSS模块KEY
	 */
	public static final String SYS_OSS_KEY = "sys_oss:";

	/**
	 * 云存储配置KEY
	 */
	public static final String CLOUD_STORAGE_CONFIG_KEY = "CloudStorageConfig";

	/**
	 * 缓存配置KEY
	 */
	public static final String CACHE_CONFIG_KEY = SYS_OSS_KEY + CLOUD_STORAGE_CONFIG_KEY;

	/**
	 * 预览列表资源开关Key
	 */
	public static final String PEREVIEW_LIST_RESOURCE_KEY = "sys.oss.previewListResource";

	/**
	 * 系统数据ids
	 */
	public static final List<Integer> SYSTEM_DATA_IDS = Arrays.asList(1, 2, 3, 4);

	public static String getPropertiesName(String key) {
		return key + CloudStorageProperties.class.getSimpleName();
	}

}

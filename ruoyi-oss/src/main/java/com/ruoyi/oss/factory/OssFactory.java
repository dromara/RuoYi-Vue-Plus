package com.ruoyi.oss.factory;

import cn.hutool.core.convert.Convert;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.RedisUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.reflect.ReflectUtils;
import com.ruoyi.oss.constant.CloudConstant;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.exception.OssException;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.service.ICloudStorageStrategy;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传Factory
 *
 * @author Lion Li
 */
@Slf4j
public class OssFactory {

	/**
	 * 服务实例缓存
	 */
	private static final Map<String, ICloudStorageStrategy> SERVICES = new ConcurrentHashMap<>();

    /**
     * 初始化工厂
     */
    public static void init() {
        log.info("初始化OSS工厂");
        RedisUtils.subscribe(CloudConstant.CACHE_CONFIG_KEY, String.class, msg -> {
            refreshService(msg);
            log.info("订阅刷新OSS配置 => " + msg);
        });
    }

	/**
	 * 获取默认实例
	 */
	public static ICloudStorageStrategy instance() {
		// 获取redis 默认类型
		String type = Convert.toStr(RedisUtils.getCacheObject(CloudConstant.CACHE_CONFIG_KEY));
		if (StringUtils.isEmpty(type)) {
			throw new OssException("文件存储服务类型无法找到!");
		}
		return instance(type);
	}

	/**
	 * 根据类型获取实例
	 */
	public static ICloudStorageStrategy instance(String type) {
		ICloudStorageStrategy service = SERVICES.get(type);
		if (service == null) {
			refreshService(type);
			service = SERVICES.get(type);
		}
		return service;
	}

	private static void refreshService(String type) {
		Object json = RedisUtils.getCacheObject(CloudConstant.SYS_OSS_KEY + type);
		CloudStorageProperties properties = JsonUtils.parseObject(json.toString(), CloudStorageProperties.class);
		if (properties == null) {
			throw new OssException("系统异常, '" + type + "'配置信息不存在!");
		}
		// 获取redis配置信息 创建对象 并缓存
		ICloudStorageStrategy service = (ICloudStorageStrategy) ReflectUtils.newInstance(CloudServiceEnumd.getServiceClass(type));
		((AbstractCloudStorageStrategy)service).init(properties);
		SERVICES.put(type, service);
	}

}

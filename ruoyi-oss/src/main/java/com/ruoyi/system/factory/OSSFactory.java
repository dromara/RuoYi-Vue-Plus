package com.ruoyi.system.factory;

import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;

/**
 * 文件上传Factory
 */
public class OSSFactory {

//    private static ISysConfigService sysConfigService;

	static {
//        OSSFactory.sysConfigService = SpringUtils.getBean(ISysConfigService.class);
	}

	public static AbstractCloudStorageService build() {
//        String jsonconfig = sysConfigService.selectConfigByKey(CloudConstant.CLOUD_STORAGE_CONFIG_KEY);
//        // 获取云存储配置信息
//        CloudStorageConfig config = JSON.parseObject(jsonconfig, CloudStorageConfig.class);
//        if (config.getType() == CloudConstant.CloudService.QINIU.getValue()) {
//            return new QiniuCloudStorageServiceImpl(config);
//        } else if (config.getType() == CloudConstant.CloudService.ALIYUN.getValue()) {
//            return new AliyunCloudStorageServiceImpl(config);
//        } else if (config.getType() == CloudConstant.CloudService.QCLOUD.getValue()) {
//            return new QcloudCloudStorageServiceImpl(config);
//        } else if (config.getType() == CloudConstant.CloudService.MINIO.getValue()) {
//			return new MinioCloudStorageServiceImpl(config);
//		}
		return null;
	}
}

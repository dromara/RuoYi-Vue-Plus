package com.ruoyi.oss.service.impl;

import cn.hutool.core.io.IoUtil;
import com.qcloud.cos.COSClient;
import com.ruoyi.oss.config.CloudStorageConfig;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;

import java.io.InputStream;

/**
 * 腾讯云存储
 */
public class QcloudCloudStorageServiceImpl extends AbstractCloudStorageService {

	private COSClient client;

	public QcloudCloudStorageServiceImpl(CloudStorageConfig config) {
		this.config = config;
		// 初始化
		init();
	}

	private void init() {
//        Credentials credentials = new Credentials(config.getQcloudAppId(), config.getQcloudSecretId(),
//                config.getQcloudSecretKey());
//         初始化客户端配置
//        ClientConfig clientConfig = new ClientConfig();
//        // 设置bucket所在的区域，华南：gz 华北：tj 华东：sh
//        clientConfig.setRegion(config.getQcloudRegion());
//        client = new COSClient(clientConfig, credentials);
	}

	@Override
	public String upload(byte[] data, String path) {
		// 腾讯云必需要以"/"开头
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		// 上传到腾讯云
//        UploadFileRequest request = new UploadFileRequest(config.getQcloudBucketName(), path, data);
//        String response = client.uploadFile(request);
//        Map<String, Object> jsonObject = JsonUtils.parseMap(response);
//        if (Convert.toInt(jsonObject.get("code")) != 0) {
//            throw new OssException("文件上传失败，" + Convert.toStr(jsonObject.get("message")));
//        }
		return config.getDomain() + path;
	}

	@Override
	public void delete(String path) {
//        path = path.replace(config.getDomain(),"");
//        DelFileRequest request = new DelFileRequest(config.getBucketName(), path);
//        String response = client.delFile(request);
//		Map<String, Object> jsonObject = JsonUtils.parseMap(response);
//        if (Convert.toInt(jsonObject.get("code")) != 0) {
//            throw new OssException("文件删除失败，" + Convert.toStr(jsonObject.get("message")));
//        }
	}


	@Override
	public String upload(InputStream inputStream, String path) {
		byte[] data = IoUtil.readBytes(inputStream);
		return this.upload(data, path);
	}

	@Override
	public String uploadSuffix(byte[] data, String suffix) {
		return upload(data, getPath(config.getPrefix(), suffix));
	}

	@Override
	public String uploadSuffix(InputStream inputStream, String suffix) {
		return upload(inputStream, getPath(config.getPrefix(), suffix));
	}

}

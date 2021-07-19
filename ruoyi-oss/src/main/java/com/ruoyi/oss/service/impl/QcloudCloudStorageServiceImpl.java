package com.ruoyi.oss.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.CloudServiceEnumd;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.properties.CloudStorageProperties;
import com.ruoyi.oss.properties.CloudStorageProperties.QcloudProperties;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 腾讯云存储
 *
 * @author Lion Li
 */
@Lazy
@Service
public class QcloudCloudStorageServiceImpl extends AbstractCloudStorageService implements InitializingBean {

	private final COSClient client;
	private final QcloudProperties properties;

	@Autowired
	public QcloudCloudStorageServiceImpl(CloudStorageProperties properties) {
		this.properties = properties.getQcloud();
		try {
			COSCredentials credentials = new BasicCOSCredentials(
				this.properties.getSecretId(),
				this.properties.getSecretKey());
			// 初始化客户端配置
			ClientConfig clientConfig = new ClientConfig();
			// 设置bucket所在的区域，华南：gz 华北：tj 华东：sh
			clientConfig.setRegion(new Region(this.properties.getRegion()));
			client = new COSClient(credentials, clientConfig);
		} catch (Exception e) {
			throw new IllegalArgumentException("腾讯云存储配置错误! 请检查系统配置!");
		}
	}

	@Override
	public String getServiceType() {
		return CloudServiceEnumd.QCLOUD.getValue();
	}

	@Override
	public UploadResult upload(byte[] data, String path) {
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
		return new UploadResult().setUrl(properties.getDomain() + "/" + path).setFilename(path);
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
	public UploadResult uploadSuffix(byte[] data, String suffix) {
		return upload(data, getPath(this.properties.getPrefix(), suffix));
	}

	@Override
	public UploadResult uploadSuffix(InputStream inputStream, String suffix) {
		return upload(inputStream, getPath(this.properties.getPrefix(), suffix));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		OssFactory.register(getServiceType(),this);
	}
}

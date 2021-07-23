package com.ruoyi.oss.service;

import com.ruoyi.oss.entity.UploadResult;

import java.io.InputStream;

/**
 * 云存储服务接口
 *
 * @author Lion Li
 */
public interface ICloudStorageService {

	void createBucket();

	/**
	 * 获取服务商类型
	 */
	String getServiceType();

	/**
	 * 文件路径
	 *
	 * @param prefix 前缀
	 * @param suffix 后缀
	 * @return 返回上传路径
	 */
	String getPath(String prefix, String suffix);

	/**
	 * 文件上传
	 *
	 * @param data 文件字节数组
	 * @param path 文件路径，包含文件名
	 * @return 返回http地址
	 */
	UploadResult upload(byte[] data, String path, String contentType);

	/**
	 * 文件删除
	 *
	 * @param path 文件路径，包含文件名
	 */
	void delete(String path);

	/**
	 * 文件上传
	 *
	 * @param data   文件字节数组
	 * @param suffix 后缀
	 * @return 返回http地址
	 */
	UploadResult uploadSuffix(byte[] data, String suffix, String contentType);

	/**
	 * 文件上传
	 *
	 * @param inputStream 字节流
	 * @param path        文件路径，包含文件名
	 * @return 返回http地址
	 */
	UploadResult upload(InputStream inputStream, String path, String contentType);

	/**
	 * 文件上传
	 *
	 * @param inputStream 字节流
	 * @param suffix      后缀
	 * @return 返回http地址
	 */
	UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType);

    String getEndpointLink();
}

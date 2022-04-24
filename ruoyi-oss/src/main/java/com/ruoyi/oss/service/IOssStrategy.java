package com.ruoyi.oss.service;

import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.enumd.OssEnumd;

import java.io.InputStream;

/**
 * 对象存储策略
 *
 * @author Lion Li
 */
public interface IOssStrategy {

    /**
     * 创建存储桶
     */
    void createBucket();

    /**
     * 获取服务商类型
     * @return 对象存储服务商枚举
     */
    OssEnumd getServiceType();

    /**
     * 文件上传
     *
     * @param data 文件字节数组
     * @param path 文件路径，包含文件名
     * @param contentType 文件类型
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
     * @param contentType 文件类型
     * @return 返回http地址
     */
    UploadResult uploadSuffix(byte[] data, String suffix, String contentType);

    /**
     * 文件上传
     *
     * @param inputStream 字节流
     * @param path        文件路径，包含文件名
     * @param contentType 文件类型
     * @return 返回http地址
     */
    UploadResult upload(InputStream inputStream, String path, String contentType);

    /**
     * 文件上传
     *
     * @param inputStream 字节流
     * @param suffix      后缀
     * @param contentType 文件类型
     * @return 返回http地址
     */
    UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType);

}

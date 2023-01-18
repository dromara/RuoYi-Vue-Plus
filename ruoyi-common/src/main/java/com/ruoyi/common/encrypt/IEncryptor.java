package com.ruoyi.common.encrypt;

import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

/**
 * 加解者
 *
 * @author 老马
 * @date 2023-01-10 16:08
 */
public interface IEncryptor {

    /**
     * 获得当前算法
     *
     * @return com.ruoyi.common.enums.AlgorithmType
     * @author 老马
     * @date 2023/1/11 11:18
     */
    AlgorithmType algorithm();

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     * @return java.lang.String 加密后的字符串
     * @throws Exception 抛出异常
     * @author 老马
     * @date 2023/1/10 16:38
     */
    String encrypt(String value, EncodeType encodeType) throws Exception;

    /**
     * 解密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     * @return java.lang.String 解密后的字符串
     * @throws Exception 抛出异常
     * @author 老马
     * @date 2023/1/10 16:38
     */
    String decrypt(String value, EncodeType encodeType) throws Exception;
}

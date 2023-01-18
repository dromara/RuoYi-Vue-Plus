package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.codec.Base64;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

/**
 * Base64算法实现。不建议在生产环境使用
 *
 * @author 老马
 * @date 2023-01-06 10:00
 */
public class Base64Encryptor extends AbstractEncryptor {

    public Base64Encryptor(EncryptContext context) {
        super(context);
    }

    /**
     * 获得当前算法
     *
     * @return com.ruoyi.common.enums.AlgorithmType
     * @author 老马
     * @date 2023/1/11 11:18
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.BASE64;
    }

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     * @return java.lang.String
     * @author 老马
     * @date 2023/1/10 16:38
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) throws Exception {
        return Base64.encode(value);
    }

    /**
     * 解密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     * @return java.lang.String
     * @author 老马
     * @date 2023/1/10 16:38
     */
    @Override
    public String decrypt(String value, EncodeType encodeType) throws Exception {
        return Base64.decodeStr(value);
    }
}

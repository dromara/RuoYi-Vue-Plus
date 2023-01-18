package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

import java.nio.charset.StandardCharsets;

/**
 * AES算法实现
 *
 * @author 老马
 * @date 2023-01-06 11:39
 */
public class AesEncryptor extends AbstractEncryptor {

    private AES aes = null;

    public AesEncryptor(EncryptContext context) {
        super(context);
        String password = context.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new RuntimeException("aes没有获得秘钥信息");
        }
        // aes算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if(!ArrayUtil.contains(array, password.length())) {
            throw new RuntimeException("aes秘钥长度应该为16位、24位、32位，实际为"+password.length()+"位");
        }
        aes = SecureUtil.aes(context.getPassword().getBytes(StandardCharsets.UTF_8));
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
        return AlgorithmType.AES;
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
        if (ObjectUtil.isNotNull(this.aes)) {
            if (encodeType == EncodeType.HEX) {
                return aes.encryptHex(value);
            } else {
                return aes.encryptBase64(value);
            }
        }
        return value;
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
        if (ObjectUtil.isNotNull(this.aes)) {
            return this.aes.decryptStr(value);
        }
        return value;
    }
}

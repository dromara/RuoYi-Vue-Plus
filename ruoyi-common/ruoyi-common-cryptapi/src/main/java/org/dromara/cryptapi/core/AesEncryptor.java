package org.dromara.cryptapi.core;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.dromara.cryptapi.enums.EncodeType;

import java.nio.charset.StandardCharsets;

/**
 * AES算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class AesEncryptor {

    private final AES aes;

    public AesEncryptor(EncryptContext context) {
        String password = context.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES没有获得秘钥信息");
        }
        // aes算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度应该为16位、24位、32位，实际为" + password.length() + "位");
        }
        aes = SecureUtil.aes(context.getPassword().getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     */
    public String encrypt(String value, EncodeType encodeType) {
        if (encodeType == EncodeType.HEX) {
            return aes.encryptHex(value);
        } else {
            return aes.encryptBase64(value);
        }
    }

    /**
     * 解密
     *
     * @param value      待加密字符串
     */
    public String decrypt(String value) {
        return this.aes.decryptStr(value);
    }
}

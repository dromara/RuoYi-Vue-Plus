package org.dromara.cryptapi.core;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.cryptapi.enums.EncodeType;


/**
 * RSA算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class RsaEncryptor {

    private final RSA rsa;

    public RsaEncryptor(EncryptContext context) {
        String privateKey = context.getPrivateKey();
        String publicKey = context.getPublicKey();
        if (StringUtils.isAnyEmpty(privateKey, publicKey)) {
            throw new IllegalArgumentException("RSA公私钥均需要提供，公钥加密，私钥解密。");
        }
        this.rsa = SecureUtil.rsa(Base64.decode(privateKey), Base64.decode(publicKey));
    }

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     */
    public String encrypt(String value, EncodeType encodeType) {
        if (encodeType == EncodeType.HEX) {
            return rsa.encryptHex(value, KeyType.PublicKey);
        } else {
            return rsa.encryptBase64(value, KeyType.PublicKey);
        }
    }

    /**
     * 解密
     *
     * @param value      待加密字符串
     */
    public String decrypt(String value) {
        return this.rsa.decryptStr(value, KeyType.PrivateKey);
    }
}

package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;
import com.ruoyi.common.utils.StringUtils;


/**
 * RSA算法实现
 *
 * @author 老马
 * @date 2023-01-06 09:37
 */
public class RsaEncryptor extends AbstractEncryptor {

    private RSA rsa = null;

    public RsaEncryptor(EncryptContext context) {
        super(context);
        String privateKey = context.getPrivateKey();
        String publicKey = context.getPublicKey();
        if (StringUtils.isAnyEmpty(privateKey, publicKey)) {
            throw new RuntimeException("rsa公私钥均需要提供，公钥加密，私钥解密。");
        }
        this.rsa = SecureUtil.rsa(Base64.decode(privateKey), Base64.decode(publicKey));
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
        return AlgorithmType.RSA;
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
        if (ObjectUtil.isNotNull(this.rsa)) {
            if (encodeType == EncodeType.HEX) {
                return rsa.encryptHex(value, KeyType.PublicKey);
            } else {
                return rsa.encryptBase64(value, KeyType.PublicKey);
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
        if (ObjectUtil.isNotNull(this.rsa)) {
            return this.rsa.decryptStr(value, KeyType.PrivateKey);
        }
        return value;
    }
}

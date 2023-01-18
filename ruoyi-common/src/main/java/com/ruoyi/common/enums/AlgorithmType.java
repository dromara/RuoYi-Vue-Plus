package com.ruoyi.common.enums;

import com.ruoyi.common.encrypt.IEncryptor;
import com.ruoyi.common.encrypt.encryptor.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 算法名称
 *
 * @author 老马
 */
@Getter
@AllArgsConstructor
public enum AlgorithmType {
    /**
     * base64
     */
    BASE64(Base64Encryptor.class),

    /**
     * aes
     */
    AES(AesEncryptor.class),

    /**
     * rsa
     */
    RSA(RsaEncryptor.class),

    /**
     * sm2
     */
    SM2(Sm2Encryptor.class),

    /**
     * sm4
     */
    SM4(Sm4Encryptor.class);

    private final Class<? extends IEncryptor> clazz;
}

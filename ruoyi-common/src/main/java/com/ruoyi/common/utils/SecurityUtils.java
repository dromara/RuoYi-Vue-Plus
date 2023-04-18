package com.ruoyi.common.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.asymmetric.SM2;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全相关工具类
 *
 * @author 老马
 */
public class SecurityUtils {
    /**
     * 公钥
     */
    public static final String PUBLIC_KEY = "publicKey";
    /**
     * 私钥
     */
    public static final String PRIVATE_KEY = "privateKey";

    /**
     * Base64加密
     *
     * @param data 待加密数据
     * @return 加密后字符串
     */
    public static String encryptByBase64(String data) {
        return Base64.encode(data, StandardCharsets.UTF_8);
    }

    /**
     * Base64解密
     *
     * @param data 待解密数据
     * @return 解密后字符串
     */
    public static String decryptByBase64(String data) {
        return Base64.decodeStr(data, StandardCharsets.UTF_8);
    }

    /**
     * AES加密
     *
     * @param data     待解密数据
     * @param password 秘钥字符串
     * @return 加密后字符串, 采用Base64编码
     */
    public static String encryptByAes(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES需要传入秘钥信息");
        }
        // aes算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度要求为16位、24位、32位");
        }
        return SecureUtil.aes(password.getBytes(StandardCharsets.UTF_8)).encryptBase64(data, StandardCharsets.UTF_8);
    }

    /**
     * AES解密
     *
     * @param data     待解密数据
     * @param password 秘钥字符串
     * @return 解密后字符串
     */
    public static String decryptByAes(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES需要传入秘钥信息");
        }
        // aes算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度要求为16位、24位、32位");
        }
        return SecureUtil.aes(password.getBytes(StandardCharsets.UTF_8)).decryptStr(data, StandardCharsets.UTF_8);
    }

    /**
     * sm4加密
     *
     * @param data     待加密数据
     * @param password 秘钥字符串
     * @return 加密后字符串, 采用Base64编码
     */
    public static String encryptBySm4(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("SM4需要传入秘钥信息");
        }
        // sm4算法的秘钥要求是16位长度
        int sm4PasswordLength = 16;
        if (sm4PasswordLength != password.length()) {
            throw new IllegalArgumentException("SM4秘钥长度要求为16位");
        }
        return SmUtil.sm4(password.getBytes(StandardCharsets.UTF_8)).encryptBase64(data, StandardCharsets.UTF_8);
    }

    /**
     * sm4解密
     *
     * @param data     待解密数据
     * @param password 秘钥字符串
     * @return 解密后字符串
     */
    public static String decryptBySm4(String data, String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("SM4需要传入秘钥信息");
        }
        // sm4算法的秘钥要求是16位长度
        int sm4PasswordLength = 16;
        if (sm4PasswordLength != password.length()) {
            throw new IllegalArgumentException("SM4秘钥长度要求为16位");
        }
        return SmUtil.sm4(password.getBytes(StandardCharsets.UTF_8)).decryptStr(data, StandardCharsets.UTF_8);
    }

    /**
     * 产生sm2加解密需要的公钥和私钥
     *
     * @return 公私钥Map
     */
    public static Map<String, String> generateSm2Key() {
        Map<String, String> keyMap = new HashMap<>();
        SM2 sm2 = SmUtil.sm2();
        keyMap.put(PRIVATE_KEY, sm2.getPrivateKeyBase64());
        keyMap.put(PUBLIC_KEY, sm2.getPublicKeyBase64());
        return keyMap;
    }

    /**
     * sm2公钥加密
     *
     * @param data      待加密数据
     * @param publicKey 公钥
     * @return 加密后字符串, 采用Base64编码
     */
    public static String encryptBySm2(String data, String publicKey) {
        if (StrUtil.isBlank(publicKey)) {
            throw new IllegalArgumentException("SM2需要传入公钥进行加密");
        }
        SM2 sm2 = SmUtil.sm2(null, publicKey);
        return sm2.encryptBase64(data, StandardCharsets.UTF_8, KeyType.PublicKey);
    }

    /**
     * sm2私钥解密
     *
     * @param data       待加密数据
     * @param privateKey 私钥
     * @return 解密后字符串
     */
    public static String decryptBySm2(String data, String privateKey) {
        if (StrUtil.isBlank(privateKey)) {
            throw new IllegalArgumentException("SM2需要传入私钥进行解密");
        }
        SM2 sm2 = SmUtil.sm2(privateKey, null);
        return sm2.decryptStr(data, KeyType.PrivateKey, StandardCharsets.UTF_8);
    }

    /**
     * 产生RSA加解密需要的公钥和私钥
     *
     * @return 公私钥Map
     */
    public static Map<String, String> generateRsaKey() {
        Map<String, String> keyMap = new HashMap<>();
        RSA rsa = SecureUtil.rsa();
        keyMap.put(PRIVATE_KEY, rsa.getPrivateKeyBase64());
        keyMap.put(PUBLIC_KEY, rsa.getPublicKeyBase64());
        return keyMap;
    }

    /**
     * rsa公钥加密
     *
     * @param data      待加密数据
     * @param publicKey 公钥
     * @return 加密后字符串, 采用Base64编码
     */
    public static String encryptByRsa(String data, String publicKey) {
        if (StrUtil.isBlank(publicKey)) {
            throw new IllegalArgumentException("RSA需要传入公钥进行加密");
        }
        RSA rsa = SecureUtil.rsa(null, publicKey);
        return rsa.encryptBase64(data, StandardCharsets.UTF_8, KeyType.PublicKey);
    }

    /**
     * rsa私钥解密
     *
     * @param data       待加密数据
     * @param privateKey 私钥
     * @return 解密后字符串
     */
    public static String decryptByRsa(String data, String privateKey) {
        if (StrUtil.isBlank(privateKey)) {
            throw new IllegalArgumentException("RSA需要传入私钥进行解密");
        }
        RSA rsa = SecureUtil.rsa(privateKey, null);
        return rsa.decryptStr(data, KeyType.PrivateKey, StandardCharsets.UTF_8);
    }

    // 测试方法
    public static void main(String[] args) {
        String str = "";
        String password = "";
        Map<String, String> keyMap = new HashMap<>();

        //base64加解密测试
        str = "老马base64加解密测试";
        String encryptByBase64 = encryptByBase64(str);
        System.out.println("base64加解密测试: encryptByBase64=" + encryptByBase64);
        String decryptByBase64 = decryptByBase64(encryptByBase64);
        System.out.println("base64加解密测试: decryptByBase64=" + decryptByBase64);
        //aes加解密测试
        str = "老马aes测试";
        password = "adsfasddg234sfwt2545sgr@";
        String encryptByAes = encryptByAes(str, password);
        System.out.println("aes加解密测试: encryptByAes=" + encryptByAes);
        String decryptByAes = decryptByAes(encryptByAes, password);
        System.out.println("aes加解密测试: decryptByAes=" + decryptByAes);
        //sm4加解密测试
        str = "老马sm4测试";
        password = "adsfasddg234sfwt";
        String encryptBySm4 = encryptBySm4(str, password);
        System.out.println("sm4加解密测试: encryptBySm4=" + encryptBySm4);
        String decryptBySm4 = decryptBySm4(encryptBySm4, password);
        System.out.println("sm4加解密测试: decryptBySm4=" + decryptBySm4);
        //sm2加解密测试
        str = "老马sm2测试";
        keyMap = generateSm2Key();
        String encryptBySm2 = encryptBySm2(str, keyMap.get(SecurityUtils.PUBLIC_KEY));
        System.out.println("sm2加解密测试: encryptBySm2=" + encryptBySm2);
        String decryptBySm2 = decryptBySm2(encryptBySm2, keyMap.get(SecurityUtils.PRIVATE_KEY));
        System.out.println("sm2加解密测试: decryptBySm2=" + decryptBySm2);
        //rsa加解密测试
        str = "老马rsa测试";
        keyMap = generateRsaKey();
        String encryptByRsa = encryptByRsa(str, keyMap.get(SecurityUtils.PUBLIC_KEY));
        System.out.println("rsa加解密测试: encryptByRsa=" + encryptByRsa);
        String decryptByRsa = decryptByRsa(encryptByRsa, keyMap.get(SecurityUtils.PRIVATE_KEY));
        System.out.println("rsa加解密测试: decryptByRsa=" + decryptByRsa);

    }
}

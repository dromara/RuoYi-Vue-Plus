package com.ruoyi.framework.encrypt;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.encrypt.IEncryptor;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密管理类
 *
 * @author 老马
 * @date 2023-01-11 10:07
 */
@Slf4j
public class EncryptorManager {

    /**
     * 缓存加密器
     */
    Map<String, IEncryptor> encryptorMap = new ConcurrentHashMap<>();


    /**
     * 注册加密执行者到缓存
     *
     * @param properties 加密执行者需要的相关配置参数
     * @author 老马
     * @date 2023/1/11 15:09
     */
    public IEncryptor registAndGetEncryptor(EncryptorProperties properties) {
        String encryptorKey = this.getEncryptorKeyFromProperties(properties);
        if (encryptorMap.containsKey(encryptorKey)) {
            return encryptorMap.get(encryptorKey);
        }
        EncryptContext encryptContext = new EncryptContext();
        encryptContext.setPassword(properties.getPassword());
        encryptContext.setPrivateKey(properties.getPrivateKey());
        encryptContext.setPublicKey(properties.getPublicKey());
        encryptContext.setEncode(properties.getEncode());
        IEncryptor encryptor = ReflectUtil.newInstance(properties.getAlgorithm().getClazz(), encryptContext);
        encryptorMap.put(encryptorKey, encryptor);
        return encryptorMap.get(encryptorKey);
    }

    /**
     * 移除缓存中的加密执行者
     *
     * @param properties 加密执行者需要的相关配置参数
     * @author 老马
     * @date 2023/1/11 15:55
     */
    public void removeEncryptor(EncryptorProperties properties) {
        this.encryptorMap.remove(getEncryptorKeyFromProperties(properties));
    }

    /**
     * 根据配置进行加密。会进行本地缓存对应的算法和对应的秘钥信息。
     *
     * @param value      待加密的值
     * @param properties 加密相关的配置信息
     * @return java.lang.String 加密后的结果
     * @author 老马
     * @date 2023/1/11 16:46
     */
    public String encrypt(String value, EncryptorProperties properties) {
        try {
            IEncryptor encryptor = this.registAndGetEncryptor(properties);
            if(ObjectUtil.isNull(encryptor)){
                return value;
            }
            return encryptor.encrypt(value, properties.getEncode());
        } catch (Exception e) {
            log.error("字段加密异常,原样返回", e);
            return value;
        }
    }

    /**
     * 根据配置进行解密
     *
     * @param value      待解密的值
     * @param properties 加密相关的配置信息
     * @return java.lang.String
     * @author 老马
     * @date 2023/1/11 17:43
     */
    public String decrypt(String value, EncryptorProperties properties) {
        try {
            IEncryptor encryptor = this.registAndGetEncryptor(properties);
            if(ObjectUtil.isNull(encryptor)){
                return value;
            }
            return encryptor.decrypt(value, properties.getEncode());
        } catch (Exception e) {
            log.error("字段解密异常,原样返回", e);
            return value;
        }
    }


    /**
     * 从配置内容中提取缓存的KEY
     *
     * @param properties 加密相关的配置信息
     * @return java.lang.String
     * @author 老马
     * @date 2023/1/11 17:39
     */
    private String getEncryptorKeyFromProperties(EncryptorProperties properties) {
        return properties.getAlgorithm() + StringUtils.defaultString(properties.getPassword()) +
            StringUtils.defaultString(properties.getPublicKey()) + StringUtils.defaultString(properties.getPrivateKey());
    }

}

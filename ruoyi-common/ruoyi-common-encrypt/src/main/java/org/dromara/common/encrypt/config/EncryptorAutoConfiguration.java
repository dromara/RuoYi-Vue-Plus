package org.dromara.common.encrypt.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.encrypt.core.EncryptContextFactory;
import org.dromara.common.encrypt.core.EncryptedFieldProcessor;
import org.dromara.common.encrypt.core.EncryptorManager;
import org.dromara.common.encrypt.enums.AlgorithmType;
import org.dromara.common.encrypt.interceptor.MybatisDecryptInterceptor;
import org.dromara.common.encrypt.interceptor.MybatisEncryptInterceptor;
import org.dromara.common.encrypt.properties.EncryptorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 加解密配置
 *
 * @author Lion Li
 */
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@EnableConfigurationProperties(EncryptorProperties.class)
@ConditionalOnProperty(value = "mybatis-encryptor.enable", havingValue = "true")
@Slf4j
public class EncryptorAutoConfiguration {

    @Autowired
    private EncryptorProperties properties;

    @Bean
    public EncryptorManager encryptorManager(MybatisPlusProperties mybatisPlusProperties) {
        validateEncryptorProperties(properties);
        return new EncryptorManager(mybatisPlusProperties.getTypeAliasesPackage());
    }

    @Bean
    public EncryptContextFactory encryptContextFactory() {
        return new EncryptContextFactory(properties);
    }

    @Bean
    public EncryptedFieldProcessor encryptedFieldProcessor(EncryptorManager encryptorManager, EncryptContextFactory encryptContextFactory) {
        return new EncryptedFieldProcessor(encryptorManager, encryptContextFactory);
    }

    @Bean
    public MybatisEncryptInterceptor mybatisEncryptInterceptor(EncryptedFieldProcessor encryptedFieldProcessor) {
        return new MybatisEncryptInterceptor(encryptedFieldProcessor);
    }

    @Bean
    public MybatisDecryptInterceptor mybatisDecryptInterceptor(EncryptedFieldProcessor encryptedFieldProcessor) {
        return new MybatisDecryptInterceptor(encryptedFieldProcessor);
    }

    private void validateEncryptorProperties(EncryptorProperties properties) {
        AlgorithmType algorithm = properties.getAlgorithm();
        if (algorithm == AlgorithmType.AES || algorithm == AlgorithmType.SM4) {
            if (StringUtils.isBlank(properties.getPassword())) {
                throw new IllegalArgumentException("mybatis-encryptor.password 不能为空");
            }
        }
        if (algorithm == AlgorithmType.RSA || algorithm == AlgorithmType.SM2) {
            if (StringUtils.isAnyBlank(properties.getPublicKey(), properties.getPrivateKey())) {
                throw new IllegalArgumentException("mybatis-encryptor.publicKey 与 privateKey 不能为空");
            }
        }
    }

}




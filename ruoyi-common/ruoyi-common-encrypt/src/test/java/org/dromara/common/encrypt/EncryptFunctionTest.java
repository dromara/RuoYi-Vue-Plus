package org.dromara.common.encrypt;

import org.dromara.common.core.constant.Constants;
import org.dromara.common.encrypt.annotation.EncryptField;
import org.dromara.common.encrypt.core.EncryptContext;
import org.dromara.common.encrypt.core.EncryptContextFactory;
import org.dromara.common.encrypt.core.EncryptorManager;
import org.dromara.common.encrypt.enums.AlgorithmType;
import org.dromara.common.encrypt.enums.EncodeType;
import org.dromara.common.encrypt.properties.EncryptorProperties;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("common-encrypt 功能单元测试")
class EncryptFunctionTest {

    /**
     * 验证 Base64、AES 和 SM4 的常用加解密能够无损往返。
     */
    @Test
    @DisplayName("常用对称算法加解密往返")
    void shouldRoundTripCommonSymmetricAlgorithms() {
        String text = "RuoYi-Vue-Plus";
        String aesKey = "1234567890abcdef";
        String sm4Key = "abcdef1234567890";

        assertEquals(text, EncryptUtils.decryptByBase64(EncryptUtils.encryptByBase64(text)));
        assertEquals(text, EncryptUtils.decryptByAes(EncryptUtils.encryptByAes(text, aesKey), aesKey));
        assertEquals(text, EncryptUtils.decryptBySm4(EncryptUtils.encryptBySm4(text, sm4Key), sm4Key));
    }

    /**
     * 验证非法对称密钥长度在执行加密前被拒绝。
     */
    @Test
    @DisplayName("校验对称算法密钥长度")
    void shouldRejectInvalidSymmetricKeys() {
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptByAes("data", "short"));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptBySm4("data", "short"));
    }

    /**
     * 验证生成的 RSA 密钥满足校验要求并可以完成公钥加密、私钥解密。
     */
    @Test
    @DisplayName("RSA 密钥生成和加解密往返")
    void shouldGenerateAndUseRsaKeys() {
        Map<String, String> keys = EncryptUtils.generateRsaKey();

        assertDoesNotThrow(() -> EncryptUtils.validateRsaPublicKey(keys.get(EncryptUtils.PUBLIC_KEY)));
        assertDoesNotThrow(() -> EncryptUtils.validateRsaPrivateKey(keys.get(EncryptUtils.PRIVATE_KEY)));
        String encrypted = EncryptUtils.encryptByRsa("secure-data", keys.get(EncryptUtils.PUBLIC_KEY));
        assertEquals("secure-data", EncryptUtils.decryptByRsa(encrypted, keys.get(EncryptUtils.PRIVATE_KEY)));
    }

    /**
     * 验证字段注解优先于默认配置构建加密上下文。
     *
     * @throws Exception 读取测试字段失败
     */
    @Test
    @DisplayName("合并字段注解和默认加密配置")
    void shouldCreateEncryptContextFromAnnotationAndDefaults() throws Exception {
        EncryptorProperties properties = new EncryptorProperties();
        properties.setAlgorithm(AlgorithmType.AES);
        properties.setEncode(EncodeType.BASE64);
        properties.setPassword("default-password");
        properties.setPublicKey("default-public");
        properties.setPrivateKey("default-private");
        Field field = TestEntity.class.getDeclaredField("secret");

        EncryptContext context = new EncryptContextFactory(properties).create(field);

        assertEquals(AlgorithmType.SM4, context.getAlgorithm());
        assertEquals(EncodeType.HEX, context.getEncode());
        assertEquals("field-password", context.getPassword());
        assertEquals("default-public", context.getPublicKey());
        assertEquals("default-private", context.getPrivateKey());
    }

    /**
     * 验证加密管理器添加统一密文头，并避免对已有密文重复加密。
     */
    @Test
    @DisplayName("管理带标识头的加密值")
    void shouldManageEncryptedValueHeader() {
        EncryptContext context = new EncryptContext();
        context.setAlgorithm(AlgorithmType.BASE64);
        context.setEncode(EncodeType.BASE64);
        EncryptorManager manager = new EncryptorManager("");

        String encrypted = manager.encrypt("plain-text", context);

        assertTrue(encrypted.startsWith(Constants.ENCRYPT_HEADER));
        assertEquals(encrypted, manager.encrypt(encrypted, context));
        assertEquals("plain-text", manager.decrypt(encrypted, context));
        assertEquals("plain-text", manager.decrypt("plain-text", context));
    }

    private static class TestEntity {

        @EncryptField(algorithm = AlgorithmType.SM4, encode = EncodeType.HEX, password = "field-password")
        private String secret;
    }
}

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

import cn.hutool.crypto.SmUtil;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("common-encrypt 功能单元测试")
class EncryptFunctionTest {

    @Test
    void sm2VerifiesEncodedSignaturesOnlyForTheSignedPayload() {
        var keys = EncryptUtils.generateSm2Key();
        String publicKey = keys.get(EncryptUtils.PUBLIC_KEY);
        String payload = "签收订单42";
        byte[] message = payload.getBytes(StandardCharsets.UTF_8);
        byte[] signature = SmUtil.sm2(keys.get(EncryptUtils.PRIVATE_KEY), publicKey).sign(message);
        String encoded = Base64.getEncoder().encodeToString(signature);
        String hex = HexFormat.of().formatHex(signature);

        assertTrue(EncryptUtils.verifySm2SignHex(HexFormat.of().formatHex(message), hex, publicKey));
        assertTrue(EncryptUtils.verifySm2Sign(payload, encoded, publicKey));
        assertFalse(EncryptUtils.verifySm2Sign(payload + "0", encoded, publicKey));
    }

    @Test
    void hexCiphertextsRoundTripUtf8Payloads() {
        String data = "订单42";
        String key = "1234567890abcdef";
        assertEquals(data, EncryptUtils.decryptByAes(EncryptUtils.encryptByAesHex(data, key), key));
        assertEquals(data, EncryptUtils.decryptBySm4(EncryptUtils.encryptBySm4Hex(data, key), key));
        var sm2 = EncryptUtils.generateSm2Key();
        String publicKey = sm2.get(EncryptUtils.PUBLIC_KEY);
        String privateKey = sm2.get(EncryptUtils.PRIVATE_KEY);
        assertEquals(data, EncryptUtils.decryptBySm2(EncryptUtils.encryptBySm2Hex(data, publicKey), privateKey));
        assertEquals(data, EncryptUtils.decryptBySm2(EncryptUtils.encryptBySm2(data, publicKey), privateKey));
        var rsa = EncryptUtils.generateRsaKey();
        assertEquals(data, EncryptUtils.decryptByRsa(EncryptUtils.encryptByRsaHex(data,
            rsa.get(EncryptUtils.PUBLIC_KEY)), rsa.get(EncryptUtils.PRIVATE_KEY)));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "short"})
    void symmetricOperationsRejectMissingAndInvalidLengthKeys(String key) {
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptByAes("order", key));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptByAesHex("order", key));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.decryptByAes("1234", key));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptBySm4("order", key));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptBySm4Hex("order", key));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.decryptBySm4("1234", key));
    }

    @Test
    void asymmetricOperationsRequireKeysAndVerificationRequiresASignatureAndMessage() {
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptBySm2("data", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptBySm2Hex("data", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.decryptBySm2("1234", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptByRsa("data", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.encryptByRsaHex("data", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.decryptByRsa("1234", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.validateRsaPublicKey(""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.validateRsaPrivateKey(""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.verifySm2Sign("", "sig", "key"));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.verifySm2Sign("data", "", "key"));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.verifySm2Sign("data", "sig", ""));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.verifySm2SignHex("", "12", "key"));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.verifySm2SignHex("12", "", "key"));
        assertThrows(IllegalArgumentException.class, () -> EncryptUtils.verifySm2SignHex("12", "12", ""));
    }
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

    @Test
    void onlyAnnotatedTextOnEntityTypesIsEligibleForFieldEncryption() throws Exception {
        var manager = new EncryptorManager("org.dromara.common.encrypt");
        for (Class<?> type : new Class<?>[]{null, int.class, byte[].class, AlgorithmType.class,
            EncryptField.class, Integer.class, String.class, Runnable.class, TestEntity.class}) {
            assertTrue(manager.getFieldCache(type).isEmpty());
        }
        class Invoice {
            @EncryptField String contact;
            @EncryptField Long amount;
            String label;
        }
        assertEquals(java.util.Set.of(Invoice.class.getDeclaredField("contact")), manager.getFieldCache(Invoice.class));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = AlgorithmType.class, names = {"AES", "SM4", "RSA", "SM2"})
    void configuredAlgorithmsSupportBothStorageEncodings(AlgorithmType algorithm) {
        var manager = new EncryptorManager("");
        var context = new EncryptContext();
        context.setAlgorithm(algorithm);
        context.setPassword("1234567890abcdef");
        if (algorithm == AlgorithmType.RSA || algorithm == AlgorithmType.SM2) {
            var keys = algorithm == AlgorithmType.RSA ? EncryptUtils.generateRsaKey() : EncryptUtils.generateSm2Key();
            context.setPrivateKey(keys.get(EncryptUtils.PRIVATE_KEY));
            context.setPublicKey(keys.get(EncryptUtils.PUBLIC_KEY));
        }
        for (var encoding : new EncodeType[]{EncodeType.BASE64, EncodeType.HEX}) {
            context.setEncode(encoding);
            String stored = manager.encrypt("invoice", context);
            String payload = stored.substring(Constants.ENCRYPT_HEADER.length());
            if (encoding == EncodeType.HEX) {
                assertTrue(HexFormat.of().parseHex(payload).length > 0);
            } else {
                assertTrue(Base64.getDecoder().decode(payload).length > 0);
            }
            assertEquals("invoice", manager.decrypt(stored, context));
        }
    }
    private static class TestEntity {

        @EncryptField(algorithm = AlgorithmType.SM4, encode = EncodeType.HEX, password = "field-password")
        private String secret;
    }
}

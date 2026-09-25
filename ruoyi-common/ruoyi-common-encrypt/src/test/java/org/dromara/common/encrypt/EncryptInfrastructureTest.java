package org.dromara.common.encrypt;

import org.dromara.common.core.constant.Constants;
import org.dromara.common.encrypt.annotation.EncryptField;
import org.dromara.common.encrypt.core.EncryptContext;
import org.dromara.common.encrypt.core.EncryptContextFactory;
import org.dromara.common.encrypt.core.EncryptedFieldProcessor;
import org.dromara.common.encrypt.core.EncryptorManager;
import org.dromara.common.encrypt.core.IEncryptor;
import org.dromara.common.encrypt.enums.AlgorithmType;
import org.dromara.common.encrypt.enums.EncodeType;
import org.dromara.common.encrypt.filter.DecryptRequestBodyWrapper;
import org.dromara.common.encrypt.filter.EncryptResponseBodyWrapper;
import org.dromara.common.encrypt.properties.EncryptorProperties;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("common-encrypt 基础设施单元测试")
class EncryptInfrastructureTest {

    /**
     * 验证集合和 Map 中的加密字段会被处理，且字段快照能够恢复持久化前的原始值。
     */
    @Test
    @DisplayName("加密并恢复容器中的字段")
    void shouldEncryptAndRestoreFieldsInsideContainers() {
        EncryptorProperties properties = new EncryptorProperties();
        properties.setAlgorithm(AlgorithmType.BASE64);
        properties.setEncode(EncodeType.BASE64);
        EncryptorManager manager = new EncryptorManager("");
        EncryptedFieldProcessor processor = new EncryptedFieldProcessor(
            manager, new EncryptContextFactory(properties));
        TestSecretEntity first = new TestSecretEntity("first");
        TestSecretEntity second = new TestSecretEntity("second");
        List<Object> source = new ArrayList<>();
        source.add(first);
        source.add(Map.of("entity", second));
        source.add(source);

        List<EncryptedFieldProcessor.FieldSnapshot> snapshots = processor.encrypt(source);

        assertEquals(2, snapshots.size());
        assertTrue(first.secret.startsWith(Constants.ENCRYPT_HEADER));
        assertTrue(second.secret.startsWith(Constants.ENCRYPT_HEADER));
        snapshots.forEach(EncryptedFieldProcessor.FieldSnapshot::restore);
        assertEquals("first", first.secret);
        assertEquals("second", second.secret);

        processor.encrypt(source);
        processor.decrypt(source);
        assertEquals("first", first.secret);
        assertEquals("second", second.secret);
    }

    /**
     * 验证相同上下文复用加密器缓存，显式移除后会重新创建实例。
     */
    @Test
    @DisplayName("复用和移除加密器缓存")
    void shouldReuseAndRemoveEncryptorCache() {
        EncryptorManager manager = new EncryptorManager("");
        EncryptContext context = new EncryptContext();
        context.setAlgorithm(AlgorithmType.BASE64);
        context.setEncode(EncodeType.BASE64);

        IEncryptor first = manager.registAndGetEncryptor(context);
        IEncryptor cached = manager.registAndGetEncryptor(context);
        manager.removeEncryptor(context);
        IEncryptor recreated = manager.registAndGetEncryptor(context);

        assertSame(first, cached);
        assertNotSame(first, recreated);
        assertTrue(manager.getFieldCache(String.class).isEmpty());
        assertTrue(manager.getFieldCache(null).isEmpty());
    }

    /**
     * 验证加密请求包装器能解开请求头中的 AES 密钥并提供可重复读取的 JSON 明文。
     */
    @Test
    @DisplayName("解密 API 请求体")
    void shouldDecryptApiRequestBody() throws Exception {
        Map<String, String> rsaKeys = EncryptUtils.generateRsaKey();
        String aesPassword = "1234567890abcdef";
        String headerName = "encrypt-key";
        String encryptedHeader = EncryptUtils.encryptByRsa(
            EncryptUtils.encryptByBase64(aesPassword), rsaKeys.get(EncryptUtils.PUBLIC_KEY));
        String json = "{\"name\":\"测试\"}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(headerName, encryptedHeader);
        request.setContent(EncryptUtils.encryptByAes(json, aesPassword).getBytes(StandardCharsets.UTF_8));

        DecryptRequestBodyWrapper wrapper = new DecryptRequestBodyWrapper(
            request, rsaKeys.get(EncryptUtils.PRIVATE_KEY), headerName);

        assertEquals(json, wrapper.getReader().readLine());
        assertEquals(json, new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("application/json", wrapper.getContentType());
        assertEquals(json.getBytes(StandardCharsets.UTF_8).length, wrapper.getContentLength());
        assertEquals(wrapper.getContentLength(), wrapper.getContentLengthLong());
    }

    /**
     * 验证加密响应包装器输出可由响应头携带的密钥完整解密，并正确设置响应元数据。
     */
    @Test
    @DisplayName("加密 API 响应体")
    void shouldEncryptApiResponseBody() throws Exception {
        Map<String, String> rsaKeys = EncryptUtils.generateRsaKey();
        String headerName = "encrypt-key";
        String body = "{\"message\":\"成功\"}";
        MockHttpServletResponse response = new MockHttpServletResponse();
        EncryptResponseBodyWrapper wrapper = new EncryptResponseBodyWrapper(response);
        wrapper.getWriter().write(body);

        String encryptedBody = wrapper.getEncryptContent(
            response, rsaKeys.get(EncryptUtils.PUBLIC_KEY), headerName);
        String encodedAes = EncryptUtils.decryptByRsa(
            response.getHeader(headerName), rsaKeys.get(EncryptUtils.PRIVATE_KEY));
        String aesPassword = EncryptUtils.decryptByBase64(encodedAes);

        assertEquals(body, EncryptUtils.decryptByAes(encryptedBody, aesPassword));
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        assertEquals(encryptedBody.getBytes(StandardCharsets.UTF_8).length, response.getContentLength());
        assertEquals(headerName, response.getHeader("Access-Control-Expose-Headers"));
    }

}

class TestSecretEntity {

    @EncryptField(algorithm = AlgorithmType.BASE64)
    String secret;

    /**
     * 创建带待加密字段的顶层测试实体，以符合生产扫描器对实体类的约束。
     *
     * @param secret 原始明文
     */
    TestSecretEntity(String secret) {
        this.secret = secret;
    }
}

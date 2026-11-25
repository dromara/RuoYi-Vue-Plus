package org.dromara.system.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.oss.client.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.model.GetObjectResult;
import org.dromara.system.domain.vo.SysOssVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssDownloadTest {
    private final SysOssServiceImpl service = new SysOssServiceImpl(mock());
    private final SysOssServiceImpl lookup = mock();

    @Test
    void missingMetadataStopsDownloadBeforeStorageLookup() {
        try (var spring = mockStatic(SpringUtils.class); var storage = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            assertEquals("文件数据不存在!", assertThrows(ServiceException.class, () -> service.download(42L)).getMessage());
            storage.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource({"application/pdf,application/pdf", "invalid,application/octet-stream"})
    void streamsBytesWithEncodedOriginalFilename(String storedType, String expectedType) {
        var file = new SysOssVo();
        file.setFileName("objects/a");
        file.setService("archive");
        file.setOriginalName("报 A.pdf");
        when(lookup.getById(42L)).thenReturn(file);
        var client = mock(OssClient.class);
        byte[] bytes = {1, 42, -1};
        var metadata = mock(GetObjectResult.class);
        when(metadata.size()).thenReturn((long) bytes.length);
        when(metadata.contentType()).thenReturn(storedType);
        when(client.download(eq("objects/a"), any(BiFunction.class))).thenAnswer(call -> {
            BiFunction<GetObjectResult, InputStream, ResponseEntity<byte[]>> transform = call.getArgument(1);
            return transform.apply(metadata, new ByteArrayInputStream(bytes));
        });
        try (var spring = mockStatic(SpringUtils.class); var storage = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            storage.when(() -> OssFactory.instance("archive")).thenReturn(client);
            var response = service.download(42L);
            var headers = response.getHeaders();
            assertEquals(200, response.getStatusCode().value());
            assertArrayEquals(bytes, response.getBody());
            assertEquals(bytes.length, headers.getContentLength());
            assertEquals(expectedType, headers.getContentType().toString());
            assertEquals("%E6%8A%A5%20A.pdf", headers.getFirst("download-filename"));
            assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("filename*=utf-8''%E6%8A%A5%20A.pdf"));
        }
    }
}

package org.dromara.system.service.impl;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.client.OssClient;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.common.oss.model.GetObjectResult;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssContentLookupTest {

    @Test
    void readCallbackPropagatesStorageFailure() {
        var service = new SysOssServiceImpl(mock());
        var lookup = mock(SysOssServiceImpl.class);
        var file = new SysOssVo();
        file.setService("archive");
        file.setFileName("missing-object");
        when(lookup.getById(8L)).thenReturn(file);
        var client = mock(OssClient.class);
        var failure = new IllegalStateException("object missing");
        when(client.download(eq("missing-object"), any(BiFunction.class))).thenThrow(failure);
        try (var spring = mockStatic(SpringUtils.class); var factory = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            factory.when(() -> OssFactory.instance("archive")).thenReturn(client);
            assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.readFile(8L, (metadata, stream) -> "unreachable")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void resolvesPrivateAndPublicAttachmentUrlsInRequestedOrder(boolean privateBucket) {
        var service = new SysOssServiceImpl(mock());
        var lookup = mock(SysOssServiceImpl.class);
        var file = new SysOssVo();
        file.setService("archive");
        file.setFileName("objects/3");
        file.setUrl("/public/3");
        when(lookup.getById(3L)).thenReturn(file);
        var client = mock(OssClient.class);
        when(client.verifyConfig(any(java.util.function.Function.class))).thenReturn(privateBucket);
        when(client.presignGetUrl("objects/3", java.time.Duration.ofSeconds(120))).thenReturn("/signed/3");
        try (var spring = mockStatic(SpringUtils.class); var factory = mockStatic(OssFactory.class);
             var executor = mockStatic(org.dromara.common.core.utils.ThreadUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            factory.when(() -> OssFactory.instance("archive")).thenReturn(client);
            executor.when(() -> org.dromara.common.core.utils.ThreadUtils.virtualSubmitAll(anyCollection()))
                .thenAnswer(call -> {
                    java.util.Collection<java.util.function.Supplier<String>> tasks = call.getArgument(0);
                    var results = new java.util.ArrayList<String>();
                    tasks.forEach(task -> results.add(task.get()));
                    return results;
                });
            assertEquals(privateBucket ? "/signed/3" : "/public/3", service.selectUrlByIds("404,3"));
        }
    }

    @Test
    void missingRecordsOrReaderReturnWithoutAccessingStorage() {
        var service = new SysOssServiceImpl(mock());
        var lookup = mock(SysOssServiceImpl.class);
        try (var spring = mockStatic(SpringUtils.class); var factory = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            assertNull(service.getInputStream(null));
            assertNull(service.getInputStream(8L));
            assertNull(service.readFile(8L, null));
            assertNull(service.readFile(8L, (info, stream) -> "unexpected"));
            factory.verifyNoInteractions();
        }
    }

    @Test
    void readsTheStoredKeyAndPassesMetadataToConsumer() {
        var service = new SysOssServiceImpl(mock());
        var lookup = mock(SysOssServiceImpl.class);
        var file = new SysOssVo();
        file.setOriginalName("合同.pdf");
        file.setFileName("files/8");
        file.setService("archive");
        when(lookup.getById(8L)).thenReturn(file);
        var client = mock(OssClient.class);
        when(client.download(eq("files/8"), any(BiFunction.class))).thenAnswer(call -> {
            BiFunction<GetObjectResult, InputStream, String> callback = call.getArgument(1);
            return callback.apply(null, new ByteArrayInputStream(new byte[]{31}));
        });
        try (var spring = mockStatic(SpringUtils.class); var factory = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            factory.when(() -> OssFactory.instance("archive")).thenReturn(client);
            String result = service.readFile(8L, (info, stream) -> {
                assertEquals("合同.pdf", info.getOriginalName());
                assertInstanceOf(ByteArrayInputStream.class, stream);
                return "read";
            });
            assertEquals("read", result);
        }
    }
}

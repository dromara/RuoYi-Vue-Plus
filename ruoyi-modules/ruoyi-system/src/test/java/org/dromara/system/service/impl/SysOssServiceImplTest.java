package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.oss.client.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.model.Options;
import org.dromara.common.oss.model.PutObjectResult;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.mapper.SysOssMapper;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysOssServiceImplTest {

    @org.junit.jupiter.api.Test
    void multipartUploadPersistsBusinessMetadataAlongWithMeasuredSize() {
        var mapper = mock(SysOssMapper.class);
        var service = new SysOssServiceImpl(mapper);
        var client = mock(OssClient.class);
        var file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[]{1,2,3});
        var ext = new org.dromara.system.domain.SysOssExt();
        ext.setBizType("report");
        ext.setRefId("57");
        ext.setFileSize(999L);
        when(client.buildPathKey("report.pdf")).thenReturn("reports/57");
        when(client.clientId()).thenReturn("archive");
        when(client.upload(eq("reports/57"), any(InputStream.class), eq(3L), any(Options.class)))
            .thenReturn(new PutObjectResult("url", "reports/57", "etag", 3));
        var json = JsonMapper.builder().build();
        try (var beans = mockStatic(SpringUtil.class); var factory = mockStatic(OssFactory.class);
             var mapping = mockStatic(MapstructUtils.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(json);
            factory.when(OssFactory::instance).thenReturn(client);
            factory.when(() -> OssFactory.instance("archive")).thenReturn(client);
            var result = new SysOssVo();
            result.setService("archive");
            mapping.when(() -> MapstructUtils.convert(any(SysOss.class), eq(SysOssVo.class))).thenReturn(result);
            service.upload(file, ext);
            var entity = ArgumentCaptor.forClass(SysOss.class);
            verify(mapper).insert(entity.capture());
            var stored = json.readTree(entity.getValue().getExt1());
            assertEquals("report", stored.path("bizType").asString());
            assertEquals("57", stored.path("refId").asString());
            assertEquals(3L, stored.path("fileSize").asLong());
            assertEquals("application/pdf", stored.path("contentType").asString());
        }
    }

    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({"README, '', true", "README, '', false", "report.final.txt, .txt, true", "report.final.txt, .txt, false"})
    void shouldPersistUploadMetadata(String name, String suffix, boolean multipart) throws Exception {
        var mapper = mock(SysOssMapper.class);
        var service = new SysOssServiceImpl(mapper);
        var client = mock(OssClient.class);
        var result = new PutObjectResult("url", "key", "etag", 1);
        when(client.buildPathKey(name)).thenReturn("key");
        when(client.clientId()).thenReturn("store");
        var json = JsonMapper.builder().build();

        try (var spring = mockStatic(SpringUtil.class); var factory = mockStatic(OssFactory.class)) {
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(json);
            factory.when(OssFactory::instance).thenReturn(client);
            factory.when(() -> OssFactory.instance("store")).thenReturn(client);
            var view = new SysOssVo();
            view.setService("store");
            try (var mappings = mockStatic(MapstructUtils.class)) {
                mappings.when(() -> MapstructUtils.convert(any(SysOss.class), eq(SysOssVo.class))).thenReturn(view);
                String contentType;
                if (multipart) {
                    contentType = "text/plain";
                    when(client.upload(eq("key"), any(InputStream.class), eq(1L), any(Options.class))).thenReturn(result);
                    service.upload(new MockMultipartFile("file", name, contentType, new byte[]{1}), null);
                } else {
                    var file = Files.write(directory.resolve(name), new byte[]{1}).toFile();
                    var options = ArgumentCaptor.forClass(Options.class);
                    when(client.upload(eq("key"), eq(file), options.capture())).thenReturn(result);
                    service.upload(file, null);
                    contentType = options.getValue().getContentType();
                }
                var entity = ArgumentCaptor.forClass(SysOss.class);
                verify(mapper).insert(entity.capture());
                var ext = json.readTree(entity.getValue().getExt1());
                assertAll(
                    () -> assertEquals(name, entity.getValue().getOriginalName()),
                    () -> assertEquals(suffix, entity.getValue().getFileSuffix()),
                    () -> assertEquals(contentType, ext.path("contentType").asString(null)),
                    () -> assertEquals(1L, ext.path("fileSize").asLong())
                );
            }
        }
    }
}

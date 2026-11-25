package org.dromara.system.service.impl;

import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.oss.client.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.system.domain.SysOss;
import org.dromara.system.mapper.SysOssMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysOssDeletionTest {

    @Test
    void missingRecordsAndUnchangedMetadataDoNotCountAsDeleted() {
        var ids = List.of(1L, 2L);
        when(mapper.selectByIds(ids)).thenReturn(List.of(file(1L)));
        when(client.delete("file1")).thenReturn(true);
        when(mapper.deleteById(1L)).thenReturn(0);
        try (var storage = mockStatic(OssFactory.class); var cache = mockStatic(CacheUtils.class)) {
            storage.when(() -> OssFactory.instance("store")).thenReturn(client);
            assertFalse(service.deleteWithValidByIds(ids, true));
            verify(client, never()).delete("file2");
            verify(mapper, never()).deleteById(2L);
        }
    }

    private final SysOssMapper mapper = mock();
    private final OssClient client = mock();
    private final SysOssServiceImpl service = new SysOssServiceImpl(mapper);

    @Test
    void successfulDeletionInvalidatesAttachmentLookup() {
        when(mapper.selectByIds(List.of(1L))).thenReturn(List.of(file(1L)));
        when(client.delete("file1")).thenReturn(true);
        when(mapper.deleteById(1L)).thenReturn(1);
        when(mapper.deleteByIds(List.of(1L))).thenReturn(1);
        try (var factory = mockStatic(OssFactory.class); var cache = mockStatic(CacheUtils.class)) {
            factory.when(() -> OssFactory.instance("store")).thenReturn(client);
            assertTrue(service.deleteWithValidByIds(List.of(1L), true));
            cache.verify(() -> CacheUtils.evict(CacheNames.SYS_OSS, 1L));
        }
    }

    @Test
    void storageFailureOnSecondFilePreservesItsRecordAndFinishesFirstFile() {
        var ids = List.of(1L, 2L);
        when(mapper.selectByIds(ids)).thenReturn(List.of(file(1L), file(2L)));
        when(client.delete("file1")).thenReturn(true);
        when(mapper.deleteById(1L)).thenReturn(1);
        when(client.delete("file2")).thenThrow(new IllegalStateException("storage unavailable"));
        try (var factory = mockStatic(OssFactory.class); var cache = mockStatic(CacheUtils.class)) {
            factory.when(() -> OssFactory.instance("store")).thenReturn(client);
            assertThrows(IllegalStateException.class, () -> service.deleteWithValidByIds(ids, true));
            verify(mapper).deleteById(1L);
            cache.verify(() -> CacheUtils.evict(CacheNames.SYS_OSS, 1L));
            verify(mapper, never()).deleteById(2L);
            cache.verify(() -> CacheUtils.evict(CacheNames.SYS_OSS, 2L), never());
        }
    }

    @Test
    void storageRefusalPreservesAttachmentMetadata() {
        when(mapper.selectByIds(List.of(1L))).thenReturn(List.of(file(1L)));
        when(client.delete("file1")).thenReturn(false);
        try (var factory = mockStatic(OssFactory.class)) {
            factory.when(() -> OssFactory.instance("store")).thenReturn(client);
            assertThrows(ServiceException.class, () -> service.deleteWithValidByIds(List.of(1L), true));
            verify(mapper, never()).deleteById(1L);
        }
    }

    private SysOss file(long id) {
        var file = new SysOss();
        file.setOssId(id);
        file.setService("store");
        file.setFileName("file" + id);
        return file;
    }
}

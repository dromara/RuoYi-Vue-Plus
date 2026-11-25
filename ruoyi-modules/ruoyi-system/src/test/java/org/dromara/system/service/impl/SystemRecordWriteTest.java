package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.system.domain.*;
import org.dromara.system.domain.bo.*;
import org.dromara.system.domain.vo.SysDictDataVo;
import org.dromara.system.mapper.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemRecordWriteTest {

    @ParameterizedTest
    @ValueSource(ints = {0,1})
    void noticeMaintenanceReturnsPersistedIdAndActualRowCount(int rows) {
        var mapper = mock(SysNoticeMapper.class);
        var service = new SysNoticeServiceImpl(mapper, mock());
        var notice = new SysNotice();
        var request = new SysNoticeBo();
        when(mapper.insert(notice)).thenAnswer(call -> {
            if (rows > 0) {
                notice.setNoticeId(17L);
            }
            return rows;
        });
        when(mapper.updateById(notice)).thenReturn(rows);
        when(mapper.deleteById(17L)).thenReturn(rows);
        when(mapper.deleteByIds(List.of(17L, 19L))).thenReturn(rows);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(request, SysNotice.class)).thenReturn(notice);
            assertEquals(rows, service.insertNotice(request));
            assertEquals(rows == 1 ? Long.valueOf(17L) : null, request.getNoticeId());
            assertEquals(rows, service.updateNotice(request));
            assertEquals(rows, service.deleteNoticeById(17L));
            assertEquals(rows, service.deleteNoticeByIds(new Long[]{17L, 19L}));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1})
    void configInsertPublishesValueOnlyForPersistedRecord(int rows) {
        var mapper = mock(SysConfigMapper.class);
        var request = new SysConfigBo();
        var entity = new SysConfig();
        entity.setConfigValue("portal");
        when(mapper.insert(entity)).thenReturn(rows);
        var service = new SysConfigServiceImpl(mapper);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(request, SysConfig.class)).thenReturn(entity);
            if (rows > 0) {
                assertEquals("portal", service.insertConfig(request));
            } else {
                assertThrows(ServiceException.class, () -> service.insertConfig(request));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true,1","true,0","false,1","false,0"})
    void dictionaryWritesReloadEntriesOnlyAfterSuccess(boolean create, int rows) {
        var mapper = mock(SysDictDataMapper.class);
        var request = new SysDictDataBo();
        var entity = new SysDictData();
        entity.setDictCode(7L);
        entity.setDictType("shipping");
        var fullList = List.of(new SysDictDataVo(), new SysDictDataVo());
        when(mapper.insert(entity)).thenReturn(rows);
        when(mapper.updateById(entity)).thenReturn(rows);
        when(mapper.selectDictDataByType("shipping")).thenReturn(fullList);
        var service = new SysDictDataServiceImpl(mapper);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var cache = mockStatic(CacheUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(request, SysDictData.class)).thenReturn(entity);
            if (rows > 0) {
                assertEquals(fullList, create ? service.insertDictData(request) : service.updateDictData(request));
            } else {
                assertThrows(ServiceException.class, () -> {
                    if (create) {
                        service.insertDictData(request);
                    } else {
                        service.updateDictData(request);
                    }
                });
                cache.verifyNoInteractions();
                verify(mapper, never()).selectDictDataByType(anyString());
            }
        }
    }

    @Test
    void deletingMixedDictionaryEntriesInvalidatesBothTypes() {
        var mapper = mock(SysDictDataMapper.class);
        var first = new SysDictData();
        first.setDictType("status");
        var second = new SysDictData();
        second.setDictType("shipping");
        var ids = List.of(7L, 9L);
        when(mapper.selectByIds(ids)).thenReturn(List.of(first, second));
        when(mapper.deleteByIds(ids)).thenReturn(2);
        try (var caches = mockStatic(CacheUtils.class)) {
            new SysDictDataServiceImpl(mapper).deleteDictDataByIds(ids);
            verify(mapper).deleteByIds(ids);
            caches.verify(() -> CacheUtils.evict(CacheNames.SYS_DICT, "status"));
            caches.verify(() -> CacheUtils.evict(CacheNames.SYS_DICT, "shipping"));
        }
    }
}

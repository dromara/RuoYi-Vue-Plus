package org.dromara.system.service.impl;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.domain.vo.SysDictDataVo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictionaryLookupTest {

    @Test
    void duplicateInputLabelsPreserveTheirOrderAndUnknownSlots() {
        var service = new SysDictTypeServiceImpl(mock(), mock());
        var cached = mock(SysDictTypeServiceImpl.class);
        var item = new SysDictDataVo();
        item.setDictValue("done");
        item.setDictLabel("完成");
        when(cached.selectDictDataByType("status")).thenReturn(List.of(item));
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(cached);
            assertEquals("done||done", service.getDictValue("status", "完成|未知|完成","|"));
            assertEquals("done", service.getDictData("status").getFirst().getDictValue());
        }
    }

    @Test
    void refreshingDictionaryCachesDropsBothDataAndTypeEntries() {
        try (var cache = mockStatic(org.dromara.common.redis.utils.CacheUtils.class)) {
            new SysDictTypeServiceImpl(mock(), mock()).resetDictCache();
            cache.verify(() -> org.dromara.common.redis.utils.CacheUtils.clear("sys_dict"));
            cache.verify(() -> org.dromara.common.redis.utils.CacheUtils.clear("sys_dict_type"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void assignedTypeBlocksEntireDeletion(boolean assigned) {
        var types = mock(org.dromara.system.mapper.SysDictTypeMapper.class);
        var entries = mock(org.dromara.system.mapper.SysDictDataMapper.class, RETURNS_DEEP_STUBS);
        var query = entries.lambda();
        when(query.eq(any(), eq("orders"))).thenReturn(query);
        when(query.exists()).thenReturn(assigned);
        var type = new org.dromara.system.domain.SysDictType();
        type.setDictType("orders");
        type.setDictName("订单");
        var ids = List.of(5L);
        when(types.selectByIds(ids)).thenReturn(List.of(type));
        var service = new SysDictTypeServiceImpl(types, entries);
        try (var cache = mockStatic(org.dromara.common.redis.utils.CacheUtils.class)) {
            if (assigned) {
                assertThrows(org.dromara.common.core.exception.ServiceException.class, () -> service.deleteDictTypeByIds(ids));
                verify(types, never()).deleteByIds(ids);
                cache.verifyNoInteractions();
            } else {
                service.deleteDictTypeByIds(ids);
                verify(types).deleteByIds(ids);
                cache.verify(() -> org.dromara.common.redis.utils.CacheUtils.evict("sys_dict", "orders"));
                cache.verify(() -> org.dromara.common.redis.utils.CacheUtils.evict("sys_dict_type", "orders"));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"done,完成", "missing,''", "done|missing|done,完成||完成", "'',''"})
    void translatesValuesWithoutDiscardingUnmatchedPositions(String input, String expected) {
        var service = new SysDictTypeServiceImpl(mock(), mock());
        var cached = mock(SysDictTypeServiceImpl.class);
        var item = new SysDictDataVo();
        item.setDictValue("done");
        item.setDictLabel("完成");
        when(cached.selectDictDataByType("status")).thenReturn(List.of(item));
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(cached);
            assertEquals(expected, service.getDictLabel("status", input, "|"));
            assertEquals(Map.of("done", "完成"), service.getAllDictByDictType("status"));
        }
    }

    @Test
    void missingTypeReturnsEmptyMappingsAndData() {
        var service = new SysDictTypeServiceImpl(mock(), mock());
        var cached = mock(SysDictTypeServiceImpl.class);
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(cached);
            assertNull(service.getDictType("absent"));
            assertTrue(service.getDictData("absent").isEmpty());
            assertTrue(service.getAllDictByDictType("absent").isEmpty());
            assertEquals("", service.getDictValue("absent", "不存在", ","));
        }
    }
}

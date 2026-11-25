package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.system.domain.SysDictData;
import org.dromara.system.domain.bo.SysDictDataBo;
import org.dromara.system.domain.vo.SysDictDataVo;
import org.dromara.system.mapper.SysDictDataMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysDictDataServiceImplTest {

    @Test
    void movingEntryInvalidatesOriginalDictionaryList() {
        var mapper = mock(SysDictDataMapper.class);
        var original = new SysDictData();
        original.setDictType("old_status");
        var updated = new SysDictData();
        updated.setDictCode(42L);
        updated.setDictType("new_status");
        var bo = new SysDictDataBo();
        bo.setDictCode(42L);
        bo.setDictType("new_status");
        when(mapper.selectById(42L)).thenReturn(original);
        when(mapper.updateById(updated)).thenReturn(1);
        try (var spring = mockStatic(SpringUtil.class); var mappings = mockStatic(MapstructUtils.class);
             var cache = mockStatic(CacheUtils.class)) {
            mappings.when(() -> MapstructUtils.convert(bo, SysDictData.class)).thenReturn(updated);
            new SysDictDataServiceImpl(mapper).updateDictData(bo);
            cache.verify(() -> CacheUtils.evict(CacheNames.SYS_DICT, "old_status"));
        }
    }

    @Test
    void missingDictionaryEntryHasEmptyLabel() {
        var mapper = mock(SysDictDataMapper.class);
        LambdaCrudChainWrapper<SysDictData, SysDictDataVo> query = mock(LambdaCrudChainWrapper.class, RETURNS_SELF);
        when(mapper.lambda()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        assertEquals("", new SysDictDataServiceImpl(mapper).selectDictLabel("status", "missing"));
    }
}

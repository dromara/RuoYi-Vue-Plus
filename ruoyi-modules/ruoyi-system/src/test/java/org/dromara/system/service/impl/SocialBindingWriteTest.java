package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SysSocial;
import org.dromara.system.domain.bo.SysSocialBo;
import org.dromara.system.mapper.SysSocialMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialBindingWriteTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void writesReportAffectedRowsAndOnlySuccessfulInsertSetsId(int rows) {
        var mapper = mock(SysSocialMapper.class);
        var service = new SysSocialServiceImpl(mapper);
        var bo = new SysSocialBo();
        var entity = new SysSocial();
        entity.setId(16L);
        when(mapper.insert(entity)).thenReturn(rows);
        when(mapper.updateById(entity)).thenReturn(rows);
        when(mapper.deleteById(16L)).thenReturn(rows);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysSocial.class)).thenReturn(entity);
            assertEquals(rows > 0, service.insertByBo(bo));
            assertEquals(rows > 0 ? Long.valueOf(16L) : null, bo.getId());
            assertEquals(rows > 0, service.updateByBo(bo));
            assertEquals(rows > 0, service.deleteWithValidById(16L));
        }
    }
}

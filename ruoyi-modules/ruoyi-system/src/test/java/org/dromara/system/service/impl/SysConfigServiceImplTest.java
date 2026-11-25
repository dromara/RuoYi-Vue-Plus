package org.dromara.system.service.impl;

import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.system.domain.SysConfig;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.common.core.utils.MapstructUtils;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import static org.mockito.Mockito.*;

class SysConfigServiceImplTest {

    @ParameterizedTest
    @CsvSource({"0", "1"})
    void renameEvictsOldKeyOnlyAfterSuccessfulWrite(int updated) {
        var mapper = mock(SysConfigMapper.class);
        var original = new SysConfig();
        original.setConfigKey("old.key");
        var config = new SysConfig();
        config.setConfigId(1L);
        config.setConfigKey("new.key");
        config.setConfigValue("value");
        var bo = new SysConfigBo();
        bo.setConfigKey("new.key");
        bo.setConfigId(1L);
        when(mapper.selectById(1L)).thenReturn(original);
        when(mapper.updateById(config)).thenReturn(updated);
        var service = new SysConfigServiceImpl(mapper);
        try (var spring = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var caches = mockStatic(CacheUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysConfig.class)).thenReturn(config);
            if (updated == 0) {
                assertThrows(ServiceException.class, () -> service.updateConfig(bo));
                caches.verifyNoInteractions();
            } else {
                caches.when(() -> CacheUtils.evict(CacheNames.SYS_CONFIG, "old.key")).thenAnswer(call -> {
                    verify(mapper).updateById(config);
                    return null;
                });
                assertEquals("value", service.updateConfig(bo));
                caches.verify(() -> CacheUtils.evict(CacheNames.SYS_CONFIG, "old.key"));
            }
        }
    }

    @Test
    void rejectedBatchPreservesAllCachedValues() {
        var mapper = mock(SysConfigMapper.class);
        var ordinary = new SysConfig();
        ordinary.setConfigKey("theme");
        var builtIn = new SysConfig();
        builtIn.setConfigType("Y");
        var ids = List.of(1L, 2L);
        when(mapper.selectByIds(ids)).thenReturn(List.of(ordinary, builtIn));
        try (var caches = mockStatic(CacheUtils.class)) {
            assertThrows(ServiceException.class, () -> new SysConfigServiceImpl(mapper).deleteConfigByIds(ids));
            verify(mapper, never()).deleteByIds(ids);
            caches.verifyNoInteractions();
        }
    }

    @Test
    void deletedValueCannotBeReloadedFromDatabaseDuringInvalidation() {
        var mapper = mock(SysConfigMapper.class);
        var config = new SysConfig();
        config.setConfigKey("theme");
        var ids = List.of(1L);
        when(mapper.selectByIds(ids)).thenReturn(List.of(config));
        when(mapper.deleteByIds(ids)).thenReturn(1);
        try (var caches = mockStatic(CacheUtils.class)) {
            caches.when(() -> CacheUtils.evict(CacheNames.SYS_CONFIG, "theme")).thenAnswer(call -> {
                verify(mapper).deleteByIds(ids);
                return null;
            });
            new SysConfigServiceImpl(mapper).deleteConfigByIds(ids);
            caches.verify(() -> CacheUtils.evict(CacheNames.SYS_CONFIG, "theme"));
        }
    }

    @ParameterizedTest
    @CsvSource({"'', false", "true, true", "false, false"})
    void shouldDefaultMissingRegistrationSettingToDisabled(String value, boolean enabled) {
        var service = spy(new SysConfigServiceImpl(mock(SysConfigMapper.class)));
        doReturn(value).when(service).getConfigValue("sys.account.registerUser");

        assertEquals(enabled, service.selectRegisterEnabled());
    }
}

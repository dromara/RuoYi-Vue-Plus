package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.domain.SysConfig;
import org.dromara.system.mapper.SysConfigMapper;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.exception.ServiceException;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigReadContractTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void editingCurrentKeyUsesRecordIdWithoutEvictingIt(boolean duplicate) {
        var mapper = mock(SysConfigMapper.class, RETURNS_DEEP_STUBS);
        var chain = mapper.lambda();
        when(chain.eq(any(), eq("theme"))).thenReturn(chain);
        when(chain.neIfPresent(any(), eq(5L))).thenReturn(chain);
        when(chain.exists()).thenReturn(duplicate);
        var request = new SysConfigBo();
        request.setConfigId(5L);
        request.setConfigKey("theme");
        var service = new SysConfigServiceImpl(mapper);
        assertEquals(!duplicate, service.checkConfigKeyUnique(request));
        verify(chain).neIfPresent(any(), eq(5L));
        if (!duplicate) {
            var config = new SysConfig();
            config.setConfigId(5L);
            config.setConfigKey("theme");
            config.setConfigValue("light");
            when(mapper.selectById(5L)).thenReturn(config);
            when(mapper.updateById(config)).thenReturn(1);
            try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
                 var cache = mockStatic(org.dromara.common.redis.utils.CacheUtils.class)) {
                mapping.when(() -> MapstructUtils.convert(request, SysConfig.class)).thenReturn(config);
                assertEquals("light", service.updateConfig(request));
                cache.verifyNoInteractions();
            }
        }
    }

    @Test
    void cacheRefreshTargetsSystemConfigNamespace() {
        try (var cache = mockStatic(org.dromara.common.redis.utils.CacheUtils.class)) {
            new SysConfigServiceImpl(mock()).resetConfigCache();
            cache.verify(() -> org.dromara.common.redis.utils.CacheUtils.clear(org.dromara.common.core.constant.CacheNames.SYS_CONFIG));
        }
    }

    @Test
    void typedConfigurationReadsUseConfiguredKeyAndTargetType() {
        var service = spy(new SysConfigServiceImpl(mock()));
        doReturn("{\"name\":\"portal\"}").when(service).getConfigValue("object");
        doReturn("[{\"name\":\"portal\"}]").when(service).getConfigValue("array");
        doReturn(" ").when(service).getConfigValue("blank");
        doReturn("invalid").when(service).getConfigValue("invalid");
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            assertEquals("portal", service.getConfigMap("object").getStr("name"));
            assertEquals("portal", service.getConfigArrayMap("array").getFirst().getStr("name"));
            assertEquals(new Setting("portal"), service.getConfigObject("object", Setting.class));
            assertEquals(List.of(new Setting("portal")), service.getConfigArray("array", Setting.class));
            assertNull(service.getConfigMap("blank"));
            assertThrows(RuntimeException.class, () -> service.getConfigArray("invalid", Setting.class));
        }
    }

    record Setting(String name) {}

    @ParameterizedTest
    @ValueSource(ints = {0,1})
    void keyOnlyUpdateRequiresMatchingRecord(int rows) {
        var mapper = mock(SysConfigMapper.class, RETURNS_DEEP_STUBS);
        var query = mapper.lambda();
        when(query.eq(any(), eq("theme"))).thenReturn(query);
        var bo = new SysConfigBo();
        var config = new SysConfig();
        config.setConfigKey("theme");
        config.setConfigValue("dark");
        when(query.updateCount(config)).thenReturn(rows);
        var service = new SysConfigServiceImpl(mapper);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysConfig.class)).thenReturn(config);
            if (rows == 0) {
                assertThrows(ServiceException.class, () -> service.updateConfig(bo));
            } else {
                assertEquals("dark", service.updateConfig(bo));
            }
            verify(query).eq(any(), eq("theme"));
            verify(mapper, never()).updateById(any(SysConfig.class));
        }
    }
}

package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.domain.SysOssConfig;
import org.dromara.system.mapper.SysOssConfigMapper;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssConfigurationStartupTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void duplicateConfigKeyRejectedAndOwnKeyMayBeUpdated(boolean ownKey) {
        var mapper = mock(SysOssConfigMapper.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ossConfig"), SysOssConfig.class);
        when(mapper.currentModelClass()).thenReturn(SysOssConfig.class);
        when(mapper.lambda()).thenAnswer(call -> new org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper<
            SysOssConfig, org.dromara.system.domain.vo.SysOssConfigVo>(mapper));
        var existing = new SysOssConfig();
        existing.setOssConfigId(81L);
        when(mapper.selectOne(any())).thenReturn(existing);
        var updated = new SysOssConfig();
        updated.setOssConfigId(ownKey ? 81L : 82L);
        updated.setConfigKey("archive");
        var bo = new org.dromara.system.domain.bo.SysOssConfigBo();
        try (var beans = mockStatic(SpringUtil.class);
             var mapping = mockStatic(org.dromara.common.core.utils.MapstructUtils.class);
             var spring = mockStatic(org.dromara.common.core.utils.SpringUtils.class)) {
            mapping.when(() -> org.dromara.common.core.utils.MapstructUtils.convert(bo, SysOssConfig.class))
                .thenReturn(updated);
            var service = new SysOssConfigServiceImpl(mapper);
            if (ownKey) {
                assertFalse(service.updateByBo(bo));
                verify(mapper).update(eq(updated), any());
                spring.verifyNoInteractions();
            } else {
                assertThrows(org.dromara.common.core.exception.ServiceException.class, () -> service.updateByBo(bo));
                verify(mapper, never()).update(eq(updated), any());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1})
    void deletePublishesRemovalForExistingConfigurationOnlyAfterWrite(int rows) {
        var mapper = mock(SysOssConfigMapper.class);
        var config = new SysOssConfig();
        config.setConfigKey("archive");
        when(mapper.selectById(900L)).thenReturn(config);
        var ids = List.of(900L, 901L);
        when(mapper.deleteByIds(ids)).thenReturn(rows);
        var context = mock(org.springframework.context.ApplicationContext.class);
        try (var spring = mockStatic(org.dromara.common.core.utils.SpringUtils.class)) {
            spring.when(org.dromara.common.core.utils.SpringUtils::context).thenReturn(context);
            assertEquals(rows == 1, new SysOssConfigServiceImpl(mapper).deleteWithValidByIds(ids, true));
            if (rows == 1) {
                verify(context).publishEvent(org.dromara.system.event.OssConfigChangeEvent.remove("archive"));
            }
            verifyNoMoreInteractions(context);
        }
    }

    @Test
    void cannotDeleteBatchContainingBuiltInConfiguration() {
        var mapper = mock(SysOssConfigMapper.class);
        var ids = new java.util.ArrayList<Long>(OssConstant.SYSTEM_DATA_IDS);
        ids.add(891L);
        assertThrows(org.dromara.common.core.exception.ServiceException.class,
            () -> new SysOssConfigServiceImpl(mapper).deleteWithValidByIds(ids, true));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cachesAllStorageConfigurationsAndSelectsEnabledDefault(boolean enabled) {
        var mapper = mock(SysOssConfigMapper.class);
        var primary = new SysOssConfig();
        primary.setConfigKey("primary");
        primary.setStatus(enabled ? "Y" : "N");
        var secondary = new SysOssConfig();
        secondary.setConfigKey("secondary");
        secondary.setStatus("N");
        when(mapper.selectList()).thenReturn(List.of(primary, secondary));
        var json = JsonMapper.builder().build();
        try (var beans = mockStatic(SpringUtil.class); var cache = mockStatic(CacheUtils.class);
             var redis = mockStatic(RedisUtils.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(json);
            new SysOssConfigServiceImpl(mapper).init();
            cache.verify(() -> CacheUtils.put(eq(CacheNames.SYS_OSS_CONFIG), eq("primary"),
                argThat((String value) -> "primary".equals(json.readTree(value).path("configKey").asString()))));
            cache.verify(() -> CacheUtils.put(eq(CacheNames.SYS_OSS_CONFIG), eq("secondary"),
                argThat((String value) -> "secondary".equals(json.readTree(value).path("configKey").asString()))));
            redis.verify(() -> RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, "primary"),
                times(enabled ? 1 : 0));
            redis.verifyNoMoreInteractions();
        }
    }
}

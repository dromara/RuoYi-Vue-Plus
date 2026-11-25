package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.event.OssConfigChangeEvent;
import org.dromara.system.listener.OssConfigChangeListener;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RedissonClient;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.domain.SysOssConfig;
import org.dromara.system.domain.bo.SysOssConfigBo;
import org.dromara.system.mapper.SysOssConfigMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SysOssConfigServiceImplTest {

    @Test
    void enablingConfigurationPublishesItsKeyAsTheNewDefault() {
        var mapper = mock(SysOssConfigMapper.class, RETURNS_DEEP_STUBS);
        var chain = mapper.lambda();
        when(chain.set(any(), eq("N"))).thenReturn(chain);
        when(chain.updateCount()).thenReturn(2);
        var target = new SysOssConfig();
        target.setConfigKey("archive");
        target.setStatus("Y");
        when(mapper.updateById(target)).thenReturn(1);
        var bo = new SysOssConfigBo();
        var context = mock(ApplicationContext.class);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var spring = mockStatic(SpringUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysOssConfig.class)).thenReturn(target);
            spring.when(SpringUtils::context).thenReturn(context);
            org.junit.jupiter.api.Assertions.assertEquals(3, new SysOssConfigServiceImpl(mapper).updateOssConfigStatus(bo));
            verify(context).publishEvent(OssConfigChangeEvent.useDefault("archive"));
        }
    }

    @Test
    void disablingOneConfigOnlyClearsItsOwnDefaultSelection() {
        var mapper = mock(SysOssConfigMapper.class, RETURNS_DEEP_STUBS);
        var chain = mapper.lambda();
        when(chain.set(any(), eq("N"))).thenReturn(chain);
        var target = new SysOssConfig();
        target.setOssConfigId(17L);
        target.setConfigKey("archive");
        target.setStatus("N");
        var bo = new SysOssConfigBo();
        bo.setOssConfigId(17L);
        bo.setConfigKey("archive");
        bo.setStatus("N");
        when(mapper.updateById(target)).thenReturn(1);
        var context = mock(ApplicationContext.class);
        var client = mock(RedissonClient.class, RETURNS_DEEP_STUBS);
        var bucket = client.getBucket(OssConstant.DEFAULT_CONFIG_KEY);
        try (var beans = mockStatic(SpringUtil.class); var spring = mockStatic(SpringUtils.class);
             var mapping = mockStatic(MapstructUtils.class); var redis = mockStatic(RedisUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(context);
            mapping.when(() -> MapstructUtils.convert(bo, SysOssConfig.class)).thenReturn(target);
            redis.when(RedisUtils::getClient).thenReturn(client);
            new SysOssConfigServiceImpl(mapper).updateOssConfigStatus(bo);
            var event = ArgumentCaptor.forClass(OssConfigChangeEvent.class);
            verify(context).publishEvent(event.capture());
            new OssConfigChangeListener().refreshOssConfig(event.getValue());
            org.junit.jupiter.api.Assertions.assertAll(
                () -> verify(chain, never()).updateCount(),
                () -> verify(bucket).compareAndSet("archive", null),
                () -> redis.verify(() -> RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, "archive"), never())
            );
        }
    }

    @Test
    void missingTargetCannotBecomeDefaultWhenOtherRowsWereUpdated() {
        var mapper = mock(SysOssConfigMapper.class, RETURNS_DEEP_STUBS);
        var chain = mapper.lambda();
        when(chain.set(any(), eq("N"))).thenReturn(chain);
        when(chain.updateCount()).thenReturn(1);
        var target = new SysOssConfig();
        target.setOssConfigId(17L);
        target.setStatus("Y");
        target.setConfigKey("missing");
        var bo = new SysOssConfigBo();
        bo.setOssConfigId(17L);
        bo.setStatus("Y");
        bo.setConfigKey("missing");
        var context = mock(ApplicationContext.class);
        try (var beans = mockStatic(SpringUtil.class); var spring = mockStatic(SpringUtils.class);
             var mapping = mockStatic(MapstructUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(context);
            mapping.when(() -> MapstructUtils.convert(bo, SysOssConfig.class)).thenReturn(target);
            assertThrows(ServiceException.class, () -> new SysOssConfigServiceImpl(mapper).updateOssConfigStatus(bo));
            verifyNoInteractions(context);
        }
    }
}

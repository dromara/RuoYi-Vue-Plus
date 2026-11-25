package org.dromara.system.service.impl;

import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.log.event.OperLogEvent;
import org.dromara.system.domain.bo.SysOperLogBo;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationAuditEventTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void pagedAuditUsesRequestedSortOrLatestFirst(boolean explicit) {
        var mapper = mock(org.dromara.system.mapper.SysOperLogMapper.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "audit"), org.dromara.system.domain.SysOperLog.class);
        var page = new org.dromara.common.mybatis.core.page.PageQuery(5, 1);
        if (explicit) {
            page.setOrderByColumn("operTime");
            page.setIsAsc("asc");
        }
        var data = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.dromara.system.domain.vo.SysOperLogVo>();
        when(mapper.selectVoPage(any(), any())).thenReturn(data);
        new SysOperLogServiceImpl(mapper).selectPageOperLogList(new SysOperLogBo(), page);
        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.dromara.system.domain.SysOperLog>> query =
            org.mockito.ArgumentCaptor.captor();
        verify(mapper).selectVoPage(any(), query.capture());
        assertEquals(!explicit, query.getValue().getSqlSegment().contains("oper_id DESC"));
        if (explicit) {
            assertEquals("oper_time", page.build().orders().getFirst().getColumn());
        }
    }

    @Test
    void clearingOperationalHistoryDoesNotRequireIndividualIds() {
        var mapper = mock(org.dromara.system.mapper.SysOperLogMapper.class, RETURNS_DEEP_STUBS);
        var query = mapper.lambda();
        var service = new SysOperLogServiceImpl(mapper);
        var row = new org.dromara.system.domain.vo.SysOperLogVo();
        when(mapper.selectVoById(41L)).thenReturn(row);
        assertSame(row, service.selectOperLogById(41L));
        service.cleanOperLog();
        verify(query).delete();
    }

    @Test
    void enrichesMappedEventWithLocationBeforePersistingIt() {
        var event = new OperLogEvent();
        var bo = new SysOperLogBo();
        bo.setOperIp("192.0.2.7");
        var recorder = spy(new SysOperLogServiceImpl(mock()));
        doNothing().when(recorder).insertOperlog(any());
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class);
             var addresses = mockStatic(AddressUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(event, SysOperLogBo.class)).thenReturn(bo);
            addresses.when(() -> AddressUtils.getRealAddressByIP("192.0.2.7")).thenReturn("内网");
            recorder.recordOper(event);
            verify(recorder).insertOperlog(bo);
            assertEquals("内网", bo.getOperLocation());
        }
    }
}

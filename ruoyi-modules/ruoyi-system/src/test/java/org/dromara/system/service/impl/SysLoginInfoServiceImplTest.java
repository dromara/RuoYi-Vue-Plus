package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.system.domain.SysLoginInfo;
import org.dromara.system.domain.bo.SysLoginInfoBo;
import org.dromara.system.domain.vo.SysLoginInfoVo;
import org.dromara.system.mapper.SysLoginInfoMapper;
import org.dromara.system.service.ISysClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysLoginInfoServiceImplTest {

    private final SysLoginInfoMapper mapper = mock(SysLoginInfoMapper.class);
    private final SysLoginInfoServiceImpl service = new SysLoginInfoServiceImpl(mapper, mock(ISysClientService.class));

    @ParameterizedTest
    @NullAndEmptySource
    void recordsLoginWithoutUserAgent(String agent) {
        var event = new LoginInfoEvent();
        event.setUsername("alice");
        event.setUserAgent(agent);
        var recorder = spy(service);
        doNothing().when(recorder).insertLoginInfo(any());
        recorder.recordLoginInfo(event);
        verify(recorder).insertLoginInfo(argThat(log -> "alice".equals(log.getUserName())));
    }

    @Test
    void loginHistoryFiltersByClientAndDevice() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "login"), SysLoginInfo.class);
        when(mapper.currentModelClass()).thenReturn(SysLoginInfo.class);
        var query = new LambdaCrudChainWrapper<SysLoginInfo, SysLoginInfoVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var bo = new SysLoginInfoBo();
        bo.setClientKey("mobile-app");
        bo.setDeviceType("android");
        service.selectLoginInfoList(bo);
        String sql = query.getSqlSegment();
        assertAll(
            () -> assertTrue(sql.contains("client_key ="), sql),
            () -> assertTrue(sql.contains("device_type ="), sql),
            () -> assertTrue(query.getParamNameValuePairs().containsValue("mobile-app")),
            () -> assertTrue(query.getParamNameValuePairs().containsValue("android"))
        );
    }
}

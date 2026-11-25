package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.mapper.SysClientMapper;
import org.dromara.system.domain.SysClient;
import org.dromara.system.domain.bo.SysClientBo;
import org.dromara.system.domain.vo.SysClientVo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientRulePersistenceTest {

    @Test
    void unknownClientIdProducesNoClient() {
        var clients = mock(SysClientMapper.class, RETURNS_DEEP_STUBS);
        var query = clients.lambda();
        when(query.eq(any(), eq("absent"))).thenReturn(query);
        when(query.voOne()).thenReturn(null);
        assertNull(new SysClientServiceImpl(clients).queryByClientId("absent"));
        verify(query).eq(any(), eq("absent"));
    }

    @Test
    void bothListEndpointsExpandPersistedRuleFields() {
        var stored = new SysClientVo();
        stored.setAccessPath("sales;*");
        stored.setIpWhitelist("127.0.0.1");
        var pageRow = new SysClientVo();
        pageRow.setAccessPath("reports");
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysClientVo>(1, 5);
        page.setRecords(List.of(pageRow));
        page.setTotal(12L);
        when(mapper.selectVoPage(any(), any())).thenReturn(page);
        when(mapper.selectVoList(any())).thenReturn(List.of(stored));
        var filter = new SysClientBo();
        assertEquals(List.of("/sales", "/**"), service.queryList(filter).getFirst().getAccessPathList());
        assertEquals(List.of("127.0.0.1"), stored.getIpWhitelistList());
        var result = service.queryPageList(filter, new org.dromara.common.mybatis.core.page.PageQuery(5, 1));
        assertEquals(12L, result.getTotal());
        assertEquals(List.of("/reports"), result.getRows().iterator().next().getAccessPathList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1})
    void statusAndRemovalReportAffectedRows(int rows) {
        var chained = mock(SysClientMapper.class, RETURNS_DEEP_STUBS);
        var query = chained.lambda();
        when(query.set(any(), eq("1"))).thenReturn(query);
        when(query.eq(any(), eq("desktop"))).thenReturn(query);
        when(query.updateCount()).thenReturn(rows);
        when(chained.deleteByIds(List.of(8L))).thenReturn(rows);
        var clients = new SysClientServiceImpl(chained);
        assertEquals(rows, clients.updateClientStatus("desktop", "1"));
        assertEquals(rows > 0, clients.deleteWithValidByIds(List.of(8L), true));
        when(chained.selectVoById(404L)).thenReturn(null);
        assertNull(clients.queryById(404L));
    }

    private final SysClientMapper mapper = mock();
    private final SysClientServiceImpl service = new SysClientServiceImpl(mapper);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void normalizesRulesBeforeWritingAndRebuildsListsOnRead(boolean create) {
        var request = new SysClientBo();
        request.setClientKey("desktop");
        request.setClientSecret("test");
        request.setGrantTypeList(List.of("password", "code"));
        request.setAccessPath(" orders/** ; * ");
        request.setAccessPathList(List.of("/ignored"));
        request.setIpWhitelistList(List.of("127.0.0.1", "10.0.0.1"));
        var entity = new SysClient();
        entity.setId(81L);
        when(mapper.insert(entity)).thenReturn(1);
        when(mapper.updateById(entity)).thenReturn(1);
        try (var beans = mockStatic(SpringUtil.class); var mappings = mockStatic(MapstructUtils.class)) {
            mappings.when(() -> MapstructUtils.convert(request, SysClient.class)).thenReturn(entity);
            assertTrue(create ? service.insertByBo(request) : service.updateByBo(request));
        }
        assertEquals("/orders/**,/**", entity.getAccessPath());
        assertEquals("127.0.0.1,10.0.0.1", entity.getIpWhitelist());
        assertEquals("password,code", entity.getGrantType());
        if (create) {
            assertEquals(81L, request.getId());
            assertEquals(cn.hutool.crypto.SecureUtil.md5("desktoptest"), entity.getClientId());
        }
        var stored = new SysClientVo();
        stored.setGrantType(entity.getGrantType());
        stored.setAccessPath(entity.getAccessPath());
        stored.setIpWhitelist(entity.getIpWhitelist());
        when(mapper.selectVoById(81L)).thenReturn(stored);
        var read = service.queryById(81L);
        assertEquals(List.of("/orders/**", "/**"), read.getAccessPathList());
        assertEquals(List.of("127.0.0.1", "10.0.0.1"), read.getIpWhitelistList());
        assertEquals(request.getGrantTypeList(), read.getGrantTypeList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void omittedRulesAreDistinctFromExplicitlyClearedRules(boolean clear) {
        var request = new SysClientBo();
        if (clear) {
            request.setAccessPathList(List.of());
            request.setIpWhitelist("");
        }
        var entity = new SysClient();
        try (var beans = mockStatic(SpringUtil.class); var mappings = mockStatic(MapstructUtils.class)) {
            mappings.when(() -> MapstructUtils.convert(request, SysClient.class)).thenReturn(entity);
            assertFalse(service.updateByBo(request));
            verify(mapper).updateById(entity);
            assertEquals(clear ? "" : null, entity.getAccessPath());
            assertEquals(clear ? "" : null, entity.getIpWhitelist());
        }
    }
}

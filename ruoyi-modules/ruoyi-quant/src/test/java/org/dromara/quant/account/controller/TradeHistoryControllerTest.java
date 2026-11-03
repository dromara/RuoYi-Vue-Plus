package org.dromara.quant.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.account.domain.vo.TradeHistoryFill;
import org.dromara.quant.account.mapper.TradeHistoryMapper;
import org.dromara.quant.account.service.TradeHistoryService;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.OrderLedgerService;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TradeHistoryControllerTest {
    @org.junit.jupiter.api.BeforeAll
    static void initializeEntityMetadata() {
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "history-test");
        assistant.setCurrentNamespace("history-test");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, QuantCredential.class);
    }

    private QuantCredential credential() {
        var result = new QuantCredential();
        result.setId(10L);
        result.setUserId(1L);
        result.setAccountName("模拟1");
        return result;
    }

    private TradeHistoryService service(QuantCredentialMapper credentials, TradeHistoryMapper history) {
        return new TradeHistoryService(credentials, history, mock(ExchangeGateway.class), mock(OrderLedgerService.class));
    }

    private TradeHistoryFill fill(long id, String symbol) {
        var f = new TradeHistoryFill();
        f.setId(id);
        f.setCredentialId(10L);
        f.setSymbol(symbol);
        f.setSide("BUY");
        f.setPositionSide("LONG");
        f.setOperationType("OPEN");
        f.setQuantity(BigDecimal.ONE);
        f.setPrice(BigDecimal.TEN);
        f.setCommission(BigDecimal.ZERO);
        f.setRealizedPnl(BigDecimal.ZERO);
        f.setTradeTime(LocalDateTime.of(2026, 10, 9, 1, 0).plusSeconds(id));
        return f;
    }

    @Test
    void rejectsOtherAccountsBeforeReadingFills() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        when(credentials.selectList(any())).thenReturn(List.of(credential()));
        var controller = new TradeHistoryController(service(credentials, history));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            assertThrows(ServiceException.class, () -> controller.page(new PageQuery(), 11L, null, null, null));
            verifyNoInteractions(history);
        }
    }

    @Test
    void paginatesCyclesAndUsesUtcInstantsWithBeijingDateFilters() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        when(credentials.selectList(any())).thenReturn(List.of(credential()));
        when(history.selectFills(eq(List.of(10L)), isNull())).thenReturn(List.of(fill(1, "BTCUSDT"), fill(2, "PUMPUSDT")));
        var query = new PageQuery();
        query.setPageNum(2);
        query.setPageSize(1);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            var result = new TradeHistoryController(service(credentials, history)).page(query, 10L, null, "2026-10-09", "2026-10-09").getData();
            assertEquals(2, result.getTotal());
            var rows = (List<?>) result.getRows();
            assertEquals(1, rows.size());
            assertEquals("2026-10-09T01:00:01Z", ((Map<?, ?>) rows.getFirst()).get("openedAt"));
        }
    }

    @Test
    void returnsEmptyPageWithoutQueryingWhenNoAccountIsAccessible() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        when(credentials.selectList(any())).thenReturn(List.of());
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            var result = new TradeHistoryController(service(credentials, history)).page(new PageQuery(), null, null, null, null).getData();
            assertEquals(0, result.getTotal());
            verifyNoInteractions(history);
        }
    }

    @Test
    void rejectsInvalidDatesAndAmbiguousDuplicateJoins() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        when(credentials.selectList(any())).thenReturn(List.of(credential()));
        when(history.selectFills(any(), any())).thenReturn(List.of(fill(1, "BTCUSDT"), fill(1, "BTCUSDT")));
        var controller = new TradeHistoryController(service(credentials, history));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            assertThrows(ServiceException.class, () -> controller.page(new PageQuery(), 10L, null, "wrong", null));
            assertThrows(ServiceException.class, () -> controller.page(new PageQuery(), 10L, null, "2026-10-10", "2026-10-09"));
            assertThrows(ServiceException.class, () -> controller.page(new PageQuery(), 10L, "BTCUSDT", null, null));
        }
    }

    @Test
    void refusesTruncatedHistoryAndNormalizesSymbol() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        when(credentials.selectList(any())).thenReturn(List.of(credential()));
        when(history.selectFills(eq(List.of(10L)), eq("BTCUSDT"))).thenReturn(Collections.nCopies(20001, fill(1, "BTCUSDT")));
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            assertThrows(ServiceException.class, () -> new TradeHistoryController(service(credentials, history)).page(new PageQuery(), 10L, " btcusdt ", null,
                null));
        }
    }

    @Test
    void exposesOnlyReadPermissionAndBindsDynamicSqlParameters() throws Exception {
        var permission =
            TradeHistoryController.class.getMethod("page", PageQuery.class, Long.class, String.class, String.class, String.class).getAnnotation(SaCheckPermission.class);
        assertArrayEquals(new String[]{"quant:trade:list"}, permission.value());
        var annotation = TradeHistoryMapper.class.getMethod("selectFills", List.class, String.class).getAnnotation(Select.class);
        var source = new XMLLanguageDriver().createSqlSource(new Configuration(), annotation.value()[0], Map.class);
        var sql = source.getBoundSql(Map.of("ids", List.of(10L, 11L), "symbol", "BTCUSDT"));
        assertTrue(sql.getSql().contains("LIMIT 20001"));
        assertFalse(sql.getSql().contains("BTCUSDT"));
        assertEquals(3, sql.getParameterMappings().size());
    }

    @Test
    void keepsExchangeRecoveryOutsideDatabaseTransactionAndStablePageContract() throws Exception {
        Class<?> service = Class.forName("org.dromara.quant.account.service.TradeHistoryService");
        assertNotNull(service.getAnnotation(org.springframework.stereotype.Service.class));
        var transaction =
            service.getMethod("page", PageQuery.class, Long.class, String.class, String.class, String.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertNull(transaction, "Binance REST recovery must not run inside a database transaction");
        var method = TradeHistoryController.class.getMethod("page", PageQuery.class, Long.class, String.class, String.class, String.class);
        assertTrue(method.getGenericReturnType().getTypeName().contains("TradeHistoryPageVo"));
    }

    @Test
    void selectsOnlyPublicCredentialFieldsAndEnforcesOwnershipInSql() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        when(credentials.selectList(any())).thenReturn(List.of());
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(7L);
            service(credentials, history).page(new PageQuery(), null, null, null, null);
        }
        var captor = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(credentials).selectList(captor.capture());
        var wrapper = captor.getValue();
        assertEquals("id,user_id,account_name", wrapper.getSqlSelect());
        assertTrue(wrapper.getSqlSegment().contains("user_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(7L));
    }

    @Test
    void rejectsMissingLoginBeforeQueryingAnyAccount() {
        var credentials = mock(QuantCredentialMapper.class);
        var history = mock(TradeHistoryMapper.class);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(null);
            assertThrows(ServiceException.class, () -> service(credentials, history).page(new PageQuery(), null, null, null, null));
        }
        verifyNoInteractions(credentials, history);
    }

    @Test
    void preservesJsonShapeAndStringPrecisionForThePage() throws Exception {
        var page = new org.dromara.quant.account.domain.vo.TradeHistoryPageVo(List.of(Map.of("id", "2105976685374525441", "entryPrice", "0.00000000123456789")), 1, 2);
        var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(page);
        assertEquals(1, json.get("total").asLong());
        assertEquals(2, json.get("incompleteCount").asInt());
        assertTrue(json.get("rows").get(0).get("id").isTextual());
        assertEquals("0.00000000123456789", json.get("rows").get(0).get("entryPrice").asText());
    }

}

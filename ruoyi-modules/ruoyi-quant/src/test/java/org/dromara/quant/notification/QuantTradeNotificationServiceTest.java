package org.dromara.quant.notification;

import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.notification.domain.QuantNotification;
import org.dromara.quant.notification.mapper.QuantNotificationMapper;
import org.dromara.quant.risk.mapper.QuantRiskConfigMapper;
import org.dromara.system.api.MessageService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class QuantTradeNotificationServiceTest {
    @Test
    void tradingCallerQueuesEmailAndBackgroundWorkerClaimsIt() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var properties = new QuantProperties();
        properties.getNotification().setDefaultEmails("test@example.invalid");
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, properties,
            mock(MessageService.class), mock(JdbcTemplate.class), mock(ExchangeGateway.class));
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId((long) invocation.getArgument(0, QuantNotification.class).getChannel().hashCode());
            return 1;
        });

        service.notifyOpen(null, null, "BTCUSDT", "LONG", BigDecimal.ONE, BigDecimal.TEN, 5, BigDecimal.ONE, "test");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper, times(2)).insert(rows.capture());
        var mail = rows.getAllValues().stream().filter(row -> "EMAIL".equals(row.getChannel())).findFirst().orElseThrow();
        assertEquals("PENDING", mail.getStatus());
        assertNotNull(mail.getNextRetryAt());
        verify(mapper).claim(eq((long) "SYSTEM".hashCode()), any(), any());

        mail.setId((long) "EMAIL".hashCode());
        when(mapper.selectList(any())).thenReturn(List.of(mail));
        // 未抢占成功时不发送邮件；验证调度器仍消费持久化 Outbox。
        service.retryPendingNotifications();
        verify(mapper).claim(eq((long) "EMAIL".hashCode()), any(), any());
    }

    @Test
    void failedSystemMessageIsPersistedForRetryInsteadOfMarkedSent() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-retry-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var jdbc = mock(JdbcTemplate.class);
        var messageService = mock(MessageService.class);
        var properties = new QuantProperties();
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, properties, messageService, jdbc, mock(ExchangeGateway.class));
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(42L);
            return 1;
        });
        when(mapper.claim(eq(42L), any(), any())).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any())).thenReturn(7L);
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<Long>>any())).thenReturn(List.of(9L));
        doThrow(new IllegalStateException("temporary message bus failure"))
            .when(messageService).publishMessage(anyList(), any());

        assertDoesNotThrow(() -> service.notifyClosePaper(null, null, "BTCUSDT", "LONG", BigDecimal.ONE, BigDecimal.TEN,
            BigDecimal.valueOf(11), BigDecimal.ONE, "USDT", BigDecimal.valueOf(101), BigDecimal.valueOf(90), BigDecimal.ZERO, "STOP", "test"));

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        assertEquals("SYSTEM", rows.getValue().getChannel());
        var updated = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).updateById(updated.capture());
        assertEquals("RETRY", updated.getValue().getStatus());
        assertEquals(1, updated.getValue().getRetryCount());
        assertNotNull(updated.getValue().getNextRetryAt());
    }
    @Test
    void closeNotificationUsesConfirmedExchangeFillsAndPostCloseAccountSnapshot() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-fill-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var gateway = mock(ExchangeGateway.class);
        var properties = new QuantProperties();
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, properties,
            mock(MessageService.class), mock(JdbcTemplate.class), gateway);
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(42L);
            return 1;
        });
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setAccountName("test-account");
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("TESTNET");
        when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(99L), isNull())).thenReturn(
            new ExchangeGateway.OrderSnapshot("BTCUSDT", 99L, "client-close", "SELL", "MARKET", "FILLED",
                BigDecimal.ZERO, BigDecimal.valueOf(2), BigDecimal.valueOf(2), Instant.now(), BigDecimal.valueOf(102)));
        when(gateway.queryTradesByOrder(any(), eq("BTCUSDT"), eq(99L), any(), eq(1000L))).thenReturn(List.of(
            new ExchangeGateway.TradeSnapshot("BTCUSDT", 1L, 99L, "SELL", BigDecimal.ONE, BigDecimal.valueOf(101),
                new BigDecimal("0.01"), BigDecimal.ONE, Instant.now(), "USDT", "LONG"),
            new ExchangeGateway.TradeSnapshot("BTCUSDT", 2L, 99L, "SELL", BigDecimal.ONE, BigDecimal.valueOf(103),
                new BigDecimal("0.02"), BigDecimal.valueOf(2), Instant.now(), "USDT", "LONG")));
        when(gateway.queryAccountFresh(any())).thenReturn(
            new ExchangeGateway.AccountSnapshot(new BigDecimal("1000"), new BigDecimal("900"), new BigDecimal("1003"), new BigDecimal("3")));

        service.notifyCloseByOrder(null, credential, "BTCUSDT", "LONG", BigDecimal.valueOf(2), BigDecimal.valueOf(100),
            99L, "USDT", "MANUAL", "test close");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        String content = rows.getValue().getContent();
        assertTrue(content.contains("实际平仓均价: 102"));
        assertTrue(content.contains("交易所已实现盈亏（未扣手续费）: 3 USDT"));
        assertTrue(content.contains("手续费: 0.03 USDT"));
        assertTrue(content.contains("钱包余额: 1000 USDT"));
        assertTrue(content.contains("账户权益: 1003 USDT"));
        assertTrue(content.contains("可用余额: 900 USDT"));
    }

}

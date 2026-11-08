package org.dromara.quant.notification;

import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
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
}

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
import org.springframework.core.task.TaskExecutor;

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
            mock(MessageService.class), mock(JdbcTemplate.class), mock(ExchangeGateway.class), mock(TaskExecutor.class));
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
    void liveOpenNotificationQueuesExchangeFillTimeVerificationWithoutBlockingCaller() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-open-pending-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var gateway = mock(ExchangeGateway.class);
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, new QuantProperties(),
            mock(MessageService.class), mock(JdbcTemplate.class), gateway, mock(TaskExecutor.class));
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(46L);
            return 1;
        });
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("TESTNET");

        service.notifyOpen(null, credential, "BTCUSDT", "LONG", BigDecimal.ONE, BigDecimal.TEN, 5,
            new BigDecimal("0.1"), Instant.now(), 102L, "test open");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        assertEquals("FACT_PENDING", rows.getValue().getChannel());
        assertEquals("OPEN:102", rows.getValue().getRecipient());
        assertTrue(rows.getValue().getContent().contains("\"taskType\":\"OPEN\""));
        verifyNoInteractions(gateway);
    }

    @Test
    void pendingOpenNotificationUsesEarliestConfirmedExchangeFillTime() throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-open-fill-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var gateway = mock(ExchangeGateway.class);
        var jdbc = mock(JdbcTemplate.class);
        var messageService = mock(MessageService.class);
        var properties = new QuantProperties();
        properties.getNotification().setDefaultEmails("test@example.invalid");
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, properties,
            messageService, jdbc, gateway, mock(TaskExecutor.class));
        var credentialMapper = mock(org.dromara.quant.credential.mapper.QuantCredentialMapper.class);
        var credentialMapperField = QuantTradeNotificationService.class.getDeclaredField("credentialMapper");
        credentialMapperField.setAccessible(true);
        credentialMapperField.set(service, credentialMapper);
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setAccountName("test-account");
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("TESTNET");
        when(credentialMapper.selectById(10L)).thenReturn(credential);

        Instant firstFill = Instant.parse("2026-11-10T02:00:00Z");
        Instant secondFill = Instant.parse("2026-11-10T02:00:03Z");
        when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(102L), isNull())).thenReturn(
            new ExchangeGateway.OrderSnapshot("BTCUSDT", 102L, "client-open", "BUY", "MARKET", "FILLED",
                BigDecimal.ZERO, BigDecimal.valueOf(2), BigDecimal.valueOf(2), secondFill, BigDecimal.valueOf(100)));
        when(gateway.queryTradesByOrder(any(), eq("BTCUSDT"), eq(102L), any(), eq(1000L))).thenReturn(List.of(
            new ExchangeGateway.TradeSnapshot("BTCUSDT", 1L, 102L, "BUY", BigDecimal.ONE, BigDecimal.valueOf(99),
                BigDecimal.ZERO, BigDecimal.ZERO, secondFill, "USDT", "LONG"),
            new ExchangeGateway.TradeSnapshot("BTCUSDT", 2L, 102L, "BUY", BigDecimal.ONE, BigDecimal.valueOf(100),
                BigDecimal.ZERO, BigDecimal.ZERO, firstFill, "USDT", "LONG")));
        var task = new QuantNotification();
        task.setId(47L);
        task.setCreatedAt(java.time.LocalDateTime.now());
        task.setContent(org.dromara.quant.util.QuantJsonMapper.writeMap(java.util.Map.of(
            "taskType", "OPEN", "credentialId", "10", "strategyName", "test-strategy", "symbol", "BTCUSDT",
            "direction", "LONG", "quantity", "2", "price", "99.5", "leverage", 5, "openRatio", "0.1",
            "orderId", "102", "reason", "test open")));
        when(mapper.selectList(any())).thenReturn(List.of(task));
        when(mapper.claim(eq(47L), any(), any())).thenReturn(1);
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(48L);
            return 1;
        });
        when(mapper.claim(eq(48L), any(), any())).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any())).thenReturn(7L);
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<Long>>any())).thenReturn(List.of(9L));

        service.retryPendingCloseFacts();

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper, atLeastOnce()).insert(rows.capture());
        var systemNotice = rows.getAllValues().stream().filter(row -> "SYSTEM".equals(row.getChannel())).findFirst().orElseThrow();
        assertTrue(systemNotice.getContent().contains("开仓时间: 2026-11-10 10:00:00"));
        verify(mapper).updateById(argThat(updated -> "SENT".equals(updated.getStatus())));
    }

    @Test
    void failedSystemMessageIsPersistedForRetryInsteadOfMarkedSent() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-retry-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var jdbc = mock(JdbcTemplate.class);
        var messageService = mock(MessageService.class);
        var properties = new QuantProperties();
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, properties,
            messageService, jdbc, mock(ExchangeGateway.class), mock(TaskExecutor.class));
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
            BigDecimal.valueOf(11), BigDecimal.ONE, "USDT", BigDecimal.valueOf(101), BigDecimal.valueOf(90), BigDecimal.ZERO,
            new BigDecimal("8"), "STOP", "test"));

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        assertEquals("SYSTEM", rows.getValue().getChannel());
        assertTrue(rows.getValue().getContent().contains("最大回报率: 8%"));
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
            mock(MessageService.class), mock(JdbcTemplate.class), gateway, mock(TaskExecutor.class));
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
            99L, "USDT", new BigDecimal("5.86351456"), "MANUAL", "test close");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper, times(2)).insert(rows.capture());
        var notifications = rows.getAllValues();
        var content = notifications.stream().filter(row -> "SYSTEM".equals(row.getChannel())).findFirst().orElseThrow().getContent();
        var factTask = notifications.stream().filter(row -> "FACT_PENDING".equals(row.getChannel())).findFirst().orElseThrow();
        assertEquals("SENT", factTask.getStatus());
        verify(mapper).updateById(argThat(updated -> "SENT".equals(updated.getStatus()) && "FACT_PENDING".equals(updated.getChannel())));
        assertTrue(content.contains("实际平仓均价: 102"));
        assertTrue(content.contains("最大回报率: 5.86351456%"));
        assertTrue(content.contains("交易所已实现盈亏（未扣手续费）: 3 USDT"));
        assertTrue(content.contains("手续费: 0.03 USDT"));
        assertTrue(content.contains("钱包余额: 1000 USDT"));
        assertTrue(content.contains("账户权益: 1003 USDT"));
        assertTrue(content.contains("可用余额: 900 USDT"));
    }


    @Test
    void canceledStrategyCloseWithPartialFillUsesConfirmedFillFacts() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-partial-close-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var gateway = mock(ExchangeGateway.class);
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, new QuantProperties(),
            mock(MessageService.class), mock(JdbcTemplate.class), gateway, mock(TaskExecutor.class));
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(43L);
            return 1;
        });
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setAccountName("test-account");
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("TESTNET");
        when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(100L), isNull())).thenReturn(
            new ExchangeGateway.OrderSnapshot("BTCUSDT", 100L, "client-close", "SELL", "MARKET", "CANCELED",
                BigDecimal.ZERO, BigDecimal.valueOf(2), BigDecimal.ONE, Instant.now(), BigDecimal.valueOf(102)));
        when(gateway.queryTradesByOrder(any(), eq("BTCUSDT"), eq(100L), any(), eq(1000L))).thenReturn(List.of(
            new ExchangeGateway.TradeSnapshot("BTCUSDT", 11L, 100L, "SELL", BigDecimal.ONE, BigDecimal.valueOf(102),
                new BigDecimal("0.01"), BigDecimal.ONE, Instant.now(), "USDT", "LONG")));

        service.notifyCloseByOrder(null, credential, "BTCUSDT", "LONG", BigDecimal.valueOf(2), BigDecimal.valueOf(100),
            100L, "USDT", "STOP", "partial close");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper, times(3)).insert(rows.capture());
        var notifications = rows.getAllValues();
        var systemNotice = notifications.stream().filter(row -> "SYSTEM".equals(row.getChannel())).findFirst().orElseThrow();
        var factTask = notifications.stream().filter(row -> "FACT_PENDING".equals(row.getChannel())
            && "100".equals(row.getRecipient())).findFirst().orElseThrow();
        var snapshotTask = notifications.stream().filter(row -> "FACT_PENDING".equals(row.getChannel())
            && "SNAPSHOT:100".equals(row.getRecipient())).findFirst().orElseThrow();
        assertEquals("SENT", factTask.getStatus());
        assertEquals("PENDING", snapshotTask.getStatus());
        assertTrue(systemNotice.getContent().contains("平仓数量: 1"));
        assertTrue(systemNotice.getContent().contains("实际平仓均价: 102"));
    }

    @Test
    void incompleteCloseFactsArePersistedForBackgroundReconciliation() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-fact-pending-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var gateway = mock(ExchangeGateway.class);
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, new QuantProperties(),
            mock(MessageService.class), mock(JdbcTemplate.class), gateway, mock(TaskExecutor.class));
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(44L);
            return 1;
        });
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("TESTNET");
        when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(101L), isNull())).thenReturn(
            new ExchangeGateway.OrderSnapshot("BTCUSDT", 101L, "client-close", "SELL", "MARKET", "PARTIALLY_FILLED",
                BigDecimal.ZERO, BigDecimal.valueOf(2), BigDecimal.ONE, Instant.now(), BigDecimal.valueOf(102)));

        service.notifyCloseByOrder(null, credential, "BTCUSDT", "LONG", BigDecimal.valueOf(2), BigDecimal.valueOf(100),
            101L, "USDT", "STOP", "pending close");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        assertEquals("FACT_PENDING", rows.getValue().getChannel());
        assertEquals("PENDING", rows.getValue().getStatus());
        assertNotNull(rows.getValue().getNextRetryAt());
        assertTrue(rows.getValue().getContent().contains("\"orderId\":\"101\""));
        assertFalse(rows.getValue().getContent().contains("test-secret"));
        var callOrder = inOrder(mapper, gateway);
        callOrder.verify(mapper).insert(any(QuantNotification.class));
        callOrder.verify(gateway).queryOrder(any(), eq("BTCUSDT"), eq(101L), isNull());
    }


    @Test
    void protectionCloseWithoutJournaledChildOrderIsPersistedForResolution() throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-unresolved-child-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, new QuantProperties(),
            mock(MessageService.class), mock(JdbcTemplate.class), mock(ExchangeGateway.class), mock(TaskExecutor.class));
        var intentMapper = mock(org.dromara.quant.execution.StrategyOrderIntentMapper.class);
        var intentMapperField = QuantTradeNotificationService.class.getDeclaredField("strategyOrderIntentMapper");
        intentMapperField.setAccessible(true);
        intentMapperField.set(service, intentMapper);
        when(intentMapper.findCloseOrderIdsByCycle(10L, "BTCUSDT", 21L, "cycle-abc", "LONG", "SELL", BigDecimal.ONE))
            .thenReturn(List.of());
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(45L);
            return 1;
        });
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("TESTNET");

        service.notifyCloseByStrategyOrder(null, credential, 21L, "cycle-abc", "BTCUSDT", "LONG",
            BigDecimal.ONE, BigDecimal.valueOf(100), "USDT", null, "EXCHANGE_PROTECTION_OR_EXTERNAL", "protection close");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        assertEquals("FACT_PENDING", rows.getValue().getChannel());
        assertTrue(rows.getValue().getRecipient().contains("STRATEGY:21:cycle-abc"));
        assertTrue(rows.getValue().getContent().contains("\"strategyInstanceId\":\"21\""));
        assertFalse(rows.getValue().getContent().contains("test-secret"));
    }


    @Test
    void strategyCloseResolverUsesExactCycleIntentInsteadOfMatchingQuantityAcrossTheInstance() throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notification-cycle-close-resolver-test"), QuantNotification.class);
        var mapper = mock(QuantNotificationMapper.class);
        var gateway = mock(ExchangeGateway.class);
        var service = new QuantTradeNotificationService(mock(QuantRiskConfigMapper.class), mapper, new QuantProperties(),
            mock(MessageService.class), mock(JdbcTemplate.class), gateway, mock(TaskExecutor.class));
        var intentMapper = mock(org.dromara.quant.execution.StrategyOrderIntentMapper.class);
        var intentMapperField = QuantTradeNotificationService.class.getDeclaredField("strategyOrderIntentMapper");
        intentMapperField.setAccessible(true);
        intentMapperField.set(service, intentMapper);
        when(intentMapper.findCloseOrderIdsByCycle(10L, "BTCUSDT", 21L, "cycle-abc", "LONG", "SELL", BigDecimal.ONE))
            .thenReturn(List.of(99L));
        when(mapper.insert(any(QuantNotification.class))).thenAnswer(invocation -> {
            ((QuantNotification) invocation.getArgument(0)).setId(46L);
            return 1;
        });
        var credential = new QuantCredential();
        credential.setId(10L);
        credential.setApiKey("test-key");
        credential.setSecretKey("test-secret");
        credential.setSignatureType("HMAC");
        credential.setRunMode("LIVE");
        when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(99L), isNull())).thenReturn(
            new ExchangeGateway.OrderSnapshot("BTCUSDT", 99L, "client-close", "SELL", "MARKET", "PARTIALLY_FILLED",
                BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("0.5"), Instant.now(), BigDecimal.valueOf(100)));

        service.notifyCloseByStrategyOrder(null, credential, 21L, "cycle-abc", "BTCUSDT", "LONG",
            BigDecimal.ONE, BigDecimal.valueOf(100), "USDT", null, "MANUAL", "cycle-keyed close");

        var rows = ArgumentCaptor.forClass(QuantNotification.class);
        verify(mapper).insert(rows.capture());
        assertEquals("99", rows.getValue().getRecipient());
        assertTrue(rows.getValue().getContent().contains("\"orderId\":\"99\""));
        verify(intentMapper).findCloseOrderIdsByCycle(10L, "BTCUSDT", 21L, "cycle-abc", "LONG", "SELL", BigDecimal.ONE);
    }


}

package org.dromara.quant.reconcile;

import org.dromara.quant.account.mapper.QuantAccountSnapshotMapper;
import org.dromara.quant.account.mapper.QuantTradeMapper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.StrictBinanceTradeCollector;
import org.dromara.quant.execution.StrictBinanceIncomeCollector;
import org.dromara.quant.execution.StrategyFillJournalService;
import org.dromara.quant.finance.mapper.QuantFinancialEventMapper;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.dromara.quant.position.mapper.QuantPositionMapper;
import org.dromara.quant.protection.mapper.QuantProtectionOrderMapper;
import org.dromara.quant.reconcile.mapper.QuantReconcileRecordMapper;
import org.dromara.quant.reconcile.mapper.StrategyFillSyncCursorMapper;
import org.dromara.quant.reconcile.mapper.FinancialEventSyncCursorMapper;
import org.dromara.quant.runtime.service.StrategyPositionStateService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * No network call may run in the transaction that persists a complete exchange snapshot.
 * Missing local position data must NEVER trigger Binance cancelProtectionOrder.
 */
@Tag("dev")
class TradingReconciliationTransactionTest {
    private final ExchangeGateway gateway = mock(ExchangeGateway.class);
    private final QuantAccountSnapshotMapper accounts = mock(QuantAccountSnapshotMapper.class);
    private final QuantPositionMapper positions = mock(QuantPositionMapper.class);
    private final QuantOrderMapper orders = mock(QuantOrderMapper.class);
    private final QuantTradeMapper trades = mock(QuantTradeMapper.class);
    private final QuantProtectionOrderMapper protection = mock(QuantProtectionOrderMapper.class);
    private final QuantFinancialEventMapper finance = mock(QuantFinancialEventMapper.class);
    private final QuantReconcileRecordMapper records = mock(QuantReconcileRecordMapper.class);
    private final ReconciliationFailureRecorder errors = mock(ReconciliationFailureRecorder.class);
    private final StrategyPositionStateService state = mock(StrategyPositionStateService.class);
    private final StrategyFillJournalService fillJournal = mock(StrategyFillJournalService.class);
    private final StrategyFillSyncCursorMapper fillCursors = mock(StrategyFillSyncCursorMapper.class);
    private final StrategyFillSyncCursorService fillCursorService = mock(StrategyFillSyncCursorService.class);
    private final FinancialEventSyncCursorMapper incomeCursors = mock(FinancialEventSyncCursorMapper.class);
    private final FinancialEventSyncCursorService incomeCursorService = mock(FinancialEventSyncCursorService.class);

    private TradingReconciliationService service() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:reconciliation_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        return new TradingReconciliationService(gateway, accounts, positions, orders, trades, protection, finance, records, errors, state,
            new DataSourceTransactionManager(source), new StrictBinanceTradeCollector(gateway), new StrictBinanceIncomeCollector(gateway), fillJournal,
            fillCursors, fillCursorService, incomeCursors, incomeCursorService);
    }

    private QuantCredential credential() {
        var value = new QuantCredential();
        value.setId(1L);
        value.setApiKey("test-key");
        value.setSecretKey("test-secret");
        value.setRunMode("LIVE");
        return value;
    }

    private void stubCommonFacts() {
        when(gateway.queryAccount(any())).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Binance called inside DB transaction");
            return new ExchangeGateway.AccountSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);
        });
        when(gateway.queryPositions(any(), isNull())).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of();
        });
        when(gateway.queryOrders(any(), isNull())).thenReturn(List.of());
        when(gateway.queryOrderHistory(any(), isNull(), any(), anyLong())).thenReturn(List.of());
        when(gateway.queryIncomeHistory(any(), isNull(), isNull(), any(), anyLong())).thenReturn(List.of());
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "DB write not inside short transaction");
            return 1;
        }).when(accounts).insert(any(org.dromara.quant.account.domain.QuantAccountSnapshot.class));
    }

    @Test
    void deferredPositionSnapshotSchedulesOneCoalescedReadOnlyRetry() {
        TradingReconciliationService reconciliation = service();
        try {
            when(gateway.queryAccount(any())).thenThrow(new org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException(60_000L));

            assertThrows(org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException.class,
                () -> reconciliation.refreshPositionsOnly(credential()));

            @SuppressWarnings("unchecked")
            java.util.Map<Long, java.util.concurrent.ScheduledFuture<?>> retries =
                (java.util.Map<Long, java.util.concurrent.ScheduledFuture<?>>)
                    org.springframework.test.util.ReflectionTestUtils.getField(reconciliation, "positionRetryTasks");
            assertNotNull(retries);
            assertTrue(retries.containsKey(1L));
            assertEquals(1, retries.size());
        } finally {
            reconciliation.shutdownPositionSnapshotRetries();
        }
    }

    @Test
    void completeIncomeHistoryAdvancesItsCursorInsideThePersistenceTransaction() {
        stubCommonFacts();
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "income watermark must share the financial-event transaction");
            return null;
        }).when(incomeCursorService).advance(eq(1L), any(java.time.LocalDateTime.class));

        service().reconcileCredential(credential(), 24);

        verify(incomeCursorService).advance(eq(1L), any(java.time.LocalDateTime.class));
    }

    @Test
    void incompleteIncomeHistoryDoesNotAdvanceItsCursor() {
        stubCommonFacts();
        when(gateway.queryIncomeHistory(any(), isNull(), isNull(), any(), anyLong())).thenAnswer(call -> {
            ExchangeGateway.TimeRange range = call.getArgument(3);
            return List.of(new ExchangeGateway.IncomeSnapshot("BTCUSDT", "FUNDING_FEE", null, "USDT", 99L, "funding", range.from().plusSeconds(1)));
        });

        assertThrows(IllegalStateException.class, () -> service().reconcileCredential(credential(), 24));

        verifyNoInteractions(incomeCursorService);
    }

    @Test
    void historyWindowUsesRealUtcInstantInShanghaiTimezone() {
        java.util.TimeZone previous = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
            stubCommonFacts();
            Instant before = Instant.now();
            service().reconcileCredential(credential(), 24);
            var range = org.mockito.ArgumentCaptor.forClass(ExchangeGateway.TimeRange.class);
            verify(gateway).queryOrderHistory(any(), isNull(), range.capture(), anyLong());
            assertFalse(range.getValue().to().isBefore(before));
            assertFalse(range.getValue().to().isAfter(Instant.now()));
            assertEquals(java.time.Duration.ofHours(24), java.time.Duration.between(range.getValue().from(), range.getValue().to()));
        } finally {
            java.util.TimeZone.setDefault(previous);
        }
    }

    @Test
    void historyWatermarkUsesExistingBinanceClockOffset() {
        stubCommonFacts();
        when(gateway.exchangeTimeOffsetMillis("LIVE")).thenReturn(-5_000L);
        Instant before = Instant.now().minusMillis(5_000);
        service().reconcileCredential(credential(), 24);
        var range = org.mockito.ArgumentCaptor.forClass(ExchangeGateway.TimeRange.class);
        verify(gateway).queryOrderHistory(any(), isNull(), range.capture(), anyLong());
        assertFalse(range.getValue().to().isBefore(before));
        assertFalse(range.getValue().to().isAfter(Instant.now().minusMillis(5_000)));
    }

    @Test
    void missingFromOpenListCannotManufactureProtectionTerminalState() {
        var local = new org.dromara.quant.protection.domain.QuantProtectionOrder();
        local.setId(71L);
        local.setCredentialId(1L);
        local.setSymbol("BTCUSDT");
        local.setStatus("WORKING");
        local.setProtectionType("HARD_STOP");
        when(protection.selectList(any())).thenReturn(List.of(local));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service(), "syncProtectionOrders", credential(), "BTCUSDT", List.of(), List.of());

        assertEquals("WORKING", local.getStatus());
        verify(protection, never()).updateById(any(org.dromara.quant.protection.domain.QuantProtectionOrder.class));
    }

    @Test
    void staleOpenProtectionSnapshotCannotOverwriteFrozenIntentOrTerminalState() {
        var local = new org.dromara.quant.protection.domain.QuantProtectionOrder();
        local.setId(71L);
        local.setCredentialId(1L);
        local.setPositionId(51L);
        local.setPositionCycleKey("old-cycle");
        local.setSymbol("BTCUSDT");
        local.setStatus("FINISHED");
        local.setQuantity(BigDecimal.ONE);
        local.setProtectionType("FIRST_TP");
        when(protection.selectOne(any())).thenReturn(local);
        var snapshot = new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 81L, "QFTP_test", "NEW", "SELL", "LONG",
            "TAKE_PROFIT_MARKET", BigDecimal.TEN, BigDecimal.TEN, Instant.now());

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service(), "syncProtectionOrders", credential(), "BTCUSDT", List.of(), List.of(snapshot));

        assertEquals("FINISHED", local.getStatus());
        assertEquals("old-cycle", local.getPositionCycleKey());
        assertEquals(BigDecimal.ONE, local.getQuantity());
        assertEquals(51L, local.getPositionId());
        verify(protection, never()).updateById(any(org.dromara.quant.protection.domain.QuantProtectionOrder.class));
    }

    @Test
    void futureCursorIsRepairedOnlyAfterCompleteHistoricalBackfill() {
        stubCommonFacts();
        var future = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(8);
        when(fillCursors.selectSymbols(1L)).thenReturn(List.of("BTCUSDT"));
        when(fillCursors.selectLastCompleteTo(1L, "BTCUSDT")).thenReturn(future);
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            ExchangeGateway.TimeRange range = call.getArgument(2);
            assertFalse(range.to().isAfter(Instant.now()));
            assertTrue(range.from().isBefore(Instant.now().minus(java.time.Duration.ofHours(30))));
            return List.of();
        });
        when(gateway.queryProtectionOrders(any(), eq("BTCUSDT"))).thenReturn(List.of());

        service().reconcileCredential(credential(), 24);

        var sequence = inOrder(gateway, fillCursorService);
        sequence.verify(gateway).queryTrades(any(), eq("BTCUSDT"), any());
        sequence.verify(fillCursorService).repairFutureAfterCollection(eq(1L), eq("BTCUSDT"), eq(future), any());
        verify(fillCursorService, never()).advance(eq(1L), eq("BTCUSDT"), any());
    }

    @Test
    void failedBackfillCannotRepairFutureCursor() {
        stubCommonFacts();
        when(fillCursors.selectSymbols(1L)).thenReturn(List.of("BTCUSDT"));
        when(fillCursors.selectLastCompleteTo(1L, "BTCUSDT")).thenReturn(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(8));
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenThrow(new IllegalStateException("history unavailable"));
        assertThrows(IllegalStateException.class, () -> service().reconcileCredential(credential(), 24));
        verifyNoInteractions(fillCursorService);
    }

    @Test
    void collectsAllExchangeFactsBeforeOpeningDatabaseWriteTransaction() {
        stubCommonFacts();
        service().reconcileCredential(credential(), 48);
        verify(accounts).insert(any(org.dromara.quant.account.domain.QuantAccountSnapshot.class));
        verify(records).insert(any(org.dromara.quant.reconcile.domain.QuantReconcileRecord.class));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void historicalOrderSnapshotsAlsoRepairDecisionLogsWithoutTradeHistory() {
        stubCommonFacts();
        var order = new ExchangeGateway.OrderSnapshot("BTCUSDT", 88L, "open-client", "BUY", "MARKET", "FILLED",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Instant.now(), new BigDecimal("101"));
        when(gateway.queryOrderHistory(any(), isNull(), any(), anyLong())).thenReturn(List.of(order));
        var reconciliation = service();
        var logs = mock(org.dromara.quant.decision.TradeDecisionLogService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "decisionLogs", logs);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
            new org.apache.ibatis.session.Configuration(), "history-log-test"), org.dromara.quant.order.domain.QuantOrder.class);

        reconciliation.reconcileCredential(credential(), 24);

        verify(logs).order(1L, order);
    }

    @Test
    void failedHistoryQueryCannotRollbackEarlierVerifiedAccountSnapshot() {
        stubCommonFacts();
        when(gateway.queryOrderHistory(any(), isNull(), any(), anyLong())).thenThrow(new IllegalStateException("Binance unavailable"));
        assertThrows(IllegalStateException.class, () -> service().reconcileCredential(credential(), 48));
        verify(accounts).insert(any(org.dromara.quant.account.domain.QuantAccountSnapshot.class));
        verify(errors).record(eq(1L), anyString());
    }

    @Test
    void orphanProtectionDoesNotGetCanceledJustBecauseLocalPositionIsMissing() {
        stubCommonFacts();
        var open = new ExchangeGateway.OrderSnapshot("BTCUSDT", 7L, "open7", "BUY", "MARKET", "NEW", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, Instant.EPOCH);
        when(gateway.queryOrders(any(), isNull())).thenReturn(List.of(open));
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenReturn(List.of());
        when(gateway.queryProtectionOrders(any(), eq("BTCUSDT"))).thenReturn(List.of(new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 123L, "protect123", "NEW", "SELL",
            "LONG", "STOP_MARKET", BigDecimal.ONE, BigDecimal.ONE, Instant.EPOCH)));
        service().reconcileCredential(credential(), 48);
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(protection).insert(any(org.dromara.quant.protection.domain.QuantProtectionOrder.class));
        var record = org.mockito.ArgumentCaptor.forClass(org.dromara.quant.reconcile.domain.QuantReconcileRecord.class);
        verify(records).insert(record.capture());
        assertEquals("DEGRADED", record.getValue().getResult());
        assertTrue(record.getValue().getDetailJson().contains("unmatchedLiveProtections"));
    }

    @Test
    void confirmedFlatAccountPositionIsNotRolledBackByLaterOrderFailure() {
        stubCommonFacts();
        var stale = new org.dromara.quant.position.domain.QuantPosition();
        stale.setId(501L);
        stale.setCredentialId(1L);
        stale.setSymbol("BTCUSDT");
        stale.setPositionSide("LONG");
        stale.setQuantity(new BigDecimal("0.25"));
        when(positions.selectList(any())).thenReturn(List.of(stale));
        doAnswer(call -> {
            var p = (org.dromara.quant.position.domain.QuantPosition) call.getArgument(0);
            assertEquals(0, p.getQuantity().compareTo(BigDecimal.ZERO));
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        }).when(positions).updateById(any(org.dromara.quant.position.domain.QuantPosition.class));
        when(gateway.queryOrderHistory(any(), isNull(), any(), anyLong())).thenReturn(List.of(new ExchangeGateway.OrderSnapshot("BTCUSDT", 78L, "filled78", "SELL", "MARKET",
            "FILLED", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, Instant.EPOCH)));
        when(gateway.queryTrades(any(), eq("BTCUSDT"), any())).thenReturn(List.of());
        when(gateway.queryProtectionOrders(any(), eq("BTCUSDT"))).thenReturn(List.of());
        when(orders.selectOne(any())).thenThrow(new IllegalStateException("order database write failed"));
        assertThrows(IllegalStateException.class, () -> service().reconcileCredential(credential(), 48));
        verify(positions).updateById(stale);
        verify(state).markClosedByPositionId(501L);
        verify(errors).record(eq(1L), anyString());
    }

    @Test
    void fullSnapshotClosesStaleStrategyStateForAlreadyZeroLocalPosition() {
        stubCommonFacts();
        var flat = new org.dromara.quant.position.domain.QuantPosition();
        flat.setId(502L);
        flat.setCredentialId(1L);
        flat.setSymbol("PUMPUSDT");
        flat.setPositionSide("LONG");
        flat.setQuantity(BigDecimal.ZERO);
        when(positions.selectList(any())).thenReturn(List.of(flat));

        service().refreshPositionsOnly(credential());

        verify(positions).selectList(any());
        verify(state).markClosedByPositionId(502L);
        verify(positions, never()).updateById(flat);
    }

    @Test
    void runtimeReconciliationReusesRecentCompleteAccountSnapshot() {
        stubCommonFacts();
        var reconciliation = service();

        reconciliation.refreshPositionsOnly(credential());
        var snapshots = reconciliation.runtimePositionSnapshot(credential());

        assertNotNull(snapshots);
        assertTrue(snapshots.isEmpty(), "an empty list is a complete confirmed-flat account snapshot");
        verify(gateway, times(1)).queryAccount(any());
        verify(gateway, times(1)).queryPositions(any(), isNull());
    }

    @Test
    void concurrentRuntimeReconciliationSharesOneAccountSnapshotFetch() throws Exception {
        stubCommonFacts();
        var reconciliation = service();
        var fetchStarted = new java.util.concurrent.CountDownLatch(1);
        var releaseFetch = new java.util.concurrent.CountDownLatch(1);
        when(gateway.queryAccount(any())).thenAnswer(invocation -> {
            fetchStarted.countDown();
            assertTrue(releaseFetch.await(3, java.util.concurrent.TimeUnit.SECONDS));
            return new ExchangeGateway.AccountSnapshot(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);
        });
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> reconciliation.runtimePositionSnapshot(credential()));
            assertTrue(fetchStarted.await(2, java.util.concurrent.TimeUnit.SECONDS));
            var second = executor.submit(() -> reconciliation.runtimePositionSnapshot(credential()));
            releaseFetch.countDown();
            assertTrue(first.get(5, java.util.concurrent.TimeUnit.SECONDS).isEmpty());
            assertTrue(second.get(5, java.util.concurrent.TimeUnit.SECONDS).isEmpty());
            verify(gateway, times(1)).queryAccount(any());
            verify(gateway, times(1)).queryPositions(any(), isNull());
        } finally {
            releaseFetch.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void privateStreamEventInvalidatesSharedRuntimeSnapshot() {
        stubCommonFacts();
        var reconciliation = service();
        reconciliation.refreshPositionsOnly(credential());

        reconciliation.invalidateRuntimePositionSnapshot(1L);
        reconciliation.runtimePositionSnapshot(credential());

        verify(gateway, times(2)).queryAccount(any());
        verify(gateway, times(2)).queryPositions(any(), isNull());
    }

    @Test
    void newerAccountEventDuringRestCollectionMustNotOverwriteLocalPositions() {
        stubCommonFacts();
        var reconciliation = service();
        var freshness = new LivePositionFreshness();
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "positionFreshness", freshness);
        when(gateway.queryPositions(any(), isNull())).thenAnswer(invocation -> {
            freshness.invalidate(1L);
            return List.of();
        });
        assertThrows(IllegalStateException.class, () -> reconciliation.refreshPositionsOnly(credential()));
        verifyNoInteractions(positions);
        verifyNoInteractions(accounts);
        assertFalse(freshness.fresh(1L));
    }

    @Test
    void olderNodeSnapshotMustNotOverwriteNewerPersistedWatermark() {
        stubCommonFacts();
        var reconciliation = service();
        var fence = mock(org.dromara.quant.reconcile.mapper.LivePositionSnapshotFenceMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "snapshotFence", fence);
        when(fence.lockAndRead(1L)).thenReturn(java.time.LocalDateTime.now().plusMinutes(5));
        assertThrows(IllegalStateException.class, () -> reconciliation.refreshPositionsOnly(credential()));
        verify(fence).ensure(1L);
        verifyNoInteractions(positions);
        verify(accounts, never()).insert(any(org.dromara.quant.account.domain.QuantAccountSnapshot.class));
        verify(fence, never()).advance(any(), any());
    }

    @Test
    void verifiedSnapshotAdvancesWatermarkWithinDatabaseTransaction() {
        stubCommonFacts();
        var reconciliation = service();
        var fence = mock(org.dromara.quant.reconcile.mapper.LivePositionSnapshotFenceMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "snapshotFence", fence);
        when(fence.advance(eq(1L), any())).thenAnswer(inv -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });
        reconciliation.refreshPositionsOnly(credential());
        verify(fence).ensure(1L);
        verify(fence).lockAndRead(1L);
        verify(fence).advance(eq(1L), any());
    }

    @Test
    void supersededRestSnapshotDoesNotRecordAnExchangeFailureOrWriteOldPositions() {
        stubCommonFacts();
        var reconciliation = service();
        when(gateway.queryPositions(any(), isNull())).thenAnswer(invocation -> {
            reconciliation.invalidateRuntimePositionSnapshot(1L);
            return List.of();
        });
        assertThrows(TradingReconciliationService.SnapshotSupersededException.class,
            () -> reconciliation.refreshPositionsOnly(credential()));
        verifyNoInteractions(errors, positions, accounts);
    }

    @Test
    void positionSnapshotAndCapacityReservationUseSameCredentialRowLock() {
        stubCommonFacts();
        var reconciliation = service();
        var capacity = mock(org.dromara.quant.risk.mapper.OpenCapacityMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "openCapacityMapper", capacity);
        when(capacity.lockCredential(1L)).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of(1L);
        });
        reconciliation.refreshPositionsOnly(credential());
        verify(capacity).lockCredential(1L);
        verify(accounts).insert(any(org.dromara.quant.account.domain.QuantAccountSnapshot.class));
    }

    @Test
    void missingCapacityCredentialLockAbortsPositionWrite() {
        stubCommonFacts();
        var reconciliation = service();
        var capacity = mock(org.dromara.quant.risk.mapper.OpenCapacityMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "openCapacityMapper", capacity);
        when(capacity.lockCredential(1L)).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> reconciliation.refreshPositionsOnly(credential()));
        verifyNoInteractions(accounts, positions);
    }

    @Test
    void olderSymbolSnapshotCannotRestorePositionAfterNewerFlatSnapshot() {
        var reconciliation = service();
        var fence = mock(org.dromara.quant.reconcile.mapper.LivePositionSnapshotFenceMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "snapshotFence", fence);
        when(fence.lockAndRead(1L)).thenReturn(java.time.LocalDateTime.now());
        var staleOpen = new ExchangeGateway.PositionSnapshot("BTCUSDT", "LONG", new BigDecimal("0.25"), new BigDecimal("100"), new BigDecimal("101"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.TEN, 10, null, null);

        assertThrows(TradingReconciliationService.SnapshotSupersededException.class, () -> reconciliation.syncSymbolPositionSnapshot(credential(), "BTCUSDT", List.of(staleOpen),
            java.time.LocalDateTime.now().minusMinutes(1), 0L));

        verify(fence).ensure(1L);
        verify(positions, never()).insert(any(org.dromara.quant.position.domain.QuantPosition.class));
        verify(positions, never()).updateById(any(org.dromara.quant.position.domain.QuantPosition.class));
    }

    @Test
    void symbolSnapshotOnlyZerosThatSymbolsLocalPositions() {
        var reconciliation = service();
        when(positions.selectList(any())).thenReturn(List.of());
        var fence = mock(org.dromara.quant.reconcile.mapper.LivePositionSnapshotFenceMapper.class);
        var capacity = mock(org.dromara.quant.risk.mapper.OpenCapacityMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "snapshotFence", fence);
        org.springframework.test.util.ReflectionTestUtils.setField(reconciliation, "openCapacityMapper", capacity);
        when(capacity.lockCredential(1L)).thenReturn(List.of(1L));
        when(fence.advance(eq(1L), any())).thenReturn(1);

        reconciliation.syncSymbolPositionSnapshot(credential(), "BTCUSDT", List.of(), java.time.LocalDateTime.now(), 0L);

        verify(positions).selectList(any());
        verify(capacity).lockCredential(1L);
        verify(fence).advance(eq(1L), any());
    }


    @Test
    void orderSnapshotReconciliationUsesAtomicMonotonicMergeInsteadOfBlindEntityUpdate() {
        var local = new org.dromara.quant.order.domain.QuantOrder();
        local.setId(88L);
        local.setCredentialId(1L);
        local.setSymbol("BTCUSDT");
        local.setExchangeOrderId(88L);
        local.setClientOrderId("client-88");
        local.setStatus("FILLED");
        local.setExecutedQuantity(BigDecimal.ONE);
        when(orders.selectOne(any())).thenReturn(local);
        when(orders.mergeExchangeFactIfMonotonic(any())).thenReturn(0);
        var stale = new ExchangeGateway.OrderSnapshot("BTCUSDT", 88L, "client-88", "BUY", "MARKET", "NEW",
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, Instant.now(), null);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service(), "syncOrders", credential(), List.of(stale));

        var fact = org.mockito.ArgumentCaptor.forClass(org.dromara.quant.order.domain.QuantOrder.class);
        verify(orders).mergeExchangeFactIfMonotonic(fact.capture());
        verify(orders, never()).updateById(any(org.dromara.quant.order.domain.QuantOrder.class));
        assertEquals("NEW", fact.getValue().getStatus());
        assertEquals(BigDecimal.ZERO, fact.getValue().getExecutedQuantity());
    }

    @Test
    void incompleteLiveFillIsJournaledAndNeverWrittenAsZeroValuedTrade() {
        var fill = new ExchangeGateway.TradeSnapshot("BTCUSDT", 501L, 202L, "BUY", BigDecimal.ONE, BigDecimal.TEN,
            null, null, Instant.parse("2026-10-17T00:00:00Z"), null);
        when(orders.selectOne(any())).thenReturn(null);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service(), "syncTrades", credential(), List.of(fill));

        verify(fillJournal).attribute(fill, null, 1L);
        verify(trades, never()).insert(any(org.dromara.quant.account.domain.QuantTrade.class));
        verify(trades, never()).updateById(any(org.dromara.quant.account.domain.QuantTrade.class));
    }

    @Test
    void replayCannotOverwriteImmutableTradeFactsOrFundingFee() {
        var fill = new ExchangeGateway.TradeSnapshot("BTCUSDT", 101L, 202L, "BUY", BigDecimal.ONE, BigDecimal.TEN,
            new BigDecimal("0.01"), BigDecimal.ZERO, Instant.parse("2026-10-17T00:00:00Z"), "USDT");
        var local = new org.dromara.quant.account.domain.QuantTrade();
        local.setId(77L);
        local.setCredentialId(1L);
        local.setSymbol("BTCUSDT");
        local.setExchangeTradeId(101L);
        local.setExchangeOrderId(202L);
        local.setSide("BUY");
        local.setQuantity(BigDecimal.ONE);
        local.setPrice(BigDecimal.TEN);
        local.setCommission(new BigDecimal("0.01"));
        local.setRealizedPnl(BigDecimal.ZERO);
        local.setTradeTime(java.time.LocalDateTime.ofInstant(fill.time(), java.time.ZoneOffset.UTC));
        local.setFundingFee(new BigDecimal("0.25"));
        when(trades.selectOne(any())).thenReturn(local);

        var reconciliation = service();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(reconciliation, "syncTrades", credential(), List.of(fill));

        assertEquals(new BigDecimal("0.25"), local.getFundingFee());
        verify(trades, never()).updateById(any(org.dromara.quant.account.domain.QuantTrade.class));
    }

    @Test
    void localTradeConflictIsQuarantinedAndDoesNotAbortRawFillEvidence() {
        var fill = new ExchangeGateway.TradeSnapshot("BTCUSDT", 601L, 202L, "BUY", BigDecimal.ONE, BigDecimal.TEN,
            new BigDecimal("0.01"), BigDecimal.ZERO, Instant.parse("2026-10-17T00:00:00Z"), "USDT");
        var local = new org.dromara.quant.account.domain.QuantTrade();
        local.setId(77L);
        local.setCredentialId(1L);
        local.setSymbol("BTCUSDT");
        local.setExchangeTradeId(601L);
        local.setExchangeOrderId(202L);
        local.setSide("BUY");
        local.setQuantity(BigDecimal.ONE);
        local.setPrice(new BigDecimal("11"));
        local.setCommission(new BigDecimal("0.01"));
        local.setRealizedPnl(BigDecimal.ZERO);
        local.setTradeTime(java.time.LocalDateTime.ofInstant(fill.time(), java.time.ZoneOffset.UTC));
        when(trades.selectOne(any())).thenReturn(local);
        when(orders.selectOne(any())).thenReturn(null);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service(), "syncTrades", credential(), List.of(fill));

        verify(fillJournal).quarantineConflict(1L, "BTCUSDT", 601L, contains("conflicts with immutable Binance fill facts"));
        verify(trades, never()).updateById(any(org.dromara.quant.account.domain.QuantTrade.class));
    }

    @Test
    void conflictingReplayCannotRewriteExistingTradePrice() {
        var fill = new ExchangeGateway.TradeSnapshot("BTCUSDT", 101L, 202L, "BUY", BigDecimal.ONE, BigDecimal.TEN,
            new BigDecimal("0.01"), BigDecimal.ZERO, Instant.parse("2026-10-17T00:00:00Z"), "USDT");
        var local = new org.dromara.quant.account.domain.QuantTrade();
        local.setId(77L);
        local.setCredentialId(1L);
        local.setSymbol("BTCUSDT");
        local.setExchangeTradeId(101L);
        local.setExchangeOrderId(202L);
        local.setSide("BUY");
        local.setQuantity(BigDecimal.ONE);
        local.setPrice(new BigDecimal("11"));
        local.setCommission(new BigDecimal("0.01"));
        local.setRealizedPnl(BigDecimal.ZERO);
        local.setTradeTime(java.time.LocalDateTime.ofInstant(fill.time(), java.time.ZoneOffset.UTC));
        when(trades.selectOne(any())).thenReturn(local);

        var testnetCredential = credential();
        testnetCredential.setRunMode("TESTNET");
        assertThrows(IllegalStateException.class,
            () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service(), "syncTrades", testnetCredential, List.of(fill)));

        verifyNoInteractions(fillJournal);
        verify(trades, never()).updateById(any(org.dromara.quant.account.domain.QuantTrade.class));
        verifyNoInteractions(fillCursors, fillCursorService);
    }

}

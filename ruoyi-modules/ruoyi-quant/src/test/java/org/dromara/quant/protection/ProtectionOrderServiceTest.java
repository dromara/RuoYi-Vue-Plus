package org.dromara.quant.protection;

import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.LiveOrderDispatcher;
import org.dromara.quant.execution.TradeExecutionApplicationService;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.dromara.quant.protection.mapper.QuantProtectionOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("dev")
class ProtectionOrderServiceTest {
    private final ExchangeGateway gateway = mock(ExchangeGateway.class);
    private final QuantProtectionOrderMapper mapper = mock(QuantProtectionOrderMapper.class);
    private final TradeExecutionApplicationService execution = mock(TradeExecutionApplicationService.class);
    private final ProtectionOrderService service = new ProtectionOrderService(gateway, mapper, execution);
    private final LiveOrderDispatcher dispatcher = mock(LiveOrderDispatcher.class);
    private final QuantPosition position = new QuantPosition();
    private final QuantCredential credential = new QuantCredential();

    @BeforeEach
    void setup() {
        service.setLiveOrderDispatcher(dispatcher);
        when(dispatcher.begin(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.<Long>nullable(Long.class),
            org.mockito.ArgumentMatchers.<Long>nullable(Long.class))).thenAnswer(call -> new LiveOrderDispatcher.Ticket(call.getArgument(0), call.getArgument(1),
            call.getArgument(2)));
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),
            "test"), QuantProtectionOrder.class);
        position.setId(10L);
        position.setSymbol("BTCUSDT");
        position.setPositionSide("LONG");
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        position.setMargin(new BigDecimal("10"));
        position.setLeverage(10);
        credential.setId(1L);
        var symbol = new org.dromara.quant.market.domain.QuantSymbol();
        when(execution.requireSymbol("BTCUSDT")).thenReturn(symbol);
        when(execution.symbolRule(symbol)).thenReturn(new ExchangeGateway.SymbolRule("BTCUSDT", "USDT", 2, 3, new BigDecimal("0.01"), new BigDecimal("0.001"), new BigDecimal("0" +
            ".001"), BigDecimal.ONE, "TRADING"));
        when(execution.requireCredential(1L)).thenReturn(credential);
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", "LIVE"));
        when(gateway.queryTradingRule(any(), any())).thenReturn(new ExchangeGateway.SymbolRule("BTCUSDT", "USDT", 2, 3, new BigDecimal("0.01"), new BigDecimal("0.001"),
            new BigDecimal("0.001"), BigDecimal.ONE, "TRADING"));
        when(gateway.queryPositionMode(any())).thenReturn(new ExchangeGateway.PositionMode(false));
        doAnswer(invocation -> {
            invocation.<QuantProtectionOrder>getArgument(0).setId(8L);
            return 1;
        }).when(mapper).insert(any(QuantProtectionOrder.class));
        when(mapper.updateById(any(QuantProtectionOrder.class))).thenReturn(1);
        when(mapper.update(any(QuantProtectionOrder.class), any())).thenReturn(1);
    }

    @Test
    void wrongBinanceAlgoIdMustNotConfirmOrReleaseAnUncertainProtection() {
        QuantProtectionOrder local = order();
        local.setStatus("UNKNOWN");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(56L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        assertFalse(service.isStrategyProtectionWorking(1L, position, "FIRST_TP", true));
        verify(dispatcher, never()).recoverVerifiedProtection(any(), any(), any(), any(), any());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void unresolvedProtectionDispatchCheckUsesProtectionTicketsOnly() {
        when(dispatcher.activeProtectionDispatches(1L, "BTCUSDT")).thenReturn(List.of(
            new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "cancel-key", "PROTECT_CANCEL", "QHARD_test", "UNKNOWN", 55L)));

        assertTrue(service.hasUnresolvedProtectionDispatches(1L, "BTCUSDT"));

        verify(dispatcher).activeProtectionDispatches(1L, "BTCUSDT");
    }

    @Test
    void uncertainExchangeStateMustNotBeReportedAsWorking() {
        QuantProtectionOrder local = order();
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("timeout"));
        assertFalse(service.isStrategyProtectionWorking(1L, position, "FIRST_TP", true));
    }

    @Test
    void acceptedProtectionWithInconclusiveLookupRemainsPendingInsteadOfBeingReportedAsCreateFailure() {
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(55L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(55L), any(), eq("BTCUSDT"), any())).thenReturn(null);

        var failure = assertThrows(ProtectionOrderService.ProtectionConfirmationPendingException.class,
            () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        assertTrue(failure.getMessage().contains("暂未返回精确记录"));
        var intent = org.mockito.ArgumentCaptor.forClass(QuantProtectionOrder.class);
        verify(mapper, atLeastOnce()).updateById(intent.capture());
        assertEquals("UNKNOWN", intent.getValue().getStatus());
        verify(gateway, times(1)).placeProtectionOrder(any());
        verify(dispatcher).acknowledged(any(LiveOrderDispatcher.Ticket.class), eq(55L));
    }

    @Test
    void liveAlgoOrderClaimsDispatchBeforeCallingBinanceAndAcksAfterPersistence() {
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> {
            verify(dispatcher).begin(eq(1L), eq("BTCUSDT"), anyString(), eq("PROTECT_CREATE"), anyString(), isNull(), isNull());
            verify(mapper).insert(any(QuantProtectionOrder.class));
            return new ExchangeGateway.ProtectionOrderResult(55L, call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW");
        });
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(call -> snapshot(55L, call.getArgument(2), BigDecimal.ONE, "NEW", null));
        assertEquals("WORKING", service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true).getStatus());
        verify(dispatcher).acknowledged(any(LiveOrderDispatcher.Ticket.class), eq(55L));
        verify(dispatcher, never()).unknown(any());
    }

    @Test
    void testnetProtectionAlsoUsesPersistentDispatchAndExactConfirmation() {
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", "TESTNET"));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(55L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(call -> snapshot(55L, call.getArgument(2), BigDecimal.ONE, "NEW", null));

        assertEquals("WORKING", service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true).getStatus());

        verify(dispatcher).begin(eq(1L), eq("BTCUSDT"), anyString(), eq("PROTECT_CREATE"), anyString(), isNull(), isNull());
        verify(dispatcher).acknowledged(any(LiveOrderDispatcher.Ticket.class), eq(55L));
    }

    @Test
    void unknownLiveAlgoPlacementKeepsDispatchGuardOccupied() {
        when(gateway.placeProtectionOrder(any())).thenThrow(new IllegalStateException("Binance timeout"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("query unavailable"));
        assertThrows(IllegalStateException.class, () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(dispatcher).unknown(any(LiveOrderDispatcher.Ticket.class));
        verify(dispatcher, never()).acknowledged(any(), any());
    }

    @Test
    void blockedDispatchIsRecordedAsNotSentAndDoesNotPretendAnExchangeRequestWasSent() {
        doThrow(new LiveOrderDispatcher.DispatchBlockedException("同账户交易对存在未完成交易所操作，订单未发送")).when(dispatcher).begin(eq(1L), eq("BTCUSDT"), anyString(), eq("PROTECT_CREATE"), anyString(), isNull(),
            isNull());
        var error = assertThrows(LiveOrderDispatcher.DispatchBlockedException.class,
            () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        var intent = org.mockito.ArgumentCaptor.forClass(QuantProtectionOrder.class);
        verify(mapper).insert(intent.capture());
        assertEquals("NOT_SENT", intent.getValue().getStatus());
        assertTrue(error.getMessage().contains("未发送"));
        verify(gateway, never()).placeProtectionOrder(any());
        verify(dispatcher, never()).unknown(any());
    }

    @Test
    void notSentProtectionIntentIsNeverCanceledAtExchangeDuringCleanup() {
        position.setProtectionCycleKey("cycle");
        QuantProtectionOrder neverSent = order();
        neverSent.setStatus("NOT_SENT");
        neverSent.setExchangeOrderId(null);
        when(mapper.selectList(any())).thenReturn(List.of(neverSent));

        assertTrue(service.quiesceCycleProtections(1L, position));

        verify(gateway, never()).cancelProtectionOrder(any());
        verify(dispatcher, never()).begin(any(), any(), any(), any(), any(), any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PREPARED", "REJECTED"})
    void preparedProtectionIntentWithoutDispatchTicketIsRecoveredWithoutExchangeLookup(String status) {
        position.setProtectionCycleKey("cycle");
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", "TESTNET"));
        QuantProtectionOrder prepared = order();
        prepared.setStatus(status);
        prepared.setExchangeOrderId(null);
        when(mapper.selectList(any())).thenReturn(List.of(prepared));

        assertTrue(service.quiesceCycleProtections(1L, position));

        assertEquals("NOT_SENT", prepared.getStatus());
        verify(gateway, never()).queryProtectionOrder(any(), any(), any(), any(), any());
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(dispatcher, never()).begin(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void preparedProtectionWithCreateTicketAndNullLookupRemainsBlocked() {
        QuantProtectionOrder prepared = order();
        prepared.setStatus("PREPARED");
        prepared.setExchangeOrderId(null);
        when(mapper.selectList(any())).thenReturn(List.of(prepared));
        when(dispatcher.activeProtectionTicket(1L, "BTCUSDT", prepared.getClientOrderId(), "PROTECT_CREATE")).thenReturn("create-ticket");

        assertFalse(service.quiesceCycleProtections(1L, position));

        assertEquals("PREPARED", prepared.getStatus());
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void preparedRecoveryLosingConditionalStateTransitionRemainsBlocked() {
        QuantProtectionOrder prepared = order();
        prepared.setStatus("PREPARED");
        prepared.setExchangeOrderId(null);
        when(mapper.selectList(any())).thenReturn(List.of(prepared));
        when(mapper.update(any(QuantProtectionOrder.class), any())).thenReturn(0);

        assertFalse(service.quiesceCycleProtections(1L, position));

        assertEquals("PREPARED", prepared.getStatus());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void createDispatchLosingConditionalStateTransitionNeverCallsExchange() {
        when(mapper.update(any(QuantProtectionOrder.class), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        verify(gateway, never()).placeProtectionOrder(any());
        verify(dispatcher).notSent(any(LiveOrderDispatcher.Ticket.class));
        verify(dispatcher, never()).unknown(any());
        verify(mapper, never()).updateById(any(QuantProtectionOrder.class));
    }

    @Test
    void unresolvedCreateTicketIsNotRetriedWhenExactLookupReportsAbsent() {
        QuantProtectionOrder local = order();
        local.setProtectionType("FIRST_TP");
        local.setStatus("UNKNOWN");
        when(mapper.selectList(any())).thenReturn(List.of(local));
        when(dispatcher.activeProtectionTicket(1L, "BTCUSDT", local.getClientOrderId(), "PROTECT_CREATE")).thenReturn("original-create-ticket");
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any()))
            .thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException());

        assertThrows(IllegalStateException.class, () -> service.syncStrategyProtections(1L, position, BigDecimal.ONE,
            List.of(new org.dromara.quant.strategy.ProtectionSignal("FIRST_TP", "STOP_MARKET", BigDecimal.ONE,
                org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("99"), "test")), true));

        assertEquals("UNKNOWN", local.getStatus());
        verify(dispatcher, never()).begin(any(), any(), any(), any(), any(), any(), any());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void missingDispatcherBlocksLiveAlgoOrderBeforeRemoteCall() {
        var unsafe = new ProtectionOrderService(gateway, mapper, execution);
        assertThrows(IllegalStateException.class, () -> unsafe.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void protectionModeMustMatchCredentialEnvironment() {
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", "PAPER"));

        assertThrows(IllegalStateException.class, () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        verify(dispatcher, never()).begin(any(), any(), any(), any(), any(), any(), any());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void lostProtectionTargetLeaseAfterDispatchReservationDoesNotSendOrMarkOutcomeUnknown() {
        ProtectionTargetCoordinator coordinator = mock(ProtectionTargetCoordinator.class);
        ProtectionTargetCoordinator.Lease lease = mock(ProtectionTargetCoordinator.Lease.class);
        when(coordinator.acquire(any(), any(), any())).thenReturn(lease);
        when(lease.state()).thenReturn(new ProtectionTargetCoordinator.State(0, 0, 0, null, null, null, null, null, null));
        doNothing().doThrow(new IllegalStateException("lease lost")).when(lease).check();
        var coordinatedService = new ProtectionOrderService(gateway, mapper, execution, coordinator);
        coordinatedService.setLiveOrderDispatcher(dispatcher);
        when(mapper.selectList(any())).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> coordinatedService.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        verify(dispatcher).begin(eq(1L), eq("BTCUSDT"), anyString(), eq("PROTECT_CREATE"), anyString(), isNull(), isNull());
        verify(dispatcher).notSent(any(LiveOrderDispatcher.Ticket.class));
        verify(dispatcher, never()).unknown(any(LiveOrderDispatcher.Ticket.class));
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void unresolvedLiveCancelChecksExactBinanceFactWithoutResending() {
        var local = order();
        when(mapper.selectOne(any())).thenReturn(local);
        when(dispatcher.activeProtectionTicket(1L, "BTCUSDT", "QFTP_test", "PROTECT_CANCEL")).thenReturn("pending-protection-cancel");
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        when(dispatcher.recoveredFromExchange(1L, "BTCUSDT", "pending-protection-cancel", "QFTP_test", 55L)).thenReturn(true);
        assertTrue(service.cancelStrategyProtection(1L, position, "FIRST_TP"));
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(dispatcher).recoveredFromExchange(1L, "BTCUSDT", "pending-protection-cancel", "QFTP_test", 55L);
    }

    @Test
    void exactRecoveredLiveAlgoPlacementUnblocksOnlyMatchingProtectionTicket() {
        QuantProtectionOrder local = order();
        local.setStatus("UNKNOWN");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), eq(55L), eq("QFTP_test"), eq("BTCUSDT"), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "NEW", null));
        when(dispatcher.recoverVerifiedProtection(1L, "BTCUSDT", "QFTP_test", "PROTECT_CREATE", 55L)).thenReturn(true);
        assertSame(local, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(dispatcher).recoverVerifiedProtection(1L, "BTCUSDT", "QFTP_test", "PROTECT_CREATE", 55L);
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void intentIsDurableBeforeSendingProtectionOrder() {
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", "PAPER"));
        when(gateway.placeProtectionOrder(any())).thenAnswer(invocation -> {
            verify(mapper).insert(any(QuantProtectionOrder.class));
            return new ExchangeGateway.ProtectionOrderResult(55L, invocation.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW");
        });
        doAnswer(invocation -> {
            invocation.<QuantProtectionOrder>getArgument(0).setId(7L);
            return 1;
        }).when(mapper).insert(any(QuantProtectionOrder.class));
        // PAPER bypasses symbol rules; persistence ordering is shared with LIVE.
        service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), false);
    }

    @Test
    void timeoutKeepsSameIntentForRecoveryInsteadOfCreatingAnotherOrder() {
        QuantProtectionOrder intent = order();
        intent.setStatus("UNKNOWN");
        intent.setExchangeOrderId(null);
        when(mapper.selectOne(any())).thenReturn(intent);
        when(gateway.queryProtectionOrders(any(), any())).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void replacementIsConfirmedBeforeOldOrderIsCanceled() {
        QuantProtectionOrder old = order();
        old.setQuantity(new BigDecimal("2"));
        when(mapper.selectOne(any())).thenReturn(old);
        when(gateway.queryProtectionOrder(any(), eq(55L), any(), any(), any())).thenReturn(snapshot(55L, old.getClientOrderId(), new BigDecimal("2"), "NEW", null), snapshot(55L,
            old.getClientOrderId(), new BigDecimal("2"), "CANCELED", BigDecimal.ZERO));
        final ExchangeGateway.ProtectionOrderRequest[] request = new ExchangeGateway.ProtectionOrderRequest[1];
        when(gateway.placeProtectionOrder(any())).thenAnswer(invocation -> {
            request[0] = invocation.getArgument(0);
            return new ExchangeGateway.ProtectionOrderResult(66L, request[0].clientOrderId(), "NEW");
        });
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenAnswer(invocation -> snapshot(66L, request[0].clientOrderId(), BigDecimal.ONE, "NEW", null));
        service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true);
        var sequence = inOrder(gateway);
        sequence.verify(gateway).queryProtectionOrder(any(), eq(55L), any(), any(), any());
        sequence.verify(gateway).placeProtectionOrder(any());
        sequence.verify(gateway).queryProtectionOrder(any(), eq(66L), any(), any(), any());
        sequence.verify(gateway).cancelProtectionOrder(any());
    }

    @Test
    void unknownAcceptedOrderIsRecoveredWithoutResubmission() {
        QuantProtectionOrder local = order();
        local.setStatus("UNKNOWN");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        assertSame(local, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        assertEquals("WORKING", local.getStatus());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void explicitlyAbsentIntentRetriesThePersistedClientId() {
        QuantProtectionOrder local = order();
        local.setStatus("UNKNOWN");
        local.setExchangeOrderId(null);
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException()).thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        when(gateway.placeProtectionOrder(any())).thenReturn(new ExchangeGateway.ProtectionOrderResult(55L, local.getClientOrderId(), "NEW"));
        service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true);
        verify(gateway).placeProtectionOrder(argThat(request -> local.getClientOrderId().equals(request.clientOrderId())));
        verify(mapper, never()).insert(any(QuantProtectionOrder.class));
    }

    @Test
    void canceledFirstTpAndManualReductionDoNotConfirmTakeProfit() {
        when(mapper.selectOne(any())).thenReturn(order());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        position.setQuantity(new BigDecimal("0.5"));
        assertFalse(service.isFirstTakeProfitFilled(1L, position, BigDecimal.ONE));
    }

    @Test
    void onlyFinishedProtectionWithRealExecutedQuantityConfirmsTakeProfit() {
        QuantProtectionOrder target = order();
        target.setQuantity(new BigDecimal("0.5"));
        when(mapper.selectOne(any())).thenReturn(target);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", new BigDecimal("0.5"), "FINISHED", new BigDecimal("0.5")));
        position.setQuantity(new BigDecimal("0.5"));
        assertTrue(service.isFirstTakeProfitFilled(1L, position, BigDecimal.ONE));
    }

    @Test
    void triggeringFirstTpMustBlockLocalFallback() {
        QuantProtectionOrder local = order();
        local.setProtectionType("FIRST_TP");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "TRIGGERING", null));
        assertThrows(IllegalStateException.class, () -> service.isStrategyProtectionWorking(1L, position, "FIRST_TP", true));
    }

    @Test
    void failedCancellationAndEmptyOpenListCannotAuthorizePartialFallback() {
        when(mapper.selectOne(any())).thenReturn(order());
        doThrow(new IllegalStateException("already triggered")).when(gateway).cancelProtectionOrder(any());
        when(gateway.queryProtectionOrders(any(), any())).thenReturn(List.of());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "FINISHED", new BigDecimal("0.5")));
        assertFalse(service.cancelStrategyProtection(1L, position, "FIRST_TP"));
    }

    @Test
    void finishedFirstTpWaitsUntilPositionReflectsExecutedQuantity() {
        when(mapper.selectOne(any())).thenReturn(order());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", new BigDecimal("0.5"), "FINISHED", new BigDecimal("0.5")));
        assertFalse(service.isFirstTakeProfitFilled(1L, position, BigDecimal.ONE));
    }

    @Test
    void fixedHardStopDoesNotMoveWithMarginFluctuation() {
        QuantProtectionOrder local = order();
        local.setPositionSide("LONG");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        position.setMargin(new BigDecimal("12"));
        assertSame(local, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(gateway, never()).placeProtectionOrder(any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void triggeredProtectionPersistsTerminalChildExecutionBeforeBlockingFurtherSync() {
        QuantProtectionOrder local = order();
        local.setProtectionType("HARD_STOP");
        local.setClientOrderId("QHARD_test");
        local.setPositionSide("LONG");
        local.setPositionCycleKey("cycle-test");
        local.setQuantity(new BigDecimal("2"));
        ProtectionTargetCoordinator coordinator = mock(ProtectionTargetCoordinator.class);
        ProtectionTargetCoordinator.Lease lease = mock(ProtectionTargetCoordinator.Lease.class);
        when(coordinator.acquire(eq(1L), any(QuantPosition.class), eq("HARD_STOP"))).thenReturn(lease);
        when(lease.state()).thenReturn(new ProtectionTargetCoordinator.State(2, 0, 0, null, null, 7L,
            BigDecimal.ONE, new BigDecimal("99"), "STOP_MARKET"));
        var coordinatedService = new ProtectionOrderService(gateway, mapper, execution, coordinator);
        coordinatedService.setLiveOrderDispatcher(dispatcher);
        when(mapper.selectList(any())).thenReturn(List.of(local));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(
            new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 55L, "QHARD_test", "TRIGGERED", "SELL", "BOTH", "STOP_MARKET",
                BigDecimal.ONE, new BigDecimal("99"), Instant.now(), null, true, false, 91L, new BigDecimal("98")));
        when(gateway.queryOrder(any(), eq("BTCUSDT"), eq(91L), isNull())).thenReturn(
            new ExchangeGateway.OrderSnapshot("BTCUSDT", 91L, "QHARD_test", "SELL", "MARKET", "FILLED",
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Instant.now(), new BigDecimal("98")));

        assertThrows(ProtectionOrderService.ProtectionExecutionPendingException.class,
            () -> coordinatedService.isStrategyProtectionWorking(1L, position, "HARD_STOP", true));

        assertEquals("FINISHED", local.getStatus());
        assertEquals(BigDecimal.ONE, local.getExecutedQuantity());
        verify(mapper).updateById(local);
        verify(lease).markOrderTerminal(local);
    }

    private record TriggeredRecoveryFixture(ProtectionOrderService service, QuantProtectionOrder local,
                                            ProtectionTargetCoordinator.Lease lease) { }

    private TriggeredRecoveryFixture triggeredRecoveryFixture(String childClientOrderId, String childStatus, String childExecutedQuantity) {
        String clientId = "QHARD_triggered";
        QuantProtectionOrder local = order();
        local.setId(7L);
        local.setCredentialId(1L);
        local.setPositionId(10L);
        local.setPositionCycleKey("cycle-triggered");
        local.setSymbol("JCTUSDT");
        local.setClientOrderId(clientId);
        local.setExchangeOrderId(null);
        local.setProtectionType("HARD_STOP");
        local.setPositionSide("SHORT");
        local.setOrderType("STOP_MARKET");
        local.setQuantity(new BigDecimal("601369"));
        local.setStopPrice(new BigDecimal("0.0051900"));
        local.setExecutedQuantity(BigDecimal.ZERO);

        QuantProtectionOrder firstTp = new QuantProtectionOrder();
        firstTp.setId(6L);
        firstTp.setStatus("FINISHED");
        firstTp.setExecutedQuantity(new BigDecimal("300684"));
        when(mapper.selectOne(any())).thenReturn(local);
        when(mapper.selectList(any())).thenReturn(List.of(firstTp));
        when(dispatcher.activeDispatch(1L, "JCTUSDT")).thenReturn(
            new LiveOrderDispatcher.ActiveDispatch(1L, "JCTUSDT", "cancel-key", "PROTECT_CANCEL", clientId, "UNKNOWN", null));

        var algo = new ExchangeGateway.ProtectionOrderSnapshot("JCTUSDT", 1000000282028535L, clientId, "FINISHED",
            "BUY", "SHORT", "STOP_MARKET", new BigDecimal("300685"), new BigDecimal("0.0051900"), Instant.now(),
            new BigDecimal("300685"), true, false, 271997586L, new BigDecimal("0.0052680"));
        when(gateway.queryProtectionOrder(any(), org.mockito.ArgumentMatchers.nullable(Long.class), eq(clientId), eq("JCTUSDT"), org.mockito.ArgumentMatchers.nullable(Long.class))).thenReturn(algo, algo);
        var child = new ExchangeGateway.OrderSnapshot("JCTUSDT", 271997586L, childClientOrderId, "BUY", "MARKET", childStatus,
            BigDecimal.ZERO, new BigDecimal("300685"), new BigDecimal(childExecutedQuantity), Instant.now(), new BigDecimal("0.0052680"),
            "SHORT", true, Instant.now(), false);
        when(gateway.queryOrder(any(), eq("JCTUSDT"), eq(271997586L), isNull())).thenReturn(child);

        ProtectionTargetCoordinator coordinator = mock(ProtectionTargetCoordinator.class);
        ProtectionTargetCoordinator.Lease lease = mock(ProtectionTargetCoordinator.Lease.class);
        when(coordinator.acquire(eq(1L), any(QuantPosition.class), eq("HARD_STOP"))).thenReturn(lease);
        when(lease.state()).thenReturn(new ProtectionTargetCoordinator.State(2, 0, 0, 7L, null, null,
            new BigDecimal("300685"), new BigDecimal("0.0051900"), "STOP_MARKET"));
        var coordinatedService = new ProtectionOrderService(gateway, mapper, execution, coordinator);
        coordinatedService.setLiveOrderDispatcher(dispatcher);
        when(dispatcher.recoveredFromExchange(1L, "JCTUSDT", "cancel-key", clientId, 1000000282028535L)).thenReturn(true);
        return new TriggeredRecoveryFixture(coordinatedService, local, lease);
    }

    @Test
    void fullyFilledTriggeredAlgoPersistsChildExecutionAndReleasesExactDispatchGate() {
        var fixture = triggeredRecoveryFixture("QHARD_triggered", "FILLED", "300685");

        assertTrue(fixture.service().recoverDispatchBeforeStrategyStart(1L, "JCTUSDT"));

        assertEquals(1000000282028535L, fixture.local().getExchangeOrderId());
        assertEquals("FINISHED", fixture.local().getStatus());
        assertEquals(new BigDecimal("300685"), fixture.local().getExecutedQuantity());
        verify(mapper).updateById(fixture.local());
        verify(fixture.lease()).markOrderTerminal(fixture.local());
        verify(dispatcher).recoveredFromExchange(1L, "JCTUSDT", "cancel-key", "QHARD_triggered", 1000000282028535L);
    }

    @Test
    void triggeredAlgoWithWrongChildIdentityRemainsBlocked() {
        var fixture = triggeredRecoveryFixture("another-client-id", "FILLED", "300685");

        assertFalse(fixture.service().recoverDispatchBeforeStrategyStart(1L, "JCTUSDT"));

        verify(mapper, never()).updateById(fixture.local());
        verify(dispatcher, never()).recoveredFromExchange(any(), any(), any(), any(), any());
    }

    @Test
    void triggeredAlgoWithPartialChildFillRemainsBlocked() {
        var fixture = triggeredRecoveryFixture("QHARD_triggered", "PARTIALLY_FILLED", "100000");

        assertFalse(fixture.service().recoverDispatchBeforeStrategyStart(1L, "JCTUSDT"));

        verify(mapper, never()).updateById(fixture.local());
        verify(dispatcher, never()).recoveredFromExchange(any(), any(), any(), any(), any());
    }

    @Test
    void definitelyNotSentStepTargetReusesItsPersistedClientAlgoId() {
        position.setProtectionCycleKey("cycle-test");
        QuantProtectionOrder notSent = order();
        notSent.setId(77L);
        notSent.setCredentialId(1L);
        notSent.setPositionCycleKey("cycle-test");
        notSent.setPositionSide("LONG");
        notSent.setProtectionType("STEP_TP");
        notSent.setOrderType("STOP_MARKET");
        notSent.setClientOrderId("QSTEP_retry");
        notSent.setExchangeOrderId(null);
        notSent.setQuantity(BigDecimal.ONE);
        notSent.setStopPrice(new BigDecimal("99"));
        notSent.setStepIndex(1);
        notSent.setTargetVersion(1L);
        notSent.setExecutedQuantity(BigDecimal.ZERO);
        notSent.setStatus("NOT_SENT");

        java.util.concurrent.atomic.AtomicInteger queryCount = new java.util.concurrent.atomic.AtomicInteger();
        when(mapper.selectList(any())).thenAnswer(call -> switch (queryCount.getAndIncrement()) {
            case 1 -> List.of(notSent);
            default -> List.of();
        });
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(66L, "QSTEP_retry", BigDecimal.ONE, "NEW", null));

        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(new org.dromara.quant.strategy.ProtectionSignal("STEP_TP", "STOP_MARKET",
            BigDecimal.ONE, org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("99"), "test", 1)), true);

        verify(gateway).placeProtectionOrder(argThat(request -> "QSTEP_retry".equals(request.clientOrderId())));
        verify(mapper, never()).insert(any(QuantProtectionOrder.class));
        verify(mapper, atLeastOnce()).updateById(same(notSent));
    }

    @Test
    void stepStopNeverMovesBackwards() {
        QuantProtectionOrder local = order();
        local.setPositionSide("LONG");
        local.setProtectionType("STEP_TP");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(new org.dromara.quant.strategy.ProtectionSignal("STEP_TP", "STOP_MARKET", BigDecimal.ONE,
            org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("98"), "test")), true);
        verify(gateway, never()).placeProtectionOrder(any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void shortStepStopNeverMovesBackwards() {
        position.setPositionSide("SHORT");
        QuantProtectionOrder local = order();
        local.setPositionSide("SHORT");
        local.setProtectionType("STEP_TP");
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 55L, local.getClientOrderId(), "NEW",
            "BUY", "BOTH", "STOP_MARKET", BigDecimal.ONE, new BigDecimal("99"), Instant.now(), null));
        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(new org.dromara.quant.strategy.ProtectionSignal("STEP_TP", "STOP_MARKET", BigDecimal.ONE,
            org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("100"), "test")), true);
        verify(gateway, never()).placeProtectionOrder(any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void localStopFailureDoesNotSkipStepProtectionAndKeepsFailureVisible() {
        QuantProtectionOrder unknownLocalStop = order();
        unknownLocalStop.setProtectionType("LOCAL_STOP");
        unknownLocalStop.setStatus("UNKNOWN");
        when(mapper.selectOne(any())).thenReturn(unknownLocalStop).thenReturn(null);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("LOCAL_STOP status unknown")).thenAnswer(call -> new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 56L, call.getArgument(2), "NEW", "SELL", "BOTH", "STOP_MARKET", BigDecimal.ONE, new BigDecimal("99"), Instant.now(), null));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(56L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));

        assertThrows(IllegalStateException.class, () -> service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(new org.dromara.quant.strategy.ProtectionSignal(
            "LOCAL_STOP", "STOP_MARKET", BigDecimal.ONE, org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT, new BigDecimal("95"), "test"),
            new org.dromara.quant.strategy.ProtectionSignal("STEP_TP", "STOP_MARKET", BigDecimal.ONE, org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT,
                new BigDecimal("99"), "test", 2)), true));

        verify(gateway).placeProtectionOrder(argThat(request -> request.clientOrderId().startsWith("QSTEP_")));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void explicitlyAbsentFirstTpDoesNotAbortPositionRecovery() {
        when(mapper.selectOne(any())).thenReturn(order());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException());
        assertFalse(service.isFirstTakeProfitFilled(1L, position, BigDecimal.ONE));
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void protectionUsesActualEnvironmentRuleInsteadOfLocalSymbolRule() {
        when(gateway.queryTradingRule(any(), any())).thenReturn(new ExchangeGateway.SymbolRule("BTCUSDT", "USDT", 2, 0, new BigDecimal("0.1"), BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, "TRADING"));
        position.setQuantity(new BigDecimal("11233"));
        when(gateway.placeProtectionOrder(any())).thenAnswer(i -> new ExchangeGateway.ProtectionOrderResult(55L,
            i.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(i -> snapshot(55L, i.getArgument(2), new BigDecimal("5616"), "NEW", null));
        service.syncStrategyProtections(1L, position, position.getQuantity(), List.of(new org.dromara.quant.strategy.ProtectionSignal("FIRST_TP", "STOP_MARKET", new BigDecimal(
            "0.5"), org.dromara.quant.strategy.ProtectionSignal.QuantityBase.INITIAL, new BigDecimal("99"), "test")), true);
        verify(gateway).placeProtectionOrder(argThat(r -> r.quantity().compareTo(new BigDecimal("5616")) == 0));
        verify(execution, never()).requireSymbol(any());
    }

    @Test
    void absentRetryPersistsNormalizedQuantityBeforeResendingSameId() {
        QuantProtectionOrder local = order();
        local.setStatus("UNKNOWN");
        local.setQuantity(new BigDecimal("1.5"));
        local.setPositionSide("LONG");
        local.setExchangeOrderId(null);
        position.setQuantity(new BigDecimal("2"));
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryTradingRule(any(), any())).thenReturn(new ExchangeGateway.SymbolRule("BTCUSDT", "USDT", 2, 0, new BigDecimal("0.01"), BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ONE, "TRADING"));
        when(mapper.updateById(any(QuantProtectionOrder.class))).thenReturn(1);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException()).thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        when(gateway.placeProtectionOrder(any())).thenAnswer(i -> {
            assertEquals(0, local.getQuantity().compareTo(BigDecimal.ONE));
            verify(mapper, atLeastOnce()).updateById(local);
            throw new IllegalStateException("timeout after submit");
        });
        assertThrows(IllegalStateException.class, () -> service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(gateway).placeProtectionOrder(argThat(r -> r.clientOrderId().equals(local.getClientOrderId()) && r.quantity().compareTo(BigDecimal.ONE) == 0));
        assertEquals("UNKNOWN", local.getStatus());
    }

    @Test
    void definiteRejectionIsNotRecordedAsUnknown() {
        when(gateway.placeProtectionOrder(any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.OrderRejectedException(-4023, "step"));
        assertThrows(IllegalStateException.class, () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(mapper, atLeastOnce()).updateById(argThat((QuantProtectionOrder o) -> "REJECTED".equals(o.getStatus())));
    }

    @Test
    void duplicateClientIdMustConfirmOriginalIntentWithoutAnotherSubmission() {
        when(gateway.placeProtectionOrder(any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ExchangeRequestException("PLACE_PROTECTION_ORDER", 400, -4116,
            "ClientOrderId is duplicated"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(invocation -> snapshot(55L, invocation.getArgument(2), BigDecimal.ONE, "NEW", null));
        var result = service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true);
        assertEquals("WORKING", result.getStatus());
        assertEquals(55L, result.getExchangeOrderId());
        verify(gateway, times(1)).placeProtectionOrder(any());
        verify(mapper, times(1)).insert(any(QuantProtectionOrder.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void duplicateSubmissionWithUnconfirmedFieldsMustRemainUnknown(boolean queryFails) {
        when(gateway.placeProtectionOrder(any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ExchangeRequestException("PLACE_PROTECTION_ORDER", 400, -4116,
            "ClientOrderId is duplicated"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            if (queryFails) {
                throw new IllegalStateException("query timeout");
            }
            return snapshot(55L, invocation.getArgument(2), new BigDecimal("2"), "NEW", null);
        });
        assertThrows(IllegalStateException.class, () -> service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        verify(mapper, atLeastOnce()).updateById(argThat((QuantProtectionOrder o) -> "UNKNOWN".equals(o.getStatus())));
        verify(gateway, times(1)).placeProtectionOrder(any());
    }

    @Test
    void recoveredIntentDuplicateSubmissionMustQuerySameClientId() {
        var local = order();
        local.setStatus("UNKNOWN");
        local.setExchangeOrderId(null);
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException()).thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        when(gateway.placeProtectionOrder(any())).thenThrow(new org.dromara.quant.exchange.ExchangeTradingPort.ExchangeRequestException("PLACE_PROTECTION_ORDER", 400, -4116,
            "ClientOrderId is duplicated"));
        assertEquals(local, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        assertEquals("WORKING", local.getStatus());
        verify(gateway, times(1)).placeProtectionOrder(argThat(r -> r.clientOrderId().equals(local.getClientOrderId())));
        verify(mapper, never()).insert(any(QuantProtectionOrder.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"TESTNET", "LIVE"})
    void hardStopUsesSameCalculationAndActualEnvironment(String mode) {
        position.setMargin(new BigDecimal("30")); // 追加保证金不改变冻结杠杆回报率触发价。
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", mode));
        when(gateway.placeProtectionOrder(any())).thenAnswer(invocation -> new ExchangeGateway.ProtectionOrderResult(55L,
            invocation.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(invocation -> snapshot(55L, invocation.getArgument(2), BigDecimal.ONE, "NEW", null));
        var result = service.placeMarginHardStop(1L, position, new BigDecimal("0.1"), true);
        assertEquals("WORKING", result.getStatus());
        verify(gateway).placeProtectionOrder(argThat(r -> mode.equals(r.credential().runMode()) && r.live() && r.quantity().compareTo(BigDecimal.ONE) == 0 && r.stopPrice().compareTo(new BigDecimal("99")) == 0));
    }

    @Test
    void partialFinishedFirstProfitCannotEnableStep() {
        when(mapper.selectOne(any())).thenReturn(order());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "FINISHED", new BigDecimal("0.2")));
        position.setQuantity(new BigDecimal("0.8"));
        assertFalse(service.isFirstTakeProfitFilled(1L, position, BigDecimal.ONE));
    }

    @Test
    void firstProfitProgressAccumulatesDistinctOrdersButNotRepeatedQueries() {
        position.setProtectionCycleKey("cycle");
        QuantProtectionOrder one = order();
        one.setClientOrderId("one");
        one.setExecutedQuantity(BigDecimal.ZERO);
        QuantProtectionOrder two = order();
        two.setId(8L);
        two.setClientOrderId("two");
        two.setExchangeOrderId(56L);
        two.setExecutedQuantity(BigDecimal.ZERO);
        when(mapper.selectList(any())).thenReturn(List.of(one, two));
        when(gateway.queryProtectionOrder(any(), eq(55L), any(), any(), any())).thenReturn(snapshot(55L, "one", BigDecimal.ONE, "CANCELED", new BigDecimal("0.2")));
        when(gateway.queryProtectionOrder(any(), eq(56L), any(), any(), any())).thenReturn(snapshot(56L, "two", BigDecimal.ONE, "FINISHED", new BigDecimal("0.3")));
        assertEquals(0, new BigDecimal("0.5").compareTo(service.firstTakeProfitExecutedQuantity(1L, position)));
        assertEquals(0, new BigDecimal("0.5").compareTo(service.firstTakeProfitExecutedQuantity(1L, position)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"NOT_SENT", "PREPARED", "REJECTED"})
    void firstProfitNeverSentIntentsContributeZeroWithoutExchangeLookup(String status) {
        QuantProtectionOrder local = order();
        local.setStatus(status);
        local.setExchangeOrderId(null);
        local.setExecutedQuantity(BigDecimal.ZERO);
        when(mapper.selectList(any())).thenReturn(List.of(local));

        assertEquals(0, BigDecimal.ZERO.compareTo(service.firstTakeProfitExecutedQuantity(1L, position)));

        verify(gateway, never()).queryProtectionOrder(any(), any(), any(), any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"UNKNOWN", "DISPATCHING", "PENDING"})
    void firstProfitUncertainIntentsWithNullLookupRemainPending(String status) {
        QuantProtectionOrder local = order();
        local.setStatus(status);
        local.setExchangeOrderId(null);
        when(mapper.selectList(any())).thenReturn(List.of(local));

        assertThrows(ProtectionOrderService.ProtectionExecutionPendingException.class,
            () -> service.firstTakeProfitExecutedQuantity(1L, position));

        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void cancellationTimeoutCannotAuthorizeLocalClose() {
        position.setProtectionCycleKey("cycle");
        when(mapper.selectList(any())).thenReturn(List.of(order()));
        doThrow(new IllegalStateException("timeout")).when(gateway).cancelProtectionOrder(any());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "one", BigDecimal.ONE, "NEW", BigDecimal.ZERO));
        assertFalse(service.quiesceCycleProtections(1L, position));
    }

    @Test
    void terminalPartialFillIsPreservedBeforeLocalSupplement() {
        position.setProtectionCycleKey("cycle");
        QuantProtectionOrder one = order();
        when(mapper.selectList(any())).thenReturn(List.of(one));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, "QFTP_test", BigDecimal.ONE, "CANCELED", new BigDecimal("0.2")));
        assertTrue(service.quiesceCycleProtections(1L, position));
        assertEquals(0, new BigDecimal("0.2").compareTo(one.getExecutedQuantity()));
    }

    @Test
    void finishedAlgoWithActiveChildOrderMustNotAuthorizeLocalFallback() {
        position.setProtectionCycleKey("cycle");
        when(mapper.selectList(any())).thenReturn(List.of(order()));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", 55L, "one", "FINISHED", "SELL", "BOTH",
            "STOP_MARKET", BigDecimal.ONE, new BigDecimal("99"), Instant.now(), BigDecimal.ZERO, true, false, 99L, null));
        when(gateway.queryOrder(any(), any(), eq(99L), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 99L, "child", "SELL", "MARKET", "NEW", BigDecimal.ZERO,
            BigDecimal.ONE, BigDecimal.ZERO, Instant.now()));
        assertThrows(ProtectionOrderService.ProtectionExecutionPendingException.class, () -> service.quiesceCycleProtections(1L, position));
    }

    @Test
    void unknownCancellationAndAbsentOpenListMustRetainCleanup() {
        when(mapper.selectList(any())).thenReturn(List.of(order()));
        doThrow(new IllegalStateException("timeout")).when(gateway).cancelProtectionOrder(any());
        when(gateway.queryProtectionOrders(any(), any())).thenReturn(List.of());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("timeout"));
        assertFalse(service.cancelActiveProtectionOrders(1L, position.getId(), "cycle", true));
        verify(gateway, never()).queryProtectionOrders(any(), any());
    }

    @Test
    void missingLocalProtectionRecoversMatchingUnknownCancelOnlyFromExactTerminalFact() {
        QuantProtectionOrder local = order();
        local.setStatus("MISSING");
        when(mapper.selectList(any())).thenReturn(List.of(local));
        when(dispatcher.activeProtectionTicket(1L, "BTCUSDT", local.getClientOrderId(), "PROTECT_CANCEL")).thenReturn("pending-protection-cancel");
        when(gateway.queryProtectionOrder(any(), eq(55L), eq(local.getClientOrderId()), eq("BTCUSDT"), any()))
            .thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        when(dispatcher.recoveredFromExchange(1L, "BTCUSDT", "pending-protection-cancel", local.getClientOrderId(), 55L)).thenReturn(true);

        assertTrue(service.cancelActiveProtectionOrders(1L, position.getId(), "cycle", true));

        assertEquals("CANCELED", local.getStatus());
        assertEquals(55L, local.getExchangeOrderId());
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(dispatcher).recoveredFromExchange(1L, "BTCUSDT", "pending-protection-cancel", local.getClientOrderId(), 55L);
    }

    @Test
    void legacyMissingProtectionWithoutCancelTicketStillNeedsExactTerminalProof() {
        var local = order();
        local.setStatus("MISSING");
        when(mapper.selectList(any())).thenReturn(List.of(local));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(null);

        assertFalse(service.cancelActiveProtectionOrders(1L, 10L, true));

        verify(gateway).queryProtectionOrder(any(), eq(local.getExchangeOrderId()), eq(local.getClientOrderId()), eq("BTCUSDT"), any());
        verify(dispatcher).unknown(any(LiveOrderDispatcher.Ticket.class));
        verify(dispatcher, never()).acknowledged(any(), any());
    }

    @Test
    void blockedCancelDispatchReturnsPendingWithoutSendingBinanceRequest() {
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", "TESTNET"));
        QuantProtectionOrder local = order();
        when(mapper.selectList(any())).thenReturn(List.of(local));
        doThrow(new LiveOrderDispatcher.DispatchBlockedException("同账户交易对存在未完成交易所操作，订单未发送"))
            .when(dispatcher).begin(eq(1L), eq("BTCUSDT"), anyString(), eq("PROTECT_CANCEL"), eq(local.getClientOrderId()), isNull(), isNull());

        assertFalse(service.cancelActiveProtectionOrders(1L, position.getId(), "cycle", true));

        assertEquals("WORKING", local.getStatus());
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(dispatcher, never()).unknown(any());
    }

    @Test
    void startupRecoversOnlyExactUnfilledProtectionCancelBeforeAllowingStrategy() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        local.setStatus("CANCELED");
        var active = new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "cancel-key", "PROTECT_CANCEL", local.getClientOrderId(), "UNKNOWN", null);
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(active);
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), eq(55L), eq(local.getClientOrderId()), eq("BTCUSDT"), any()))
            .thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        when(dispatcher.recoveredFromExchange(1L, "BTCUSDT", "cancel-key", local.getClientOrderId(), 55L)).thenReturn(true);

        assertTrue(service.recoverDispatchBeforeStrategyStart(1L, "BTCUSDT"));

        verify(mapper).updateById(local);
        verify(dispatcher).recoveredFromExchange(1L, "BTCUSDT", "cancel-key", local.getClientOrderId(), 55L);
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void startupRemainsBlockedWhenExactProtectionCancelCannotBeConfirmed() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(
            new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "cancel-key", "PROTECT_CANCEL", local.getClientOrderId(), "UNKNOWN", null));
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("timeout"));

        assertFalse(service.recoverDispatchBeforeStrategyStart(1L, "BTCUSDT"));

        verify(dispatcher, never()).recoveredFromExchange(any(), any(), any(), any(), any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void unresolvedCancelRetriesSameExactOrderOnlyAfterExchangeConfirmsItIsStillWorking() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        local.setPositionSide("LONG");
        local.setStatus("WORKING");
        when(mapper.selectOne(any())).thenReturn(local);
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(
            new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "cancel-key", "PROTECT_CANCEL", local.getClientOrderId(), "UNKNOWN", null));
        when(gateway.queryProtectionOrder(any(), eq(55L), eq(local.getClientOrderId()), eq("BTCUSDT"), any()))
            .thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO),
                snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        when(dispatcher.recoveredFromExchange(1L, "BTCUSDT", "cancel-key", local.getClientOrderId(), 55L)).thenReturn(true);

        assertEquals(1, service.recoverUnresolvedProtectionDispatches(1L, "BTCUSDT"));

        var request = org.mockito.ArgumentCaptor.forClass(ExchangeGateway.CancelProtectionOrderRequest.class);
        verify(gateway).cancelProtectionOrder(request.capture());
        assertEquals(55L, request.getValue().orderId());
        assertEquals(local.getClientOrderId(), request.getValue().clientOrderId());
        verify(dispatcher).recoveredFromExchange(1L, "BTCUSDT", "cancel-key", local.getClientOrderId(), 55L);
    }

    @Test
    void startupRecoversExactProtectionCreateTicketAfterPersistingExchangeOrder() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        local.setStatus("UNKNOWN");
        var active = new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "create-key", "PROTECT_CREATE", local.getClientOrderId(), "UNKNOWN", 55L);
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(active, (LiveOrderDispatcher.ActiveDispatch) null);
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), eq(55L), eq(local.getClientOrderId()), eq("BTCUSDT"), any()))
            .thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        when(dispatcher.recoverVerifiedProtection(1L, "BTCUSDT", local.getClientOrderId(), "PROTECT_CREATE", 55L)).thenReturn(true);

        assertTrue(service.recoverDispatchBeforeStrategyStart(1L, "BTCUSDT"));

        assertEquals("WORKING", local.getStatus());
        verify(dispatcher).recoverVerifiedProtection(1L, "BTCUSDT", local.getClientOrderId(), "PROTECT_CREATE", 55L);
    }

    @Test
    void privateStreamRecoveryChecksActiveProtectionCreateTicket() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        local.setStatus("UNKNOWN");
        var active = new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "create-key", "PROTECT_CREATE", local.getClientOrderId(), "UNKNOWN", 55L);
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(active, (LiveOrderDispatcher.ActiveDispatch) null);
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), eq(55L), eq(local.getClientOrderId()), eq("BTCUSDT"), any()))
            .thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "NEW", null));
        when(dispatcher.recoverVerifiedProtection(1L, "BTCUSDT", local.getClientOrderId(), "PROTECT_CREATE", 55L)).thenReturn(true);

        assertEquals(1, service.recoverUnresolvedProtectionDispatches(1L, "BTCUSDT"));

        verify(dispatcher).recoverVerifiedProtection(1L, "BTCUSDT", local.getClientOrderId(), "PROTECT_CREATE", 55L);
    }

    @Test
    void startupKeepsProtectionCreateTicketBlockedWhenExactOrderCannotBeConfirmed() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        local.setStatus("UNKNOWN");
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(
            new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "create-key", "PROTECT_CREATE", local.getClientOrderId(), "UNKNOWN", 55L));
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("timeout"));

        assertFalse(service.recoverDispatchBeforeStrategyStart(1L, "BTCUSDT"));

        verify(dispatcher, never()).recoverVerifiedProtection(any(), any(), any(), any(), any());
    }

    @Test
    void startupDoesNotReleaseTerminalCreateTicketWhenLocalFactPersistenceFails() {
        QuantProtectionOrder local = order();
        local.setCredentialId(1L);
        local.setStatus("UNKNOWN");
        when(dispatcher.activeDispatch(1L, "BTCUSDT")).thenReturn(
            new LiveOrderDispatcher.ActiveDispatch(1L, "BTCUSDT", "create-key", "PROTECT_CREATE", local.getClientOrderId(), "UNKNOWN", 55L));
        when(mapper.selectOne(any())).thenReturn(local);
        when(gateway.queryProtectionOrder(any(), eq(55L), eq(local.getClientOrderId()), eq("BTCUSDT"), any()))
            .thenReturn(snapshot(55L, local.getClientOrderId(), BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        when(mapper.updateById(local)).thenReturn(0);

        assertFalse(service.recoverDispatchBeforeStrategyStart(1L, "BTCUSDT"));

        verify(dispatcher, never()).recoverVerifiedProtection(any(), any(), any(), any(), any());
    }

    @Test
    void firstProfitReplacementWaitsForOldFillReconciliation() {
        QuantProtectionOrder old = order();
        old.setProtectionType("FIRST_TP");
        old.setPositionSide("LONG");
        when(mapper.selectOne(any())).thenReturn(old);
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, old.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO), snapshot(55L,
            old.getClientOrderId(), BigDecimal.ONE, "CANCELED", new BigDecimal("0.2")));
        assertThrows(ProtectionOrderService.ProtectionExecutionPendingException.class, () -> service.syncStrategyProtections(1L, position, BigDecimal.ONE,
            List.of(new org.dromara.quant.strategy.ProtectionSignal("FIRST_TP", "STOP_MARKET", new BigDecimal("0.5"),
                org.dromara.quant.strategy.ProtectionSignal.QuantityBase.INITIAL, new BigDecimal("99"), "test")), true));
        verify(gateway, never()).placeProtectionOrder(any());
        assertEquals(0, new BigDecimal("0.2").compareTo(old.getExecutedQuantity()));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LIVE", "TESTNET"})
    void confirmedHardStopCoversIdenticalLocalMirror(String mode) {
        when(execution.toCredential(credential)).thenReturn(new ExchangeGateway.Credential("test", "test", "HMAC", mode));
        var hard = scopedStop("HARD_STOP", "99");
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(hard)));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, hard.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO));

        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(stopSignal("LOCAL_STOP", "99")), true);

        verify(gateway, never()).placeProtectionOrder(any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LONG", "SHORT"})
    void confirmedStepReplacesWeakerStopsAndIsReusedForHardStop(String direction) {
        position.setPositionSide(direction);
        var step = scopedStop("STEP_TP", "100");
        step.setStepIndex(1);
        var hard = scopedStop("HARD_STOP", "LONG".equals(direction) ? "99" : "101");
        hard.setId(9L);
        hard.setExchangeOrderId(56L);
        hard.setClientOrderId("QHARD_old");
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, "WORKING".equals(hard.getStatus()) ? List.of(step, hard) : List.of(step)));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(call -> {
            var local = Long.valueOf(55L).equals(call.getArgument(1)) ? step : hard;
            return new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", local.getExchangeOrderId(), local.getClientOrderId(),
                "CANCELED".equals(local.getStatus()) ? "CANCELED" : "NEW", "SHORT".equals(direction) ? "BUY" : "SELL", "BOTH", "STOP_MARKET",
                BigDecimal.ONE, local.getStopPrice(), Instant.now(), BigDecimal.ZERO);
        });
        doAnswer(call -> {
            hard.setStatus("CANCELED");
            return null;
        }).when(gateway).cancelProtectionOrder(any());

        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(stopSignal("STEP_TP", "100")), true);
        assertSame(step, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        verify(gateway).cancelProtectionOrder(argThat(request -> hard.getClientOrderId().equals(request.clientOrderId())));
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void unknownCoverCannotRemoveExistingHardStop() {
        var step = scopedStop("STEP_TP", "100");
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(step)));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        verify(gateway, never()).cancelProtectionOrder(any());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"cycle", "direction", "quantity", "price", "account", "position", "symbol"})
    void differentScopeOrInsufficientProtectionCannotCoverHardStop(String mismatch) {
        var step = scopedStop("STEP_TP", "100");
        switch (mismatch) {
            case "cycle" -> step.setPositionCycleKey("old-cycle");
            case "direction" -> step.setPositionSide("SHORT");
            case "quantity" -> step.setQuantity(new BigDecimal("0.5"));
            case "price" -> step.setStopPrice(new BigDecimal("98"));
            case "account" -> step.setCredentialId(2L);
            case "position" -> step.setPositionId(20L);
            case "symbol" -> step.setSymbol("ETHUSDT");
        }
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(step)));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenAnswer(call -> snapshot(66L, call.getArgument(2), BigDecimal.ONE, "NEW", BigDecimal.ZERO));

        assertNotSame(step, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));

        verify(gateway).placeProtectionOrder(any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void reusedStepCannotBeRemovedWithoutReplacementProtection() {
        var step = scopedStop("STEP_TP", "100");
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(step)));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 55L, step.getClientOrderId(), "NEW", "SELL", "BOTH", "STOP_MARKET", BigDecimal.ONE, new BigDecimal("100"), Instant.now(), BigDecimal.ZERO));

        assertSame(step, service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true));
        assertThrows(IllegalStateException.class, () -> service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(), true));

        verify(gateway, never()).cancelProtectionOrder(any());
        verify(gateway, never()).placeProtectionOrder(any());
    }

    @Test
    void failedStepConfirmationKeepsExistingHardStop() {
        var hard = scopedStop("HARD_STOP", "99");
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(hard)));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(stopSignal("STEP_TP", "100")), true));

        verify(gateway, never()).cancelProtectionOrder(any());
        assertEquals("WORKING", hard.getStatus());
    }

    @Test
    void firstProfitRemainsIndependentOfFullPositionStop() {
        var hard = scopedStop("HARD_STOP", "99");
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(hard)));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 66L, call.getArgument(2), "NEW", "SELL", "BOTH", "TAKE_PROFIT_MARKET", new BigDecimal("0.5"), new BigDecimal("101"), Instant.now(), BigDecimal.ZERO));
        var first = new org.dromara.quant.strategy.ProtectionSignal("FIRST_TP", "TAKE_PROFIT_MARKET", new BigDecimal("0.5"),
            org.dromara.quant.strategy.ProtectionSignal.QuantityBase.INITIAL, new BigDecimal("101"), "first profit");

        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(first), true);

        verify(gateway).placeProtectionOrder(argThat(request -> "TAKE_PROFIT_MARKET".equals(request.orderType()) && new BigDecimal("0.5").compareTo(request.quantity()) == 0));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void entryCleanupDoesNotCancelProtectionWhileEitherSideIsOpen() {
        when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of(new ExchangeGateway.PositionSnapshot(
            "BTCUSDT", "SHORT", BigDecimal.ONE, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ZERO, null, BigDecimal.TEN, 10, null, null)));
        Boolean ready = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareEntryProtectionCleanup", 1L, "BTCUSDT", true);
        assertFalse(ready);
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void entryCleanupPreservesManualOrdersButBlocksUnownedSystemIds() {
        when(gateway.queryPositions(any(), eq("BTCUSDT"))).thenReturn(List.of());
        when(mapper.selectList(any())).thenReturn(List.of());
        when(gateway.queryProtectionOrders(any(), any())).thenReturn(List.of(snapshot(55L, "manual-order", BigDecimal.ONE, "NEW", BigDecimal.ZERO)));
        assertEquals(Boolean.TRUE, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareEntryProtectionCleanup", 1L, "BTCUSDT", true));
        when(gateway.queryProtectionOrders(any(), any())).thenReturn(List.of(snapshot(55L, "QHARD_unowned", BigDecimal.ONE, "NEW", BigDecimal.ZERO)));
        assertEquals(Boolean.FALSE, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareEntryProtectionCleanup", 1L, "BTCUSDT", true));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void localFirstProfitFallbackRetainsWorkingStops() {
        var hard = scopedStop("HARD_STOP", "99");
        when(mapper.selectList(any())).thenReturn(List.of(hard));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(snapshot(55L, hard.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO));
        assertEquals(Boolean.TRUE, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareLocalExit", 1L, position, "FIRST_TP"));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void localHardStopFallbackCancelsOnlyItsMatchingExchangeProtection() {
        var hard = scopedStop("HARD_STOP", "99");
        var first = scopedStop("FIRST_TP", "101");
        when(mapper.selectList(any())).thenReturn(List.of(hard, first));
        when(gateway.queryProtectionOrder(any(), any(), eq(hard.getClientOrderId()), any(), any())).thenReturn(
            snapshot(55L, hard.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO),
            snapshot(55L, hard.getClientOrderId(), BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));
        when(gateway.queryProtectionOrder(any(), any(), eq(first.getClientOrderId()), any(), any())).thenReturn(
            snapshot(56L, first.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO));

        assertTrue(service.prepareLocalExit(1L, position, "HARD_STOP"));

        verify(gateway).cancelProtectionOrder(argThat(request -> hard.getClientOrderId().equals(request.clientOrderId())));
        verify(gateway, never()).cancelProtectionOrder(argThat(request -> first.getClientOrderId().equals(request.clientOrderId())));
    }

    @Test
    void localStepTakeProfitFallbackCancelsStepOrderAndRetainsHardStop() {
        var hard = scopedStop("HARD_STOP", "99");
        var step = scopedStop("STEP_TP", "101");
        when(mapper.selectList(any())).thenReturn(List.of(hard, step));
        when(gateway.queryProtectionOrder(any(), any(), eq(hard.getClientOrderId()), any(), any())).thenReturn(
            snapshot(55L, hard.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO));
        when(gateway.queryProtectionOrder(any(), any(), eq(step.getClientOrderId()), any(), any())).thenReturn(
            snapshot(56L, step.getClientOrderId(), BigDecimal.ONE, "NEW", BigDecimal.ZERO),
            snapshot(56L, step.getClientOrderId(), BigDecimal.ONE, "CANCELED", BigDecimal.ZERO));

        assertTrue(service.prepareLocalExit(1L, position, "STEP_TP"));

        verify(gateway).cancelProtectionOrder(argThat(request -> step.getClientOrderId().equals(request.clientOrderId())));
        verify(gateway, never()).cancelProtectionOrder(argThat(request -> hard.getClientOrderId().equals(request.clientOrderId())));
    }

    @Test
    void entryCleanupDoesNotTreatImportedManualLedgerAsSystemOwnership() {
        var manual = order();
        manual.setProtectionType("EXCHANGE_CONDITIONAL");
        manual.setClientOrderId("manual-order");
        when(mapper.selectList(any())).thenReturn(List.of(manual));
        when(gateway.queryPositions(any(), any())).thenReturn(List.of());
        when(gateway.queryProtectionOrders(any(), any())).thenReturn(List.of(snapshot(55L, "manual-order", BigDecimal.ONE, "NEW", BigDecimal.ZERO)));
        assertEquals(Boolean.TRUE, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareEntryProtectionCleanup", 1L, "BTCUSDT", true));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void unifiedPolicyNeverReplacesHardStopWithStep() {
        var step = scopedStop("STEP_TP", "100");
        position.setProtectionPolicyVersion(2);
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(step)));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenAnswer(call -> snapshot(66L, call.getArgument(2), BigDecimal.ONE, "NEW", BigDecimal.ZERO));
        var hard = service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true);
        assertEquals("HARD_STOP", hard.getProtectionType());
        verify(gateway).placeProtectionOrder(any());
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void localExitWaitsForProtectionChildTerminalWithoutCancelingStops() {
        var hard = scopedStop("HARD_STOP", "99");
        when(mapper.selectList(any())).thenReturn(List.of(hard));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenReturn(new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 55L, hard.getClientOrderId(), "TRIGGERED", "SELL", "BOTH", "STOP_MARKET", BigDecimal.ONE,
            new BigDecimal("99"), Instant.now(), BigDecimal.ZERO, true, false, 777L, null));
        when(gateway.queryOrder(any(), any(), eq(777L), any())).thenReturn(new ExchangeGateway.OrderSnapshot("BTCUSDT", 777L,
            "child", "SELL", "MARKET", "PARTIALLY_FILLED", BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("0.2"), Instant.now()));
        assertEquals(Boolean.FALSE, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareLocalExit", 1L, position, "HARD_STOP"));
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void replacesHardStopWithRemainingPositionQuantityAfterPartialExit() {
        position.setQuantity(new BigDecimal("0.8"));
        var old = scopedStop("HARD_STOP", "99");
        old.setClientOrderId("QHARD_old");
        old.setQuantity(BigDecimal.ONE);
        java.util.concurrent.atomic.AtomicInteger oldOrderQueries = new java.util.concurrent.atomic.AtomicInteger();
        when(mapper.selectList(any())).thenAnswer(call -> "WORKING".equals(old.getStatus()) ? List.of(old) : List.of());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(call -> {
            String clientOrderId = call.getArgument(2);
            if (old.getClientOrderId().equals(clientOrderId)) {
                boolean firstQuery = oldOrderQueries.getAndIncrement() == 0;
                return snapshot(55L, clientOrderId, BigDecimal.ONE, firstQuery ? "NEW" : "CANCELED", BigDecimal.ZERO);
            }
            return snapshot(66L, clientOrderId, new BigDecimal("0.8"), "NEW", BigDecimal.ZERO);
        });
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));

        QuantProtectionOrder replacement = service.ensureMarginHardStop(1L, position, new BigDecimal("0.1"), true);

        assertEquals(0, new BigDecimal("0.8").compareTo(replacement.getQuantity()));
        verify(gateway).placeProtectionOrder(argThat(request -> new BigDecimal("0.8").compareTo(request.quantity()) == 0));
        assertEquals("CANCELED", old.getStatus());
    }

    @Test
    void replacesStepTakeProfitWithRemainingPositionQuantityAfterPartialExit() {
        position.setQuantity(new BigDecimal("0.8"));
        var old = scopedStop("STEP_TP", "105");
        old.setClientOrderId("QSTEP_old");
        old.setQuantity(BigDecimal.ONE);
        old.setStepIndex(1);
        java.util.concurrent.atomic.AtomicInteger oldOrderQueries = new java.util.concurrent.atomic.AtomicInteger();
        when(mapper.selectList(any())).thenAnswer(call -> "WORKING".equals(old.getStatus()) ? List.of(old) : List.of());
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(call -> {
            String clientOrderId = call.getArgument(2);
            boolean previous = old.getClientOrderId().equals(clientOrderId);
            boolean firstQuery = !previous || oldOrderQueries.getAndIncrement() == 0;
            return new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", previous ? 55L : 66L, clientOrderId,
                previous && !firstQuery ? "CANCELED" : "NEW", "SELL", "BOTH", "STOP_MARKET",
                previous ? BigDecimal.ONE : new BigDecimal("0.8"), new BigDecimal("105"), Instant.now(), BigDecimal.ZERO);
        });
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));

        service.syncStrategyProtections(1L, position, BigDecimal.ONE, List.of(stopSignal("STEP_TP", "105")), null, true);

        verify(gateway).placeProtectionOrder(argThat(request -> new BigDecimal("0.8").compareTo(request.quantity()) == 0));
        assertEquals("CANCELED", old.getStatus());
    }

    @Test
    void unifiedStopPriceRoundsLongUpAndKeepsLegacyFloor() {
        position.setEntryPrice(new BigDecimal("100.003"));
        position.setLeverage(5);
        position.setProtectionPolicyVersion(2);
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), any(), any(), any(), any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 66L, call.getArgument(2), "NEW", "SELL", "BOTH", "STOP_MARKET", BigDecimal.ONE, new BigDecimal("99.01"), Instant.now(), BigDecimal.ZERO));
        var hard = service.ensureMarginHardStop(1L, position, new BigDecimal("0.05"), true);
        assertEquals(0, new BigDecimal("99.01").compareTo(hard.getStopPrice()));
    }

    @Test
    void entryCleanupWaitsForOrdinaryOpenOrdersWithoutCancelingManualOrders() {
        when(gateway.queryPositions(any(), any())).thenReturn(List.of());
        when(gateway.queryOrders(any(), any())).thenReturn(List.of(new ExchangeGateway.OrderSnapshot("BTCUSDT", 123L, "manual-limit",
            "BUY", "LIMIT", "NEW", new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, Instant.now())));
        assertEquals(Boolean.FALSE, org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "prepareEntryProtectionCleanup", 1L, "BTCUSDT", true));
        verify(gateway, never()).cancelProtectionOrder(any());
        verify(gateway, never()).cancelOrder(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"0.3", "0.8"})
    void unifiedFirstProfitDeductsFreshExchangeAndLocalFills(String exchangeFilled) {
        var old = scopedStop("FIRST_TP", "101");
        position.setProtectionPolicyVersion(2);
        position.setQuantity(new BigDecimal("1.5"));
        old.setOrderType("TAKE_PROFIT_MARKET");
        old.setStatus("FINISHED");
        old.setQuantity(new BigDecimal(exchangeFilled));
        old.setExecutedQuantity(new BigDecimal(exchangeFilled));
        when(mapper.selectList(any())).thenAnswer(call -> selectedStops(call, List.of(old)));
        when(gateway.queryProtectionOrder(any(), eq(55L), any(), any(), any())).thenReturn(new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 55L, old.getClientOrderId(), "FINISHED", "SELL", "BOTH", "TAKE_PROFIT_MARKET", old.getQuantity(),
            new BigDecimal("101"), Instant.now(), new BigDecimal(exchangeFilled)));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 66L, call.getArgument(2), "NEW", "SELL", "BOTH", "TAKE_PROFIT_MARKET", new BigDecimal("0.5"),
            new BigDecimal("101"), Instant.now(), BigDecimal.ZERO));
        var first = new org.dromara.quant.strategy.ProtectionSignal("FIRST_TP", "TAKE_PROFIT_MARKET", new BigDecimal("0.5"),
            org.dromara.quant.strategy.ProtectionSignal.QuantityBase.INITIAL, new BigDecimal("101"), "first profit");
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "syncStrategyProtections", 1L, position, new BigDecimal("2"),
            List.of(first), new BigDecimal("0.8"), new BigDecimal("0.2"), true);
        if ("0.8".equals(exchangeFilled)) {
            verify(gateway, never()).placeProtectionOrder(any());
        } else {
            verify(gateway).placeProtectionOrder(argThat(request -> new BigDecimal("0.5").compareTo(request.quantity()) == 0));
        }
        verify(gateway, never()).cancelProtectionOrder(any());
    }

    @Test
    void firstProfitFinishingBetweenFactReadAndActiveLookupDoesNotCreateSecondIntent() {
        var old = scopedStop("FIRST_TP", "101");
        old.setOrderType("TAKE_PROFIT_MARKET");
        position.setProtectionPolicyVersion(2);
        when(mapper.selectList(any())).thenAnswer(call -> {
            var query = call.<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuantProtectionOrder>>getArgument(0);
            if (query.getSqlSegment().contains("status IN")) {
                old.setStatus("FINISHED"); // 私有成交事件在精确查询后、活动订单查询前到达。
                old.setExecutedQuantity(BigDecimal.ONE);
            }
            return selectedStops(call, List.of(old));
        });
        when(gateway.queryProtectionOrder(any(), eq(55L), any(), any(), any())).thenReturn(new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 55L, old.getClientOrderId(), "NEW", "SELL", "BOTH", "TAKE_PROFIT_MARKET", BigDecimal.ONE,
            new BigDecimal("101"), Instant.now(), BigDecimal.ZERO));
        when(gateway.placeProtectionOrder(any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderResult(66L,
            call.<ExchangeGateway.ProtectionOrderRequest>getArgument(0).clientOrderId(), "NEW"));
        when(gateway.queryProtectionOrder(any(), eq(66L), any(), any(), any())).thenAnswer(call -> new ExchangeGateway.ProtectionOrderSnapshot(
            "BTCUSDT", 66L, call.getArgument(2), "NEW", "SELL", "BOTH", "TAKE_PROFIT_MARKET", BigDecimal.ONE,
            new BigDecimal("101"), Instant.now(), BigDecimal.ZERO));
        var first = new org.dromara.quant.strategy.ProtectionSignal("FIRST_TP", "TAKE_PROFIT_MARKET", new BigDecimal("0.5"),
            org.dromara.quant.strategy.ProtectionSignal.QuantityBase.INITIAL, new BigDecimal("101"), "first profit");
        assertThrows(IllegalStateException.class, () -> service.syncStrategyProtections(1L, position, new BigDecimal("2"),
            List.of(first), BigDecimal.ONE, BigDecimal.ZERO, true));
        verify(gateway, never()).placeProtectionOrder(any());
    }

    private List<QuantProtectionOrder> selectedStops(org.mockito.invocation.InvocationOnMock call, List<QuantProtectionOrder> rows) {
        var query = call.<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuantProtectionOrder>>getArgument(0);
        String sql = query.getSqlSegment();
        var values = query.getParamNameValuePairs().values();
        return rows.stream().filter(row -> values.contains(row.getProtectionType()))
            .filter(row -> !sql.contains("status IN") || values.contains(row.getStatus()))
            .filter(row -> !sql.contains("id <>") || !values.contains(row.getId())).toList();
    }

    private QuantProtectionOrder scopedStop(String type, String price) {
        position.setProtectionCycleKey("current-cycle");
        var result = order();
        result.setCredentialId(1L);
        result.setProtectionType(type);
        result.setPositionSide(position.getPositionSide());
        result.setPositionCycleKey("current-cycle");
        result.setStopPrice(new BigDecimal(price));
        return result;
    }

    private org.dromara.quant.strategy.ProtectionSignal stopSignal(String type, String price) {
        return new org.dromara.quant.strategy.ProtectionSignal(type, "STOP_MARKET", BigDecimal.ONE,
            org.dromara.quant.strategy.ProtectionSignal.QuantityBase.CURRENT, new BigDecimal(price), "test", "STEP_TP".equals(type) ? 1 : 0);
    }

    private ExchangeGateway.ProtectionOrderSnapshot snapshot(Long id, String clientId, BigDecimal qty, String status, BigDecimal filled) {
        return new ExchangeGateway.ProtectionOrderSnapshot("BTCUSDT", id, clientId, status, "SELL", "BOTH", "STOP_MARKET", qty, new BigDecimal("99"), Instant.now(), filled);
    }

    private QuantProtectionOrder order() {
        QuantProtectionOrder order = new QuantProtectionOrder();
        order.setId(7L);
        order.setPositionId(10L);
        order.setSymbol("BTCUSDT");
        order.setClientOrderId("QFTP_test");
        order.setExchangeOrderId(55L);
        order.setQuantity(BigDecimal.ONE);
        order.setStopPrice(new BigDecimal("99"));
        order.setOrderType("STOP_MARKET");
        order.setStatus("WORKING");
        return order;
    }
}

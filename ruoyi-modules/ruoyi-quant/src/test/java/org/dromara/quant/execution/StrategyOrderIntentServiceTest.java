package org.dromara.quant.execution;

import org.dromara.quant.exchange.ExchangeGateway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("dev")
class StrategyOrderIntentServiceTest {
    private final StrategyOrderIntentMapper mapper = mock(StrategyOrderIntentMapper.class);
    private final StrategyOrderIntentService service = new StrategyOrderIntentService(mapper);

    private ExchangeGateway.OrderRequest request(String side, String position, boolean reduce) {
        return new ExchangeGateway.OrderRequest(null, "BTCUSDT", side, "MARKET", BigDecimal.ONE, null, reduce, position, "cid-1", true);
    }

    private ExchangeGateway.OrderResult result() {
        return new ExchangeGateway.OrderResult(77L, "cid-1", "NEW");
    }

    private Map<String, Object> preDispatch() {
        return Map.of("credential_id", 1L, "symbol", "BTCUSDT", "strategy_id", 2L,
            "strategy_instance_id", 3L, "operation_type", "OPEN", "position_side", "LONG",
            "exchange_side", "BUY", "client_order_id", "cid-1", "position_cycle_key", "cycle-1");
    }

    @Test
    void matchedHedgePositionAndExplicitOperationCanBeRecorded() {
        when(mapper.pendingByClient(1L, "BTCUSDT", "cid-1")).thenReturn(preDispatch());
        when(mapper.insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        service.record(1L, 2L, 3L, "OPEN", request("BUY", "LONG", false), result());
        verify(mapper).insert(1L, "BTCUSDT", 77L, 2L, 3L, "LONG", "OPEN", "cycle-1", "BUY", "cid-1");
    }

    @Test
    void oneWayBothUsesPersistedLogicalPositionSideAndCycleKey() {
        when(mapper.pendingByClient(1L, "BTCUSDT", "cid-1")).thenReturn(preDispatch());
        when(mapper.insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        service.record(1L, 2L, 3L, "OPEN", request("BUY", "BOTH", false), result());
        verify(mapper).insert(1L, "BTCUSDT", 77L, 2L, 3L, "LONG", "OPEN", "cycle-1", "BUY", "cid-1");
    }

    @Test
    void wrongCloseDirectionIsRejectedWithoutAcceptedIntentWrite() {
        when(mapper.pendingByClient(1L, "BTCUSDT", "cid-1")).thenReturn(preDispatch());
        assertThrows(IllegalStateException.class, () -> service.record(1L, 2L, 3L, "CLOSE", request("BUY", "LONG", true), result()));
        verify(mapper, never()).insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void missingCycleKeyCannotCreateAcceptedOrderIntent() {
        when(mapper.pendingByClient(1L, "BTCUSDT", "cid-1")).thenReturn(Map.of(
            "strategy_id", 2L, "strategy_instance_id", 3L, "symbol", "BTCUSDT",
            "operation_type", "OPEN", "position_side", "LONG", "exchange_side", "BUY", "client_order_id", "cid-1"));
        assertThrows(IllegalStateException.class, () -> service.record(1L, 2L, 3L, "OPEN", request("BUY", "LONG", false), result()));
        verify(mapper, never()).insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void conflictingDuplicateFailsClosed() {
        when(mapper.pendingByClient(1L, "BTCUSDT", "cid-1")).thenReturn(preDispatch());
        when(mapper.insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new DuplicateKeyException("collision"));
        when(mapper.find(1L, "BTCUSDT", 77L)).thenReturn(Map.of("strategy_id", 2L, "strategy_instance_id", 3L,
            "fill_role", "OPEN", "position_side", "LONG", "position_cycle_key", "other-cycle",
            "exchange_side", "BUY", "client_order_id", "cid-1"));
        assertThrows(IllegalStateException.class, () -> service.record(1L, 2L, 3L, "OPEN", request("BUY", "LONG", false), result()));
    }

    @Test
    void verifiedProtectionChildPersistsLogicalPositionSideEvenWhenExchangePositionSideIsBoth() {
        when(mapper.insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(),
            anyString(), anyString(), anyString())).thenReturn(1);
        service.recordVerifiedProtectionChild(1L, 2L, 3L, "BTCUSDT", "cycle-1",
            "LONG", "SELL", 91L, "child-client");
        verify(mapper).insert(1L, "BTCUSDT", 91L, 2L, 3L, "LONG", "CLOSE",
            "cycle-1", "SELL", "child-client");
        verify(mapper, never()).pendingByClient(anyLong(), anyString(), anyString());
    }

    @Test
    void conflictingProtectionChildReplayIsRejected() {
        when(mapper.insert(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyString(), anyString(),
            anyString(), anyString(), anyString())).thenThrow(new DuplicateKeyException("collision"));
        when(mapper.find(1L, "BTCUSDT", 91L)).thenReturn(Map.of(
            "strategy_id", 2L, "strategy_instance_id", 3L, "fill_role", "CLOSE",
            "position_side", "LONG", "position_cycle_key", "other-cycle",
            "exchange_side", "SELL", "client_order_id", "child-client"));
        assertThrows(IllegalStateException.class, () -> service.recordVerifiedProtectionChild(
            1L, 2L, 3L, "BTCUSDT", "cycle-1", "LONG", "SELL", 91L, "child-client"));
    }

    @Test
    void protectionChildCannotBeAttributedFromAnInconsistentCloseSide() {
        assertThrows(IllegalStateException.class, () -> service.recordVerifiedProtectionChild(
            1L, 2L, 3L, "BTCUSDT", "cycle-1", "LONG", "BUY", 91L, "child-client"));
        verifyNoInteractions(mapper);
    }


}

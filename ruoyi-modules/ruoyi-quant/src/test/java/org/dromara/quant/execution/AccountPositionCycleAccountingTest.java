package org.dromara.quant.execution;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountPositionCycleAccountingTest {
    @Test
    void splitsOneWayReversalIntoCloseAndNewOpen() {
        var transition = AccountPositionCycleAccounting.apply("BOTH", "SELL", new BigDecimal("5"), new BigDecimal("7"));

        assertEquals(new BigDecimal("-2"), transition.nextQuantity());
        assertEquals(new BigDecimal("5"), transition.closingQuantity());
        assertEquals("LONG", transition.closingDirection());
        assertEquals(new BigDecimal("2"), transition.openingQuantity());
        assertEquals("SHORT", transition.openingDirection());
    }

    @Test
    void partiallyClosesOneWayShortWithoutOpeningAnotherCycle() {
        var transition = AccountPositionCycleAccounting.apply("BOTH", "BUY", new BigDecimal("-3"), BigDecimal.ONE);

        assertEquals(new BigDecimal("-2"), transition.nextQuantity());
        assertEquals(BigDecimal.ONE, transition.closingQuantity());
        assertEquals("SHORT", transition.closingDirection());
        assertEquals(BigDecimal.ZERO, transition.openingQuantity());
        assertNull(transition.openingDirection());
    }

    @Test
    void keepsHedgeShortInventoryIndependentAndRejectsOverClose() {
        var increase = AccountPositionCycleAccounting.apply("SHORT", "SELL", new BigDecimal("4"), new BigDecimal("2"));
        assertEquals(new BigDecimal("6"), increase.nextQuantity());
        assertEquals(new BigDecimal("2"), increase.openingQuantity());
        assertEquals("SHORT", increase.openingDirection());

        assertThrows(IllegalStateException.class,
            () -> AccountPositionCycleAccounting.apply("SHORT", "BUY", new BigDecimal("4"), new BigDecimal("5")));
    }

    @Test
    void rejectsOpeningAgainstInvalidHedgePositionSide() {
        assertThrows(IllegalStateException.class,
            () -> AccountPositionCycleAccounting.apply("LONG", "SELL", BigDecimal.ZERO, BigDecimal.ONE));
    }
}

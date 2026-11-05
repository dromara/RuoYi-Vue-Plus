package org.dromara.quant.strategy;

import org.dromara.quant.runtime.PositionProtectionState;
import org.dromara.quant.runtime.domain.QuantStrategyPositionState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class StepProtectionPersistenceTest {
    private StrategyContext context(String side, String price, String favorable, int reached) {
        return new StrategyContext("BTCUSDT", side, new BigDecimal("100"), new BigDecimal(price), null, null, Instant.now(), false, Instant.now(), BigDecimal.ONE, BigDecimal.ONE
            , BigDecimal.TEN, 1, "LONG".equals(side) ? new BigDecimal(favorable) : new BigDecimal("100"), "SHORT".equals(side) ? new BigDecimal(favorable) :
            new BigDecimal("100"), true, reached);
    }

    @Test
    void persistentStepCannotFallWhenCurrentOrRecoveredExtremaRetrace() {
        var strategy = new KlineSlopeRateStrategy();
        for (String side : new String[]{"LONG", "SHORT"}) {
            for (String price : "LONG".equals(side) ? new String[]{"110", "109", "110"} : new String[]{"90", "91", "90"}) {
                var c = context(side, price, price, 1);
                assertEquals(1, KlineSlopeRateStrategy.reachedStep(c, Map.of("stepRatio", "0.10")));
                var signal = strategy.evaluateProtection(c, Map.of("stepRatio", "0.10")).stream().filter(s -> "STEP_TP".equals(s.type())).findFirst().orElseThrow();
                assertEquals(1, signal.stepIndex());
                assertEquals(0, new BigDecimal("100").compareTo(signal.triggerPrice()));
            }
            assertEquals(2, KlineSlopeRateStrategy.reachedStep(context(side, "LONG".equals(side) ? "120" : "80", "LONG".equals(side) ? "120" : "80", 1), Map.of("stepRatio", "0" +
                ".10")));
            assertEquals(3, KlineSlopeRateStrategy.reachedStep(context(side, "LONG".equals(side) ? "135" : "65", "LONG".equals(side) ? "135" : "65", 1), Map.of("stepRatio", "0" +
                ".10")));
        }
    }

    @Test
    void localExitAndExchangeProtectionSharePersistedHighestStep() {
        var strategy = new KlineSlopeRateStrategy();
        var c = context("LONG", "119", "119", 3);
        assertTrue(strategy.evaluateExit(c, Map.of("stepRatio", "0.10", "firstProfitTakingRate", "0.15")).stream().anyMatch(s -> KlineSlopeRateStrategy.EXIT_STEP_TP.equals(s.type())));
        var p =
            strategy.evaluateProtection(c, Map.of("stepRatio", "0.10", "firstProfitTakingRate", "0.15")).stream().filter(s -> "STEP_TP".equals(s.type())).findFirst().orElseThrow();
        assertEquals(3, p.stepIndex());
        assertEquals(0, new BigDecimal("120").compareTo(p.triggerPrice()));
    }

    @Test
    void firstTakeProfitRequiresItsFullTargetBeforeItIsMarkedTriggered() {
        var state = new QuantStrategyPositionState();
        state.setFirstTakeProfitTargetQty(new BigDecimal("50"));

        PositionProtectionState.confirmLocalFirstProfit(state, new BigDecimal("20"));
        assertEquals(false, state.getFirstTakeProfitTriggered());

        PositionProtectionState.confirmLocalFirstProfit(state, new BigDecimal("30"));
        assertEquals(true, state.getFirstTakeProfitTriggered());
    }

    @Test
    void newCycleResetsObservedAndConfirmedSteps() {
        var state = new QuantStrategyPositionState();
        state.setHighestStep(3);
        state.setConfirmedProtectionStep(2);
        PositionProtectionState.startCycle(state, 10, null, new BigDecimal("100"));
        assertEquals(0, state.getHighestStep());
        assertEquals(0, state.getConfirmedProtectionStep());
    }
}

package org.dromara.quant.execution;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 按交易所实际持仓方向计算账户级成交对当前周期的影响。
 * BOTH 使用有符号净仓；LONG/SHORT 对冲仓分别维护非负数量。
 */
public final class AccountPositionCycleAccounting {
    private AccountPositionCycleAccounting() {
    }

    public static Transition apply(String positionSide, String exchangeSide, BigDecimal currentQuantity, BigDecimal fillQuantity) {
        Objects.requireNonNull(currentQuantity);
        Objects.requireNonNull(fillQuantity);
        if (fillQuantity.signum() <= 0 || !("BUY".equals(exchangeSide) || "SELL".equals(exchangeSide))) {
            throw new IllegalArgumentException("Invalid account fill");
        }
        if (!("BOTH".equals(positionSide) || "LONG".equals(positionSide) || "SHORT".equals(positionSide))) {
            throw new IllegalArgumentException("Invalid exchange position side");
        }
        if (!"BOTH".equals(positionSide) && currentQuantity.signum() < 0) {
            throw new IllegalStateException("Hedge-side inventory cannot be negative");
        }

        BigDecimal delta;
        if ("BOTH".equals(positionSide)) {
            delta = "BUY".equals(exchangeSide) ? fillQuantity : fillQuantity.negate();
        } else {
            boolean increases = ("LONG".equals(positionSide) && "BUY".equals(exchangeSide))
                || ("SHORT".equals(positionSide) && "SELL".equals(exchangeSide));
            delta = increases ? fillQuantity : fillQuantity.negate();
        }

        BigDecimal nextQuantity = currentQuantity.add(delta);
        if (!"BOTH".equals(positionSide) && nextQuantity.signum() < 0) {
            throw new IllegalStateException("Hedge fill exceeds the existing position-side quantity");
        }

        BigDecimal closingQuantity = BigDecimal.ZERO;
        BigDecimal openingQuantity = BigDecimal.ZERO;
        String closingDirection = null;
        String openingDirection = null;
        if (currentQuantity.signum() == 0 || currentQuantity.signum() == delta.signum()) {
            openingQuantity = fillQuantity;
            openingDirection = "BOTH".equals(positionSide)
                ? (delta.signum() > 0 ? "LONG" : "SHORT")
                : positionSide;
        } else {
            closingQuantity = currentQuantity.abs().min(fillQuantity);
            closingDirection = "BOTH".equals(positionSide)
                ? (currentQuantity.signum() > 0 ? "LONG" : "SHORT")
                : positionSide;
            openingQuantity = fillQuantity.subtract(closingQuantity);
            if (openingQuantity.signum() > 0) {
                openingDirection = delta.signum() > 0 ? "LONG" : "SHORT";
            }
        }
        return new Transition(nextQuantity, closingQuantity, closingDirection, openingQuantity, openingDirection);
    }

    public record Transition(BigDecimal nextQuantity, BigDecimal closingQuantity, String closingDirection,
                             BigDecimal openingQuantity, String openingDirection) {
    }
}

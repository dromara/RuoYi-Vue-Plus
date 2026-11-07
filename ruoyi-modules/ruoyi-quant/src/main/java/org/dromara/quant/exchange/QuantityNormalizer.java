package org.dromara.quant.exchange;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 按交易所 PRICE_FILTER/LOT_SIZE 归一化，缺少正过滤器时直接拒绝。
 */
public final class QuantityNormalizer {
    // 纯静态工具类，禁止实例化。
    private QuantityNormalizer() {
    }

    /**
     * 根据交易规则规范化价格。
     */
    public static BigDecimal price(BigDecimal value, ExchangeGateway.SymbolRule rule) {
        if (value == null || rule == null || rule.priceTickSize() == null || rule.priceTickSize().signum() <= 0) {
            throw new IllegalArgumentException("交易对缺少有效价格精度");
        }
        return floor(value, rule.priceTickSize());
    }

    /** 保护目标按收益不劣于目标值取整：多头向上、空头向下；普通订单仍使用 price。 */
    public static BigDecimal protectionPrice(BigDecimal value, ExchangeGateway.SymbolRule rule, String direction) {
        price(value, rule); // 复用价格过滤器校验。
        if (!"LONG".equalsIgnoreCase(direction) && !"SHORT".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("保护价格缺少明确持仓方向");
        }
        return value.divide(rule.priceTickSize(), 0, "LONG".equalsIgnoreCase(direction) ? RoundingMode.CEILING : RoundingMode.FLOOR)
            .multiply(rule.priceTickSize()).stripTrailingZeros();
    }

    /**
     * 市价单必须同时满足 LOT_SIZE 与 MARKET_LOT_SIZE。
     * 两个步长采用最小公倍数合并，不能仅选其中一个，否则可能被交易所拒单。
     */
    public static BigDecimal quantity(BigDecimal value, ExchangeGateway.SymbolRule rule, String orderType) {
        if (rule == null || !"MARKET".equalsIgnoreCase(orderType)) {
            return quantity(value, rule);
        }
        BigDecimal lot = rule.stepSize();
        BigDecimal market = rule.marketStepSize();
        if (lot == null || lot.signum() <= 0 || market == null || market.signum() <= 0) {
            throw new IllegalArgumentException("交易对缺少有效市价数量步长");
        }
        int scale = Math.max(0, Math.max(lot.scale(), market.scale()));
        var a = lot.movePointRight(scale).toBigIntegerExact();
        var b = market.movePointRight(scale).toBigIntegerExact();
        BigDecimal step = new BigDecimal(a.divide(a.gcd(b)).multiply(b), scale);
        BigDecimal maxQuantity = minPositive(rule.maxQuantity(), rule.marketMaxQuantity());
        return quantity(value, new ExchangeGateway.SymbolRule(rule.symbol(), rule.quoteAsset(), rule.pricePrecision(), rule.quantityPrecision(), rule.priceTickSize(), step,
            market, maxQuantity, maxQuantity, rule.minNotional(), rule.status()));
    }

    /**
     * 按 LOT_SIZE 步长向零方向截断；此处仅检查最小名义价值规则是否存在，
     * 实际订单金额校验由 requireNotional 在确定价格后执行。
     */
    public static BigDecimal quantity(BigDecimal value, ExchangeGateway.SymbolRule rule) {
        if (value == null || rule == null || rule.stepSize() == null || rule.stepSize().signum() <= 0) {
            throw new IllegalArgumentException("交易对缺少有效数量步长");
        }
        BigDecimal normalized = floor(value, rule.stepSize());
        if (normalized.signum() <= 0 || rule.minNotional() == null || rule.minNotional().signum() <= 0) {
            throw new IllegalArgumentException("交易对缺少有效最小名义价值");
        }
        if (rule.maxQuantity() != null && rule.maxQuantity().signum() > 0 && normalized.compareTo(rule.maxQuantity()) > 0) {
            throw new IllegalArgumentException("交易对 " + rule.symbol() + " 数量 " + normalized.toPlainString()
                + " 超过最大允许数量 " + rule.maxQuantity().toPlainString());
        }
        return normalized;
    }

    private static BigDecimal minPositive(BigDecimal first, BigDecimal second) {
        boolean firstValid = first != null && first.signum() > 0;
        boolean secondValid = second != null && second.signum() > 0;
        if (!firstValid) {
            return secondValid ? second : BigDecimal.ZERO;
        }
        return !secondValid || first.compareTo(second) <= 0 ? first : second;
    }

    /**
     * 校验订单名义价值是否满足最小要求。
     */
    public static void requireNotional(BigDecimal price, BigDecimal quantity, ExchangeGateway.SymbolRule rule) {
        if (price == null || quantity == null || rule == null || rule.minNotional() == null || price.multiply(quantity).compareTo(rule.minNotional()) < 0) {
            throw new IllegalArgumentException("订单名义价值低于交易所最小限制");
        }
    }

    /**
     * 按指定步进向下取整。
     */
    private static BigDecimal floor(BigDecimal value, BigDecimal step) {
        return value.divide(step, 0, RoundingMode.DOWN).multiply(step).stripTrailingZeros();
    }
}

package org.dromara.quant.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 实时 K 线斜率 + 回报率阶梯止盈策略。
 *
 * <p>职责严格限制为“计算信号/保护意图”，不直接访问 Binance、数据库、Mapper、邮件或线程池。</p>
 * <p>开仓：按 SLOPE_LOOKBACK_BARS 选择历史 K 线 Open，与当前实时价格计算角度，绝对角度达到 entryAngleDegrees 时顺势开仓。</p>
 * <p>本地止损：杠杆后回报率 &lt;= -localStopLossRate 时市价全平；同一保护同时可挂 Binance STOP_MARKET。</p>
 * <p>首次止盈：杠杆后回报率达到 firstProfitTakingRate 时，平初始总仓位的 firstClosePositionRate。</p>
 * <p>阶梯止盈：首次止盈后，最高回报率处于第 n 个 stepRatio 阶梯时，回撤到第 n-1 阶梯平剩余仓位。</p>
 * <p>默认止损和交易对绑定止损不属于 params，由 Runtime 冻结并交给 ProtectionOrderService，各层同时生效。</p>
 * <p>默认参数：首次平仓 50%、首次止盈回报率 10%、本地兜底止损 5%、阶梯间隔 10%；均可通过 params 覆盖。</p>
 * <p>本地持续判断相同条件；触发后由 Runtime 在确认窗口后精确撤单、对账并补平，避免双重减仓。</p>
 * <p>“同账户 + 同交易对 + 同一根 K 线只开一次”由 StrategyEntryGuardService 在 Runtime 层保证。</p>
 */
@Slf4j
@Component
public final class KlineSlopeRateStrategy implements StrategyPlugin {
    private static final long SLOPE_LOG_INTERVAL_MILLIS = 5_000L;
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> NEXT_SLOPE_LOG_AT = new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * 斜率参考 K 线根数：1 表示往前第 1 根，2 表示往前第 2 根，3 表示往前第 3 根，最大是3。
     */
    public static final int SLOPE_LOOKBACK_BARS = 1;
    /**
     * 策略插件唯一编码。
     */
    public static final String CODE = "kline_slope_rate";
    /** 仅由服务端在新持仓周期参数快照中写入；无标识的历史持仓保持原规则。 */
    public static final String PROTECTION_POLICY_VERSION_KEY = "_quantProtectionPolicyVersion";

    /** 判断冻结快照是否启用统一保护规则；运行时不会直接信任客户端提供的版本标识。 */
    public static boolean unifiedProtection(Map<String, Object> params) {
        return params != null && "2".equals(String.valueOf(params.get(PROTECTION_POLICY_VERSION_KEY)));
    }

    /** 新周期硬止损比例与策略参数共用校验、默认值及历史别名。 */
    public static BigDecimal localStopLossRate(Map<String, Object> params) {
        return rate(params, "localStopLossRate", "LocalStopLossRate", DEFAULT_LOCAL_STOP_LOSS_RATE);
    }

    /** 本地兜底止损退出类型。 */
    public static final String EXIT_LOCAL_STOP = "LOCAL_STOP_LOSS";
    /**
     * 首次分批止盈退出类型。
     */
    public static final String EXIT_FIRST_TP = "FIRST_TAKE_PROFIT";
    /**
     * 阶梯追踪止盈退出类型。
     */
    public static final String EXIT_STEP_TP = "STEP_TRAILING_TAKE_PROFIT";
    /**
     * 交易所侧本地止损镜像保护类型。
     */
    public static final String PROTECTION_LOCAL_STOP = "LOCAL_STOP";
    /**
     * 交易所侧首次止盈保护类型。
     */
    public static final String PROTECTION_FIRST_TP = "FIRST_TP";
    /**
     * 交易所侧阶梯追踪止盈保护类型。
     */
    public static final String PROTECTION_STEP_TP = "STEP_TP";
    /**
     * 策略插件版本。
     */
    private static final String VERSION = "1.0";
    /**
     * 默认斜率开仓角度阈值，单位为度。
     */
    private static final double DEFAULT_ENTRY_ANGLE_DEGREES = 60.0d;
    /**
     * 默认首次止盈平仓比例：平初始仓位的 50%。
     */
    private static final BigDecimal DEFAULT_FIRST_CLOSE_POSITION_RATE = new BigDecimal("0.50");
    /**
     * 默认首次止盈杠杆回报率：10%。
     */
    private static final BigDecimal DEFAULT_FIRST_PROFIT_TAKING_RATE = new BigDecimal("0.10");
    /**
     * 默认本地兜底止损杠杆回报率：亏损 5%。
     */
    private static final BigDecimal DEFAULT_LOCAL_STOP_LOSS_RATE = new BigDecimal("0.05");
    /**
     * 默认阶梯间隔：每 10% 杠杆回报率一个阶梯。
     */
    private static final BigDecimal DEFAULT_STEP_RATIO = new BigDecimal("0.10");

    /**
     * 判断当前持仓是否具备策略/保护计算所需的有效字段。
     */
    private static boolean validPosition(StrategyContext c) {
        return c != null && c.entryPrice() != null && c.entryPrice().signum() > 0 && c.currentPrice() != null && c.currentPrice().signum() > 0 && c.quantity() != null && c.quantity().signum() > 0 && c.direction() != null;
    }

    /**
     * 读取有效杠杆倍数；缺失时按 1 倍处理。
     */
    private static int leverage(StrategyContext c) {
        return c.leverage() == null || c.leverage() <= 0 ? 1 : c.leverage();
    }

    /**
     * 根据开仓后最有利价格计算最大杠杆回报率。
     */
    private static BigDecimal maxFavorableReturn(StrategyContext c) {
        BigDecimal favorable = "SHORT".equalsIgnoreCase(c.direction()) ? c.lowestPriceSinceEntry() : c.highestPriceSinceEntry();
        if (favorable == null) {
            favorable = c.currentPrice();
        }
        return leveragedReturn(c.direction(), favorable, c.entryPrice(), leverage(c));
    }

    /**
     * 按 LONG/SHORT 方向计算价格收益率，并乘以杠杆得到策略使用的回报率。
     */
    public static BigDecimal leveragedReturn(String direction, BigDecimal currentPrice, BigDecimal entryPrice, int leverage) {
        BigDecimal delta = "SHORT".equalsIgnoreCase(direction) ? entryPrice.subtract(currentPrice) : currentPrice.subtract(entryPrice);
        return delta.divide(entryPrice, 12, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(Math.max(1, leverage)));
    }

    /**
     * 将目标杠杆回报率换算为价格触发点，用于构造 Binance 条件单。
     */
    public static BigDecimal priceForReturn(BigDecimal entryPrice, String direction, BigDecimal returnRate, int leverage) {
        BigDecimal priceRate = returnRate.divide(BigDecimal.valueOf(Math.max(1, leverage)), 12, RoundingMode.HALF_UP);
        return "SHORT".equalsIgnoreCase(direction) ? entryPrice.multiply(BigDecimal.ONE.subtract(priceRate)) : entryPrice.multiply(BigDecimal.ONE.add(priceRate));
    }

    /**
     * 本地退出与交易所保护共享单调台阶，历史高水位不因回撤降低。
     */
    public static int reachedStep(StrategyContext context, Map<String, Object> params) {
        if (!validPosition(context)) {
            return Math.max(0, context == null ? 0 : context.highestStep());
        }
        BigDecimal step = rate(params, "stepRatio", "StepRatio", DEFAULT_STEP_RATIO);
        BigDecimal favorable = maxFavorableReturn(context).max(leveragedReturn(context.direction(), context.currentPrice(), context.entryPrice(), leverage(context)));
        int reached = Math.max(Math.max(0, context.highestStep()), rateStep(favorable, step));
        return unifiedProtection(params) && stepProtectionStarted(context)
            ? Math.max(1, reached) : reached;
    }

    /**
     * 根据回报率和阶梯间隔计算当前阶梯序号。
     */

    private static int rateStep(BigDecimal value, BigDecimal step) {
        if (value == null || step == null || step.signum() <= 0 || value.signum() <= 0) {
            return 0;
        }
        return value.divide(step, 0, RoundingMode.FLOOR).intValue();
    }

    /**
     * 把小数比例格式化为百分比文本。
     */
    private static String percent(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * 读取并校验比例型策略参数。
     */
    private static void requireRate(Map<String, Object> params, String key, String legacy, BigDecimal fallback, String... aliases) {
        rate(params, key, legacy, fallback, aliases);
    }

    /**
     * 读取比例型策略参数并校验必须处于 0~1 之间。
     */
    private static BigDecimal rate(Map<String, Object> params, String key, String legacy, BigDecimal fallback, String... aliases) {
        BigDecimal value = decimal(params, key, legacy, fallback, aliases);
        if (value.signum() <= 0 || value.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException(key + " 必须 > 0 且 < 1");
        }
        return value;
    }

    /**
     * 按主参数名、兼容别名和默认值读取 BigDecimal 参数。
     */
    private static BigDecimal decimal(Map<String, Object> params, String key, String legacy, BigDecimal fallback, String... aliases) {
        Object raw = params == null ? null : params.get(key);
        if (raw == null && params != null && legacy != null) {
            raw = params.get(legacy);
        }
        if (raw == null && params != null && aliases != null) {
            for (String alias : aliases) {
                if (params.get(alias) != null) {
                    raw = params.get(alias);
                    break;
                }
            }
        }
        if (raw == null) {
            return fallback;
        }
        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("策略参数无效: " + key, ex);
        }
    }

    /**
     * 计算策略实际使用的斜率数据；无足够行情数据时返回 null。
     * FIXED: 阈值判断使用 &gt;=，与“达到即开仓”语义一致。
     */
    public static SlopeEvaluation calculateSlope(StrategyContext context, Map<String, Object> params) {
        if (context == null || context.currentPrice() == null || context.previousKlineOpenPrice() == null || context.currentPrice().signum() <= 0 || context.previousKlineOpenPrice().signum() <= 0) {
            return null;
        }
        BigDecimal threshold = decimal(params, "entryAngleDegrees", null, BigDecimal.valueOf(DEFAULT_ENTRY_ANGLE_DEGREES));
        BigDecimal slopePercent = context.currentPrice().subtract(context.previousKlineOpenPrice()).multiply(BigDecimal.valueOf(100)).divide(context.previousKlineOpenPrice(), 2,
            RoundingMode.HALF_EVEN);
        double rawDegrees = Math.toDegrees(Math.atan(slopePercent.doubleValue()));
        BigDecimal displayDegrees = BigDecimal.valueOf(rawDegrees).setScale(2, RoundingMode.HALF_UP);
        return new SlopeEvaluation(context.currentPrice(), context.previousKlineOpenPrice(), slopePercent, displayDegrees, rawDegrees, threshold,
            Math.abs(rawDegrees) >= threshold.doubleValue());
    }

    /**
     * 读取并校验首次分批止盈的平仓比例参数。
     */
    public static BigDecimal firstClosePositionRate(Map<String, Object> params) {
        return rate(params, "firstClosePositionRate", "FirstClosePositionRate", DEFAULT_FIRST_CLOSE_POSITION_RATE);
    }

    /**
     * 阶梯保护仅在首次分批止盈目标全部成交确认后启用。
     */
    private static boolean stepProtectionStarted(StrategyContext context) {
        return context.firstTakeProfitTriggered();
    }

    /**
     * 返回当前策略插件的编码、版本和展示名称。
     * FIXED: 统一对外示例为 localStopLossRate；runtime 仍兼容旧名 stopLossRatio。
     */
    @Override
    public StrategyDescriptor descriptor() {
        Map<String, Object> example = new java.util.LinkedHashMap<>();
        example.put("entryAngleDegrees", 60.0d);
        example.put("firstProfitTakingRate", 0.10d);
        example.put("firstClosePositionRate", 0.50d);
        example.put("stepRatio", 0.10d);
        example.put("localStopLossRate", 0.05d);
        example.put("entryMaDirectionFilterEnabled", true);
        return new StrategyDescriptor(CODE, VERSION, "实时K线斜率阶梯止盈", example);
    }

    /**
     * 校验策略参数是否合法；不合法时抛出参数异常。
     */
    @Override
    public void validateConfig(Map<String, Object> params) {
        double angle = decimal(params, "entryAngleDegrees", null, BigDecimal.valueOf(DEFAULT_ENTRY_ANGLE_DEGREES)).doubleValue();
        if (!Double.isFinite(angle) || angle <= 0 || angle >= 90) {
            throw new IllegalArgumentException("entryAngleDegrees 必须 > 0 且 < 90");
        }
        requireRate(params, "firstClosePositionRate", "FirstClosePositionRate", DEFAULT_FIRST_CLOSE_POSITION_RATE);
        requireRate(params, "firstProfitTakingRate", "FirstProfitTakingRate", DEFAULT_FIRST_PROFIT_TAKING_RATE);
        // 旧版本曾使用 stopLossRatio 表示程序止损；新版本明确命名 localStopLossRate，避免和策略表硬止损混淆。
        requireRate(params, "localStopLossRate", "LocalStopLossRate", DEFAULT_LOCAL_STOP_LOSS_RATE);
        requireRate(params, "stepRatio", "StepRatio", DEFAULT_STEP_RATIO);
        Object maFilter = params == null ? null : params.get("entryMaDirectionFilterEnabled");
        if (maFilter != null && !(maFilter instanceof Boolean)
            && !"true".equalsIgnoreCase(String.valueOf(maFilter))
            && !"false".equalsIgnoreCase(String.valueOf(maFilter))) {
            throw new IllegalArgumentException("entryMaDirectionFilterEnabled 必须为 true 或 false");
        }
    }

    /**
     * 根据当前市场上下文计算是否产生开仓信号。
     */
    @Override
    public Optional<EntrySignal> evaluateEntry(StrategyContext context) {
        return evaluateEntry(context, Map.of());
    }

    /**
     * 根据当前市场上下文计算是否产生开仓信号。
     */
    @Override
    public Optional<EntrySignal> evaluateEntry(StrategyContext context, Map<String, Object> params) {
        SlopeEvaluation evaluation = calculateSlope(context, params);
        if (evaluation == null) {
            return Optional.empty();
        }
//        if("ERAUSDT".equalsIgnoreCase(context.symbol())){
//            log.info("计算交易对：" + context.symbol());
//        }
//        log.info("计算交易对：" + context.symbol());
        double degrees = evaluation.rawDegrees();
        if (Math.abs(degrees) > 58 && shouldLogSlope(context.symbol(), System.currentTimeMillis())) {
            log.warn("  ***  策略实时斜率 时间：{} 交易对：{} 上根K线开盘价：{} 当前价：{} 坡度百分：{} 度数：{}°", context.eventTime().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime(), context.symbol(),
                evaluation.referenceOpenPrice(), evaluation.currentPrice(), evaluation.slopePercent(), BigDecimal.valueOf(degrees).setScale(2, RoundingMode.HALF_UP));
        }
        if (!evaluation.entryApproved()) {
            return Optional.empty();
        }
        EntrySignal.Direction direction = degrees > 0 ? EntrySignal.Direction.LONG : EntrySignal.Direction.SHORT;
        return Optional.of(new EntrySignal(direction, "K线斜率角度=" + evaluation.degrees() + "°, 阈值=" + evaluation.entryThreshold() + "°"));
    }

    /**
     * 在斜率信号通过后，使用最近已收盘的 1 分钟 K 线 5MA/7MA 做方向过滤。
     * 均线不包含当前未收盘 K 线；缺少完整窗口或均线方向不一致时不允许开仓。
     */
    public Optional<EntrySignal> evaluateEntry(StrategyContext context, Map<String, Object> params, BigDecimal ma5, BigDecimal ma7) {
        Optional<EntrySignal> signal = evaluateEntry(context, params);
        if (signal.isEmpty() || !entryMaDirectionFilterEnabled(params)) {
            return signal;
        }
        if (ma5 == null || ma7 == null) {
            return Optional.empty();
        }
        EntrySignal entry = signal.get();
        int comparison = ma5.compareTo(ma7);
        boolean directionApproved = entry.direction() == EntrySignal.Direction.LONG ? comparison > 0
            : entry.direction() == EntrySignal.Direction.SHORT && comparison < 0;
        if (!directionApproved) {
            return Optional.empty();
        }
        String maReason = comparison > 0 ? "1分钟5MA高于7MA" : "1分钟5MA低于7MA";
        return Optional.of(new EntrySignal(entry.direction(), entry.reason() + "，" + maReason + "，均线方向过滤通过"));
    }

    private static boolean entryMaDirectionFilterEnabled(Map<String, Object> params) {
        Object value = params == null ? null : params.get("entryMaDirectionFilterEnabled");
        return value == null || Boolean.parseBoolean(String.valueOf(value));
    }

    static boolean shouldLogSlope(String symbol, long nowMillis) {
        java.util.concurrent.atomic.AtomicBoolean allowed = new java.util.concurrent.atomic.AtomicBoolean();
        NEXT_SLOPE_LOG_AT.compute(symbol, (ignored, nextAt) -> {
            if (nextAt == null || nowMillis >= nextAt) {
                allowed.set(true);
                return nowMillis + SLOPE_LOG_INTERVAL_MILLIS;
            }
            return nextAt;
        });
        return allowed.get();
    }

    /**
     * 根据当前持仓和实时价格计算退出信号。
     */
    @Override
    public List<ExitSignal> evaluateExit(StrategyContext context) {
        return evaluateExit(context, Map.of());
    }

    /**
     * 根据当前持仓和实时价格计算退出信号。
     * FIXED: 首次止盈按初始总仓位比例，不再按当前仓位。
     */
    @Override
    public List<ExitSignal> evaluateExit(StrategyContext context, Map<String, Object> params) {
        if (!validPosition(context)) {
            return List.of();
        }
        BigDecimal localStop = rate(params, "localStopLossRate", "LocalStopLossRate", DEFAULT_LOCAL_STOP_LOSS_RATE);
        BigDecimal firstProfit = rate(params, "firstProfitTakingRate", "FirstProfitTakingRate", DEFAULT_FIRST_PROFIT_TAKING_RATE);
        BigDecimal firstClose = rate(params, "firstClosePositionRate", "FirstClosePositionRate", DEFAULT_FIRST_CLOSE_POSITION_RATE);
        BigDecimal step = rate(params, "stepRatio", "StepRatio", DEFAULT_STEP_RATIO);
        BigDecimal currentReturn = leveragedReturn(context.direction(), context.currentPrice(), context.entryPrice(), leverage(context));

        if (currentReturn.compareTo(localStop.negate()) <= 0) {
            return List.of(new ExitSignal(EXIT_LOCAL_STOP, BigDecimal.ONE, "本地兜底止损触发，止损阈值=-" + percent(localStop) + "，触发时回报率=" + percent(currentReturn), false, PROTECTION_LOCAL_STOP,
                ExitSignal.QuantityBase.CURRENT));
        }
        if (!context.firstTakeProfitTriggered() && currentReturn.compareTo(firstProfit) >= 0) {
            return List.of(new ExitSignal(EXIT_FIRST_TP, firstClose, "首次止盈触发，首次止盈阈值=" + percent(firstProfit) + "，触发时回报率=" + percent(currentReturn) + "，计划平初始仓位比例=" + percent(firstClose), true, PROTECTION_FIRST_TP,
                ExitSignal.QuantityBase.INITIAL));
        }
        if (stepProtectionStarted(context)) {
            BigDecimal maxReturn = maxFavorableReturn(context);
            int stepIndex = reachedStep(context, params);
            if (stepIndex > 0) {
                BigDecimal locked = step.multiply(BigDecimal.valueOf(Math.max(0, stepIndex - 1L)));
                if (currentReturn.compareTo(locked) <= 0) {
                    return List.of(new ExitSignal(EXIT_STEP_TP, BigDecimal.ONE, "阶梯追踪止盈触发，持仓期间最大回报率=" + percent(maxReturn) + "，触发时回报率=" + percent(currentReturn) + "，回撤保护比例=" + percent(locked), false, PROTECTION_STEP_TP,
                        ExitSignal.QuantityBase.CURRENT));
                }
            }
        }
        return List.of();
    }

    /**
     * 根据当前持仓状态计算交易所侧应存在的保护单意图。
     */
    @Override
    public List<ProtectionSignal> evaluateProtection(StrategyContext context, Map<String, Object> params) {
        if (!validPosition(context)) {
            return List.of();
        }
        BigDecimal localStop = rate(params, "localStopLossRate", "LocalStopLossRate", DEFAULT_LOCAL_STOP_LOSS_RATE);
        BigDecimal firstProfit = rate(params, "firstProfitTakingRate", "FirstProfitTakingRate", DEFAULT_FIRST_PROFIT_TAKING_RATE);
        BigDecimal firstClose = rate(params, "firstClosePositionRate", "FirstClosePositionRate", DEFAULT_FIRST_CLOSE_POSITION_RATE);
        BigDecimal step = rate(params, "stepRatio", "StepRatio", DEFAULT_STEP_RATIO);
        int leverage = leverage(context);
        List<ProtectionSignal> result = new ArrayList<>();
        if (!unifiedProtection(params)) {
            result.add(new ProtectionSignal(PROTECTION_LOCAL_STOP, "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT, priceForReturn(context.entryPrice(),
                context.direction(), localStop.negate(), leverage), "Binance 本地止损镜像"));
        }

        boolean firstProfitReached = stepProtectionStarted(context);
        if (!context.firstTakeProfitTriggered()) {
            result.add(new ProtectionSignal(PROTECTION_FIRST_TP, "TAKE_PROFIT_MARKET", firstClose, ProtectionSignal.QuantityBase.INITIAL, priceForReturn(context.entryPrice(),
                context.direction(), firstProfit, leverage), "Binance 首次分批止盈"));
        }
        if (firstProfitReached) {
            BigDecimal maxReturn = maxFavorableReturn(context);
            int stepIndex = reachedStep(context, params);
            if (stepIndex > 0) {
                BigDecimal locked = step.multiply(BigDecimal.valueOf(Math.max(0, stepIndex - 1L)));
                result.add(new ProtectionSignal(PROTECTION_STEP_TP, "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT, priceForReturn(context.entryPrice(),
                    context.direction(), locked, leverage), "Binance 阶梯追踪止盈，第" + stepIndex + "阶，锁定回报率=" + percent(locked), stepIndex));
            }
        }
        if (unifiedProtection(params)) {
            result.sort(java.util.Comparator.comparingInt(signal -> "STEP_TP".equals(signal.type()) ? 0 : 1));
        }
        return result;
    }

    /**
     * 斜率计算结果，供运行时交易决策和终端 SSE 共用。
     */
    public record SlopeEvaluation(BigDecimal currentPrice, BigDecimal referenceOpenPrice, BigDecimal slopePercent, BigDecimal degrees, double rawDegrees, BigDecimal entryThreshold,
                                  boolean entryApproved) {
    }
}

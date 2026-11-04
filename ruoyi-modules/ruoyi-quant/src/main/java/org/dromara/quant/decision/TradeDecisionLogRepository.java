package org.dromara.quant.decision;

import org.dromara.common.core.domain.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 日志线程专用数据库访问；所有查询只读取交易系统已经取得的事实。
 */
@Repository
public class TradeDecisionLogRepository {
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final String COLUMNS = "event_key,record_type,credential_id,account_name,run_mode,strategy_id,strategy_instance_id,strategy_name,symbol,operation,direction," +
        "status,reason_code,reason,angle,threshold,kline_time,quantity,price,margin,idempotency_key,order_id,client_order_id,event_time,trade_time,repeat_count,last_time";
    private static final String VALUES = ":eventKey,:recordType,:credentialId,:accountName,:runMode,:strategyId,:strategyInstanceId,:strategyName,:symbol,:operation,:direction," +
        ":status,:reasonCode,:reason,:angle,:threshold,:klineTime,:quantity,:price,:margin,:idempotencyKey,:orderId,:clientOrderId,:eventTime,:tradeTime,1,:eventTime";
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public TradeDecisionLogRepository(JdbcTemplate shared) {
        jdbc = new JdbcTemplate(Objects.requireNonNull(shared.getDataSource()));
        jdbc.setQueryTimeout(3);
        named = new NamedParameterJdbcTemplate(jdbc);
    }

    private static void add(StringBuilder sql, List<Object> args, String column, Object value, boolean like) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        sql.append(" AND ").append(column).append(like ? " LIKE ?" : "=?");
        args.add(like ? "%" + value + "%" : value);
    }

    /**
     * 生成执行事件的稳定去重标识。
     */
    public static String executionKey(Long credentialId, String key) {
        return "X:" + credentialId + ":" + key;
    }

    static String key(String value) {
        if (value == null) {
            throw new IllegalArgumentException("日志事件键为空");
        }
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String direction(String operation, String side) {
        if (side == null || !("BUY".equalsIgnoreCase(side) || "SELL".equalsIgnoreCase(side))) {
            return "UNKNOWN";
        }
        if (!"OPEN".equals(operation) && !"CLOSE".equals(operation)) {
            return "UNKNOWN";
        }
        return "OPEN".equals(operation) ? ("BUY".equalsIgnoreCase(side) ? "LONG" : "SHORT") : ("BUY".equalsIgnoreCase(side) ? "SHORT" : "LONG");
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private static Timestamp asTimestamp(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof LocalDateTime time) {
            return Timestamp.valueOf(time);
        }
        if (value instanceof Instant instant) {
            return Timestamp.from(instant);
        }
        throw new IllegalArgumentException("数据库返回了不支持的日期类型: " + (value == null ? "null" : value.getClass().getName()));
    }

    private static String text(Map<String, Object> row, String key) {
        return Objects.toString(row.get(key), null);
    }

    private static Long number(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : Long.valueOf(v.toString());
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai")));
    }

    private static String percent(BigDecimal rate) {
        return rate == null ? null : rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }

    private static String amount(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal snapshotRate(Map<String, Object> position, String key) {
        String snapshot = text(position, "strategy_params_snapshot");
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            var value = JSON.readTree(snapshot).path(key);
            return value.isNumber() ? value.decimalValue() : null;
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    private static BigDecimal returnRate(String direction, BigDecimal price, BigDecimal entryPrice, Integer leverage) {
        if (price == null || entryPrice == null || price.signum() <= 0 || entryPrice.signum() <= 0) {
            return null;
        }
        BigDecimal change = "SHORT".equalsIgnoreCase(direction) ? entryPrice.subtract(price).divide(entryPrice, 12, java.math.RoundingMode.HALF_UP) : price.subtract(entryPrice).divide(entryPrice, 12, java.math.RoundingMode.HALF_UP);
        return change.multiply(BigDecimal.valueOf(leverage == null || leverage <= 0 ? 1 : leverage));
    }

    /**
     * 从本地保护单意图和冻结持仓快照生成可解释的交易所止盈止损原因。
     */
    private static String protectionReason(Map<String, Object> row, DecisionLogEvent event) {
        String type = text(row, "protection_type");
        String direction = Objects.toString(text(row, "position_direction"), text(row, "position_side"));
        BigDecimal trigger = asDecimal(row.get("stop_price"));
        BigDecimal entry = asDecimal(row.get("entry_price"));
        Integer leverage = row.get("frozen_leverage") == null ? null : Integer.valueOf(row.get("frozen_leverage").toString());
        BigDecimal quantity = event.cumulativeQuantity() != null && event.cumulativeQuantity().signum() > 0 ? event.cumulativeQuantity() : event.quantity() != null ? event.quantity() : asDecimal(row.get("quantity"));
        BigDecimal price = event.price();
        String qtyText = amount(quantity);
        String notional = quantity != null && price != null && price.signum() > 0 ? amount(quantity.multiply(price)) + " USDT" : quantity != null && trigger != null ? "约" + amount(quantity.multiply(trigger)) + " USDT（按触发价估算）" : null;
        String quantityLabel = "HARD_STOP".equals(type) || "LOCAL_STOP".equals(type) || "SYMBOL_STOP".equals(type) ? "止损数量" : "止盈数量";
        String qtyDetail = qtyText == null ? "" : "，" + quantityLabel + "=" + qtyText + (notional == null ? "" : "，名义金额=" + notional);
        if ("FIRST_TP".equals(type)) {
            BigDecimal rate = snapshotRate(row, "firstProfitTakingRate");
            return "首次止盈：达到首次止盈率" + Objects.toString(percent(rate), "（冻结参数不可用）") + "，交易所触发价=" + Objects.toString(amount(trigger), "未知") + qtyDetail;
        }
        if ("STEP_TP".equals(type)) {
            BigDecimal step = snapshotRate(row, "stepRatio");
            Integer stepIndex = row.get("step_index") == null ? null : Integer.valueOf(row.get("step_index").toString());
            BigDecimal locked = step == null || stepIndex == null ? null : step.multiply(BigDecimal.valueOf(Math.max(0, stepIndex - 1L)));
            BigDecimal favorable = "SHORT".equalsIgnoreCase(direction) ? asDecimal(row.get("lowest_price_since_entry")) : asDecimal(row.get("highest_price_since_entry"));
            BigDecimal peakReturn = returnRate(direction, favorable, entry, leverage);
            return "阶梯止盈：持仓期间最大回报率=" + Objects.toString(percent(peakReturn), "未知") + "，回撤至第" + Objects.toString(stepIndex, "未知") + "阶保护线（锁定回报率=" + Objects.toString(percent(locked), "未知") + "）触发，触发价=" + Objects.toString(amount(trigger), "未知") + qtyDetail;
        }
        if ("HARD_STOP".equals(type) || "LOCAL_STOP".equals(type) || "SYMBOL_STOP".equals(type)) {
            BigDecimal threshold = "SYMBOL_STOP".equals(type) ? asDecimal(row.get("frozen_symbol_stop_loss_ratio")) : "HARD_STOP".equals(type) ? asDecimal(row.get("frozen_stop_loss_ratio")) : snapshotRate(row, "localStopLossRate");
            if (threshold == null) {
                BigDecimal triggerReturn = returnRate(direction, trigger, entry, leverage);
                threshold = triggerReturn == null ? null : triggerReturn.abs();
            }
            return "止损：达到止损比例" + Objects.toString(percent(threshold), "（冻结参数不可用）") + "，交易所触发价=" + Objects.toString(amount(trigger), "未知") + qtyDetail;
        }
        return "交易所保护单触发：" + Objects.toString(type, "类型未知") + "，触发价=" + Objects.toString(amount(trigger), "未知") + qtyDetail;
    }

    /**
     * 在单个批次中写入已脱敏的决策日志事件。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBatch(List<DecisionLogEvent> batch) {
        Map<List<Object>, List<Map<String, Object>>> lookupCache = new HashMap<>();
        writeEvaluations(batch);
        for (DecisionLogEvent original : batch) {
            if ("EVALUATION".equals(original.recordType())) {
                continue;
            }
            DecisionLogEvent event = "PRIVATE".equals(original.recordType()) ? privateEvent(original) : original;
            if (event == null) {
                continue;
            }
            event = enrich(event, lookupCache);
            if ("ORDER".equals(event.recordType()) && event.cumulativeQuantity() != null && event.cumulativeQuantity().signum() > 0 && event.averagePrice() != null && event.averagePrice().signum() > 0) {
                event = event.toBuilder().quantity(event.cumulativeQuantity()).price(event.averagePrice()).tradeTime(event.eventTime()).status(event.orderStatus()).build();
            }
            if ("FILL".equals(event.recordType())) {
                writeFill(event);
            } else {
                write(event);
                migrateOrderFacts(event);
            }
        }
    }

    private void writeEvaluations(List<DecisionLogEvent> batch) {
        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
        for (DecisionLogEvent e : batch) {
            if (!"EVALUATION".equals(e.recordType())) {
                continue;
            }
            Map<String, Object> values = parameters(e);
            Map<String, Object> previous = groups.get(e.eventKey());
            values.put("count", previous == null ? 1L : (Long) previous.get("count") + 1);
            values.put("firstTime", previous == null ? values.get("eventTime") : previous.get("firstTime"));
            groups.put(e.eventKey(), values);
        }
        if (groups.isEmpty()) {
            return;
        }
        String values = VALUES.replace(":eventTime,:tradeTime,1,:eventTime", ":firstTime,:tradeTime,:count,:eventTime");
        named.batchUpdate("INSERT INTO quant_trade_decision_log (" + COLUMNS + ") VALUES (" + values + ") ON DUPLICATE KEY UPDATE repeat_count=repeat_count+VALUES(repeat_count)," +
            "last_time=GREATEST(last_time,VALUES(last_time)),price=VALUES(price),angle=VALUES(angle),reason=VALUES(reason)", groups.values().toArray(Map[]::new));
    }

    private DecisionLogEvent privateEvent(DecisionLogEvent original) {
        try {
            var json = JSON.readTree(original.rawEvent());
            var o = json.path("o");
            if (o.isMissingNode()) {
                return null;
            }
            String status = o.path("X").asText();
            boolean fill = "TRADE".equals(o.path("x").asText()) && o.path("t").asLong(-1) >= 0;
            return original.toBuilder().recordType(fill ? "FILL" : "ORDER").symbol(o.path("s").asText()).orderId(o.path("i").asLong()).clientOrderId(o.path("c").asText()).orderStatus(status).status(status).cumulativeQuantity(decimal(o.path("z").asText(null))).averagePrice(decimal(o.path("ap").asText(null))).direction(o.path("S").asText()).reduceOnly(o.path("R").asBoolean(false)).tradeId(fill ? o.path("t").asLong() : null).quantity(fill ? decimal(o.path("l").asText(null)) : null).price(fill ? decimal(o.path("L").asText(null)) : null).tradeTime(fill ? Instant.ofEpochMilli(o.path("T").asLong(json.path("T").asLong())) : null).eventTime(Instant.ofEpochMilli(json.path("E").asLong(System.currentTimeMillis()))).rawEvent(null).build();
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("无法解析订单日志事件", ex);
        }
    }

    private DecisionLogEvent enrich(DecisionLogEvent e, Map<List<Object>, List<Map<String, Object>>> lookupCache) {
        var builder = e.toBuilder();
        List<Map<String, Object>> previous = e.eventKey() == null ? List.of() : jdbc.queryForList("SELECT account_name,strategy_name,run_mode FROM quant_trade_decision_log WHERE" +
            " event_key=?", key(e.eventKey()));
        String account = e.accountName();
        String strategy = e.strategyName();
        String mode = e.runMode();
        if (!previous.isEmpty()) {
            account = text(previous.getFirst(), "account_name");
            strategy = text(previous.getFirst(), "strategy_name");
            mode = text(previous.getFirst(), "run_mode");
        }
        if (account == null || mode == null) {
            var credential = cached(lookupCache, "SELECT account_name,run_mode FROM quant_credential WHERE id=?", e.credentialId());
            if (!credential.isEmpty()) {
                if (account == null) {
                    account = text(credential.getFirst(), "account_name");
                }
                if (mode == null) {
                    mode = text(credential.getFirst(), "run_mode");
                }
            }
        }
        Long strategyId = e.strategyId();
        Long instanceId = e.strategyInstanceId();
        String operation = e.operation();
        String executionKey = e.idempotencyKey();
        String direction = e.direction();
        String reason = e.reason();
        String reasonCode = e.reasonCode();
        String eventKey = e.eventKey();
        if ("FILL".equals(e.recordType()) || "ORDER".equals(e.recordType())) {
            var records = cached(lookupCache, "SELECT * FROM quant_execution_record WHERE credential_id=? AND symbol=? AND operation_type IN ('OPEN','CLOSE') AND " +
                "(exchange_order_id=? OR client_order_id=?) ORDER BY created_at DESC LIMIT 1", e.credentialId(), e.symbol(), e.orderId(), e.clientOrderId());
            if (!records.isEmpty()) {
                var record = records.getFirst();
                strategyId = number(record, "strategy_id");
                instanceId = number(record, "strategy_instance_id");
                operation = text(record, "operation_type");
                executionKey = text(record, "idempotency_key");
                eventKey = executionKey(e.credentialId(), executionKey);
            } else {
                var orders = cached(lookupCache, "SELECT strategy_id,strategy_instance_id,reduce_only,position_side,status FROM quant_order WHERE credential_id=? AND symbol=? " +
                    "AND exchange_order_id=? LIMIT 1", e.credentialId(), e.symbol(), e.orderId());
                if (!orders.isEmpty()) {
                    var order = orders.getFirst();
                    strategyId = number(order, "strategy_id");
                    instanceId = number(order, "strategy_instance_id");
                    Object reducing = order.get("reduce_only");
                    if (reducing != null) {
                        operation = (Boolean.TRUE.equals(reducing) || "1".equals(reducing.toString())) ? "CLOSE" : "OPEN";
                    }
                }
                // 保护单通过本地账本和持仓关联策略；绝不按交易对猜测实例归属。
                var protections = cached(lookupCache, "SELECT p.protection_type,p.position_side,p.stop_price,p.quantity,p.step_index,s.instance_id,s.direction position_direction,s.entry_price,s.frozen_leverage," +
                    "s.highest_price_since_entry,s.lowest_price_since_entry,s.strategy_params_snapshot,s.frozen_stop_loss_ratio,s.frozen_symbol_stop_loss_ratio,i.strategy_id FROM quant_protection_order p LEFT JOIN quant_strategy_position_state" +
                    " s ON s.position_id=p.position_id LEFT JOIN quant_strategy_instance i ON i.id=s.instance_id WHERE p.credential_id=? AND p.symbol=? AND (p" +
                    ".exchange_order_id=? OR p.client_order_id=?) LIMIT 1", e.credentialId(), e.symbol(), e.orderId(), e.clientOrderId());
                if (!protections.isEmpty()) {
                    var protection = protections.getFirst();
                    strategyId = number(protection, "strategy_id");
                    instanceId = number(protection, "instance_id");
                    operation = "CLOSE";
                    reasonCode = "EXCHANGE_PROTECTION";
                    reason = protectionReason(protection, e);
                }
                if (operation == null && e.reduceOnly() != null) {
                    operation = e.reduceOnly() ? "CLOSE" : "UNKNOWN";
                }
                eventKey = "O:" + e.credentialId() + ":" + e.symbol() + ":" + e.orderId();
            }
            var existing = jdbc.queryForList("SELECT * FROM quant_trade_decision_log WHERE event_key=?", key(eventKey));
            if (!existing.isEmpty()) {
                var row = existing.getFirst();
                account = text(row, "account_name");
                mode = text(row, "run_mode");
                if (!"EXTERNAL_UNKNOWN".equals(text(row, "reason_code"))) {
                    strategy = text(row, "strategy_name");
                    strategyId = number(row, "strategy_id");
                    instanceId = number(row, "strategy_instance_id");
                    operation = text(row, "operation");
                    direction = text(row, "direction");
                    if (!("EXCHANGE_PROTECTION".equals(reasonCode) && "EXCHANGE_PROTECTION".equals(text(row, "reason_code")))) {
                        reason = text(row, "reason");
                        reasonCode = text(row, "reason_code");
                    }
                } else {
                    direction = direction(operation, direction);
                }
            } else {
                direction = direction(operation, direction);
            }
            if (reason == null) {
                reason = "外部交易，触发原因未确认";
                reasonCode = "EXTERNAL_UNKNOWN";
            }
        }
        if ("PROTECTION".equals(e.recordType())) {
            var rows = cached(lookupCache, "SELECT p.protection_type,p.position_side,p.stop_price,p.quantity,p.step_index,s.direction position_direction,s.entry_price,s.frozen_leverage,s.highest_price_since_entry," +
                "s.lowest_price_since_entry,s.strategy_params_snapshot,s.frozen_stop_loss_ratio,s.frozen_symbol_stop_loss_ratio,i.strategy_id,i.id instance_id FROM quant_protection_order p JOIN quant_strategy_position_state s ON s.position_id=p" +
                ".position_id JOIN quant_strategy_instance i ON i.id=s.instance_id WHERE p.credential_id=? AND p.symbol=? AND p.client_order_id=? LIMIT 1", e.credentialId(),
                e.symbol(), e.clientOrderId());
            if (!rows.isEmpty()) {
                strategyId = number(rows.getFirst(), "strategy_id");
                instanceId = number(rows.getFirst(), "instance_id");
                reason = protectionReason(rows.getFirst(), e);
                reasonCode = "EXCHANGE_PROTECTION";
            }
            builder.recordType("EXECUTION");
        }
        if (strategy == null) {
            if (strategyId != null) {
                var names = cached(lookupCache, "SELECT strategy_name FROM quant_strategy WHERE id=?", strategyId);
                strategy = names.isEmpty() ? "策略已删除（" + strategyId + "）" : text(names.getFirst(), "strategy_name");
            } else {
                strategy = executionKey != null ? "手动操作" : "外部交易／策略未确认";
            }
        }
        return builder.accountName(account).runMode(mode).strategyId(strategyId).strategyInstanceId(instanceId).strategyName(strategy).operation(operation).direction(direction).idempotencyKey(executionKey).eventKey(eventKey).reason(reason).reasonCode(reasonCode).eventTime(e.eventTime() == null ? Instant.now() : e.eventTime()).build();
    }

    private List<Map<String, Object>> cached(Map<List<Object>, List<Map<String, Object>>> cache, String sql, Object... args) {
        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add(sql);
        cacheKey.addAll(Arrays.asList(args));
        return cache.computeIfAbsent(cacheKey, ignored -> jdbc.queryForList(sql, args));
    }

    private void write(DecisionLogEvent e) {
        Map<String, Object> values = parameters(e);
        // Duplicate handling uses the existing row so late acknowledgements cannot erase fills.
        var rows = jdbc.queryForList("SELECT * FROM quant_trade_decision_log WHERE event_key=?", key(e.eventKey()));
        if (rows.isEmpty()) {
            named.update("INSERT INTO quant_trade_decision_log (" + COLUMNS + ") VALUES (" + VALUES + ")", values);
            return;
        }
        var old = rows.getFirst();
        boolean actual = old.get("trade_time") != null;
        boolean terminal = Set.of("FILLED", "CANCELED", "EXPIRED", "REJECTED", "EXPIRED_IN_MATCH").contains(Objects.toString(old.get("status"), ""));
        boolean incomingTerminal = Set.of("CANCELED", "EXPIRED", "REJECTED", "EXPIRED_IN_MATCH").contains(Objects.toString(e.status(), ""));
        boolean newer = timestamp(e.eventTime()).compareTo(asTimestamp(old.get("last_time"))) >= 0;
        String status = e.tradeTime() != null || (!terminal && incomingTerminal && newer) || (!actual && !terminal && newer) ? e.status() : text(old, "status");
        if (terminal && !"FILLED".equals(e.status())) {
            status = text(old, "status");
        }
        boolean olderFill = actual && e.quantity() != null && old.get("quantity") instanceof BigDecimal oldQuantity && e.quantity().compareTo(oldQuantity) < 0;
        if (olderFill) {
            status = text(old, "status");
        }
        if ("EXTERNAL_UNKNOWN".equals(text(old, "reason_code")) && !"EXTERNAL_UNKNOWN".equals(e.reasonCode())) {
            jdbc.update("UPDATE quant_trade_decision_log SET strategy_id=?,strategy_instance_id=?,strategy_name=?,operation=? WHERE event_key=?", e.strategyId(),
                e.strategyInstanceId(), e.strategyName(), e.operation(), key(e.eventKey()));
        }
        boolean preserveReason = "EXECUTION_RESULT".equals(e.reasonCode());
        // 首次确认成交时清除计划数值；缺少成交均价时留空，不能把参考价标为实际成交价。
        boolean firstActual = !actual && e.tradeTime() != null;
        Object quantity = firstActual ? e.quantity() : (olderFill || actual && e.tradeTime() == null || e.quantity() == null) ? old.get("quantity") : e.quantity();
        // 累计数量增加但均价未知时，旧均价不能代表新的累计成交。
        boolean growingActual =
            actual && e.tradeTime() != null && e.quantity() != null && (!(old.get("quantity") instanceof BigDecimal oldQuantity) || e.quantity().compareTo(oldQuantity) > 0);
        Object price = firstActual || growingActual ? e.price() : (olderFill || actual && e.tradeTime() == null || e.price() == null) ? old.get("price") : e.price();
        jdbc.update("UPDATE quant_trade_decision_log SET status=?,reason=?,reason_code=?,order_id=COALESCE(?,order_id),client_order_id=COALESCE(?,client_order_id),quantity=?," +
            "price=?,trade_time=COALESCE(?,trade_time),direction=COALESCE(?,direction),margin=COALESCE(?,margin),last_time=GREATEST(last_time,?) WHERE event_key=?", status,
            preserveReason ? old.get("reason") : e.reason(), preserveReason ? old.get("reason_code") : e.reasonCode(), e.orderId(), e.clientOrderId(), quantity, price,
            olderFill ? null : timestamp(e.tradeTime()), e.direction(), e.margin(), timestamp(e.eventTime()), key(e.eventKey()));
    }

    private void migrateOrderFacts(DecisionLogEvent e) {
        if (e.idempotencyKey() == null || e.orderId() == null) {
            return;
        }
        String source = key("O:" + e.credentialId() + ":" + e.symbol() + ":" + e.orderId());
        String target = key(e.eventKey());
        if (source.equals(target)) {
            return;
        }
        var sources = jdbc.queryForList("SELECT * FROM quant_trade_decision_log WHERE event_key=?", source);
        if (!sources.isEmpty()) {
            var row = sources.getFirst();
            if (row.get("trade_time") != null) {
                write(e.toBuilder().recordType("EXECUTION").quantity(asDecimal(row.get("quantity"))).price(asDecimal(row.get("price"))).tradeTime(asTimestamp(row.get("trade_time"
                )).toInstant()).status(text(row, "status")).build());
            }
        }
        jdbc.update("UPDATE quant_trade_decision_log SET parent_key=? WHERE parent_key=? AND record_type='FILL'", target, source);
        // Only remove the old display row after its facts have been transferred.
        jdbc.update("DELETE FROM quant_trade_decision_log WHERE event_key=? AND record_type<>'FILL'", source);
        aggregateFills(e);
    }

    private void writeFill(DecisionLogEvent e) {
        if (e.tradeId() == null || e.quantity() == null || e.quantity().signum() <= 0 || e.price() == null || e.price().signum() <= 0) {
            return;
        }
        String parent = key(e.eventKey());
        DecisionLogEvent fill = e.toBuilder().eventKey("F:" + e.credentialId() + ":" + e.symbol() + ":" + e.tradeId()).build();
        Map<String, Object> values = parameters(fill);
        values.put("parentKey", parent);
        named.update("INSERT INTO quant_trade_decision_log (" + COLUMNS + ",parent_key) VALUES (" + VALUES + ",:parentKey) ON DUPLICATE KEY UPDATE parent_key=VALUES(parent_key)"
            , values);
        // 创建/补充执行主行，成交明细仅供聚合，不在列表中重复显示。
        boolean cumulative = e.cumulativeQuantity() != null && e.cumulativeQuantity().signum() > 0 && e.averagePrice() != null && e.averagePrice().signum() > 0;
        write(e.toBuilder().recordType("EXECUTION").tradeTime(cumulative ? e.tradeTime() : null).quantity(cumulative ? e.cumulativeQuantity() : null).price(cumulative ?
            e.averagePrice() : null).status(cumulative ? Objects.toString(e.orderStatus(), "PARTIALLY_FILLED") : "ACCEPTED").build());
        migrateOrderFacts(e);
        aggregateFills(e);
    }

    private void aggregateFills(DecisionLogEvent e) {
        String parent = key(e.eventKey());
        var totals = jdbc.queryForMap("SELECT SUM(quantity) qty,SUM(quantity*price) amount,MAX(trade_time) last_fill FROM quant_trade_decision_log WHERE parent_key=? AND " +
            "record_type='FILL'", parent);
        BigDecimal quantity = asDecimal(totals.get("qty"));
        BigDecimal amount = asDecimal(totals.get("amount"));
        if (quantity == null || amount == null || quantity.signum() <= 0) {
            return;
        }
        BigDecimal avg = amount.divide(quantity, 16, java.math.RoundingMode.HALF_UP);
        String status = "FILLED".equals(e.orderStatus()) ? "FILLED" : "PARTIALLY_FILLED";
        // REST 成交列表没有订单终态时使用已存在的本地订单状态；不能凭一次成交推断全部成交。
        if (e.orderStatus() == null) {
            var orders = jdbc.queryForList("SELECT status FROM quant_order WHERE credential_id=? AND symbol=? AND exchange_order_id=? LIMIT 1", e.credentialId(), e.symbol(),
                e.orderId());
            if (!orders.isEmpty() && "FILLED".equals(text(orders.getFirst(), "status"))) {
                status = "FILLED";
            }
        }
        jdbc.update("UPDATE quant_trade_decision_log SET quantity=?,price=?,trade_time=?,status=CASE WHEN status IN ('FILLED','CANCELED','EXPIRED','REJECTED','EXPIRED_IN_MATCH')" +
            " THEN status ELSE ? END,last_time=GREATEST(last_time,?) WHERE event_key=? AND (trade_time IS NULL OR quantity IS NULL OR quantity<=?)", quantity, avg, totals.get(
                "last_fill"), status, timestamp(e.eventTime()), parent, quantity);
    }

    private Map<String, Object> parameters(DecisionLogEvent e) {
        Map<String, Object> m = new HashMap<>();
        m.put("eventKey", key(e.eventKey()));
        m.put("recordType", e.recordType());
        m.put("credentialId", e.credentialId());
        m.put("accountName", e.accountName());
        m.put("runMode", e.runMode());
        m.put("strategyId", e.strategyId());
        m.put("strategyInstanceId", e.strategyInstanceId());
        m.put("strategyName", e.strategyName());
        m.put("symbol", e.symbol());
        m.put("operation", e.operation());
        m.put("direction", e.direction());
        m.put("status", e.status());
        m.put("reasonCode", e.reasonCode());
        m.put("reason", e.reason());
        m.put("angle", e.angle());
        m.put("threshold", e.threshold());
        m.put("klineTime", timestamp(e.klineTime()));
        m.put("quantity", e.quantity());
        m.put("price", e.price());
        m.put("margin", e.margin());
        m.put("idempotencyKey", e.idempotencyKey());
        m.put("orderId", e.orderId());
        m.put("clientOrderId", e.clientOrderId());
        m.put("eventTime", timestamp(e.eventTime()));
        m.put("tradeTime", timestamp(e.tradeTime()));
        return m;
    }

    /**
     * 按查询条件分页读取决策日志。
     */
    public PageResult<Map<String, Object>> list(TradeDecisionQuery q) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE record_type<>'FILL'");
        add(where, args, "credential_id", q.getCredentialId(), false);
        add(where, args, "strategy_name", q.getStrategyName(), true);
        add(where, args, "strategy_instance_id", q.getStrategyInstanceId(), false);
        add(where, args, "symbol", q.getSymbol(), false);
        add(where, args, "run_mode", q.getRunMode(), false);
        add(where, args, "operation", q.getOperation(), false);
        add(where, args, "status", q.getStatus(), false);
        add(where, args, "reason", q.getReason(), true);
        LocalDateTime to = q.getTo() == null ? LocalDateTime.now(ZoneId.of("Asia/Shanghai")) : q.getTo();
        LocalDateTime from = q.getFrom() == null ? to.minusHours(24) : q.getFrom();
        if (from.isAfter(to)) {
            throw new org.dromara.common.core.exception.ServiceException("开始时间不能晚于结束时间");
        }
        where.append(" AND event_time>=? AND event_time<=?");
        args.add(Timestamp.valueOf(from));
        args.add(Timestamp.valueOf(to));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM quant_trade_decision_log" + where, Long.class, args.toArray());
        int size = Math.clamp(q.getPageSize() == null ? 20 : q.getPageSize(), 1, 100);
        long page = Math.max(1, q.getPageNum() == null ? 1 : q.getPageNum());
        args.add(size);
        args.add((page - 1) * size);
        var rows = jdbc.queryForList("SELECT * FROM quant_trade_decision_log" + where + " ORDER BY event_time DESC,id DESC LIMIT ? OFFSET ?", args.toArray());
        return PageResult.build(rows.stream().map(this::display).toList(), total == null ? 0 : total);
    }

    /**
     * 查询单条决策日志及其可用关联记录。
     */
    public Map<String, Object> detail(Long id) {
        var rows = jdbc.queryForList("SELECT * FROM quant_trade_decision_log WHERE id=? AND record_type<>'FILL'", id);
        return rows.isEmpty() ? null : display(rows.getFirst());
    }

    private Map<String, Object> display(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((column, value) -> {
            String[] parts = column.split("_");
            StringBuilder name = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                name.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
            if (value instanceof Number && (column.equals("id") || column.endsWith("_id"))) {
                value = value.toString();
            }
            if (value instanceof BigDecimal) {
                value = value.toString();
            }
            if (value instanceof Timestamp t) {
                value = t.toLocalDateTime().toString().replace('T', ' ');
            }
            if (value instanceof LocalDateTime t) {
                value = t.toString().replace('T', ' ');
            }
            result.put(name.toString(), value);
        });
        BigDecimal qty = decimal(Objects.toString(row.get("quantity"), null));
        BigDecimal price = decimal(Objects.toString(row.get("price"), null));
        result.put("amount", qty == null || price == null ? null : qty.multiply(price).stripTrailingZeros().toPlainString());
        result.put("priceKind", row.get("trade_time") == null ? "REFERENCE" : "ACTUAL");
        return result;
    }
}

package org.dromara.quant.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.account.domain.vo.TradeHistoryPageVo;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.execution.AccountPositionCycleProjectionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账户开平仓周期查询服务。所有周期、成交分配和手续费汇总均来自本地持久化投影。
 * GET 请求只执行权限校验及数据库分页，不调用 Binance，也不在请求中重建成交周期。
 */
@Service
@RequiredArgsConstructor
public class TradeHistoryService {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    private final QuantCredentialMapper credentialMapper;
    private final AccountPositionCycleProjectionMapper cycleMapper;

    public TradeHistoryPageVo page(PageQuery query, Long credentialId, String symbol, String from, String to) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录后查询开平仓记录");
        }
        List<QuantCredential> credentials = credentialMapper.selectList(new LambdaQueryWrapper<QuantCredential>()
            .select(QuantCredential::getId, QuantCredential::getUserId, QuantCredential::getAccountName)
            .eq(!LoginHelper.isSuperAdmin(userId), QuantCredential::getUserId, userId));
        Map<Long, String> accountNames = credentials.stream()
            .collect(Collectors.toMap(QuantCredential::getId, QuantCredential::getAccountName));
        if (credentialId != null && !accountNames.containsKey(credentialId)) {
            throw new ServiceException("无权访问该交易账户");
        }
        List<Long> credentialIds = credentialId == null
            ? credentials.stream().map(QuantCredential::getId).toList()
            : List.of(credentialId);
        if (credentialIds.isEmpty()) {
            return new TradeHistoryPageVo(List.of(), 0, 0);
        }

        LocalDateTime start = boundary(from, false);
        LocalDateTime end = boundary(to, true);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }
        String normalizedSymbol = symbol == null || symbol.isBlank()
            ? null : symbol.trim().toUpperCase(Locale.ROOT);
        if (normalizedSymbol != null && !normalizedSymbol.matches("[A-Z0-9]{2,32}")) {
            throw new ServiceException("交易对格式无效");
        }

        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        long offset = (long) (pageNum - 1) * pageSize;
        long total = cycleMapper.countCycles(credentialIds, normalizedSymbol, start, end);
        long incompleteCount = cycleMapper.countIncompleteScopes(credentialIds, normalizedSymbol);
        if (total == 0) {
            return new TradeHistoryPageVo(List.of(), 0, (int) Math.min(Integer.MAX_VALUE, incompleteCount));
        }

        List<Map<String, Object>> cycles = cycleMapper.selectCycles(credentialIds, normalizedSymbol, start, end, offset, pageSize);
        if (cycles == null || cycles.isEmpty()) {
            return new TradeHistoryPageVo(List.of(), total, (int) Math.min(Integer.MAX_VALUE, incompleteCount));
        }
        List<Long> cycleIds = cycles.stream().map(row -> longValue(row.get("id"))).filter(id -> id != null).toList();
        Map<Long, List<Map<String, Object>>> allocations = groupByCycle(cycleMapper.selectCycleAllocations(cycleIds));
        Map<Long, Map<String, BigDecimal>> fees = groupFees(cycleMapper.selectCycleFeeTotals(cycleIds));

        List<Map<String, Object>> rows = new ArrayList<>(cycles.size());
        for (Map<String, Object> cycle : cycles) {
            Long id = longValue(cycle.get("id"));
            rows.add(toRow(cycle, accountNames, allocations.getOrDefault(id, List.of()), fees.getOrDefault(id, Map.of())));
        }
        return new TradeHistoryPageVo(rows, total, (int) Math.min(Integer.MAX_VALUE, incompleteCount));
    }

    private static Map<String, Object> toRow(Map<String, Object> cycle, Map<Long, String> accountNames,
                                               List<Map<String, Object>> allocations, Map<String, BigDecimal> fees) {
        Long id = longValue(cycle.get("id"));
        Long credentialId = longValue(cycle.get("credentialId"));
        BigDecimal openingQuantity = decimal(cycle.get("openingQuantity"));
        BigDecimal closingQuantity = decimal(cycle.get("closingQuantity"));
        BigDecimal remainingQuantity = decimal(cycle.get("remainingQuantity"));
        BigDecimal openingNotional = nullableDecimal(cycle.get("openingNotional"));
        BigDecimal closingNotional = decimal(cycle.get("closingNotional"));
        LocalDateTime openedAt = localTime(cycle.get("openedAt"));
        LocalDateTime closedAt = localTime(cycle.get("closedAt"));

        Set<String> openingOrderIds = new LinkedHashSet<>();
        Set<String> closingOrderIds = new LinkedHashSet<>();
        Set<String> openingTradeIds = new LinkedHashSet<>();
        Set<String> closingTradeIds = new LinkedHashSet<>();
        for (Map<String, Object> allocation : allocations) {
            String role = string(allocation.get("allocationRole"));
            String orderId = string(allocation.get("exchangeOrderId"));
            String tradeId = string(allocation.get("exchangeTradeId"));
            if ("OPEN".equalsIgnoreCase(role)) {
                if (orderId != null) openingOrderIds.add(orderId);
                if (tradeId != null) openingTradeIds.add(tradeId);
            } else if ("CLOSE".equalsIgnoreCase(role)) {
                if (orderId != null) closingOrderIds.add(orderId);
                if (tradeId != null) closingTradeIds.add(tradeId);
            }
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id == null ? null : id.toString());
        row.put("credentialId", credentialId == null ? null : credentialId.toString());
        row.put("accountName", accountNames.get(credentialId));
        row.put("symbol", string(cycle.get("symbol")));
        row.put("direction", string(cycle.get("direction")));
        row.put("positionSide", string(cycle.get("positionSide")));
        row.put("positionCycleKey", string(cycle.get("positionCycleKey")));
        row.put("openingQuantity", decimalText(openingQuantity));
        row.put("closingQuantity", decimalText(closingQuantity));
        row.put("remainingQuantity", decimalText(remainingQuantity));
        row.put("openingFillCount", intValue(cycle.get("openingFillCount")));
        row.put("closingFillCount", intValue(cycle.get("closingFillCount")));
        row.put("openingOrderIds", List.copyOf(openingOrderIds));
        row.put("closingOrderIds", List.copyOf(closingOrderIds));
        row.put("openingTradeIds", List.copyOf(openingTradeIds));
        row.put("closingTradeIds", List.copyOf(closingTradeIds));
        row.put("entryPrice", openingNotional == null || openingQuantity.signum() == 0
            ? null : decimalText(openingNotional.divide(openingQuantity, 18, RoundingMode.HALF_UP)));
        row.put("openingNotional", decimalText(openingNotional));
        row.put("leverage", cycle.get("leverage"));
        row.put("openingBalance", fixedDecimal(nullableDecimal(cycle.get("openingBalance")), 4));
        row.put("openingBalanceAt", timestamp(localTime(cycle.get("openingBalanceAt"))));
        row.put("openedAt", timestamp(openedAt));
        row.put("closePrice", closingQuantity.signum() == 0
            ? null : decimalText(closingNotional.divide(closingQuantity, 18, RoundingMode.HALF_UP)));
        row.put("closingBalance", fixedDecimal(nullableDecimal(cycle.get("closingBalance")), 4));
        row.put("closingBalanceAt", timestamp(localTime(cycle.get("closingBalanceAt"))));
        row.put("realizedPnl", decimalText(decimal(cycle.get("realizedPnl"))));
        row.put("commissionByAsset", fees.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey, entry -> decimalText(entry.getValue()), (a, b) -> a, LinkedHashMap::new)));
        row.put("commission", fees.size() == 1 ? decimalText(fees.values().iterator().next()) : null);
        row.put("commissionAsset", fees.size() == 1 ? fees.keySet().iterator().next() : null);
        row.put("closedAt", timestamp(closedAt));
        row.put("status", string(cycle.get("status")));
        row.put("baselineUnknown", Boolean.TRUE.equals(cycle.get("baselineUnknown")) || intValue(cycle.get("baselineUnknown")) == 1);
        return row;
    }

    private static Map<Long, List<Map<String, Object>>> groupByCycle(List<Map<String, Object>> rows) {
        Map<Long, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Long cycleId = longValue(row.get("cycleId"));
                if (cycleId != null) {
                    result.computeIfAbsent(cycleId, ignored -> new ArrayList<>()).add(row);
                }
            }
        }
        return result;
    }

    private static Map<Long, Map<String, BigDecimal>> groupFees(List<Map<String, Object>> rows) {
        Map<Long, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Long cycleId = longValue(row.get("cycleId"));
                String asset = string(row.get("commissionAsset"));
                if (cycleId != null && asset != null) {
                    result.computeIfAbsent(cycleId, ignored -> new LinkedHashMap<>())
                        .put(asset, decimal(row.get("commission")));
                }
            }
        }
        return result;
    }

    private static LocalDateTime boundary(String date, boolean nextDay) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(date);
            return LocalDateTime.ofInstant((nextDay ? parsed.plusDays(1) : parsed).atStartOfDay(DISPLAY_ZONE).toInstant(), ZoneOffset.UTC);
        } catch (java.time.DateTimeException ex) {
            throw new ServiceException("日期格式应为 yyyy-MM-dd");
        }
    }

    private static String timestamp(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }

    private static String fixedDecimal(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal decimal(Object value) {
        BigDecimal result = nullableDecimal(value);
        return result == null ? BigDecimal.ZERO : result;
    }

    private static BigDecimal nullableDecimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        if (value == null) return null;
        return new BigDecimal(value.toString());
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        return Long.parseLong(value.toString());
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        return Integer.parseInt(value.toString());
    }

    private static LocalDateTime localTime(Object value) {
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }
}

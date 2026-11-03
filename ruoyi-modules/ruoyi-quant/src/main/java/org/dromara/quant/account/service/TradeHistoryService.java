package org.dromara.quant.account.service;

import lombok.RequiredArgsConstructor;
import org.dromara.quant.account.domain.vo.TradeHistoryPageVo;
import org.dromara.quant.account.TradeHistoryAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.account.domain.vo.TradeHistoryFill;
import org.dromara.quant.account.mapper.TradeHistoryMapper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.OrderLedgerService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Locale;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 开平仓记录查询服务，仅读取本地账本及交易所历史普通订单用于补齐，不参与策略决策和交易所下单。
 * 时间范围按北京时间的开仓日期筛选，完整成交周期汇总之后再分页。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeHistoryService {
    private static final Duration ORDER_HISTORY_WINDOW = Duration.ofDays(6);
    private static final Duration ORDER_CREATION_LOOKBACK = Duration.ofDays(7);
    private static final Duration ORDER_UPDATE_LOOKAHEAD = Duration.ofMinutes(1);
    private static final int MAX_MISSING_ORDER_IDS = 5000;
    private static final int MAX_ORDER_HISTORY_REQUESTS = 15;
    private static final int MAX_EXACT_ORDER_REQUESTS = 5;
    private static final int ORDER_HISTORY_LIMIT = 1000;

    private final QuantCredentialMapper credentialMapper;
    private final TradeHistoryMapper historyMapper;
    private final ExchangeGateway exchangeGateway;
    private final OrderLedgerService orderLedgerService;

    /**
     * 从 Binance 历史普通订单中补齐成交账本缺失的订单事实。
     * 每次页面请求限制 REST 次数；网络请求在数据库事务之外执行。
     */
    private void recoverMissingOrderFacts(List<Long> credentialIds, List<TradeHistoryFill> fills, String requestedSymbol) {
        if (credentialIds.isEmpty() || fills.isEmpty()) {
            return;
        }
        int[] remainingRequests = {MAX_ORDER_HISTORY_REQUESTS};
        int[] remainingExactRequests = {MAX_EXACT_ORDER_REQUESTS};
        Instant retentionStart = Instant.now().atZone(ZoneOffset.UTC).minusMonths(3).toInstant();

        for (Long credentialId : credentialIds) {
            List<String> symbols = fills.stream()
                .filter(fill -> credentialId.equals(fill.getCredentialId()))
                .map(TradeHistoryFill::getSymbol)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
            QuantCredential credential = null;
            for (String currentSymbol : symbols) {
                if (requestedSymbol != null && !requestedSymbol.equals(currentSymbol)) {
                    continue;
                }
                List<Long> missingOrderIds = historyMapper.selectOrderIdsMissingOrderFacts(credentialId, currentSymbol, MAX_MISSING_ORDER_IDS);
                if (missingOrderIds.isEmpty()) {
                    continue;
                }
                log.info("开平仓记录发现缺失订单事实 credentialId={} symbol={} missingOrderCount={}",
                    credentialId, currentSymbol, missingOrderIds.size());
                if (credential == null) {
                    credential = credentialMapper.selectById(credentialId);
                }
                if (credential == null || "PAPER".equalsIgnoreCase(credential.getRunMode())
                    || credential.getApiKey() == null || credential.getApiKey().isBlank()
                    || credential.getSecretKey() == null || credential.getSecretKey().isBlank()) {
                    continue;
                }

                Set<Long> targetOrderIds = new HashSet<>(missingOrderIds);
                Set<Long> recoveredOrderIds = new HashSet<>();
                List<TradeHistoryFill> scopedFills = fills.stream()
                    .filter(fill -> credentialId.equals(fill.getCredentialId()) && currentSymbol.equals(fill.getSymbol())
                        && fill.getExchangeOrderId() != null && targetOrderIds.contains(fill.getExchangeOrderId())
                        && fill.getTradeTime() != null)
                    .toList();
                if (scopedFills.isEmpty()) {
                    continue;
                }
                Instant from = scopedFills.stream().map(fill -> fill.getTradeTime().toInstant(ZoneOffset.UTC)).min(Instant::compareTo)
                    .orElseThrow().minus(ORDER_CREATION_LOOKBACK);
                Instant to = scopedFills.stream().map(fill -> fill.getTradeTime().toInstant(ZoneOffset.UTC)).max(Instant::compareTo)
                    .orElseThrow().plus(ORDER_UPDATE_LOOKAHEAD);
                Instant now = Instant.now();
                if (to.isAfter(now.plusMillis(1))) {
                    to = now.plusMillis(1);
                }
                if (from.isBefore(retentionStart)) {
                    from = retentionStart;
                }
                if (!from.isBefore(to)) {
                    continue;
                }

                ExchangeGateway.Credential exchangeCredential = new ExchangeGateway.Credential(
                    credential.getApiKey(), credential.getSecretKey(), credential.getSignatureType(), credential.getRunMode());
                for (Instant windowStart = from; windowStart.isBefore(to) && remainingRequests[0] > 0;) {
                    Instant windowEnd = windowStart.plus(ORDER_HISTORY_WINDOW);
                    if (windowEnd.isAfter(to)) {
                        windowEnd = to;
                    }
                    try {
                        recoverOrderHistoryRange(credentialId, currentSymbol, exchangeCredential, targetOrderIds, recoveredOrderIds,
                            windowStart, windowEnd, 0, remainingRequests);
                    } catch (RuntimeException ex) {
                        log.warn("开平仓记录历史订单补齐失败 credentialId={} symbol={} errorType={}",
                            credentialId, currentSymbol, ex.getClass().getSimpleName());
                        break;
                    }
                    windowStart = windowEnd;
                }
                Set<Long> recentOrderIds = scopedFills.stream()
                    .filter(fill -> !fill.getTradeTime().toInstant(ZoneOffset.UTC).isBefore(retentionStart))
                    .map(TradeHistoryFill::getExchangeOrderId)
                    .collect(Collectors.toSet());
                for (Long orderId : missingOrderIds.stream().filter(recentOrderIds::contains).sorted(Comparator.reverseOrder()).toList()) {
                    if (remainingExactRequests[0] <= 0) {
                        break;
                    }
                    if (recoveredOrderIds.contains(orderId)) {
                        continue;
                    }
                    remainingExactRequests[0]--;
                    try {
                        ExchangeGateway.OrderSnapshot snapshot = exchangeGateway.queryOrder(exchangeCredential, currentSymbol, orderId, null);
                        if (snapshot != null && orderId.equals(snapshot.orderId()) && currentSymbol.equalsIgnoreCase(snapshot.symbol())) {
                            orderLedgerService.syncHistoricalSnapshot(credentialId, currentSymbol, snapshot);
                            if (snapshot.positionSide() != null && !snapshot.positionSide().isBlank() && snapshot.reduceOnly() != null) {
                                recoveredOrderIds.add(orderId);
                            }
                        }
                    } catch (RuntimeException ex) {
                        log.warn("Binance 精确历史订单补齐失败 credentialId={} symbol={} orderId={} errorType={}",
                            credentialId, currentSymbol, orderId, ex.getClass().getSimpleName());
                    }
                }
                if (remainingRequests[0] <= 0 && remainingExactRequests[0] <= 0) {
                    return;
                }
            }
        }
    }

    private void recoverOrderHistoryRange(Long credentialId, String symbol, ExchangeGateway.Credential credential,
                                          Set<Long> targetOrderIds, Set<Long> recoveredOrderIds, Instant from, Instant to,
                                          int depth, int[] remainingRequests) {
        if (remainingRequests[0] <= 0 || !from.isBefore(to)) {
            return;
        }
        remainingRequests[0]--;
        List<ExchangeGateway.OrderSnapshot> snapshots = exchangeGateway.queryOrderHistory(
            credential, symbol, new ExchangeGateway.TimeRange(from, to), (long) ORDER_HISTORY_LIMIT);
        if (snapshots == null) {
            return;
        }
        long matchedCount = snapshots.stream().filter(snapshot -> snapshot != null && snapshot.orderId() != null
            && targetOrderIds.contains(snapshot.orderId())).count();
        long completePositionFacts = snapshots.stream().filter(snapshot -> snapshot != null && snapshot.orderId() != null
            && targetOrderIds.contains(snapshot.orderId()) && snapshot.positionSide() != null && !snapshot.positionSide().isBlank()
            && snapshot.reduceOnly() != null).count();
        log.info("开平仓记录历史订单查询完成 credentialId={} symbol={} returnedCount={} matchedCount={} completePositionFacts={}",
            credentialId, symbol, snapshots.size(), matchedCount, completePositionFacts);
        for (ExchangeGateway.OrderSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.orderId() == null || !targetOrderIds.contains(snapshot.orderId())) {
                continue;
            }
            try {
                orderLedgerService.syncHistoricalSnapshot(credentialId, symbol, snapshot);
                if (snapshot.positionSide() != null && !snapshot.positionSide().isBlank() && snapshot.reduceOnly() != null) {
                    recoveredOrderIds.add(snapshot.orderId());
                }
            } catch (RuntimeException ex) {
                log.warn("历史订单事实写入冲突 credentialId={} symbol={} orderId={} errorType={}",
                    credentialId, symbol, snapshot.orderId(), ex.getClass().getSimpleName());
            }
        }
        if (snapshots.size() >= ORDER_HISTORY_LIMIT && depth < 8) {
            long spanMillis = Duration.between(from, to).toMillis();
            if (spanMillis > 1) {
                Instant middle = from.plusMillis(spanMillis / 2);
                recoverOrderHistoryRange(credentialId, symbol, credential, targetOrderIds, recoveredOrderIds, from, middle, depth + 1, remainingRequests);
                recoverOrderHistoryRange(credentialId, symbol, credential, targetOrderIds, recoveredOrderIds, middle, to, depth + 1, remainingRequests);
                return;
            }
            log.warn("Binance 历史订单查询达到单次上限且时间窗口无法继续拆分 credentialId={} symbol={}", credentialId, symbol);
        }
    }


    /**
     * 查询当前用户有权访问的账户开平仓周期。
     *
     * 先读取完整成交链并归组，再应用开仓日期筛选与分页，避免分页或日期范围截断持仓周期。
     */
    public TradeHistoryPageVo page(PageQuery query, Long credentialId, String symbol, String from, String to) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录后查询开平仓记录");
        }
        List<QuantCredential> credentials = credentialMapper.selectList(new LambdaQueryWrapper<QuantCredential>()
            .select(QuantCredential::getId, QuantCredential::getUserId, QuantCredential::getAccountName)
            .eq(!LoginHelper.isSuperAdmin(userId), QuantCredential::getUserId, userId));
        Map<Long, String> names = credentials.stream()
            .collect(Collectors.toMap(QuantCredential::getId, QuantCredential::getAccountName));
        if (credentialId != null && !names.containsKey(credentialId)) {
            throw new ServiceException("无权访问该交易账户");
        }

        List<Long> credentialIds = credentialId == null ? new ArrayList<>(names.keySet()) : List.of(credentialId);
        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        Instant start = boundary(from, false);
        Instant end = boundary(to, true);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }
        String normalizedSymbol = symbol == null || symbol.isBlank() ? null : symbol.trim().toUpperCase(Locale.ROOT);

        List<TradeHistoryFill> fills = credentialIds.isEmpty() ? List.of() : historyMapper.selectFills(credentialIds, normalizedSymbol);
        if (fills.size() > 20000) {
            throw new ServiceException("成交账本超过单次汇总上限，请选择客户及交易对后查询，避免截断持仓链");
        }
        if (fills.isEmpty()) {
            log.info("开平仓记录没有找到本地成交 credentialCount={} symbol={}", credentialIds.size(), normalizedSymbol);
        }

        recoverMissingOrderFacts(credentialIds, fills, normalizedSymbol);
        fills = credentialIds.isEmpty() ? List.of() : historyMapper.selectFills(credentialIds, normalizedSymbol);
        if (fills.size() > 20000) {
            throw new ServiceException("成交账本超过单次汇总上限，请选择客户及交易对后查询，避免截断持仓链");
        }
        // 一对多来源关联不能使用 DISTINCT 掩盖，否则同一成交可能被重复计入数量和手续费。
        if (fills.stream().map(TradeHistoryFill::getId).distinct().count() != fills.size()) {
            throw new ServiceException("成交来源关联重复，暂不能可靠汇总开平仓记录");
        }

        var result = TradeHistoryAssembler.assemble(fills.stream().map(fill -> new TradeHistoryAssembler.Fill(
            fill.getId(), fill.getCredentialId(), fill.getSymbol(), fill.getSide(), fill.getPositionSide(), fill.getOperationType(),
            fill.getReduceOnly(), fill.getQuantity(), fill.getPrice(), fill.getCommission(), fill.getRealizedPnl(), fill.getTradeTime(),
            fill.getLeverageSnapshot(), fill.getPriceTickSizeSnapshot(), fill.getPostFillWalletBalance(), fill.getPostFillBalanceAt(),
            fill.getCurrentPriceTickSize(), fill.getExchangeTradeId(), fill.getExchangeOrderId(), fill.getPositionCycleConflict(),
            fill.getPositionCycleKey()
        )).toList(), names);

        List<Map<String, Object>> rows = result.rows().stream().filter(row -> {
            Instant openedAt = Instant.parse((String) row.get("openedAt"));
            return (start == null || !openedAt.isBefore(start)) && (end == null || openedAt.isBefore(end));
        }).toList();
        if (rows.isEmpty() && !fills.isEmpty()) {
            long missingPositionSide = fills.stream()
                .filter(fill -> fill.getPositionSide() == null || fill.getPositionSide().isBlank()).count();
            long missingOperationType = fills.stream()
                .filter(fill -> fill.getOperationType() == null || fill.getOperationType().isBlank()).count();
            log.warn("开平仓记录没有可展示周期 credentialCount={} symbol={} fillCount={} assembledCycleCount={} incompleteCount={} missingPositionSide={} missingOperationType={}",
                credentialIds.size(), normalizedSymbol, fills.size(), result.rows().size(), result.incompleteCount(),
                missingPositionSide, missingOperationType);
        }

        long offset = (long) (pageNum - 1) * pageSize;
        int first = (int) Math.min(offset, rows.size());
        return new TradeHistoryPageVo(rows.subList(first, Math.min(first + pageSize, rows.size())),
            rows.size(), result.incompleteCount());
    }

    private static Instant boundary(String date, boolean nextDay) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(date);
            return (nextDay ? parsed.plusDays(1) : parsed).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        } catch (java.time.DateTimeException ex) {
            throw new ServiceException("日期格式应为 yyyy-MM-dd");
        }
    }
}

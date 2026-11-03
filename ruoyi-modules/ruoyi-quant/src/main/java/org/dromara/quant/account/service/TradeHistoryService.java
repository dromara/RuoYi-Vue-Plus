package org.dromara.quant.account.service;

import lombok.RequiredArgsConstructor;
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

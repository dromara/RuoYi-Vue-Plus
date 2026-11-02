package org.dromara.quant.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.account.domain.QuantAccountSnapshot;
import org.dromara.quant.account.domain.QuantTrade;
import org.dromara.quant.account.domain.vo.QuantTradeVo;
import org.dromara.quant.account.mapper.QuantAccountSnapshotMapper;
import org.dromara.quant.account.mapper.QuantTradeMapper;
import org.dromara.quant.account.mapper.TradeHistoryMapper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.OrderLedgerService;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 账户、持仓、委托实时查询门面，以及本地成交账本同步入口。
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/quant")
public class QuantAccountQueryController {
    /**
     * 交易凭证 Mapper。
     */
    private final QuantCredentialMapper credentialMapper;
    /**
     * 交易所统一网关。
     */
    private final ExchangeGateway gateway;
    /**
     * 成交 Mapper。
     */
    private final QuantTradeMapper tradeMapper;
    /**
     * 已持久化的账户权益快照。
     */
    private final QuantAccountSnapshotMapper accountSnapshotMapper;
    private final TradeHistoryMapper tradeHistoryMapper;
    private final OrderLedgerService orderLedgerService;
    private final QuantOrderMapper orderMapper;

    /**
     * 查询指定凭证的账户资金快照。
     */
    @SaCheckPermission(value = {"quant:account:list", "quant:terminal:list"}, mode = SaMode.OR)
    @GetMapping("/account/{credentialId}")
    public R<ExchangeGateway.AccountSnapshot> account(@PathVariable Long credentialId) {
        return R.ok(gateway.queryAccount(credential(credentialId)));
    }

    /**
     * 查询当日零点附近的账户权益基线；缺少可信基线时返回空。
     */
    @SaCheckPermission(value = {"quant:account:list", "quant:terminal:list"}, mode = SaMode.OR)
    @GetMapping("/account/{credentialId}/daily-baseline")
    public R<DailyAccountBaseline> dailyBaseline(@PathVariable Long credentialId) {
        credentialEntity(credentialId);
        LocalDateTime dayStart = LocalDate.now(ZoneId.of("Asia/Shanghai")).atStartOfDay();
        QuantAccountSnapshot baseline = accountSnapshotMapper.selectOne(new LambdaQueryWrapper<QuantAccountSnapshot>()
            .select(QuantAccountSnapshot::getMarginBalance, QuantAccountSnapshot::getObservedAt)
            .eq(QuantAccountSnapshot::getCredentialId, credentialId)
            .ge(QuantAccountSnapshot::getObservedAt, dayStart)
            .lt(QuantAccountSnapshot::getObservedAt, dayStart.plusMinutes(5))
            .orderByAsc(QuantAccountSnapshot::getObservedAt)
            .last("LIMIT 1"));
        return R.ok(baseline == null ? null : new DailyAccountBaseline(baseline.getMarginBalance(), baseline.getObservedAt()));
    }

    /**
     * 当日账户权益基线及其观测时间。
     */
    public record DailyAccountBaseline(java.math.BigDecimal equity, LocalDateTime observedAt) {
    }

    /**
     * 查询指定凭证的当前持仓。
     */
    @SaCheckPermission(value = {"quant:position:list", "quant:terminal:list"}, mode = SaMode.OR)
    @GetMapping("/position/{credentialId}")
    public R<List<ExchangeGateway.PositionSnapshot>> positions(@PathVariable Long credentialId, @RequestParam(required = false) String symbol) {
        return R.ok(gateway.queryPositions(credential(credentialId), symbol));
    }

    /**
     * 查询指定凭证的当前/历史订单。
     */
    @SaCheckPermission(value = {"quant:order:list", "quant:terminal:list"}, mode = SaMode.OR)
    @GetMapping("/order/{credentialId}")
    public R<List<ExchangeGateway.OrderSnapshot>> orders(@PathVariable Long credentialId, @RequestParam(required = false) String symbol) {
        return R.ok(gateway.queryOrders(credential(credentialId), symbol));
    }

    /** 按客户、交易对、方向和状态分页查询本地订单，并返回关联成交数。 */
    @SaCheckPermission("quant:order:list")
    @GetMapping("/order/page")
    public R<PageResult<Map<String, Object>>> orderPage(@RequestParam(required = false) Long credentialId,
                                                        @RequestParam(required = false) String symbol,
                                                        @RequestParam(required = false) String side,
                                                        @RequestParam(required = false) String status,
                                                        PageQuery pageQuery) {
        List<QuantCredential> credentials;
        if (credentialId != null) {
            credentials = List.of(credentialEntity(credentialId));
        } else {
            LambdaQueryWrapper<QuantCredential> credentialQuery = new LambdaQueryWrapper<QuantCredential>()
                .select(QuantCredential::getId, QuantCredential::getUserId, QuantCredential::getAccountName);
            if (!LoginHelper.isSuperAdmin(LoginHelper.getUserId())) credentialQuery.eq(QuantCredential::getUserId, LoginHelper.getUserId());
            credentials = credentialMapper.selectList(credentialQuery);
        }
        if (credentials.isEmpty()) return R.ok(PageResult.build(List.of(), 0L));
        List<Long> credentialIds = credentials.stream().map(QuantCredential::getId).toList();
        String normalizedSymbol = StringUtils.isBlank(symbol) ? null : symbol.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedSide = StringUtils.isBlank(side) ? null : side.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedStatus = StringUtils.isBlank(status) ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
        LambdaQueryWrapper<QuantOrder> orderQuery = new LambdaQueryWrapper<QuantOrder>()
            .in(QuantOrder::getCredentialId, credentialIds)
            .eq(normalizedSymbol != null, QuantOrder::getSymbol, normalizedSymbol)
            .eq(normalizedSide != null, QuantOrder::getSide, normalizedSide)
            .eq(normalizedStatus != null, QuantOrder::getStatus, normalizedStatus)
            .orderByDesc(QuantOrder::getUpdatedAt).orderByDesc(QuantOrder::getId);
        Page<QuantOrder> page = orderMapper.selectPage(pageQuery.build(), orderQuery);
        Map<Long, QuantCredential> credentialById = credentials.stream().collect(Collectors.toMap(QuantCredential::getId, Function.identity()));
        List<Long> orderIds = page.getRecords().stream().map(QuantOrder::getExchangeOrderId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<String, Long> fillCounts = orderIds.isEmpty() ? Map.of() : tradeMapper.selectList(new LambdaQueryWrapper<QuantTrade>()
                .in(QuantTrade::getCredentialId, credentialIds).in(QuantTrade::getExchangeOrderId, orderIds))
            .stream().collect(Collectors.groupingBy(fill -> fill.getCredentialId() + ":" + fill.getSymbol() + ":" + fill.getExchangeOrderId(), Collectors.counting()));
        List<Map<String, Object>> rows = page.getRecords().stream().map(item -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", String.valueOf(item.getId()));
            row.put("credentialId", String.valueOf(item.getCredentialId()));
            row.put("customerName", credentialById.get(item.getCredentialId()) == null ? null : credentialById.get(item.getCredentialId()).getAccountName());
            row.put("symbol", item.getSymbol());
            row.put("orderId", item.getExchangeOrderId() == null ? null : String.valueOf(item.getExchangeOrderId()));
            row.put("clientOrderId", item.getClientOrderId());
            row.put("side", item.getSide());
            row.put("type", item.getOrderType());
            row.put("status", item.getStatus());
            row.put("positionSide", item.getPositionSide());
            row.put("reduceOnly", item.getReduceOnly());
            row.put("price", item.getPrice());
            row.put("originalQuantity", item.getOriginalQuantity());
            row.put("executedQuantity", item.getExecutedQuantity());
            row.put("tradeCount", fillCounts.getOrDefault(item.getCredentialId() + ":" + item.getSymbol() + ":" + item.getExchangeOrderId(), 0L));
            row.put("updateTime", item.getUpdatedAt());
            return row;
        }).toList();
        return R.ok(PageResult.build(rows, page.getTotal()));
    }

    /** 将交易所历史订单和当前活动订单同步到本地账本。 */
    @SaCheckPermission("quant:order:sync")
    @PostMapping("/order/{credentialId}/sync")
    public R<Integer> syncOrders(@PathVariable Long credentialId, @RequestParam String symbol) {
        return R.ok(syncOrderSnapshots(credentialEntity(credentialId), symbol));
    }

    private int syncOrderSnapshots(QuantCredential credential, String symbol) {
        if (StringUtils.isBlank(symbol)) throw new ServiceException("请指定交易对同步订单");
        String normalizedSymbol = symbol.trim().toUpperCase(java.util.Locale.ROOT);
        ExchangeGateway.Credential exchangeCredential = credential(credential.getId());
        Map<Long, ExchangeGateway.OrderSnapshot> snapshots = new java.util.LinkedHashMap<>();
        for (ExchangeGateway.OrderSnapshot snapshot : gateway.queryOrderHistory(exchangeCredential, normalizedSymbol,
            new ExchangeGateway.TimeRange(null, null), 1000L)) {
            if (snapshot != null && snapshot.orderId() != null) snapshots.put(snapshot.orderId(), snapshot);
        }
        // 活动订单覆盖历史快照，避免旧状态覆盖当前状态。
        for (ExchangeGateway.OrderSnapshot snapshot : gateway.queryOrders(exchangeCredential, normalizedSymbol)) {
            if (snapshot != null && snapshot.orderId() != null) snapshots.put(snapshot.orderId(), snapshot);
        }
        snapshots.values().forEach(snapshot -> orderLedgerService.syncHistoricalSnapshot(credential.getId(), normalizedSymbol, snapshot));
        if (snapshots.size() == 1000) log.warn("订单历史同步命中单次返回上限，仅完成本批订单同步 credentialId={} symbol={}", credential.getId(), normalizedSymbol);
        return snapshots.size();
    }

    /**
     * 只读查询交易所当前条件单，包含止盈止损，复用账户访问权限校验。
     */
    @SaCheckPermission(value = {"quant:order:list", "quant:terminal:list"}, mode = SaMode.OR)
    @GetMapping("/order/{credentialId}/protection")
    public R<List<ExchangeGateway.ProtectionOrderSnapshot>> protectionOrders(@PathVariable Long credentialId, @RequestParam(required = false) String symbol) {
        return R.ok(gateway.queryProtectionOrders(credential(credentialId), symbol));
    }

    /**
     * 分页查询指定凭证的成交记录。
     */
    @SaCheckPermission("quant:trade:list")
    @GetMapping("/trade/{credentialId}/page")
    public R<PageResult<QuantTradeVo>> tradePage(@PathVariable Long credentialId, @RequestParam(required = false) String symbol, @RequestParam(required = false) String side,
                                                 @RequestParam(required = false) String from, @RequestParam(required = false) String to,
                                                 @RequestParam(required = false) Long orderId, PageQuery pageQuery) {
        QuantCredential credential = credentialEntity(credentialId);
        LambdaQueryWrapper<QuantTrade> lqw = new LambdaQueryWrapper<QuantTrade>();
        lqw.eq(QuantTrade::getCredentialId, credentialId);
        lqw.eq(StringUtils.isNotBlank(symbol), QuantTrade::getSymbol, symbol);
        lqw.eq(orderId != null, QuantTrade::getExchangeOrderId, orderId);
        lqw.eq(StringUtils.isNotBlank(side), QuantTrade::getSide, side);
        lqw.ge(StringUtils.isNotBlank(from), QuantTrade::getTradeTime, StringUtils.isBlank(from) ? null : LocalDate.parse(from).atStartOfDay());
        lqw.lt(StringUtils.isNotBlank(to), QuantTrade::getTradeTime, StringUtils.isBlank(to) ? null : LocalDate.parse(to).plusDays(1).atStartOfDay());
        lqw.orderByDesc(QuantTrade::getTradeTime).orderByDesc(QuantTrade::getId);
        Page<QuantTrade> page = tradeMapper.selectPage(pageQuery.build(), lqw);
        List<QuantTradeVo> rows = page.getRecords().stream().map(item -> toVo(item, credential)).toList();
        return R.ok(PageResult.build(rows, page.getTotal()));
    }

    /**
     * 分页查询当前用户有权访问的全部凭证成交记录；可选按凭证筛选。
     */
    @SaCheckPermission("quant:trade:list")
    @GetMapping("/trade/page")
    public R<PageResult<QuantTradeVo>> tradePageForAccessibleAccounts(@RequestParam(required = false) Long credentialId,
                                                                      @RequestParam(required = false) String symbol,
                                                                      @RequestParam(required = false) String side,
                                                                      @RequestParam(required = false) String from,
                                                                      @RequestParam(required = false) String to,
                                                                      @RequestParam(required = false) Long orderId,
                                                                      PageQuery pageQuery) {
        List<QuantCredential> credentials;
        if (credentialId != null) {
            credentials = List.of(credentialEntity(credentialId));
        } else {
            LambdaQueryWrapper<QuantCredential> credentialQuery = new LambdaQueryWrapper<QuantCredential>()
                .select(QuantCredential::getId, QuantCredential::getUserId, QuantCredential::getAccountName);
            if (!LoginHelper.isSuperAdmin(LoginHelper.getUserId())) {
                credentialQuery.eq(QuantCredential::getUserId, LoginHelper.getUserId());
            }
            credentials = credentialMapper.selectList(credentialQuery);
        }
        if (credentials.isEmpty()) {
            return R.ok(PageResult.build(List.of(), 0L));
        }

        List<Long> credentialIds = credentials.stream().map(QuantCredential::getId).toList();
        Map<Long, QuantCredential> credentialById = credentials.stream()
            .collect(Collectors.toMap(QuantCredential::getId, Function.identity()));
        LambdaQueryWrapper<QuantTrade> tradeQuery = new LambdaQueryWrapper<QuantTrade>()
            .in(QuantTrade::getCredentialId, credentialIds)
            .eq(StringUtils.isNotBlank(symbol), QuantTrade::getSymbol, symbol)
            .eq(StringUtils.isNotBlank(side), QuantTrade::getSide, side)
            .eq(orderId != null, QuantTrade::getExchangeOrderId, orderId)
            .ge(StringUtils.isNotBlank(from), QuantTrade::getTradeTime, StringUtils.isBlank(from) ? null : LocalDate.parse(from).atStartOfDay())
            .lt(StringUtils.isNotBlank(to), QuantTrade::getTradeTime, StringUtils.isBlank(to) ? null : LocalDate.parse(to).plusDays(1).atStartOfDay())
            .orderByDesc(QuantTrade::getTradeTime).orderByDesc(QuantTrade::getId);
        Page<QuantTrade> page = tradeMapper.selectPage(pageQuery.build(), tradeQuery);
        List<QuantTradeVo> rows = page.getRecords().stream()
            .map(item -> toVo(item, credentialById.get(item.getCredentialId())))
            .toList();
        return R.ok(PageResult.build(rows, page.getTotal()));
    }

    /**
     * 同步交易所成交事实。
     */
    @SaCheckPermission("quant:trade:sync")
    @PostMapping("/trade/{credentialId}/sync")
    public R<Integer> syncTrades(@PathVariable Long credentialId, @RequestParam String symbol) {
        if (StringUtils.isBlank(symbol)) throw new ServiceException("请指定交易对同步成交记录");
        symbol = symbol.trim().toUpperCase(java.util.Locale.ROOT);
        QuantCredential credential = credentialEntity(credentialId);
        List<ExchangeGateway.TradeSnapshot> snapshots = gateway.queryTrades(credential(credentialId), symbol, new ExchangeGateway.TimeRange(null, null));
        int count = 0;
        for (ExchangeGateway.TradeSnapshot snapshot : snapshots) {
            QuantTrade entity = tradeMapper.selectOne(new LambdaQueryWrapper<QuantTrade>().eq(QuantTrade::getCredentialId, credentialId).eq(QuantTrade::getExchangeTradeId, snapshot.tradeId()));
            if (entity == null) {
                entity = new QuantTrade();
                entity.setCredentialId(credentialId);
                entity.setExchangeTradeId(snapshot.tradeId());
            }
            entity.setSymbol(snapshot.symbol());
            entity.setExchangeOrderId(snapshot.orderId());
            entity.setSide(snapshot.side());
            entity.setQuantity(snapshot.quantity());
            entity.setPrice(snapshot.price());
            entity.setCommission(snapshot.commission());
            entity.setRealizedPnl(snapshot.realizedPnl());
            entity.setFundingFee(java.math.BigDecimal.ZERO);
            entity.setTradeTime(LocalDateTime.ofInstant(snapshot.time(), ZoneOffset.UTC));
            if (entity.getId() == null) tradeMapper.insert(entity); else tradeMapper.updateById(entity);
            count++;
        }
        syncOrderSnapshots(credential, symbol);
        backfillTradeOrderFacts(credential, symbol);
        return R.ok(count);
    }

    private void backfillTradeOrderFacts(QuantCredential credential, String symbol) {
        List<Long> orderIds = tradeHistoryMapper.selectOrderIdsMissingOrderFacts(credential.getId(), symbol.trim().toUpperCase(), 200);
        int restored = 0;
        for (Long orderId : orderIds) {
            try {
                ExchangeGateway.OrderSnapshot snapshot = gateway.queryOrder(credential(credential.getId()), symbol, orderId, null);
                if (snapshot == null || !symbol.equalsIgnoreCase(snapshot.symbol()) || !orderId.equals(snapshot.orderId())
                    || (snapshot.positionSide() == null && snapshot.reduceOnly() == null)) {
                    continue;
                }
                orderLedgerService.syncHistoricalSnapshot(credential.getId(), symbol, snapshot);
                restored++;
            } catch (RuntimeException ex) {
                log.warn("历史成交订单来源补齐暂停，相关交易记录将按来源不完整处理 credentialId={} symbol={} orderId={} errorType={}",
                    credential.getId(), symbol, orderId, ex.getClass().getSimpleName());
                break;
            }
        }
        if (restored > 0) {
            log.info("历史成交订单来源事实已补齐 credentialId={} symbol={} restoredOrderCount={}", credential.getId(), symbol, restored);
        }
    }

    private ExchangeGateway.Credential credential(Long id) {
        QuantCredential entity = credentialEntity(id);
        return new ExchangeGateway.Credential(entity.getApiKey(), entity.getSecretKey(), entity.getSignatureType(), entity.getRunMode());
    }

    private QuantCredential credentialEntity(Long id) {
        QuantCredential entity = credentialMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("凭证不存在");
        }
        if (!LoginHelper.isSuperAdmin(LoginHelper.getUserId()) && !LoginHelper.getUserId().equals(entity.getUserId())) {
            throw new ServiceException("无权访问该交易账户");
        }
        return entity;
    }

    private QuantTradeVo toVo(QuantTrade entity, QuantCredential credential) {
        QuantTradeVo vo = new QuantTradeVo();
        vo.setId(entity.getId());
        vo.setCredentialId(entity.getCredentialId());
        vo.setCustomerName(credential.getAccountName());
        vo.setUserId(credential.getUserId());
        vo.setSymbol(entity.getSymbol());
        vo.setSide(entity.getSide());
        vo.setQuantity(entity.getQuantity());
        vo.setPrice(entity.getPrice());
        vo.setCommission(entity.getCommission());
        vo.setRealizedPnl(entity.getRealizedPnl());
        vo.setTradeTime(entity.getTradeTime());
        vo.setExchangeTradeId(entity.getExchangeTradeId());
        vo.setExchangeOrderId(entity.getExchangeOrderId());
        return vo;
    }
}

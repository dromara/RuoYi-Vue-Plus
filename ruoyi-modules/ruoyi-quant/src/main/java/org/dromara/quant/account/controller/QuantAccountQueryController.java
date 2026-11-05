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
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.dromara.quant.reconcile.TradingReconciliationService;
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
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

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
    private final QuantOrderMapper orderMapper;
    private final TradingReconciliationService tradingReconciliationService;

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
        LocalDateTime dayStart = LocalDate.now(BEIJING_ZONE).atStartOfDay(BEIJING_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        QuantAccountSnapshot baseline = accountSnapshotMapper.selectOne(new LambdaQueryWrapper<QuantAccountSnapshot>()
            .select(QuantAccountSnapshot::getMarginBalance, QuantAccountSnapshot::getObservedAt)
            .eq(QuantAccountSnapshot::getCredentialId, credentialId)
            .ge(QuantAccountSnapshot::getObservedAt, dayStart)
            .lt(QuantAccountSnapshot::getObservedAt, dayStart.plusMinutes(5))
            .orderByAsc(QuantAccountSnapshot::getObservedAt)
            .last("LIMIT 1"));
        return R.ok(baseline == null ? null : new DailyAccountBaseline(baseline.getMarginBalance(), baseline.getObservedAt().atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8))));
    }

    /**
     * 当日账户权益基线及其观测时间。
     */
    public record DailyAccountBaseline(java.math.BigDecimal equity, java.time.OffsetDateTime observedAt) {
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
    @GetMapping({"/order/page", "/order/ledger/page"})
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
            row.put("updateTime", item.getUpdatedAt() == null ? null : item.getUpdatedAt().toInstant(ZoneOffset.UTC));
            return row;
        }).toList();
        return R.ok(PageResult.build(rows, page.getTotal()));
    }

    /** 同步交易所订单和成交事实；保持订单管理原有权限入口。 */
    @SaCheckPermission(value = {"quant:order:sync", "quant:trade:sync"}, mode = SaMode.AND)
    @PostMapping("/order/{credentialId}/sync")
    public R<Integer> syncOrders(@PathVariable Long credentialId, @RequestParam(required = false) String symbol) {
        return R.ok(tradingReconciliationService.syncTradingData(credentialEntity(credentialId), symbol).orderCount());
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
        lqw.ge(StringUtils.isNotBlank(from), QuantTrade::getTradeTime, StringUtils.isBlank(from) ? null : tradeDateBoundary(from, false));
        lqw.lt(StringUtils.isNotBlank(to), QuantTrade::getTradeTime, StringUtils.isBlank(to) ? null : tradeDateBoundary(to, true));
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
            .ge(StringUtils.isNotBlank(from), QuantTrade::getTradeTime, StringUtils.isBlank(from) ? null : tradeDateBoundary(from, false))
            .lt(StringUtils.isNotBlank(to), QuantTrade::getTradeTime, StringUtils.isBlank(to) ? null : tradeDateBoundary(to, true))
            .orderByDesc(QuantTrade::getTradeTime).orderByDesc(QuantTrade::getId);
        Page<QuantTrade> page = tradeMapper.selectPage(pageQuery.build(), tradeQuery);
        List<QuantTradeVo> rows = page.getRecords().stream()
            .map(item -> toVo(item, credentialById.get(item.getCredentialId())))
            .toList();
        return R.ok(PageResult.build(rows, page.getTotal()));
    }

    /**
     * 同步交易所成交和订单事实；与订单管理共用同一协调流程。
     */
    @SaCheckPermission(value = {"quant:order:sync", "quant:trade:sync"}, mode = SaMode.AND)
    @PostMapping("/trade/{credentialId}/sync")
    public R<Integer> syncTrades(@PathVariable Long credentialId, @RequestParam(required = false) String symbol) {
        return R.ok(tradingReconciliationService.syncTradingData(credentialEntity(credentialId), symbol).tradeCount());
    }

    static LocalDateTime tradeDateBoundary(String date, boolean nextDay) {
        LocalDate target = LocalDate.parse(date);
        if (nextDay) {
            target = target.plusDays(1);
        }
        return target.atStartOfDay(BEIJING_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
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

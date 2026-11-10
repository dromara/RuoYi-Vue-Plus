package org.dromara.quant.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.decision.TradeDecisionLogService;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.market.AccountPositionCache;
import org.dromara.quant.market.domain.QuantSymbol;
import org.dromara.quant.market.mapper.QuantSymbolMapper;
import org.dromara.quant.paper.PaperTradingService;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.position.mapper.QuantPositionMapper;
import org.dromara.quant.risk.OpenCapacityService;
import org.dromara.quant.risk.RiskDecisionService;
import org.dromara.quant.risk.domain.QuantRiskConfig;
import org.dromara.quant.risk.mapper.QuantRiskConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 交易执行应用层。所有手工/策略交易统一经过这里进入 Execution Engine。
 */
@RequiredArgsConstructor
@Service
public class TradeExecutionApplicationService {
    /**
     * 交易凭证 Mapper。
     */
    private final QuantCredentialMapper credentialMapper;
    /**
     * 交易对 Mapper。
     */
    private final QuantSymbolMapper symbolMapper;
    /**
     * risk 数据访问 Mapper。
     */
    private final QuantRiskConfigMapper riskMapper;
    /**
     * 风险决策服务。
     */
    private final RiskDecisionService riskDecisionService;
    /**
     * 统一交易执行端口。
     */
    private final TradeExecutionPort executionPort;
    /**
     * 量化模块配置。
     */
    private final QuantProperties properties;
    /**
     * 交易所统一网关。
     */
    private final ExchangeGateway gateway;
    /**
     * 持仓 Mapper。
     */
    private final QuantPositionMapper positionMapper;
    /**
     * PAPER 模拟交易服务。
     */
    private final PaperTradingService paperTradingService;
    private final TradingPreparationService preparationService;
    @Autowired
    private TradeDecisionLogService decisionLogs;
    @Autowired
    private AccountPositionCache positionCache;
    @Autowired
    private OpenCapacityService openCapacityService;
    @Autowired
    private org.dromara.quant.notification.QuantTradeNotificationService notificationService;

    /**
     * 由用户手工发起统一平仓执行。
     */
    public ExecutionResult closeByUser(ClosePositionRequest request) {
        QuantCredential credential = requireCredential(request.getCredentialId());
        assertOwner(credential);
        recordManualClose(request, credential, "REQUESTED", "手动平仓", null, null, false);
        try {
            return closeByUserAuthorized(request, credential);
        } catch (RuntimeException ex) {
            String logStatus =
                ex instanceof org.dromara.common.core.exception.ServiceException || ex instanceof IllegalArgumentException || ex instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException || ex instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderRejectedException ? "FAILED" : "UNKNOWN";
            recordManualClose(request, credential, logStatus, org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex), null, null, false);
            throw ex;
        }
    }

    private ExecutionResult closeByUserAuthorized(ClosePositionRequest request, QuantCredential credential) {
        QuantSymbol symbol = requireSymbol(request.getSymbol());
        ExchangeGateway.Credential exchangeCredential = toCredential(credential);
        if ("PAPER".equalsIgnoreCase(credential.getRunMode())) {
            QuantPosition position =
                positionMapper.selectOne(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credential.getId()).eq(QuantPosition::getSymbol,
                    request.getSymbol()).eq(QuantPosition::getPositionSide, request.getPositionSide()));
            if (position == null || position.getQuantity() == null || position.getQuantity().signum() <= 0) {
                throw new ServiceException("当前 PAPER 持仓不存在");
            }
            if (request.getQuantity().compareTo(position.getQuantity()) > 0) {
                throw new ServiceException("平仓数量超过当前 PAPER 持仓");
            }
            String closeSide = "SHORT".equalsIgnoreCase(position.getPositionSide()) ? "BUY" : "SELL";
            ExecutionResult result = executionPort.close(new ClosePositionCommand(credential.getId(), null, null, exchangeCredential, request.getSymbol(), closeSide, "MARKET",
                request.getQuantity(), null, position.getPositionSide(), request.getIdempotencyKey(), credential.getRunMode(), symbolRule(symbol), safety(credential, null, true)));
            BigDecimal closePrice = position.getMarkPrice() == null ? position.getEntryPrice() : position.getMarkPrice();
            PaperTradingService.PaperCloseSettlement settlement = paperTradingService.settlePaperExecution(
                credential.getId(), request.getIdempotencyKey(), position.getId(), request.getQuantity(), closePrice);
            var paperAccount = paperTradingService.find(credential.getId());
            String direction = position.getQuantity().signum() < 0 || "SHORT".equalsIgnoreCase(position.getPositionSide()) ? "SHORT" : "LONG";
            notificationService.notifyClosePaper(null, credential, request.getSymbol(), direction, request.getQuantity(),
                position.getEntryPrice(), closePrice, settlement.realizedPnl(), symbol.getQuoteAsset(),
                paperAccount == null ? null : paperAccount.getBalance(), paperAccount == null ? null : paperAccount.getAvailableBalance(),
                paperAccount == null ? null : paperAccount.getUnrealizedPnl(), "MANUAL", "手动平仓");
            recordManualClose(request, credential, "FILLED", "手动平仓", position.getPositionSide(), closePrice, true);
            return result;
        }
        ExchangeGateway.PositionSnapshot position =
            gateway.queryPositions(exchangeCredential, request.getSymbol()).stream().filter(item -> request.getPositionSide() == null || request.getPositionSide().isBlank() || item.side().equalsIgnoreCase(request.getPositionSide())).findFirst().orElseThrow(() -> new ServiceException("当前持仓不存在"));
        BigDecimal available = position.quantity().abs();
        if (request.getQuantity().compareTo(available) > 0) {
            throw new ServiceException("平仓数量超过当前持仓");
        }
        String closeSide = position.quantity().signum() < 0 || "SHORT".equalsIgnoreCase(position.side()) ? "BUY" : "SELL";
        String positionSide = normalizePositionSide(exchangeCredential, closeSide, position.side());
        QuantRiskConfig risk = riskMapper.selectOne(new LambdaQueryWrapper<QuantRiskConfig>().eq(QuantRiskConfig::getCredentialId, credential.getId()));
        ExecutionResult result = executionPort.close(new ClosePositionCommand(credential.getId(), null, null, exchangeCredential, request.getSymbol(), closeSide, "MARKET", request.getQuantity(),
            null, positionSide, request.getIdempotencyKey(), credential.getRunMode(), symbolRule(symbol), safety(credential, risk, true)));
        if (result.orderId() != null) {
            String direction = position.quantity().signum() < 0 || "SHORT".equalsIgnoreCase(position.side()) ? "SHORT" : "LONG";
            try {
                notificationService.notifyCloseByOrder(null, credential, request.getSymbol(), direction, request.getQuantity(),
                    position.entryPrice(), result.orderId(), symbol.getQuoteAsset(), "MANUAL", "手动平仓");
            } catch (RuntimeException ex) {
                // 平仓事实已提交，通知查询失败不得把成功的交易伪装成执行失败。
                org.slf4j.LoggerFactory.getLogger(TradeExecutionApplicationService.class)
                    .error("手动平仓已提交，但成交通知核算失败 credentialId={} symbol={} orderId={}",
                        credential.getId(), request.getSymbol(), result.orderId(), ex);
            }
        }
        return result;
    }

    private void recordManualClose(ClosePositionRequest request, QuantCredential credential, String status, String reason, String direction, BigDecimal price, boolean filled) {
        if (decisionLogs == null) {
            return;
        }
        decisionLogs.record(org.dromara.quant.decision.DecisionLogEvent.builder().eventKey(org.dromara.quant.decision.TradeDecisionLogRepository.executionKey(credential.getId(),
            request.getIdempotencyKey())).recordType("EXECUTION").credentialId(credential.getId()).accountName(credential.getAccountName()).strategyName("手动操作").runMode(credential.getRunMode()).symbol(request.getSymbol()).operation("CLOSE").direction(direction == null ? ("BOTH".equalsIgnoreCase(request.getPositionSide()) ? null : request.getPositionSide()) : direction).quantity(request.getQuantity()).price(price).status(status).reasonCode("REQUESTED".equals(status) || filled ? "MANUAL_CLOSE" : "EXECUTION_FAILURE").reason(reason).idempotencyKey(request.getIdempotencyKey()).eventTime(java.time.Instant.now()).tradeTime(filled ? java.time.Instant.now() : null).build());
    }

    /**
     * 由用户手工发起统一撤单执行。
     */
    public ExecutionResult cancelByUser(CancelOrderRequest request) {
        QuantCredential credential = requireCredential(request.getCredentialId());
        assertOwner(credential);
        return executionPort.cancel(new CancelOrderCommand(credential.getId(), toCredential(credential), request.getSymbol(), request.getOrderId(), request.getClientOrderId(),
            request.getIdempotencyKey(), credential.getRunMode()));
    }

    /**
     * 由策略 Runtime 发起统一平仓执行。
     */
    public ExecutionResult closeByRuntime(Long credentialId, Long strategyId, Long strategyInstanceId, String symbol, String side, String positionSide, BigDecimal quantity,
                                          String idempotencyKey) {
        QuantCredential credential = requireCredential(credentialId);
        QuantSymbol symbolEntity = requireSymbol(symbol);
        ExchangeGateway.Credential exchangeCredential = toCredential(credential);
        String resolvedPositionSide = normalizePositionSide(exchangeCredential, side, positionSide);
        QuantRiskConfig risk = riskMapper.selectOne(new LambdaQueryWrapper<QuantRiskConfig>().eq(QuantRiskConfig::getCredentialId, credentialId));
        return executionPort.close(new ClosePositionCommand(credentialId, strategyId, strategyInstanceId, exchangeCredential, symbol, side, "MARKET", quantity, null,
            resolvedPositionSide, idempotencyKey, credential.getRunMode(), symbolRule(symbolEntity), safety(credential, risk, true)));
    }

    /**
     * 由策略 Runtime 发起统一开仓执行。
     */
    public ExecutionResult openByRuntime(Long credentialId, Long strategyId, Long strategyInstanceId, String symbol, String side, String positionSide, BigDecimal quantity,
                                         BigDecimal price, int leverage, String marginMode, String idempotencyKey) {
        return openByRuntime(credentialId, strategyId, strategyInstanceId, symbol, side, positionSide, quantity, price, leverage, marginMode, idempotencyKey, null);
    }

    /**
     * 运行时信号带上截止时间，REST 等待后不得按过期点位追单。
     */
    public ExecutionResult openByRuntime(Long credentialId, Long strategyId, Long strategyInstanceId, String symbol, String side, String positionSide, BigDecimal quantity,
                                         BigDecimal price, int leverage, String marginMode, String idempotencyKey, java.time.Instant eventTime) {
        QuantCredential credential = requireCredential(credentialId);
        QuantSymbol symbolEntity = requireSymbol(symbol);
        ExchangeGateway.Credential exchangeCredential = toCredential(credential);
        if (leverage <= 0) {
            throw new ServiceException("杠杆必须大于 0");
        }
        BigDecimal notional = quantity.multiply(price == null ? ("TESTNET".equalsIgnoreCase(credential.getRunMode()) ? gateway.queryPrice(credential.getRunMode(), symbol) :
            gateway.queryPrice(symbol)).markPrice() : price);
        // 最终风险审批在统一执行入口与容量占位的同一短事务中完成，避免重复查询和过期审批。
        QuantRiskConfig risk = riskMapper.selectOne(new LambdaQueryWrapper<QuantRiskConfig>().eq(QuantRiskConfig::getCredentialId, credentialId));
        String resolvedPositionSide = "PAPER".equalsIgnoreCase(credential.getRunMode()) ? normalizePositionSide(exchangeCredential, side, positionSide) : positionSide;
        return executionPort.open(new OpenPositionCommand(credentialId, strategyId, strategyInstanceId, exchangeCredential, symbol, side, "MARKET", quantity, null,
            resolvedPositionSide, idempotencyKey, credential.getRunMode(), symbolRule(symbolEntity), leverage, marginMode, safety(credential, risk, true), notional,
            eventTime == null ? null : eventTime.plusMillis(Math.max(1, properties.getMarket().getMaxEventAgeMillis())), eventTime));
    }

    /**
     * 只读取可信缓存，在覆盖旧 CLOSED 状态之前清理已确认关闭的开仓预留。
     */
    public void releaseClosedOpenReservationsForRuntime(Long credentialId, Long instanceId, String symbol, String runMode) {
        var positions = "PAPER".equalsIgnoreCase(runMode) ? java.util.List.<ExchangeGateway.PositionSnapshot>of() : positionCache.requirePositions(credentialId);
        openCapacityService.releaseClosedForInstance(credentialId, instanceId, symbol, positions);
    }

    /**
     * 只读重试准入校验，复用可信缓存及容量规则，不产生交易请求或账本记录。
     */
    public void requireOpenCapacityForRuntime(Long credentialId, Long strategyId, Long instanceId, String symbol, String side, BigDecimal quantity, BigDecimal price,
                                              int leverage, String marginMode, String runMode, org.dromara.quant.market.AccountBalanceSnapshot balance) {
        var positions = "PAPER".equalsIgnoreCase(runMode) ? null : positionCache.requirePositions(credentialId);
        var command = new OpenPositionCommand(credentialId, strategyId, instanceId, null, symbol, side, "MARKET", quantity, null, null, "RETRY_CHECK:" + instanceId, runMode,
            null, leverage, null, null);
        boolean hedgeModeConfirmed = false;
        if ("LIVE".equalsIgnoreCase(runMode)) {
            var prepared = preparationService.requireReady(credentialId, symbol, leverage, marginMode);
            hedgeModeConfirmed = prepared.positionMode() != null && prepared.positionMode().hedgeMode();
        }
        openCapacityService.requireCanReserve(command, quantity.multiply(price), positions, balance, hedgeModeConfirmed);
    }

    /**
     * 由启动/配置刷新线程调用，交易所请求由准备服务异步执行。
     */
    public void prepareOpenForRuntime(Long credentialId, String symbol, int leverage, String marginMode) {
        QuantCredential credential = requireCredential(credentialId);
        if ("PAPER".equalsIgnoreCase(credential.getRunMode())) {
            return;
        }
        QuantRiskConfig risk = riskMapper.selectOne(new LambdaQueryWrapper<QuantRiskConfig>().eq(QuantRiskConfig::getCredentialId, credentialId));
        preparationService.request(credentialId, toCredential(credential), symbol, leverage, marginMode, safety(credential, risk, true));
    }

    /**
     * 构建当前交易请求的 ExecutionSafetyContext。
     */
    private ExecutionSafetyContext safety(QuantCredential credential, QuantRiskConfig risk, boolean approved) {
        boolean paper = "PAPER".equalsIgnoreCase(credential.getRunMode());
        return new ExecutionSafetyContext(properties.getLive().isEnabled(), "0".equals(credential.getStatus()), paper || Boolean.TRUE.equals(credential.getTradePermission()),
            paper || Boolean.TRUE.equals(credential.getWithdrawalDisabled()), paper || "NORMAL".equals(credential.getConnectionStatus()), true, paper || risk != null, approved);
    }

    /**
     * 读取必需数据，不满足条件时抛出异常。
     */
    public QuantCredential requireCredential(Long credentialId) {
        QuantCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            throw new ServiceException("凭证不存在");
        }
        return credential;
    }

    /**
     * 读取必需数据，不满足条件时抛出异常。
     */
    public QuantSymbol requireSymbol(String symbolCode) {
        QuantSymbol symbol = symbolMapper.selectOne(new LambdaQueryWrapper<QuantSymbol>().eq(QuantSymbol::getSymbol, symbolCode));
        if (symbol == null || !"0".equals(symbol.getStatus())) {
            throw new ServiceException("交易对不存在或已停用");
        }
        return symbol;
    }

    /**
     * 把持仓方向标准化为 LONG/SHORT/BOTH。
     */
    private String normalizePositionSide(ExchangeGateway.Credential credential, String side, String currentSide) {
        if ("PAPER".equalsIgnoreCase(credential.runMode())) {
            return currentSide == null || currentSide.isBlank() ? null : currentSide.toUpperCase();
        }
        ExchangeGateway.PositionMode mode = gateway.queryPositionMode(credential);
        if (mode.hedgeMode()) {
            if (currentSide != null && !currentSide.isBlank() && !"BOTH".equalsIgnoreCase(currentSide)) {
                return currentSide.toUpperCase();
            }
            return "BUY".equalsIgnoreCase(side) ? "LONG" : "SHORT";
        }
        return "BOTH";
    }

    /**
     * 校验当前用户是否有权使用指定交易凭证。
     */
    private void assertOwner(QuantCredential credential) {
        Long userId = LoginHelper.getUserId();
        if (!LoginHelper.isSuperAdmin(userId) && !userId.equals(credential.getUserId())) {
            throw new ServiceException("无权操作该交易账户");
        }
    }

    /**
     * 把数据库凭证实体转换为 ExchangeGateway 内部凭证模型。
     */
    public ExchangeGateway.Credential toCredential(QuantCredential credential) {
        return new ExchangeGateway.Credential(credential.getApiKey(), credential.getSecretKey(), credential.getSignatureType(), credential.getRunMode());
    }

    /**
     * 把本地交易对实体转换为统一 SymbolRule。
     */
    public ExchangeGateway.SymbolRule symbolRule(QuantSymbol s) {
        return new ExchangeGateway.SymbolRule(s.getSymbol(), s.getQuoteAsset(), s.getPricePrecision(), s.getQuantityPrecision(), s.getPriceTickSize(), s.getQuantityStep(),
            s.getMarketQuantityStep(), s.getMinNotional(), s.getExchangeStatus());
    }
}

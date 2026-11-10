package org.dromara.quant.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mail.core.MailBuilder;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.dromara.quant.notification.domain.QuantNotification;
import org.dromara.quant.notification.mapper.QuantNotificationMapper;
import org.dromara.quant.risk.domain.QuantRiskConfig;
import org.dromara.quant.risk.mapper.QuantRiskConfigMapper;
import org.dromara.quant.strategy.domain.QuantStrategy;
import org.dromara.quant.util.QuantJsonMapper;
import org.dromara.system.api.MessageService;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
/**
 * 开仓/平仓统一通知；投递失败进入 Outbox 重试，不回滚已完成的交易事实。
 */
@Slf4j
@Service
public class QuantTradeNotificationService {
    /**
     * 风险配置 Mapper。
     */
    private final QuantRiskConfigMapper riskConfigMapper;
    /**
     * 通知 Outbox Mapper。
     */
    private final QuantNotificationMapper notificationMapper;
    /**
     * 量化模块配置。
     */
    private final QuantProperties properties;
    private final MessageService messageService;
    private final JdbcTemplate jdbcTemplate;
    private final ExchangeGateway gateway;
    @Autowired
    private QuantCredentialMapper credentialMapper;
    @Autowired
    private QuantOrderMapper orderMapper;
    private final java.util.concurrent.ConcurrentHashMap<String, Long> blockedNoticeTimes = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 创建 QuantTradeNotificationService 实例并初始化其依赖。
     */
    public QuantTradeNotificationService(QuantRiskConfigMapper riskConfigMapper, QuantNotificationMapper notificationMapper, QuantProperties properties,
                                         MessageService messageService, JdbcTemplate jdbcTemplate, ExchangeGateway gateway) {
        this.riskConfigMapper = riskConfigMapper;
        this.notificationMapper = notificationMapper;
        this.properties = properties;
        this.messageService = messageService;
        this.jdbcTemplate = jdbcTemplate;
        this.gateway = gateway;
    }

    /**
     * 把 LONG/SHORT 转换为中文方向文本。
     */
    private static String cnDirection(String direction) {
        return "SHORT".equalsIgnoreCase(direction) ? "做空" : "做多";
    }

    /**
     * 安全转换可能为空的通知文本。
     */
    private static String percentage(BigDecimal value) {
        // maximumReturnRate is persisted as a percentage value (for example, 5.86 means 5.86%).
        return value == null ? "待核实" : value.stripTrailingZeros().toPlainString() + "%";
    }

    private static String returnRatePercentage(BigDecimal ratio) {
        return ratio == null ? "待核实" : percentage(ratio.multiply(BigDecimal.valueOf(100)));
    }

    private static String plain(BigDecimal value) {
        return value == null ? "N/A" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 隔离执行单个后台同步步骤，单项异常不阻断整轮任务。
     */
    private static String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    /**
     * 日志线程调用；与交易风险消息分开，发布到顶部工作分类。
     */
    public void notifyDecisionLogLoss(long count) {
        List<Long> admins = jdbcTemplate.query("SELECT DISTINCT ur.user_id FROM sys_user_role ur JOIN sys_role r ON r.role_id=ur.role_id JOIN sys_user u ON u.user_id=ur.user_id " +
            "WHERE r.role_key='admin' AND u.status='0' AND u.del_flag='0'", (rs, rowNum) -> rs.getLong(1));
        if (!admins.isEmpty()) {
            messageService.publishMessage(admins, PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.WORKFLOW, "开平仓日志写入故障，累计丢失 " + count + " 条，请检查日志队列和数据库。", null, "/trade" +
                "/trade-decision-log"));
        }
    }

    /**
     * 把实盘闸门拒绝原因推送到管理员的顶部工作消息。
     */
    public void notifyLiveBlocked(QuantCredential credential, String symbol, String reason) {
        long now = System.currentTimeMillis();
        String noticeKey = (credential == null ? "unknown" : credential.getId()) + ":" + symbol + ":" + reason;
        var publish = new java.util.concurrent.atomic.AtomicBoolean();
        blockedNoticeTimes.compute(noticeKey, (key, previous) -> {
            if (previous == null || now - previous >= 60_000L) {
                publish.set(true);
                return now;
            }
            return previous;
        });
        if (!publish.get()) {
            return;
        }
        if (blockedNoticeTimes.size() > 1024) {
            blockedNoticeTimes.entrySet().removeIf(entry -> now - entry.getValue() > 60_000L);
        }
        String content =
            "实盘开仓已阻止\n账户: " + safe(credential == null ? null : credential.getAccountName()) + "\n交易对: " + safe(symbol) + "\n原因: " + safe(reason) + "\n时间: " + LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        try {
            List<Long> adminIds = jdbcTemplate.query("SELECT DISTINCT ur.user_id FROM sys_user_role ur JOIN sys_role r ON r.role_id=ur.role_id JOIN sys_user u ON u.user_id=ur" +
                ".user_id " + "WHERE r.role_key='admin' AND u.status='0' AND u.del_flag='0'", (rs, rowNum) -> rs.getLong(1));
            if (!adminIds.isEmpty()) {
                messageService.publishMessage(adminIds, PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.WORKFLOW, content, null, "/quant/terminal/index"));
            }
            log.warn("实盘开仓已阻止信息：{}", content);
        } catch (RuntimeException ex) {
            log.error("实盘闸门原因推送管理员消息失败 symbol={}", symbol, ex);
        }
    }

    /**
     * 发布开仓站内消息并持久化待发送邮件，由后台 Outbox 任务投递。
     */
    public void notifyOpen(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal quantity, BigDecimal price, int leverage,
                           BigDecimal openRatio, String reason) {
        String subject = "量化开仓通知 - " + symbol + " " + cnDirection(direction);
        String content = "【量化开仓通知】\n" + "策略名称: " + safe(strategy == null ? null : strategy.getStrategyName()) + "\n" + "账户: " + safe(credential == null ? null :
            credential.getAccountName()) + "\n" + "交易对: " + symbol + "\n" + "方向: " + cnDirection(direction) + "\n" + "开仓数量: " + plain(quantity) + "\n" + "开仓价格: " + plain(price) + "\n" + "杠杆倍数: " + leverage + "倍\n" + "开仓比例: " + plain(openRatio) + "\n" + "触发原因: " + safe(reason) + "\n" + "时间: " + LocalDateTime.now();
        send(credential == null ? null : credential.getId(), subject, content);
    }

    /**
     * 按交易所订单成交明细核算平仓结果；成交事实不完整时不发送估算通知。
     */
    public void notifyCloseByStrategyOrder(QuantStrategy strategy, QuantCredential credential, Long strategyInstanceId, String positionCycleKey,
                                           String symbol, String direction, BigDecimal requestedQuantity, BigDecimal entryPrice, String quoteAsset,
                                           BigDecimal maximumReturnRate, String closeType, String reason) {
        notifyCloseByStrategyOrder(strategy, credential, strategyInstanceId, positionCycleKey, symbol, direction, requestedQuantity,
            entryPrice, quoteAsset, maximumReturnRate, null, closeType, reason);
    }

    public void notifyCloseByStrategyOrder(QuantStrategy strategy, QuantCredential credential, Long strategyInstanceId, String positionCycleKey,
                                           String symbol, String direction, BigDecimal requestedQuantity, BigDecimal entryPrice, String quoteAsset,
                                           BigDecimal maximumReturnRate, BigDecimal triggerReturnRate, String closeType, String reason) {
        Long orderId = resolveStrategyCloseOrderId(credential == null ? null : credential.getId(), strategyInstanceId, symbol, direction, requestedQuantity);
        if (orderId != null) {
            notifyCloseByOrder(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, quoteAsset,
                maximumReturnRate, triggerReturnRate, closeType, reason);
            return;
        }
        enqueuePendingCloseFact(strategy, credential, symbol, direction, requestedQuantity, entryPrice, null, strategyInstanceId,
            positionCycleKey, quoteAsset, maximumReturnRate, triggerReturnRate, closeType, reason);
    }

    private Long resolveStrategyCloseOrderId(Long credentialId, Long strategyInstanceId, String symbol, String direction, BigDecimal quantity) {
        if (orderMapper == null || credentialId == null || strategyInstanceId == null || symbol == null || direction == null
            || quantity == null || quantity.signum() <= 0) {
            return null;
        }
        String closeSide = "SHORT".equalsIgnoreCase(direction) ? "BUY" : "SELL";
        List<QuantOrder> matches = orderMapper.selectList(new LambdaQueryWrapper<QuantOrder>()
            .eq(QuantOrder::getCredentialId, credentialId)
            .eq(QuantOrder::getStrategyInstanceId, strategyInstanceId)
            .eq(QuantOrder::getSymbol, symbol)
            .in(QuantOrder::getPositionSide, direction, "BOTH")
            .eq(QuantOrder::getReduceOnly, true)
            .eq(QuantOrder::getSide, closeSide)
            .in(QuantOrder::getStatus, "FILLED", "CANCELED", "EXPIRED", "REJECTED")
            .gt(QuantOrder::getExecutedQuantity, BigDecimal.ZERO)
            .ge(QuantOrder::getUpdatedAt, LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(6))
            .orderByDesc(QuantOrder::getUpdatedAt).last("LIMIT 20")).stream()
            .filter(order -> order.getExchangeOrderId() != null && order.getExecutedQuantity() != null
                && order.getExecutedQuantity().compareTo(quantity) == 0)
            .toList();
        if (matches.size() != 1) {
            return null;
        }
        return matches.getFirst().getExchangeOrderId();
    }

    public void notifyCloseByOrder(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal requestedQuantity,
                                  BigDecimal entryPrice, Long orderId, String quoteAsset, String closeType, String reason) {
        notifyCloseByOrder(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, quoteAsset, null, null, closeType, reason);
    }

    public void notifyCloseByOrder(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal requestedQuantity,
                                  BigDecimal entryPrice, Long orderId, String quoteAsset, BigDecimal maximumReturnRate, String closeType, String reason) {
        notifyCloseByOrder(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, quoteAsset,
            maximumReturnRate, null, closeType, reason);
    }

    public void notifyCloseByOrder(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal requestedQuantity,
                                  BigDecimal entryPrice, Long orderId, String quoteAsset, BigDecimal maximumReturnRate,
                                  BigDecimal triggerReturnRate, String closeType, String reason) {
        if (credential == null || orderId == null || entryPrice == null || entryPrice.signum() <= 0) {
            log.warn("平仓成交通知暂缓：缺少账户、订单或开仓价 credentialId={} symbol={} orderId={}",
                credential == null ? null : credential.getId(), symbol, orderId);
            return;
        }
        try {
            if (!tryNotifyCloseByOrder(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, quoteAsset,
                maximumReturnRate, triggerReturnRate, closeType, reason)) {
                enqueuePendingCloseFact(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, quoteAsset,
                    maximumReturnRate, triggerReturnRate, closeType, reason);
            }
        } catch (RuntimeException ex) {
            log.warn("平仓成交事实核验失败，已安排后台补偿 credentialId={} symbol={} orderId={}", credential.getId(), symbol, orderId, ex);
            enqueuePendingCloseFact(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, quoteAsset,
                maximumReturnRate, triggerReturnRate, closeType, reason);
        }
    }

    private boolean tryNotifyCloseByOrder(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal requestedQuantity,
                                          BigDecimal entryPrice, Long orderId, String quoteAsset, BigDecimal maximumReturnRate,
                                          BigDecimal triggerReturnRate, String closeType, String reason) {
        if (credential == null || orderId == null || entryPrice == null || entryPrice.signum() <= 0 || requestedQuantity == null
            || requestedQuantity.signum() <= 0) {
            return false;
        }
        Instant to = Instant.now().plusSeconds(1);
        Instant from = to.minus(Duration.ofDays(6));
        ExchangeGateway.Credential exchangeCredential = new ExchangeGateway.Credential(
            credential.getApiKey(), credential.getSecretKey(), credential.getSignatureType(), credential.getRunMode());
        ExchangeGateway.OrderSnapshot order = gateway.queryOrder(exchangeCredential, symbol, orderId, null);
        if (order == null || !orderId.equals(order.orderId()) || !symbol.equalsIgnoreCase(order.symbol())
            || !java.util.Set.of("FILLED", "CANCELED", "EXPIRED", "REJECTED").contains(order.status() == null ? "" : order.status().toUpperCase(java.util.Locale.ROOT))
            || order.executedQuantity() == null || order.executedQuantity().signum() <= 0
            || order.executedQuantity().compareTo(requestedQuantity) > 0) {
            log.warn("平仓订单尚未确认终态成交，等待补偿 credentialId={} symbol={} orderId={} status={} executedQuantity={}",
                credential.getId(), symbol, orderId, order == null ? null : order.status(), order == null ? null : order.executedQuantity());
            return false;
        }
        List<ExchangeGateway.TradeSnapshot> fills = gateway.queryTradesByOrder(
            exchangeCredential, symbol, orderId, new ExchangeGateway.TimeRange(from, to), 1000L);
        if (fills == null || fills.isEmpty() || fills.size() >= 1000) {
            log.warn("平仓成交明细未完整确认，等待补偿 credentialId={} symbol={} orderId={} fillCount={}",
                credential.getId(), symbol, orderId, fills == null ? null : fills.size());
            return false;
        }
        BigDecimal filledQuantity = BigDecimal.ZERO;
        BigDecimal quoteAmount = BigDecimal.ZERO;
        BigDecimal realizedPnl = BigDecimal.ZERO;
        java.util.Map<String, BigDecimal> commissions = new java.util.TreeMap<>();
        boolean commissionComplete = true;
        for (ExchangeGateway.TradeSnapshot fill : fills) {
            if (fill == null || !symbol.equalsIgnoreCase(fill.symbol()) || !orderId.equals(fill.orderId())
                || fill.quantity() == null || fill.quantity().signum() <= 0 || fill.price() == null || fill.price().signum() <= 0
                || fill.realizedPnl() == null) {
                log.warn("平仓成交明细字段不完整，等待补偿 credentialId={} symbol={} orderId={}",
                    credential.getId(), symbol, orderId);
                return false;
            }
            filledQuantity = filledQuantity.add(fill.quantity());
            quoteAmount = quoteAmount.add(fill.quantity().multiply(fill.price()));
            realizedPnl = realizedPnl.add(fill.realizedPnl());
            if (fill.commission() == null || fill.commissionAsset() == null || fill.commissionAsset().isBlank()) {
                commissionComplete = false;
            } else {
                commissions.merge(fill.commissionAsset(), fill.commission(), BigDecimal::add);
            }
        }
        if (order.executedQuantity().compareTo(filledQuantity) != 0) {
            log.warn("平仓订单累计成交量与成交明细不一致，等待补偿 credentialId={} symbol={} orderId={} orderExecuted={} tradeExecuted={}",
                credential.getId(), symbol, orderId, order.executedQuantity(), filledQuantity);
            return false;
        }
        BigDecimal averageClosePrice = quoteAmount.divide(filledQuantity, 18, RoundingMode.HALF_UP);
        String feeText = !commissionComplete || commissions.isEmpty() ? "N/A（成交手续费字段不完整）" :
            commissions.entrySet().stream().map(entry -> plain(entry.getValue()) + " " + entry.getKey()).collect(java.util.stream.Collectors.joining(", "));
        ExchangeGateway.AccountSnapshot account = null;
        try {
            account = gateway.queryAccountFresh(new ExchangeGateway.Credential(
                credential.getApiKey(), credential.getSecretKey(), credential.getSignatureType(), credential.getRunMode()));
        } catch (RuntimeException ex) {
            log.warn("平仓后账户资金快照获取失败 credentialId={} orderId={}，通知将标记资金待核实",
                credential.getId(), orderId, ex);
        }
        String asset = safe(quoteAsset);
        String subject = "量化平仓通知 - " + symbol + " " + cnDirection(direction);
        String content = "【量化平仓通知】\n"
            + "策略名称: " + safe(strategy == null ? null : strategy.getStrategyName()) + "\n"
            + "账户: " + safe(credential.getAccountName()) + "\n"
            + "交易对: " + symbol + "\n"
            + "方向: " + cnDirection(direction) + "\n"
            + "平仓类型: " + safe(closeType) + "\n"
            + "触发条件: " + safe(reason) + "\n"
            + "平仓订单: " + orderId + "\n"
            + "平仓数量: " + plain(filledQuantity) + "\n"
            + "开仓价格: " + plain(entryPrice) + "\n"
            + "实际平仓均价: " + plain(averageClosePrice) + "\n"
            + "平仓触发时回报率: " + returnRatePercentage(triggerReturnRate) + "\n"
            + "最大回报率: " + percentage(maximumReturnRate) + "\n"
            + "交易所已实现盈亏（未扣手续费）: " + plain(realizedPnl) + " " + asset + "\n"
            + "手续费: " + feeText + "\n"
            + "平仓后账户资金\n"
            + "钱包余额: " + (account == null ? "待核实" : plain(account.walletBalance()) + " " + asset) + "\n"
            + "账户权益: " + (account == null ? "待核实" : plain(account.marginBalance()) + " " + asset) + "\n"
            + "可用余额: " + (account == null ? "待核实" : plain(account.availableBalance()) + " " + asset) + "\n"
            + "时间: " + LocalDateTime.now();
        return sendCloseOnce(credential.getId(), orderId, subject, content);
    }

    private boolean sendCloseOnce(Long credentialId, Long orderId, String subject, String content) {
        String orderMarker = "平仓订单: " + orderId;
        try {
            long systemCount = notificationMapper.selectCount(new LambdaQueryWrapper<QuantNotification>()
                .eq(QuantNotification::getCredentialId, credentialId).eq(QuantNotification::getChannel, "SYSTEM")
                .like(QuantNotification::getContent, orderMarker));
            if (systemCount == 0) {
                QuantNotification systemNotification = new QuantNotification();
                systemNotification.setCredentialId(credentialId);
                systemNotification.setChannel("SYSTEM");
                systemNotification.setRecipient("TERMINAL");
                systemNotification.setSubject(subject);
                systemNotification.setContent(content);
                systemNotification.setStatus("PENDING");
                systemNotification.setRetryCount(0);
                systemNotification.setNextRetryAt(LocalDateTime.now());
                systemNotification.setCreatedAt(LocalDateTime.now());
                if (notificationMapper.insert(systemNotification) != 1) {
                    return false;
                }
                if (systemNotification.getId() != null) {
                    claimAndDeliver(systemNotification);
                }
            }
            List<String> emailRecipients = recipients(credentialId);
            for (String email : emailRecipients) {
                long emailCount = notificationMapper.selectCount(new LambdaQueryWrapper<QuantNotification>()
                    .eq(QuantNotification::getCredentialId, credentialId).eq(QuantNotification::getChannel, "EMAIL")
                    .eq(QuantNotification::getRecipient, email).like(QuantNotification::getContent, orderMarker));
                if (emailCount > 0) {
                    continue;
                }
                QuantNotification emailNotification = new QuantNotification();
                emailNotification.setCredentialId(credentialId);
                emailNotification.setChannel("EMAIL");
                emailNotification.setRecipient(email);
                emailNotification.setSubject(subject);
                emailNotification.setContent(content);
                emailNotification.setStatus("PENDING");
                emailNotification.setRetryCount(0);
                emailNotification.setNextRetryAt(LocalDateTime.now());
                emailNotification.setCreatedAt(LocalDateTime.now());
                if (notificationMapper.insert(emailNotification) != 1) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException ex) {
            log.error("平仓通知 Outbox 写入失败 credentialId={} orderId={}", credentialId, orderId, ex);
            return false;
        }
    }

    private void enqueuePendingCloseFact(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal requestedQuantity,
                                         BigDecimal entryPrice, Long orderId, String quoteAsset, BigDecimal maximumReturnRate, String closeType, String reason) {
        enqueuePendingCloseFact(strategy, credential, symbol, direction, requestedQuantity, entryPrice, orderId, null, null, quoteAsset,
            maximumReturnRate, null, closeType, reason);
    }

    private void enqueuePendingCloseFact(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal requestedQuantity,
                                         BigDecimal entryPrice, Long orderId, Long strategyInstanceId, String positionCycleKey, String quoteAsset,
                                         BigDecimal maximumReturnRate, BigDecimal triggerReturnRate, String closeType, String reason) {
        if (credential == null || credential.getId() == null || entryPrice == null || entryPrice.signum() <= 0
            || orderId == null && strategyInstanceId == null) {
            log.error("平仓通知无法建立待核验任务：缺少账户、订单或策略身份 credentialId={} symbol={} orderId={} strategyInstanceId={}",
                credential == null ? null : credential.getId(), symbol, orderId, strategyInstanceId);
            return;
        }
        String taskKey = orderId == null
            ? "STRATEGY:" + strategyInstanceId + ":" + (positionCycleKey == null ? "UNKNOWN" : positionCycleKey) + ":" + UUID.randomUUID()
            : String.valueOf(orderId);
        try {
            long existing = notificationMapper.selectCount(new LambdaQueryWrapper<QuantNotification>()
                .eq(QuantNotification::getCredentialId, credential.getId()).eq(QuantNotification::getChannel, "FACT_PENDING")
                .eq(QuantNotification::getRecipient, taskKey));
            if (existing > 0) {
                return;
            }
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("credentialId", String.valueOf(credential.getId()));
            task.put("strategyName", strategy == null ? null : strategy.getStrategyName());
            task.put("strategyInstanceId", strategyInstanceId == null ? null : String.valueOf(strategyInstanceId));
            task.put("positionCycleKey", positionCycleKey);
            task.put("symbol", symbol);
            task.put("direction", direction);
            task.put("requestedQuantity", requestedQuantity == null ? null : requestedQuantity.toPlainString());
            task.put("entryPrice", entryPrice.toPlainString());
            task.put("orderId", orderId == null ? null : String.valueOf(orderId));
            task.put("quoteAsset", quoteAsset);
            task.put("maximumReturnRate", maximumReturnRate == null ? null : maximumReturnRate.toPlainString());
            task.put("triggerReturnRate", triggerReturnRate == null ? null : triggerReturnRate.toPlainString());
            task.put("closeType", closeType);
            task.put("reason", reason);
            QuantNotification pending = new QuantNotification();
            pending.setCredentialId(credential.getId());
            pending.setChannel("FACT_PENDING");
            pending.setRecipient(taskKey);
            pending.setSubject("平仓成交事实待核验");
            pending.setContent(QuantJsonMapper.writeMap(task));
            pending.setStatus("PENDING");
            pending.setRetryCount(0);
            pending.setNextRetryAt(LocalDateTime.now().plusSeconds(30));
            pending.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(pending);
            log.warn("平仓通知已进入持久化待核验队列 credentialId={} symbol={} orderId={} strategyInstanceId={}",
                credential.getId(), symbol, orderId, strategyInstanceId);
        } catch (Exception ex) {
            log.error("平仓通知待核验任务持久化失败 credentialId={} symbol={} orderId={} strategyInstanceId={}",
                credential.getId(), symbol, orderId, strategyInstanceId, ex);
        }
    }

    @Scheduled(fixedDelay = 30000L, initialDelay = 30000L)
    public void retryPendingCloseFacts() {
        LocalDateTime now = LocalDateTime.now();
        List<QuantNotification> pending = notificationMapper.selectList(new LambdaQueryWrapper<QuantNotification>()
            .eq(QuantNotification::getChannel, "FACT_PENDING")
            .and(query -> query.in(QuantNotification::getStatus, "PENDING", "RETRY").le(QuantNotification::getNextRetryAt, now)
                .or().eq(QuantNotification::getStatus, "SENDING").lt(QuantNotification::getLeaseUntil, now))
            .orderByAsc(QuantNotification::getCreatedAt).last("LIMIT 50"));
        for (QuantNotification task : pending) {
            if (task.getCreatedAt() != null && task.getCreatedAt().isBefore(now.minusDays(5))) {
                task.setStatus("FAILED");
                task.setNextRetryAt(null);
                task.setLastError("超过 Binance 成交明细可查询窗口，无法继续自动核验");
                notificationMapper.updateById(task);
                log.error("平仓通知待核验任务超过可查询窗口，停止自动重试 notificationId={} recipient={}", task.getId(), task.getRecipient());
                continue;
            }
            String ownerToken = UUID.randomUUID().toString();
            LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(2);
            if (notificationMapper.claim(task.getId(), ownerToken, leaseUntil) != 1) {
                continue;
            }
            task.setStatus("SENDING");
            task.setOwnerToken(ownerToken);
            task.setLeaseUntil(leaseUntil);
            try {
                Map<String, Object> data = QuantJsonMapper.readMap(task.getContent());
                Long credentialId = Long.valueOf(String.valueOf(data.get("credentialId")));
                QuantCredential credential = credentialMapper.selectById(credentialId);
                if (credential == null) {
                    throw new IllegalStateException("平仓通知待核验任务对应的交易凭证不存在");
                }
                QuantStrategy strategy = null;
                if (data.get("strategyName") != null) {
                    strategy = new QuantStrategy();
                    strategy.setStrategyName(String.valueOf(data.get("strategyName")));
                }
                String symbol = String.valueOf(data.get("symbol"));
                String direction = String.valueOf(data.get("direction"));
                BigDecimal requestedQuantity = decimal(data.get("requestedQuantity"));
                Long orderId = data.get("orderId") == null ? null : Long.valueOf(String.valueOf(data.get("orderId")));
                if (orderId == null && data.get("strategyInstanceId") != null) {
                    orderId = resolveStrategyCloseOrderId(credentialId, Long.valueOf(String.valueOf(data.get("strategyInstanceId"))),
                        symbol, direction, requestedQuantity);
                }
                boolean sent = orderId != null && tryNotifyCloseByOrder(strategy, credential, symbol, direction, requestedQuantity,
                    decimal(data.get("entryPrice")), orderId, String.valueOf(data.get("quoteAsset")),
                    decimal(data.get("maximumReturnRate")), decimal(data.get("triggerReturnRate")),
                    String.valueOf(data.get("closeType")), String.valueOf(data.get("reason")));
                if (sent) {
                    task.setStatus("SENT");
                    task.setSentAt(LocalDateTime.now());
                    task.setNextRetryAt(null);
                    task.setLastError(null);
                } else {
                    retryCloseFactTask(task, "交易所订单或成交明细尚未完整");
                }
            } catch (Exception ex) {
                retryCloseFactTask(task, ex.getClass().getSimpleName() + ": " + ex.getMessage());
                log.warn("平仓通知待核验任务执行失败 notificationId={} orderId={}", task.getId(), task.getRecipient(), ex);
            }
            task.setOwnerToken(null);
            task.setLeaseUntil(null);
            notificationMapper.updateById(task);
        }
    }

    private static BigDecimal decimal(Object value) {
        return value == null || String.valueOf(value).isBlank() || "null".equals(String.valueOf(value))
            ? null : new BigDecimal(String.valueOf(value));
    }

    private static void retryCloseFactTask(QuantNotification task, String reason) {
        int retry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        task.setRetryCount(retry);
        task.setStatus("RETRY");
        task.setLastError(reason);
        task.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(1800L, 30L * (1L << Math.min(retry - 1, 6)))));
    }

    /**
     * PAPER 平仓按模拟账本结算结果通知，不使用行情估算替代结算金额。
     */
    public void notifyClosePaper(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal quantity,
                                 BigDecimal entryPrice, BigDecimal closePrice, BigDecimal realizedPnl, String quoteAsset,
                                 BigDecimal walletBalance, BigDecimal availableBalance, BigDecimal unrealizedPnl, String closeType, String reason) {
        notifyClosePaper(strategy, credential, symbol, direction, quantity, entryPrice, closePrice, realizedPnl, quoteAsset,
            walletBalance, availableBalance, unrealizedPnl, null, null, closeType, reason);
    }

    public void notifyClosePaper(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal quantity,
                                 BigDecimal entryPrice, BigDecimal closePrice, BigDecimal realizedPnl, String quoteAsset,
                                 BigDecimal walletBalance, BigDecimal availableBalance, BigDecimal unrealizedPnl, BigDecimal maximumReturnRate,
                                 String closeType, String reason) {
        notifyClosePaper(strategy, credential, symbol, direction, quantity, entryPrice, closePrice, realizedPnl, quoteAsset,
            walletBalance, availableBalance, unrealizedPnl, maximumReturnRate, null, closeType, reason);
    }

    public void notifyClosePaper(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal quantity,
                                 BigDecimal entryPrice, BigDecimal closePrice, BigDecimal realizedPnl, String quoteAsset,
                                 BigDecimal walletBalance, BigDecimal availableBalance, BigDecimal unrealizedPnl, BigDecimal maximumReturnRate,
                                 BigDecimal triggerReturnRate, String closeType, String reason) {
        String asset = safe(quoteAsset);
        BigDecimal marginBalance = walletBalance == null || unrealizedPnl == null ? null : walletBalance.add(unrealizedPnl);
        String subject = "量化平仓通知 - " + symbol + " " + cnDirection(direction);
        String content = "【量化平仓通知】\n"
            + "策略名称: " + safe(strategy == null ? null : strategy.getStrategyName()) + "\n"
            + "账户: " + safe(credential == null ? null : credential.getAccountName()) + "\n"
            + "交易对: " + symbol + "\n"
            + "方向: " + cnDirection(direction) + "\n"
            + "平仓类型: " + safe(closeType) + "\n"
            + "触发条件: " + safe(reason) + "\n"
            + "平仓数量: " + plain(quantity) + "\n"
            + "开仓价格: " + plain(entryPrice) + "\n"
            + "实际平仓价格: " + plain(closePrice) + "\n"
            + "平仓触发时回报率: " + returnRatePercentage(triggerReturnRate) + "\n"
            + "最大回报率: " + percentage(maximumReturnRate) + "\n"
            + "模拟账本已实现盈亏: " + plain(realizedPnl) + " " + asset + "\n"
            + "手续费: PAPER 模式未计入交易所手续费\n"
            + "平仓后账户资金\n"
            + "钱包余额: " + plain(walletBalance) + " " + asset + "\n"
            + "账户权益: " + plain(marginBalance) + " " + asset + "\n"
            + "可用余额: " + plain(availableBalance) + " " + asset + "\n"
            + "时间: " + LocalDateTime.now();
        send(credential == null ? null : credential.getId(), subject, content);
    }

    /**
     * 站内消息和邮件先写入 Outbox；邮件 SMTP 网络请求由后台任务执行。
     */
    private void send(Long credentialId, String subject, String content) {
        // 站内消息也走 Outbox，避免即时推送失败后仍被误标记为已发送。
        QuantNotification systemNotification = new QuantNotification();
        systemNotification.setCredentialId(credentialId);
        systemNotification.setChannel("SYSTEM");
        systemNotification.setRecipient("TERMINAL");
        systemNotification.setSubject(subject);
        systemNotification.setContent(content);
        systemNotification.setStatus("PENDING");
        systemNotification.setRetryCount(0);
        systemNotification.setNextRetryAt(LocalDateTime.now());
        systemNotification.setCreatedAt(LocalDateTime.now());
        try {
            notificationMapper.insert(systemNotification);
            if (systemNotification.getId() != null) {
                claimAndDeliver(systemNotification);
            }
        } catch (RuntimeException ex) {
            log.error("量化交易站内通知入队或即时投递失败 credentialId={} subject={}", credentialId, subject, ex);
        }

        List<String> recipients;
        try {
            recipients = recipients(credentialId);
        } catch (RuntimeException ex) {
            log.error("量化交易通知邮箱解析失败 credentialId={} subject={}", credentialId, subject, ex);
            return;
        }
        if (recipients.isEmpty()) {
            log.warn("未配置量化交易通知邮箱，邮件未发送: subject={}", subject);
            return;
        }
        for (String email : recipients) {
            QuantNotification notification = new QuantNotification();
            notification.setCredentialId(credentialId);
            notification.setChannel("EMAIL");
            notification.setRecipient(email);
            notification.setSubject(subject);
            notification.setContent(content);
            notification.setStatus("PENDING");
            notification.setRetryCount(0);
            notification.setNextRetryAt(LocalDateTime.now());
            notification.setCreatedAt(LocalDateTime.now());
            try {
                notificationMapper.insert(notification);
                // 邮件网络等待不能阻塞行情消费；现有调度器负责抢占、发送和失败重试。
            } catch (RuntimeException ex) {
                // Outbox 入库本身失败时不能影响交易，但必须留下高优先级错误日志。
                log.error("量化交易通知 Outbox 写入失败 credentialId={} recipient={} subject={}", credentialId, email, subject, ex);
            }
        }
    }

    /**
     * 通过系统内置消息中心推送通知；失败由 Outbox 记录并重试。
     * 收件人：
     * 1. 凭证所属用户（若可解析）
     * 2. 系统管理员（兜底）
     * 保证交易账户所有者无论如何都能收到。
     */
    private void publishSystemMessage(Long credentialId, String subject, String content) {
        java.util.LinkedHashSet<Long> recipients = new java.util.LinkedHashSet<>();
        if (credentialId != null) {
            try {
                Long ownerId = jdbcTemplate.queryForObject("SELECT user_id FROM quant_credential WHERE id=? AND del_flag='0'", Long.class, credentialId);
                if (ownerId != null) {
                    recipients.add(ownerId);
                }
            } catch (org.springframework.dao.EmptyResultDataAccessException ignored) {
                // 凭证不存在或已删除：交给管理员兜底
            }
        }
        List<Long> admins = jdbcTemplate.query("SELECT DISTINCT ur.user_id FROM sys_user_role ur " + "JOIN sys_role r ON r.role_id=ur.role_id " + "JOIN sys_user u ON u" +
            ".user_id=ur.user_id " + "WHERE r.role_key='admin' AND u.status='0' AND u.del_flag='0'", (rs, rowNum) -> rs.getLong(1));
        if (admins != null) {
            recipients.addAll(admins);
        }
        if (recipients.isEmpty()) {
            throw new IllegalStateException("量化交易系统消息没有有效接收人");
        }
        messageService.publishMessage(new java.util.ArrayList<>(recipients), PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.WORKFLOW, subject + "\n" + content, null,
            "/quant/terminal/index"));
    }

    /**
     * 由管理端发起单条通知重试。
     */
    public void retry(Long id) {
        QuantNotification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new ServiceException("通知不存在");
        }
        if ("FACT_PENDING".equalsIgnoreCase(notification.getChannel())) {
            throw new ServiceException("该记录为平仓成交核验任务，由后台自动处理");
        }
        if (claimAndDeliver(notification)) {
            return;
        }
        throw new ServiceException("通知正在发送或已处理");
    }

    /**
     * 定期重试仍处于可重试状态的通知。
     */
    @Scheduled(fixedDelay = 30000L, initialDelay = 30000L)
    public void retryPendingNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<QuantNotification> pending = notificationMapper.selectList(new LambdaQueryWrapper<QuantNotification>()
            .in(QuantNotification::getChannel, "EMAIL", "SYSTEM")
            .and(query -> query.in(QuantNotification::getStatus, "PENDING", "RETRY").le(QuantNotification::getNextRetryAt, now)
                .or().eq(QuantNotification::getStatus, "SENDING").lt(QuantNotification::getLeaseUntil, now))
            .orderByAsc(QuantNotification::getCreatedAt).last("LIMIT 50"));
        String ownerToken = UUID.randomUUID().toString();
        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(2);
        for (QuantNotification notification : pending) {
            claimAndDeliver(notification, ownerToken, leaseUntil);
        }
    }

    private boolean claimAndDeliver(QuantNotification notification) {
        return claimAndDeliver(notification, UUID.randomUUID().toString(), LocalDateTime.now().plusMinutes(2));
    }

    private boolean claimAndDeliver(QuantNotification notification, String ownerToken, LocalDateTime leaseUntil) {
        if (notificationMapper.claim(notification.getId(), ownerToken, leaseUntil) != 1) {
            return false;
        }
        notification.setStatus("SENDING");
        notification.setOwnerToken(ownerToken);
        notification.setLeaseUntil(leaseUntil);
        deliver(notification);
        return true;
    }

    /**
     * 执行实际通知投递并更新 Outbox 状态。
     */
    private void deliver(QuantNotification notification) {
        try {
            if ("SYSTEM".equalsIgnoreCase(notification.getChannel())) {
                publishSystemMessage(notification.getCredentialId(), notification.getSubject(), notification.getContent());
            } else {
                MailBuilder.of().to(notification.getRecipient()).subject(notification.getSubject()).text(notification.getContent()).send();
            }
            notification.setStatus("SENT");
            notification.setNextRetryAt(null);
            notification.setOwnerToken(null);
            notification.setLeaseUntil(null);
            notification.setLastError(null);
            notification.setSentAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        } catch (RuntimeException ex) {
            int retry = (notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1;
            notification.setRetryCount(retry);
            notification.setStatus("RETRY");
            notification.setOwnerToken(null);
            notification.setLeaseUntil(null);
            notification.setLastError(ex.getMessage());
            long delaySeconds = Math.min(1800L, 30L * (1L << Math.min(retry - 1, 6)));
            notification.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            try {
                notificationMapper.updateById(notification);
            } catch (RuntimeException dbEx) {
                log.error("更新通知重试状态失败 notificationId={}", notification.getId(), dbEx);
            }
            log.error("量化交易通知发送失败，将自动重试 notificationId={} channel={} recipient={} subject={}", notification.getId(), notification.getChannel(), notification.getRecipient(), notification.getSubject(), ex);
        }
    }

    /**
     * 解析交易通知收件人列表。
     */
    private List<String> recipients(Long credentialId) {
        String raw = null;
        if (credentialId != null) {
            try {
                QuantRiskConfig risk = riskConfigMapper.selectOne(new LambdaQueryWrapper<QuantRiskConfig>().eq(QuantRiskConfig::getCredentialId, credentialId));
                if (risk != null && risk.getNotificationEmails() != null && !risk.getNotificationEmails().isBlank()) {
                    raw = risk.getNotificationEmails();
                }
            } catch (RuntimeException ex) {
                log.error("量化交易通知邮箱配置查询失败，尝试使用默认邮箱 credentialId={}", credentialId, ex);
            }
        }
        if ((raw == null || raw.isBlank()) && properties.getNotification() != null) {
            raw = properties.getNotification().getDefaultEmails();
        }
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+")).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
    }
}

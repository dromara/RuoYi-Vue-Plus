package org.dromara.quant.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mail.core.MailBuilder;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.notification.domain.QuantNotification;
import org.dromara.quant.notification.mapper.QuantNotificationMapper;
import org.dromara.quant.risk.domain.QuantRiskConfig;
import org.dromara.quant.risk.mapper.QuantRiskConfigMapper;
import org.dromara.quant.strategy.domain.QuantStrategy;
import org.dromara.system.api.MessageService;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
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
    private final java.util.concurrent.ConcurrentHashMap<String, Long> blockedNoticeTimes = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 创建 QuantTradeNotificationService 实例并初始化其依赖。
     */
    public QuantTradeNotificationService(QuantRiskConfigMapper riskConfigMapper, QuantNotificationMapper notificationMapper, QuantProperties properties,
                                         MessageService messageService, JdbcTemplate jdbcTemplate) {
        this.riskConfigMapper = riskConfigMapper;
        this.notificationMapper = notificationMapper;
        this.properties = properties;
        this.messageService = messageService;
        this.jdbcTemplate = jdbcTemplate;
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
     * 发布平仓站内消息并持久化待发送邮件，由后台 Outbox 任务投递。
     */
    public void notifyClose(QuantStrategy strategy, QuantCredential credential, String symbol, String direction, BigDecimal quantity, BigDecimal entryPrice,
                            BigDecimal closePrice, BigDecimal profitAmount, BigDecimal profitRate, String closeType, String reason) {
        String subject = "量化平仓通知 - " + symbol + " " + cnDirection(direction);
        String content = "【量化平仓通知】\n" + "策略名称: " + safe(strategy == null ? null : strategy.getStrategyName()) + "\n" + "账户: " + safe(credential == null ? null :
            credential.getAccountName()) + "\n" + "交易对: " + symbol + "\n" + "方向: " + cnDirection(direction) + "\n" + "平仓类型: " + safe(closeType) + "\n" + "触发条件: " + safe(reason) + "\n" + "平仓数量: " + plain(quantity) + "\n" + "开仓价格: " + plain(entryPrice) + "\n" + "平仓价格: " + plain(closePrice) + "\n" + "收益金额: " + (profitAmount == null ? "N/A" : profitAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()) + " USDT\n" + "收益率: " + (profitRate == null ? "N/A" : profitRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%") + "\n" + "时间: " + LocalDateTime.now();
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

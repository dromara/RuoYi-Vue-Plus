package org.dromara.quant.reconcile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.util.QuantJsonMapper;
import org.dromara.quant.protection.ProtectionOrderService;
import org.dromara.quant.execution.DispatchBlockedEvent;
import org.dromara.quant.execution.DispatchRecoveryRequestedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nonblocking callback-to-REST bridge. WebSocket callbacks merely flag an
 * account; one fixed-delay worker coalesces bursts and executes REST off the
 * stream thread. The periodic full-position heartbeat remains authoritative
 * if callbacks are lost or the pending set cannot be drained.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LivePositionEventCoordinator {
    private static final long MIN_REFRESH_GAP_MILLIS = 2000L;
    private final QuantCredentialMapper credentials;
    private final TradingReconciliationService reconciliation;
    private final Set<Long> pending = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, Long> retryNotBefore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> failures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lastAttempt = new ConcurrentHashMap<>();
    private final Set<ProtectionRecoveryRequest> protectionRecoveryPending = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<ProtectionRecoveryRequest, Long> protectionRetryNotBefore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ProtectionRecoveryRequest, Integer> protectionRecoveryFailures = new ConcurrentHashMap<>();
    private ProtectionOrderService protectionOrderService;
    @Autowired(required = false)
    private LivePositionFreshness freshness;

    /**
     * Existing private-stream supervisors should forward decoded events here
     * instead of opening a second subscription. Event callback is constant-time
     * and never accesses the database or Binance REST.
     */
    public void onUserDataEvent(Long credentialId, ExchangeGateway.UserDataEvent event) {
        if (credentialId == null || event == null || event.eventType() == null) {
            return;
        }
        String type = event.eventType();
        if ("DISCONNECTED".equalsIgnoreCase(type) || "listenKeyExpired".equalsIgnoreCase(type)) {
            if (freshness != null) {
                freshness.invalidate(credentialId);
            }
            return;
        }
        if ("CONNECTED".equalsIgnoreCase(type)) {
            protectionRecoveryPending.add(new ProtectionRecoveryRequest(credentialId, null));
            request(credentialId);
            return;
        }
        if ("ALGO_UPDATE".equalsIgnoreCase(type)) {
            // Algorithm-order events affect protection recovery, not account position facts.
            // Keep the position snapshot valid unless a subsequent trade/account event confirms a position change.
            protectionRecoveryPending.add(new ProtectionRecoveryRequest(credentialId, algoEventSymbol(event.rawJson())));
            return;
        }
        if ("ACCOUNT_UPDATE".equalsIgnoreCase(type)) {
            if (mayChangeAccountPosition(event.rawJson())) {
                request(credentialId);
            }
            return;
        }
        if ("ORDER_TRADE_UPDATE".equalsIgnoreCase(type)) {
            if (mayChangePosition(event.rawJson())) {
                request(credentialId);
            }
            return;
        }
        // Unknown event types retain fail-closed behavior, but also queue a refresh so the gate can recover.
        request(credentialId);
    }

    private static String algoEventSymbol(String rawJson) {
        try {
            java.util.Map<String, Object> root = QuantJsonMapper.readMap(rawJson);
            Object orderValue = root.get("o");
            if (orderValue instanceof java.util.Map<?, ?> order && order.get("s") != null) {
                return order.get("s").toString();
            }
            Object symbol = root.get("s");
            return symbol == null ? null : symbol.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Binance ACCOUNT_UPDATE may contain balance-only changes with an empty P array.
     * Only a non-empty or unrecognized position payload invalidates the account position snapshot.
     */
    private static boolean mayChangeAccountPosition(String rawJson) {
        try {
            java.util.Map<String, Object> root = QuantJsonMapper.readMap(rawJson);
            Object accountValue = root.get("a");
            if (!(accountValue instanceof java.util.Map<?, ?> account)) {
                return true;
            }
            Object positionValue = account.get("P");
            return !(positionValue instanceof java.util.List<?> positions) || !positions.isEmpty();
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * 判断订单私有事件是否表示成交；无法确认事件类型时按可能改变持仓处理。
     */
    private static boolean mayChangePosition(String rawJson) {
        try {
            Object orderValue = QuantJsonMapper.readMap(rawJson).get("o");
            if (!(orderValue instanceof java.util.Map<?, ?> order)) {
                return true;
            }
            Object executionType = order.get("x");
            if (executionType == null) {
                return true;
            }
            String type = executionType.toString();
            return !"NEW".equalsIgnoreCase(type) && !"CANCELED".equalsIgnoreCase(type)
                && !"EXPIRED".equalsIgnoreCase(type) && !"AMENDMENT".equalsIgnoreCase(type);
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * 合并同一账户的重复刷新请求并安排持仓事实对账。
     */
    public void request(Long credentialId) {
        if (credentialId != null) {
            // A stream event may make the previously verified snapshot stale.
            // No further LIVE opening until a full REST snapshot commits.
            if (freshness != null) {
                freshness.invalidate(credentialId);
            }
            pending.add(credentialId);
        }
    }

    /**
     * 取出待处理账户并执行串行刷新，避免并发覆盖新事实。
     */
    @Scheduled(fixedDelay = 2000, initialDelay = 3000)
    public void drain() {
        // Resolve gate-blocked protection tickets before potentially slower position snapshots.
        drainProtectionRecoveryRequests();
        for (Long id : Set.copyOf(pending)) {
            long now = System.currentTimeMillis();
            if (now < retryNotBefore.getOrDefault(id, 0L) || now - lastAttempt.getOrDefault(id, 0L) < MIN_REFRESH_GAP_MILLIS || !pending.remove(id)) {
                continue;
            }
            lastAttempt.put(id, now);
            try {
                QuantCredential credential = credentials.selectById(id);
                if (credential == null || !"0".equals(credential.getStatus()) || !"LIVE".equalsIgnoreCase(credential.getRunMode())) {
                    failures.remove(id);
                    retryNotBefore.remove(id);
                    continue;
                }
                reconciliation.refreshPositionsOnly(credential);
                failures.remove(id);
                retryNotBefore.remove(id);
            } catch (TradingReconciliationService.SnapshotSupersededException ex) {
                // 正常的快照竞争不累计网络故障退避；保留请求，下一轮按账户最小间隔重新获取。
                failures.remove(id);
                retryNotBefore.remove(id);
                pending.add(id);
                log.debug("LIVE 事件刷新快照已被更新事实取代，保留刷新请求 credentialId={}", id);
            } catch (RuntimeException ex) {
                // Failed refresh never marks an account fresh. Periodic REST
                // heartbeat retries independently; allow next event to queue.
                int count = failures.merge(id, 1, (previous, ignored) -> Math.min(10, previous + 1));
                long backoffMillis = Math.min(60000L, 2000L << Math.min(count - 1, 5));
                retryNotBefore.put(id, System.currentTimeMillis() + backoffMillis);
                pending.add(id);
                log.warn("LIVE event-triggered position refresh failed credentialId={} retryInMs={}", id, backoffMillis, ex);
            }
        }
    }

    private void drainProtectionRecoveryRequests() {
        ProtectionOrderService recoveryService = protectionOrderService;
        if (recoveryService == null) {
            return;
        }
        for (ProtectionRecoveryRequest request : Set.copyOf(protectionRecoveryPending)) {
            long now = System.currentTimeMillis();
            if (now < protectionRetryNotBefore.getOrDefault(request, 0L) || !protectionRecoveryPending.remove(request)) {
                continue;
            }
            try {
                QuantCredential credential = credentials.selectById(request.credentialId());
                if (credential == null || !"0".equals(credential.getStatus()) || !"LIVE".equalsIgnoreCase(credential.getRunMode())) {
                    protectionRecoveryFailures.remove(request);
                    protectionRetryNotBefore.remove(request);
                    continue;
                }
                int recovered = recoveryService.recoverUnresolvedProtectionDispatches(request.credentialId(), request.symbol());
                if (recovered > 0) {
                    log.info("私有流触发保护派发恢复完成 credentialId={} symbol={} recoveredCount={}", request.credentialId(), request.symbol(), recovered);
                }
                if (recoveryService.hasUnresolvedProtectionDispatches(request.credentialId(), request.symbol())) {
                    retryProtectionRecovery(request, now, null);
                } else {
                    protectionRecoveryFailures.remove(request);
                    protectionRetryNotBefore.remove(request);
                }
            } catch (RuntimeException ex) {
                log.warn("私有流触发保护派发核实失败，派发门禁保持不变 credentialId={} symbol={} errorType={}", request.credentialId(), request.symbol(),
                    ex.getClass().getSimpleName());
                retryProtectionRecovery(request, now, ex);
            }
        }
    }

    /** 未决票据继续保留在队列中，以有上限的指数退避重查精确交易所事实。 */
    private void retryProtectionRecovery(ProtectionRecoveryRequest request, long now, RuntimeException failure) {
        int count = protectionRecoveryFailures.merge(request, 1, (previous, ignored) -> Math.min(10, previous + 1));
        long retryInMillis = Math.min(60000L, 2000L << Math.min(count - 1, 5));
        protectionRetryNotBefore.put(request, now + retryInMillis);
        protectionRecoveryPending.add(request);
        if (failure == null) {
            log.warn("保护派发仍未决，保留门禁并安排精确核实 credentialId={} symbol={} retryInMs={}", request.credentialId(), request.symbol(), retryInMillis);
        } else {
            log.warn("保护派发核实失败，保留门禁并安排退避重试 credentialId={} symbol={} retryInMs={} errorType={}", request.credentialId(), request.symbol(), retryInMillis,
                failure.getClass().getSimpleName());
        }
    }

    /** 注入保护撤单的精确事实恢复服务。 */
    @Autowired(required = false)
    public void setProtectionOrderService(ProtectionOrderService protectionOrderService) {
        this.protectionOrderService = protectionOrderService;
    }

    private record ProtectionRecoveryRequest(Long credentialId, String symbol) {
    }

    /**
     * 派发槽被占用时排队核实当前旧票据；监听器不执行 REST，避免阻塞交易调用和数据库事务。
     */
    @EventListener
    public void onDispatchBlocked(DispatchBlockedEvent event) {
        if (event != null && event.credentialId() != null && event.symbol() != null) {
            protectionRecoveryPending.add(new ProtectionRecoveryRequest(event.credentialId(), event.symbol()));
        }
    }

    /** 未知派发结果立即进入异步精确核实队列；不在订单线程中追加 REST 请求。 */
    @EventListener
    public void onDispatchRecoveryRequested(DispatchRecoveryRequestedEvent event) {
        if (event != null && event.credentialId() != null && event.symbol() != null) {
            protectionRecoveryPending.add(new ProtectionRecoveryRequest(event.credentialId(), event.symbol()));
        }
    }
}

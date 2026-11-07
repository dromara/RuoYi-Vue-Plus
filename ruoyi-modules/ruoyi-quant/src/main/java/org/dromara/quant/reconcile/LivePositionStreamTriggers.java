package org.dromara.quant.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.market.UserDataStreamHub;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keep one LIVE subscription reference per enabled credential, via the existing
 * UserDataStreamHub. The hub owns all Binance connections, listenKey renewal,
 * reconnect and fan-out; this component MUST NEVER subscribe to Binance itself.
 * Other consumers (including terminal SSE) share the same in-JVM connection.
 * LIVE lease supervision is mandatory even if event-driven position refresh is disabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quant.live", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LivePositionStreamTriggers implements org.springframework.boot.ApplicationRunner {
    private static final int LEASE_SECONDS = 75;
    private final QuantCredentialMapper credentials;
    private final UserDataStreamHub hub;
    private final LivePositionEventCoordinator coordinator;
    private final LivePositionFreshness freshness;
    private final LiveUserStreamLeaseService lease;
    private final Map<Long, AutoCloseable> subscriptions = new HashMap<>();
    private final Map<Long, Long> subscriptionEpochs = new HashMap<>();
    /**
     * Avoid flooding WARN when an account is leased to a different LIVE node.
     */
    private final Map<Long, Long> nextLeaseConflictLogAt = new HashMap<>();

    /**
     * Establish the private-stream ownership before the application reports startup
     * complete and before the strategy runtime supervisor's initial tick.
     * The scheduled supervisor remains responsible for continuous renewal.
     */
    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        supervise();
    }

    /**
     * 检查活动账户的私有流状态并按需重建订阅。
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 0)
    public synchronized void supervise() {
        Set<Long> liveIds = new HashSet<>();
        final java.util.List<QuantCredential> enabledCredentials;
        try {
            enabledCredentials = credentials.selectList(new LambdaQueryWrapper<QuantCredential>().eq(QuantCredential::getStatus, "0"));
            if (enabledCredentials == null) {
                throw new IllegalStateException("LIVE credential discovery returned null");
            }
        } catch (RuntimeException ex) {
            // On discovery failure we cannot establish which accounts are still
            // enabled. Stop owned LIVE streams rather than continuing with a
            // previously authorized account and a renewable stale lease.
            for (Long id : Set.copyOf(subscriptions.keySet())) {
                closeSubscription(id);
                freshness.invalidate(id, "CREDENTIAL_DISCOVERY_FAILED");
                try {
                    lease.release(id);
                } catch (RuntimeException releaseError) {
                    log.warn("LIVE stream discovery failure lease cleanup failed credentialId={}", id, releaseError);
                }
            }
            log.error("LIVE user-stream credential discovery failed; subscriptions revoked", ex);
            return;
        }
        for (QuantCredential credential : enabledCredentials) {
            if (!"LIVE".equalsIgnoreCase(credential.getRunMode())) {
                continue;
            }
            Long id = credential.getId();
            liveIds.add(id);
            try {
                Long leaseEpoch = lease.acquire(id, LEASE_SECONDS);
                if (leaseEpoch == null) {
                    long now = System.currentTimeMillis();
                    if (now >= nextLeaseConflictLogAt.getOrDefault(id, 0L)) {
                        nextLeaseConflictLogAt.put(id, now + 60_000L);
                        String leaseState;
                        try {
                            leaseState = lease.describe(id);
                        } catch (RuntimeException diagnosticError) {
                            leaseState = "lease diagnostic unavailable: " + diagnosticError.getClass().getSimpleName();
                        }
                        log.warn("LIVE私有流租约竞争 credentialId={} dbLease=[{}]；本节点禁止创建交易策略运行时", id, leaseState);
                    }
                    closeSubscription(id);
                    freshness.invalidate(id, "LEASE_NOT_OWNED");
                    continue;
                }
                Long previousEpoch = subscriptionEpochs.get(id);
                if (previousEpoch != null && !previousEpoch.equals(leaseEpoch)) {
                    // A process can reacquire an expired lease with the same token.
                    // Epoch changes still fence the old socket and its account facts.
                    closeSubscription(id);
                    freshness.invalidate(id, "LEASE_EPOCH_CHANGED");
                }
                // Hub may have forcibly removed the old WebSocket after a
                // lease loss while this supervisor still holds its old handle.
                // Discard that handle and re-enroll instead of silently
                // treating the lost stream as active after lease reacquisition.
                if (subscriptions.containsKey(id) && !hub.hasLocalStream(id)) {
                    closeSubscription(id);
                }
                hub.registerLeaseOwner(id, leaseEpoch);
                subscriptionEpochs.put(id, leaseEpoch);
                if (subscriptions.containsKey(id)) {
                    continue;
                }
                // A new stream ownership generation cannot inherit a position
                // snapshot verified under the previous WebSocket/lease. Block
                // LIVE entries until a fresh authenticated REST snapshot wins.
                freshness.invalidate(id, "STREAM_SUBSCRIPTION_CREATED");
                AutoCloseable reference = hub.subscribe(credential, event -> {
                    // Hub centrally forwards events into the coordinator; this
                    // listener exists only to hold an active subscription.
                });
                subscriptions.put(id, reference);
                nextLeaseConflictLogAt.remove(id);
                log.info("LIVE私有流租约已获取并注册共享订阅 credentialId={}，允许本节点策略运行时继续初始化", id);
                coordinator.queueRefresh(id);
            } catch (RuntimeException ex) {
                closeSubscription(id);
                freshness.invalidate(id, "STREAM_ENROLLMENT_FAILED");
                // A failed Hub enrollment must not strand a DB lease for 75 seconds.
                // A conditional release cannot affect another node's ownership.
                try {
                    lease.release(id);
                } catch (RuntimeException releaseError) {
                    log.warn("LIVE shared stream lease cleanup failed credentialId={}", id, releaseError);
                }
                log.warn("LIVE shared user stream enrollment failed credentialId={}", id, ex);
            }
        }
        for (Long id : Set.copyOf(subscriptions.keySet())) {
            if (!liveIds.contains(id)) {
                closeSubscription(id);
                freshness.invalidate(id, "CREDENTIAL_DISABLED");
                try {
                    lease.release(id);
                } catch (RuntimeException ex) {
                    // Account removal must never abort cleanup of other accounts.
                    // The DB lease expires without renewal; no stale local stream remains.
                    log.warn("LIVE account removal lease cleanup failed credentialId={}", id, ex);
                }
            }
        }
    }

    private void closeSubscription(Long id) {
        Long epoch = subscriptionEpochs.remove(id);
        if (epoch != null) {
            hub.unregisterLeaseOwner(id, epoch);
        }
        AutoCloseable subscription = subscriptions.remove(id);
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Exception ex) {
                log.warn("LIVE shared stream unsubscribe failed credentialId={}", id, ex);
            }
        }
    }

    /**
     * 停止监督任务并释放私有流订阅。
     */
    @PreDestroy
    public synchronized void close() {
        for (Long id : Set.copyOf(subscriptions.keySet())) {
            closeSubscription(id);
            freshness.invalidate(id, "APPLICATION_SHUTDOWN");
            try {
                lease.release(id);
            } catch (RuntimeException ex) {
                log.warn("LIVE stream shutdown lease cleanup failed credentialId={}", id, ex);
            }
        }
    }
}

package com.wudgaby.stars.support;

import com.wudgaby.stars.config.StarsProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.redisson.api.RSemaphore;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Enrichment 并发槽位：集群全局（Redis）+ 单 Pod（内存）+ 单用户（内存）。
 */
@Slf4j
@Component
public class EnrichmentSlotLimiter {

    private static final String GLOBAL_SEMAPHORE_KEY = "stars:enrichment:global";

    private final int maxConcurrentPerPod;
    private final int maxConcurrentGlobal;
    private final int maxConcurrentPerUser;
    private final Semaphore podSlots;
    private final ConcurrentHashMap<Long, Semaphore> userSlots = new ConcurrentHashMap<>();
    private RSemaphore globalSlots;

    public EnrichmentSlotLimiter(StarsProperties starsProperties) {
        StarsProperties.Summary summary = starsProperties.summary();
        this.maxConcurrentPerPod = summary.maxConcurrentPerPod();
        this.maxConcurrentGlobal = summary.maxConcurrentGlobal();
        this.maxConcurrentPerUser = summary.maxConcurrentPerUser();
        this.podSlots = new Semaphore(maxConcurrentPerPod, true);
    }

    @PostConstruct
    void initGlobalSemaphore() {
        globalSlots = RedisUtils.getClient().getSemaphore(GLOBAL_SEMAPHORE_KEY);
        if (!globalSlots.isExists()) {
            boolean initialized = globalSlots.trySetPermits(maxConcurrentGlobal);
            log.info("Initialized global enrichment semaphore: permits={}, ok={}",
                maxConcurrentGlobal, initialized);
        } else {
            log.info("Global enrichment semaphore already exists: available={}",
                globalSlots.availablePermits());
        }
    }

    /**
     * 阻塞直到取得全局 + Pod + 用户槽位。
     */
    public void acquire(Long userId) throws InterruptedException {
        globalSlots.acquire();
        try {
            podSlots.acquire();
            try {
                userSemaphore(userId).acquire();
            } catch (InterruptedException ex) {
                podSlots.release();
                throw ex;
            }
        } catch (InterruptedException ex) {
            globalSlots.release();
            throw ex;
        }
    }

    public void release(Long userId) {
        userSemaphore(userId).release();
        podSlots.release();
        globalSlots.release();
    }

    public int maxConcurrentPerPod() {
        return maxConcurrentPerPod;
    }

    private Semaphore userSemaphore(Long userId) {
        return userSlots.computeIfAbsent(userId, ignored ->
            new Semaphore(maxConcurrentPerUser, true));
    }
}

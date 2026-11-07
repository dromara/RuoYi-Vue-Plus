package org.dromara.quant.exchange.binance;

import org.dromara.quant.exchange.ExchangeMarketPort;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

/**
 * 有界轮转队列；交易占两个轮次，账户校准与后台查询各占一个，避免饥饿。
 */
final class RestRequestScheduler {
    private final ArrayDeque<Object>[] queues;
    private final int[] rotation = {0, 1, 0, 2, 1, 0, 1, 3};
    private static final long MANUAL_BLOCK_RETRY_HINT_MILLIS = TimeUnit.HOURS.toMillis(24);
    private final long interval;
    private long nextStart;
    private long blockedUntil;
    /** A 418 without a trustworthy expiry requires operator intervention; never auto-expire this latch. */
    private boolean manualBlock;
    private int cursor;
    @SuppressWarnings("unchecked")
    RestRequestScheduler(long intervalMillis) {
        interval = TimeUnit.MILLISECONDS.toNanos(intervalMillis);
        queues = new ArrayDeque[]{new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>()};
    }

    synchronized void acquire(Kind kind) {
        long initialNow = System.nanoTime();
        if (manualBlock) {
            throw deferred("MANUAL_BLOCK", kind, 0L, MANUAL_BLOCK_RETRY_HINT_MILLIS);
        }
        if (initialNow < blockedUntil) {
            throw deferred("BLOCKED_UNTIL", kind, 0L, TimeUnit.NANOSECONDS.toMillis(blockedUntil - initialNow) + 1L);
        }
        var queue = queues[kind.ordinal()];
        if (queue.size() >= 16) {
            throw deferred("QUEUE_FULL", kind, 0L, 2_000L);
        }
        Object ticket = new Object();
        queue.addLast(ticket);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(kind == Kind.TRADE ? 2 : 8);
        try {
            for (; ; ) {
                if (Thread.currentThread().isInterrupted()) {
                    throw deferred("INTERRUPTED", kind, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialNow), 0L);
                }
                if (manualBlock) {
                    throw deferred("MANUAL_BLOCK", kind, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialNow), MANUAL_BLOCK_RETRY_HINT_MILLIS);
                }
                long now = System.nanoTime();
                if (now < blockedUntil) {
                    throw deferred("BLOCKED_UNTIL", kind, TimeUnit.NANOSECONDS.toMillis(now - initialNow), TimeUnit.NANOSECONDS.toMillis(blockedUntil - now) + 1L);
                }
                if (now >= deadline) {
                    throw deferred("QUEUE_TIMEOUT", kind, TimeUnit.NANOSECONDS.toMillis(now - initialNow), 1_500L);
                }
                int selected = cursor;
                for (int i = 0; i < rotation.length; i++) {
                    selected = (cursor + i) % rotation.length;
                    if (!queues[rotation[selected]].isEmpty()) {
                        break;
                    }
                }
                if (rotation[selected] == kind.ordinal() && queue.peekFirst() == ticket && now >= nextStart) {
                    queue.removeFirst();
                    nextStart = now + interval;
                    cursor = (selected + 1) % rotation.length;
                    notifyAll();
                    return;
                }
                long delay = nextStart > now ? nextStart - now : TimeUnit.MILLISECONDS.toNanos(10);
                TimeUnit.NANOSECONDS.timedWait(this, Math.min(delay, deadline - now));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw deferred("INTERRUPTED", kind, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialNow), 0L);
        } finally {
            queue.remove(ticket);
            notifyAll();
        }
    }

    private ExchangeMarketPort.RequestDeferredException deferred(String reason, Kind kind, long waitedMillis, long retryAfterMillis) {
        int[] queueSizes = new int[queues.length];
        for (int i = 0; i < queues.length; i++) {
            queueSizes[i] = queues[i].size();
        }
        return new ExchangeMarketPort.RequestDeferredException(reason, kind.name(), queueSizes[kind.ordinal()],
            queueSizes, waitedMillis, retryAfterMillis);
    }

    /**
     * Permanently blocks REST requests for this process after an unbounded HTTP 418.
     * Restarting the process is an explicit operator action, not an automatic expiry.
     */
    synchronized void blockUntilManualReset() {
        manualBlock = true;
        notifyAll();
    }

    synchronized boolean isManuallyBlocked() {
        return manualBlock;
    }

    synchronized void deferFor(long delayMillis) {
        long duration = TimeUnit.MILLISECONDS.toNanos(Math.max(1L, delayMillis));
        blockedUntil = Math.max(blockedUntil, System.nanoTime() + duration);
        notifyAll();
    }

    /**
     * REST 请求优先级：交易请求优先于账户和后台补偿查询。
     */
    enum Kind {TRADE, POSITION_VERIFY, ACCOUNT, BACKGROUND}
}

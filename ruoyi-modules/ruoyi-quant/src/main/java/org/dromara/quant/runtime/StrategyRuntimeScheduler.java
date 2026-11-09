package org.dromara.quant.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Strategy supervisor and fact-reconciliation timer adapter.
 *
 * <p>All trade decisions and session state remain inside StrategyRuntimeService,
 * so changing scheduling cannot bypass leases, risk, or entry idempotency.
 */
@RequiredArgsConstructor
@Component
public class StrategyRuntimeScheduler {
    private final StrategyRuntimeService runtime;

    /** 周期性发现运行中的策略实例，并驱动其运行时启动或停止。 */
    @Scheduled(fixedDelayString = "#{@quantProperties.runtime.tickSeconds * 1000}", initialDelay = 5000)
    public void supervise() {
        runtime.supervise();
    }

    /**
     * 独立于行情回调续期策略运行租约；续期失败会关闭本节点行情驱动决策。
     * 运行租约最短 15 秒，因此 5 秒监督周期为其保留至少两次重试机会。
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void renewStrategyLeases() {
        runtime.renewActiveRuntimeLeases();
    }

    /**
     * 定期刷新运行策略依赖的持仓事实。
     */
    @Scheduled(fixedDelayString = "#{@quantProperties.reconcile.intervalSeconds * 1000}", initialDelay = 15000)
    public void reconcilePositions() {
        runtime.reconcileRunningPositions();
    }

    /**
     * 行情流异常时独立运行本地止盈止损价格兜底，不参与开仓决策。
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void checkStaleMarketProtectionPrices() {
        runtime.checkStaleMarketProtectionPrices();
    }
}

package org.dromara.quant.execution;

import lombok.extern.slf4j.Slf4j;
import org.dromara.quant.account.TradeCycleAssembler;
import org.dromara.quant.account.domain.vo.TradeCycleFillRecord;
import org.dromara.quant.account.domain.vo.TradeCycleRecord;
import org.dromara.quant.account.domain.vo.TradeCycleSourceFill;
import org.dromara.quant.account.mapper.TradeCycleMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Maintains durable account-symbol trade-cycle projections from immutable fills.
 * Rebuilds happen only after source facts change or during the one-time SQL backfill.
 */
@Service
@Slf4j
public class TradeCycleProjectionService {
    private static final int SCOPE_BATCH_SIZE = 20;
    private static final int MAX_SCOPE_FILLS = 100_000;

    private final TradeCycleMapper mapper;
    private final TransactionTemplate transaction;

    public TradeCycleProjectionService(TradeCycleMapper mapper, PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(30);
    }

    /** Mark one account-symbol projection dirty after its order/fill facts are persisted. */
    public void markDirty(Long credentialId, String symbol) {
        if (credentialId == null || credentialId <= 0 || symbol == null || !symbol.trim().toUpperCase(Locale.ROOT).matches("[A-Z0-9]{2,32}")) {
            throw new IllegalArgumentException("有效的账户和交易对是交易周期投影的必需条件");
        }
        mapper.markDirty(credentialId, symbol.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Rebuild one account-symbol projection synchronously after its source transaction commits.
     * The scheduler remains as the retry path for failed or independently dirtied scopes.
     */
    public void projectNow(Long credentialId, String symbol) {
        rebuild(credentialId, symbol);
    }

    /** The scope row lock makes this safe when more than one application node runs the scheduler. */
    @Scheduled(fixedDelay = 5000, initialDelay = 15000)
    public void projectPendingScopes() {
        List<Map<String, Object>> pending = mapper.selectPendingScopes(SCOPE_BATCH_SIZE);
        for (Map<String, Object> scope : pending) {
            Long credentialId = number(scope.get("credentialId"));
            String symbol = String.valueOf(scope.get("symbol"));
            try {
                rebuild(credentialId, symbol);
            } catch (RuntimeException failure) {
                String reason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                mapper.markFailed(credentialId, symbol, reason);
                log.error("交易周期投影失败 credentialId={} symbol={} errorType={}",
                    credentialId, symbol, failure.getClass().getSimpleName(), failure);
            }
        }
    }

    private void rebuild(Long credentialId, String symbol) {
        transaction.executeWithoutResult(status -> {
            Map<String, Object> scope = mapper.lockScope(credentialId, symbol);
            if (scope == null) {
                return;
            }
            long dirtyVersion = number(scope.get("dirtyVersion"));
            long projectedVersion = number(scope.get("projectedVersion"));
            if (dirtyVersion <= projectedVersion) {
                return;
            }
            long sourceCount = mapper.countSourceFills(credentialId, symbol);
            if (sourceCount > MAX_SCOPE_FILLS) {
                throw new IllegalStateException("账户交易对成交数超过单次重建上限，请按历史区间分段核实后再投影");
            }
            List<TradeCycleSourceFill> sourceFills = mapper.selectSourceFills(credentialId, symbol);
            TradeCycleAssembler.Result result = TradeCycleAssembler.assemble(sourceFills);

            mapper.deleteCycleFills(credentialId, symbol);
            mapper.deleteCycles(credentialId, symbol);
            for (TradeCycleRecord cycle : result.cycles()) {
                if (mapper.insertCycle(cycle) != 1) {
                    throw new IllegalStateException("交易周期汇总记录未能持久化");
                }
            }
            for (TradeCycleFillRecord fill : result.allocations()) {
                if (mapper.insertCycleFill(fill) != 1) {
                    throw new IllegalStateException("交易周期与成交关联记录未能持久化");
                }
            }
            if (mapper.completeProjection(credentialId, symbol, dirtyVersion,
                result.incompleteCount(), result.inferredCount()) != 1) {
                throw new IllegalStateException("交易周期投影版本更新失败");
            }
        });
    }

    private static Long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("交易周期投影范围缺少有效的数据库标识");
    }
}

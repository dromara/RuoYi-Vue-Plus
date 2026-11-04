package org.dromara.quant.runtime.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.runtime.domain.QuantStrategyPositionState;
import org.dromara.quant.runtime.mapper.QuantStrategyPositionStateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略持仓运行状态持久化服务。
 */
@RequiredArgsConstructor
@Service
public class StrategyPositionStateService {
    /** 状态已被其他写入者推进，调用方必须重新核实，不得重发旧状态。 */
    public static final class StateConflictException extends IllegalStateException {
        public StateConflictException(Long instanceId) {
            super("策略持仓状态乐观锁冲突，拒绝覆盖最新运行状态: instanceId=" + instanceId);
        }
    }

    /**
     * 当前领域 Mapper。
     */
    private final QuantStrategyPositionStateMapper mapper;
    /**
     * 按实例维护的进程内更新锁。
     * FIXED: 不再在 finally 中移除锁对象，避免 A、B、C 三线程并发时互相穿越临界区。
     * 实例数量有限，长期持有锁对象的代价可以接受。
     */
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    /**
     * 查询策略实例对应的运行状态。
     */
    public QuantStrategyPositionState findByInstance(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<QuantStrategyPositionState>().eq(QuantStrategyPositionState::getInstanceId, instanceId));
    }

    /** Update the cycle's return-rate peak atomically without advancing the strategy-state optimistic-lock version. */
    @Transactional(rollbackFor = Exception.class)
    public int updateMaximumReturnRate(Long instanceId, Long positionId, String cycleKey, BigDecimal rate) {
        if (instanceId == null || positionId == null || cycleKey == null || rate == null) {
            return 0;
        }
        return mapper.updateMaximumReturnRate(instanceId, positionId, cycleKey, rate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetClosedMaximumReturnRate(Long instanceId) {
        if (instanceId != null) {
            mapper.resetClosedMaximumReturnRate(instanceId);
        }
    }

    /**
     * 返回仍需监管的持仓或待确认执行，支持应用重启恢复。
     */
    public java.util.List<QuantStrategyPositionState> findUnclosed() {
        return mapper.selectList(new LambdaQueryWrapper<QuantStrategyPositionState>().ne(QuantStrategyPositionState::getStatus, "CLOSED").and(query -> query.gt(QuantStrategyPositionState::getPositionQty, BigDecimal.ZERO).or().isNotNull(QuantStrategyPositionState::getPendingAction).or().eq(QuantStrategyPositionState::getStatus, "PROTECTION_CLEANUP")));
    }

    /**
     * 保存或更新策略运行状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(QuantStrategyPositionState state) {
        if (state == null || state.getInstanceId() == null) {
            return;
        }
        // FIXED: 锁对象仅创建不删除，防止多线程下锁被替换后并发穿越。
        Object lock = locks.computeIfAbsent(state.getInstanceId(), ignored -> new Object());
        synchronized (lock) {
            // updated_at 在数据库中是 NOT NULL 且没有默认值，插入和更新都由服务统一维护。
            state.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            if (state.getVersion() == null) {
                state.setVersion(0);
            }
            if (state.getHighestStep() == null) {
                state.setHighestStep(0);
            }
            if (state.getConfirmedProtectionStep() == null) {
                state.setConfirmedProtectionStep(0);
            }
            if (state.getFirstTakeProfitTriggered() == null) {
                state.setFirstTakeProfitTriggered(false);
            }
            if (state.getPendingNotFoundConfirmations() == null) {
                state.setPendingNotFoundConfirmations(0);
            }
            if (state.getRecoveryRequired() == null) {
                state.setRecoveryRequired(false);
            }
            if (state.getUnsafeRecovery() == null) {
                state.setUnsafeRecovery(false);
            }
            if (state.getId() == null) {
                QuantStrategyPositionState existing = findByInstance(state.getInstanceId());
                if (existing == null) {
                    mapper.insert(state);
                } else {
                    state.setId(existing.getId());
                    state.setVersion(existing.getVersion());
                    updateExisting(state);
                }
            } else {
                updateExisting(state);
            }
        }
    }

    private void updateExisting(QuantStrategyPositionState state) {
        Integer expectedVersion = state.getVersion();
        try {
            if (mapper.updateById(state) != 1) {
                throw new StateConflictException(state.getInstanceId());
            }
        } catch (RuntimeException ex) {
            // 乐观锁插件在 SQL 执行前修改实体版本；失败时不能把这个未提交版本留给后续状态比较。
            state.setVersion(expectedVersion);
            throw ex;
        }
    }

    /**
     * 把策略运行状态标记为已平仓。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markClosed(QuantStrategyPositionState state) {
        if (state == null) {
            return;
        }
        state.setStatus("CLOSED");
        state.setPositionQty(BigDecimal.ZERO);
        state.setPendingAction(null);
        state.setPendingClientOrderId(null);
        state.setPendingOrderId(null);
        state.setPendingOrderTime(null);
        state.setPendingIdempotencyKey(null);
        state.setRecoveryRequired(false);
        state.setUnsafeRecovery(false);
        save(state);
    }

    /**
     * 快照确认空仓时先关闭数量事实，但保留恢复阻断，直到运行时持久化平仓 K 线封锁并核实保护单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markClosedAwaitingCandleFence(QuantStrategyPositionState state) {
        if (state == null) {
            return;
        }
        state.setStatus("CLOSED");
        state.setPositionQty(BigDecimal.ZERO);
        state.setRecoveryRequired(true);
        state.setUnsafeRecovery(true);
        save(state);
    }

    /**
     * 快照确认空仓后先归零关联策略状态，但保留恢复位等待运行时封锁平仓 K 线并清理保护单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markClosedByPositionId(Long positionId) {
        if (positionId == null) {
            return;
        }
        mapper.selectList(new LambdaQueryWrapper<QuantStrategyPositionState>().eq(QuantStrategyPositionState::getPositionId, positionId).ne(QuantStrategyPositionState::getStatus
            , "CLOSED")).stream().filter(this::isClosableFromFlatPositionSnapshot).forEach(this::markClosedAwaitingCandleFence);
    }

    private boolean isClosableFromFlatPositionSnapshot(QuantStrategyPositionState state) {
        return "OPEN".equals(state.getStatus()) && state.getPendingAction() == null && !Boolean.TRUE.equals(state.getRecoveryRequired())
            && !Boolean.TRUE.equals(state.getUnsafeRecovery());
    }
}

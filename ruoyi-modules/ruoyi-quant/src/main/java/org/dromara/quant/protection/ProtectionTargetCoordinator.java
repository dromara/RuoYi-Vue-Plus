package org.dromara.quant.protection;

import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.dromara.quant.protection.mapper.ProtectionTargetMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 持仓周期保护目标协调：短事务保存意图，网络请求必须在事务外执行。
 */
@Service
public class ProtectionTargetCoordinator {
    private final ProtectionTargetMapper mapper;
    private final TransactionTemplate transaction;

    public ProtectionTargetCoordinator(ProtectionTargetMapper mapper, PlatformTransactionManager manager) {
        this.mapper = Objects.requireNonNull(mapper);
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(5);
    }

    private static State convert(Map<String, Object> row) {
        return new State(((Number) row.get("target_version")).longValue(), ((Number) row.get("step_index")).intValue(), ((Number) row.get("confirmed_step")).intValue(),
            number(row.get("current_order_id")), number(row.get("pending_order_id")), number(row.get("previous_order_id")), (BigDecimal) row.get("quantity"),
            (BigDecimal) row.get("trigger_price"), (String) row.get("order_type"));
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    /**
     * 创建或读取持仓周期目标，并取得唯一租约所有权。
     */
    public Lease acquire(Long credentialId, QuantPosition position, String type) {
        if (credentialId == null || position == null || position.getId() == null || position.getProtectionCycleKey() == null || position.getProtectionCycleKey().isBlank() || position.getPositionSide() == null || type == null) {
            throw new IllegalStateException("保护目标缺少可信持仓周期，禁止盲目创建");
        }
        String key;
        try {
            key =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((credentialId + ":" + position.getProtectionCycleKey() + ":" + position.getPositionSide() +
                    ":" + type).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("保护目标标识生成失败", ex);
        }
        try {
            mapper.insertTarget(key, credentialId, position.getId(), position.getProtectionCycleKey(), position.getPositionSide(), type);
        } catch (DuplicateKeyException existing) { /* 不覆盖已有意图。 */ }
        String owner = UUID.randomUUID().toString();
        if (mapper.acquireLease(key, owner) != 1) {
            throw new IllegalStateException("保护目标正在同步，等待原目标恢复");
        }
        return new Lease(key, owner);
    }

    /**
     * 查询当前持仓周期已经落库确认的最高阶梯。
     */
    public int confirmedStep(Long credentialId, QuantPosition position) {
        if (position.getProtectionCycleKey() == null) {
            throw new IllegalStateException("缺少持仓周期");
        }
        var steps = mapper.confirmedSteps(credentialId, position.getProtectionCycleKey(), position.getPositionSide());
        return steps.isEmpty() ? 0 : steps.getFirst();
    }

    /**
     * 数据库中的保护目标版本、阶梯状态及当前订单关联。
     */
    public record State(long version, int stepIndex, int confirmedStep, Long currentOrderId, Long pendingOrderId, Long previousOrderId, BigDecimal quantity,
                        BigDecimal triggerPrice, String orderType) {
    }

    /**
     * 持有目标租约期间维护唯一写入权，并在失租时阻止继续派发。
     */
    public final class Lease implements AutoCloseable {
        private final String key;
        private final String owner;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean lost = new AtomicBoolean();
        private final Thread heartbeat;

        private Lease(String key, String owner) {
            this.key = key;
            this.owner = owner;
            heartbeat = Thread.ofVirtual().name("quant-protection-target-lease").start(() -> {
                try {
                    while (!closed.get()) {
                        Thread.sleep(10000);
                        if (closed.get()) {
                            break;
                        }
                        if (mapper.renewLease(key, owner) != 1) {
                            lost.set(true);
                            break;
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ex) {
                    lost.set(true);
                }
            });
        }

        /**
         * 在外部副作用前验证租约仍归当前持有者。
         */
        public void check() {
            if (closed.get() || lost.get() || !Long.valueOf(1).equals(mapper.countOwned(key, owner))) {
                lost.set(true);
                throw new IllegalStateException("保护目标租约失效，禁止提交或覆盖意图");
            }
        }

        private Map<String, Object> row(boolean lock) {
            check();
            return lock ? mapper.lockTarget(key) : mapper.getTarget(key);
        }

        /**
         * 读取当前保护目标状态。
         */
        public State state() {
            return convert(row(false));
        }

        /**
         * 持久化待执行目标；网络派发回调在事务提交后执行。
         */
        public void prepare(QuantProtectionOrder candidate, int step, QuantProtectionOrder previous, Consumer<QuantProtectionOrder> insert) {
            transaction.executeWithoutResult(status -> {
                var old = convert(row(true));
                if (old.pendingOrderId() != null || old.previousOrderId() != null) {
                    throw new PendingTargetException(old.pendingOrderId(), old.previousOrderId());
                }
                boolean sameNotSentTargetRetry = candidate.getId() != null && candidate.getTargetVersion() != null
                    && candidate.getTargetVersion() == old.version() && old.stepIndex() == step
                    && old.quantity() != null && candidate.getQuantity() != null && old.quantity().compareTo(candidate.getQuantity()) == 0
                    && old.triggerPrice() != null && candidate.getStopPrice() != null && old.triggerPrice().compareTo(candidate.getStopPrice()) == 0
                    && Objects.equals(old.orderType(), candidate.getOrderType());
                candidate.setTargetVersion(sameNotSentTargetRetry ? old.version() : old.version() + 1);
                candidate.setStepIndex(Math.max(old.stepIndex(), step));
                insert.accept(candidate);
                if (candidate.getId() == null) {
                    throw new IllegalStateException("保护意图缺少持久化主键");
                }
                requireUpdate(mapper.prepareTarget(key, owner, candidate.getTargetVersion(), candidate.getStepIndex(), candidate.getQuantity(), candidate.getStopPrice(),
                    candidate.getOrderType(), candidate.getId(), previous == null ? old.currentOrderId() : previous.getId()));
            });
        }

        /**
         * 记录交易所已确认的新保护单及其阶梯。
         */
        public void confirmed(QuantProtectionOrder local, int step) {
            transaction.executeWithoutResult(status -> {
                var old = convert(row(true));
                if (old.pendingOrderId() != null && !old.pendingOrderId().equals(local.getId())) {
                    throw new IllegalStateException("必须先恢复持久化待确认保护意图");
                }
                requireUpdate(mapper.confirmTarget(key, owner, local.getId(), Math.max(old.stepIndex(), step), Math.max(old.confirmedStep(), step), local.getQuantity(),
                    local.getStopPrice(), local.getOrderType()));
            });
        }

        /**
         * 仅在精确确认未生效或已终止后放开原待确认意图。
         */
        public void terminal(QuantProtectionOrder local) {
            transaction.executeWithoutResult(status -> {
                var old = convert(row(true));
                if (Objects.equals(old.pendingOrderId(), local.getId())) {
                    requireUpdate(mapper.clearPending(key, owner));
                }
            });
        }

        /**
         * 仅在替换流程确认完成后清理旧订单关联。
         */
        public void cleaned() {
            check();
            requireUpdate(mapper.clearPrevious(key, owner));
        }

        private void requireUpdate(int affected) {
            if (affected != 1) {
                lost.set(true);
                throw new IllegalStateException("保护目标更新失去所有权");
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            heartbeat.interrupt();
            mapper.releaseLease(key, owner);
        }
    }

    /**
     * 已有保护订单仍待交易所确认或旧单清理；调用方应保留交易阻断并稍后重核。
     */
    public static final class PendingTargetException extends IllegalStateException {
        public PendingTargetException(Long pendingOrderId, Long previousOrderId) {
            super("保护目标尚待确认或清理，禁止生成第二份意图 pendingOrderId=" + pendingOrderId + " previousOrderId=" + previousOrderId);
        }
    }
}

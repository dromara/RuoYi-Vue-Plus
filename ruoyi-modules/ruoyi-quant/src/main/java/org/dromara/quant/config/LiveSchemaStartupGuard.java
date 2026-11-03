package org.dromara.quant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * LIVE must never start against a half-migrated quant database. The checks are read-only.
 * Schema presence is necessary, not sufficient: Binance integration drills remain mandatory.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quant.live", name = "enabled", havingValue = "true")
public class LiveSchemaStartupGuard implements ApplicationRunner {
    private static final Set<String> TABLES = Set.of("quant_credential", "quant_strategy", "quant_strategy_instance", "quant_strategy_position_state",
        "quant_strategy_runtime_lease", "quant_strategy_entry_guard", "quant_execution_record", "quant_open_capacity", "quant_order", "quant_position", "quant_protection_order",
        "quant_protection_target", "quant_trade", "quant_financial_event", "quant_financial_event_sync_cursor", "quant_trade_decision_log", "quant_live_dispatch_guard", "quant_order_dispatch_ticket", "quant_strategy_fill_allocation",
        "quant_strategy_fill_journal", "quant_strategy_fill_sync_cursor", "quant_live_account_settings_intent", "quant_position_sync_fence", "quant_live_user_stream_lease",
        "quant_strategy_order_intent", "quant_strategy_pre_dispatch_intent", "quant_live_recovery_case", "quant_live_recovery_audit");

    private static final Set<String> ORDER_BALANCE_SNAPSHOT_COLUMNS = Set.of("leverage_snapshot", "price_tick_size_snapshot",
        "post_fill_wallet_balance", "post_fill_balance_at", "post_fill_balance_requested_at");
    private static final Set<String> POSITION_CYCLE_KEY_COLUMNS = Set.of(
        "quant_strategy_pre_dispatch_intent|position_cycle_key", "quant_strategy_order_intent|position_cycle_key");

    private static final Set<String> UNIQUE = Set.of("quant_strategy_runtime_lease|credential_id,symbol", "quant_strategy_entry_guard|credential_id,symbol,kline_open_time",
        "quant_execution_record|credential_id,idempotency_key", "quant_open_capacity|credential_id,idempotency_key", "quant_strategy_position_state|instance_id",
        "quant_protection_target|target_key", "quant_live_dispatch_guard|credential_id,symbol", "quant_order_dispatch_ticket|credential_id,idempotency_key",
        "quant_strategy_fill_allocation|credential_id,symbol,exchange_trade_id", "quant_strategy_fill_journal|credential_id,symbol,exchange_trade_id",
        "quant_strategy_fill_sync_cursor|credential_id,symbol", "quant_financial_event|credential_id,event_identity_key", "quant_financial_event_sync_cursor|credential_id",
        "quant_live_recovery_case|credential_id,symbol,active_case_key",
        "quant_live_account_settings_intent|credential_id,symbol", "quant_strategy_order_intent|credential_id,symbol,client_order_id",
        "quant_strategy_pre_dispatch_intent|credential_id,idempotency_key", "quant_position_sync_fence|credential_id");

    private final LiveSchemaContractMapper mapper;

    /**
     * 应用启动时验证 LIVE 运行所需的数据库结构。
     */
    @Override
    public void run(ApplicationArguments args) {
        verify();
    }

    void verify() {
        List<String> actualTables = mapper.innodbTables();
        List<String> actualUnique = mapper.uniqueContracts();
        List<String> leaseEpoch = mapper.leaseEpochColumns();
        List<String> entryGuardColumns = mapper.entryGuardColumns();
        List<String> protectionOwnerColumns = mapper.protectionOwnerColumns();
        List<String> positionReturnRateColumns = mapper.positionReturnRateColumns();
        List<String> orderBalanceSnapshotColumns = mapper.orderBalanceSnapshotColumns();
        List<String> positionCycleKeyColumns = mapper.positionCycleKeyColumns();
        if (actualTables == null || actualUnique == null || leaseEpoch == null || entryGuardColumns == null
            || positionReturnRateColumns == null || orderBalanceSnapshotColumns == null || positionCycleKeyColumns == null) {
            throw new IllegalStateException("LIVE database metadata unavailable; refuse to enable real trading");
        }
        var missingTables = new java.util.TreeSet<>(TABLES);
        missingTables.removeAll(actualTables);
        var missingUnique = new java.util.TreeSet<>(UNIQUE);
        missingUnique.removeAll(actualUnique);
        boolean missingEpoch = !leaseEpoch.contains("quant_strategy_runtime_lease|lease_epoch");
        boolean missingEntryGuardType = !entryGuardColumns.contains("quant_strategy_entry_guard|guard_type");
        boolean missingProtectionOwner = protectionOwnerColumns == null || !protectionOwnerColumns.containsAll(List.of("strategy_id", "strategy_instance_id"));
        boolean missingPositionReturnRate = !positionReturnRateColumns.contains("maximum_return_rate");
        var missingOrderBalanceSnapshotColumns = new java.util.TreeSet<>(ORDER_BALANCE_SNAPSHOT_COLUMNS);
        missingOrderBalanceSnapshotColumns.removeAll(orderBalanceSnapshotColumns);
        var missingPositionCycleKeyColumns = new java.util.TreeSet<>(POSITION_CYCLE_KEY_COLUMNS);
        missingPositionCycleKeyColumns.removeAll(positionCycleKeyColumns);
        if (!missingTables.isEmpty() || !missingUnique.isEmpty() || missingEpoch || missingEntryGuardType || missingProtectionOwner
            || missingPositionReturnRate || !missingOrderBalanceSnapshotColumns.isEmpty() || !missingPositionCycleKeyColumns.isEmpty()) {
            List<String> requiredMigrations = new java.util.ArrayList<>();
            if (missingTables.contains("quant_strategy_fill_journal") || missingTables.contains("quant_strategy_fill_sync_cursor")
                || missingUnique.contains("quant_strategy_fill_journal|credential_id,symbol,exchange_trade_id")
                || missingUnique.contains("quant_strategy_fill_sync_cursor|credential_id,symbol")) {
                requiredMigrations.add("script/sql/quant/V20261025_01__strategy_fill_journal.sql");
            }
            if (missingTables.contains("quant_strategy_order_intent") || missingUnique.contains("quant_strategy_order_intent|credential_id,symbol,client_order_id")) {
                requiredMigrations.add("script/sql/quant/V20261014_09__strategy_order_intent.sql");
            }
            if (missingTables.contains("quant_strategy_pre_dispatch_intent") || missingUnique.contains("quant_strategy_pre_dispatch_intent|credential_id,idempotency_key")) {
                requiredMigrations.add("script/sql/quant/V20261014_10__pre_dispatch_strategy_intent.sql");
            }
            if (missingTables.contains("quant_position_sync_fence") || missingUnique.contains("quant_position_sync_fence|credential_id")) {
                requiredMigrations.add("script/sql/quant/V20261014_12__position_sync_fence.sql");
            }
            if (missingTables.contains("quant_financial_event_sync_cursor") || missingUnique.contains("quant_financial_event_sync_cursor|credential_id")) {
                requiredMigrations.add("script/sql/quant/V20261025_02__financial_event_sync_cursor.sql");
            }
            if (missingTables.contains("quant_financial_event") || missingUnique.contains("quant_financial_event|credential_id,event_identity_key")) {
                requiredMigrations.add("script/sql/quant/V20261026_01__financial_event_identity_key.sql");
            }
            if (missingTables.contains("quant_live_recovery_case") || missingTables.contains("quant_live_recovery_audit")
                || missingUnique.contains("quant_live_recovery_case|credential_id,symbol,active_case_key")) {
                requiredMigrations.add("script/sql/quant/V20261014_05__recovery_case_audit.sql");
            }
            if (missingEntryGuardType) {
                requiredMigrations.add("script/sql/quant/V20261016_01__close_candle_entry_guard.sql");
            }
            if (missingProtectionOwner) {
                requiredMigrations.add("script/sql/quant/V20261020_01__protection_strategy_owner.sql");
            }
            if (missingPositionReturnRate) {
                requiredMigrations.add("script/sql/quant/V20261021_01__strategy_position_maximum_return_rate.sql");
            }
            if (!missingOrderBalanceSnapshotColumns.isEmpty()) {
                requiredMigrations.add("script/sql/quant/V20261027_02__quant_order_balance_snapshot.sql");
            }
            if (!missingPositionCycleKeyColumns.isEmpty()) {
                requiredMigrations.add("script/sql/quant/V20261103_01__trade_history_position_cycle_key.sql");
            }
            String migrationHint = requiredMigrations.isEmpty()
                ? "apply the migrations corresponding to the missing schema contracts"
                : "apply required migration(s): " + String.join(", ", requiredMigrations);
            throw new IllegalStateException("LIVE schema preflight rejected startup; missing InnoDB tables=" + missingTables + ", missing unique-index contracts=" + missingUnique
                + ", leaseEpochMissing=" + missingEpoch + ", entryGuardTypeMissing=" + missingEntryGuardType
                + ", missingOrderBalanceSnapshotColumns=" + missingOrderBalanceSnapshotColumns
                + ", missingPositionCycleKeyColumns=" + missingPositionCycleKeyColumns
                + ". On the application's configured MySQL database, " + migrationHint + ", then restart; LIVE schema preflight does not auto-create trading tables.");
        }
    }
}

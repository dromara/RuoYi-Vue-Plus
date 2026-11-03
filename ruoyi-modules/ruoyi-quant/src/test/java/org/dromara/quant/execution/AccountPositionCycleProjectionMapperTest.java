package org.dromara.quant.execution;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AccountPositionCycleProjectionMapperTest {
    private Configuration configuration() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/quant/AccountPositionCycleProjectionMapper.xml";
        try (InputStream input = org.apache.ibatis.io.Resources.getResourceAsStream(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private Connection database() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE quant_strategy_fill_journal (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "exchange_trade_id BIGINT NOT NULL, exchange_order_id BIGINT, side VARCHAR(8), quantity DECIMAL(32,16), price DECIMAL(32,16), " +
                "commission DECIMAL(32,16), commission_asset VARCHAR(20), realized_pnl DECIMAL(32,16), trade_time TIMESTAMP, attribution_status VARCHAR(24))");
            statement.execute("CREATE TABLE quant_position_cycle (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, position_side VARCHAR(8) NOT NULL)");
            statement.execute("CREATE TABLE quant_trade_cycle_allocation (id BIGINT PRIMARY KEY, position_cycle_id BIGINT NOT NULL)");
        }
        return connection;
    }

    private List<String> blockedSymbols(Connection connection, Long credentialId, String symbol) throws Exception {
        MappedStatement mapped = configuration().getMappedStatement(
            AccountPositionCycleProjectionMapper.class.getName() + ".selectProjectionBlockedSymbols");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("credentialId", credentialId);
        parameters.put("symbol", symbol);
        BoundSql sql = mapped.getBoundSql(parameters);
        try (PreparedStatement statement = connection.prepareStatement(sql.getSql())) {
            new org.apache.ibatis.scripting.defaults.DefaultParameterHandler(mapped, parameters, sql).setParameters(statement);
            try (ResultSet result = statement.executeQuery()) {
                java.util.ArrayList<String> values = new java.util.ArrayList<>();
                while (result.next()) values.add(result.getString(1));
                return values;
            }
        }
    }

    private int execute(Connection connection, String statementId, Map<String, Object> parameters) throws Exception {
        MappedStatement mapped = configuration().getMappedStatement(statementId);
        BoundSql sql = mapped.getBoundSql(parameters);
        try (PreparedStatement statement = connection.prepareStatement(sql.getSql())) {
            new org.apache.ibatis.scripting.defaults.DefaultParameterHandler(mapped, parameters, sql).setParameters(statement);
            return statement.executeUpdate();
        }
    }

    @Test
    void blocksIncompleteRawFillEvenWhenAttributionWaitsForAnOrder() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_strategy_fill_journal VALUES " +
                "(1,10,'BTCUSDT',101,NULL,'BUY',1,100,NULL,NULL,NULL,'2026-11-03 00:00:00','WAITING_ORDER')," +
                "(2,10,'ETHUSDT',102,202,'BUY',1,100,0.01,'USDT',0,'2026-11-03 00:00:00','WAITING_ORDER')," +
                "(3,10,'XRPUSDT',103,203,'SELL',1,100,0.01,'USDT!',0,'2026-11-03 00:00:01','WAITING_ORDER')");

            assertEquals(List.of("BTCUSDT"), blockedSymbols(connection, 10L, "BTCUSDT"));
            assertTrue(blockedSymbols(connection, 10L, "ETHUSDT").isEmpty());
            assertEquals(List.of("XRPUSDT"), blockedSymbols(connection, 10L, "XRPUSDT"));
        }
    }

    @Test
    void blocksPendingAndConflictingFactsEvenWhenNumericFieldsArePresent() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_strategy_fill_journal VALUES " +
                "(1,10,'BTCUSDT',101,201,'BUY',1,100,0.01,'USDT',0,'2026-11-03 00:00:00','PENDING_FACTS')," +
                "(2,10,'BTCUSDT',102,202,'SELL',1,101,0.01,'BNB',1,'2026-11-03 00:00:01','CONFLICT')");

            assertEquals(List.of("BTCUSDT"), blockedSymbols(connection, 10L, "BTCUSDT"));
        }
    }

    private Connection balanceDatabase() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE quant_position_cycle (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "position_side VARCHAR(8) NOT NULL, baseline_unknown BOOLEAN NOT NULL, status VARCHAR(24) NOT NULL, " +
                "opening_balance DECIMAL(32,16), opening_balance_at TIMESTAMP, closing_balance DECIMAL(32,16), closing_balance_at TIMESTAMP, updated_at TIMESTAMP)");
            statement.execute("CREATE TABLE quant_trade_cycle_allocation (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "exchange_trade_id BIGINT NOT NULL, exchange_order_id BIGINT NOT NULL, position_cycle_id BIGINT NOT NULL, " +
                "allocation_role VARCHAR(8) NOT NULL, trade_time TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE quant_order (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "exchange_order_id BIGINT NOT NULL, post_fill_wallet_balance DECIMAL(32,16), post_fill_balance_at TIMESTAMP)");
            statement.execute("INSERT INTO quant_position_cycle VALUES " +
                "(1,10,'BTCUSDT','BOTH',FALSE,'CLOSED',NULL,NULL,NULL,NULL,NULL)," +
                "(2,10,'BTCUSDT','BOTH',FALSE,'OPEN',NULL,NULL,NULL,NULL,NULL)," +
                "(3,10,'ETHUSDT','BOTH',FALSE,'CLOSED',NULL,NULL,NULL,NULL,NULL)");
            statement.execute("INSERT INTO quant_trade_cycle_allocation VALUES " +
                "(11,10,'BTCUSDT',101,201,1,'OPEN','2026-11-03 00:00:00')," +
                "(12,10,'BTCUSDT',102,202,1,'CLOSE','2026-11-03 00:01:00')," +
                "(13,10,'BTCUSDT',102,202,2,'OPEN','2026-11-03 00:01:00')," +
                "(31,10,'ETHUSDT',301,301,3,'OPEN','2026-11-03 00:00:00')," +
                "(32,10,'ETHUSDT',302,302,3,'CLOSE','2026-11-03 00:01:00')");
            statement.execute("INSERT INTO quant_order VALUES " +
                "(201,10,'BTCUSDT',201,1000,'2026-11-03 00:00:01')," +
                "(202,10,'BTCUSDT',202,990,'2026-11-03 00:01:01')," +
                "(301,10,'ETHUSDT',301,800,'2026-11-03 00:00:01')," +
                "(302,10,'ETHUSDT',302,790,'2026-11-03 00:01:01')");
        }
        return connection;
    }

    @Test
    void refreshesBalancesFromFirstOpenAndFinalCloseOrdersButExcludesReversalOrders() throws Exception {
        try (Connection connection = balanceDatabase(); var statement = connection.createStatement()) {
            String namespace = AccountPositionCycleProjectionMapper.class.getName();
            Map<String, Object> btc = Map.of("credentialId", 10L, "symbol", "BTCUSDT");
            Map<String, Object> eth = Map.of("credentialId", 10L, "symbol", "ETHUSDT");

            assertEquals(1, execute(connection, namespace + ".refreshBalanceSnapshots", btc));
            try (ResultSet result = statement.executeQuery("SELECT opening_balance,closing_balance FROM quant_position_cycle WHERE id=1")) {
                assertTrue(result.next());
                assertEquals(new java.math.BigDecimal("1000.0000000000000000"), result.getBigDecimal(1));
                assertNull(result.getBigDecimal(2), "the reversal order cannot provide a close-only wallet balance");
            }
            try (ResultSet result = statement.executeQuery("SELECT opening_balance,closing_balance FROM quant_position_cycle WHERE id=2")) {
                assertTrue(result.next());
                assertNull(result.getBigDecimal(1), "the reversal order cannot provide an open-only wallet balance");
            }

            assertEquals(1, execute(connection, namespace + ".refreshBalanceSnapshots", eth));
            try (ResultSet result = statement.executeQuery("SELECT opening_balance,closing_balance FROM quant_position_cycle WHERE id=3")) {
                assertTrue(result.next());
                assertEquals(new java.math.BigDecimal("800.0000000000000000"), result.getBigDecimal(1));
                assertEquals(new java.math.BigDecimal("790.0000000000000000"), result.getBigDecimal(2));
            }
        }
    }

    @Test
    void rebuildDeletesOnlyDerivedRowsForTheLockedAccountSymbolAndSide() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_position_cycle VALUES (1,10,'BTCUSDT','BOTH'),(2,10,'ETHUSDT','BOTH')");
            statement.execute("INSERT INTO quant_trade_cycle_allocation VALUES (11,1),(22,2)");
            Map<String, Object> parameters = Map.of("credentialId", 10L, "symbol", "BTCUSDT", "positionSide", "BOTH");
            String namespace = AccountPositionCycleProjectionMapper.class.getName();

            assertEquals(1, execute(connection, namespace + ".deleteCycleAllocationsByScope", parameters));
            assertEquals(1, execute(connection, namespace + ".deleteCyclesByScope", parameters));

            try (ResultSet cycles = statement.executeQuery("SELECT COUNT(*) FROM quant_position_cycle")) {
                assertTrue(cycles.next());
                assertEquals(1, cycles.getInt(1));
            }
            try (ResultSet allocations = statement.executeQuery("SELECT COUNT(*) FROM quant_trade_cycle_allocation")) {
                assertTrue(allocations.next());
                assertEquals(1, allocations.getInt(1));
            }
        }
    }
}

package org.dromara.quant.account.mapper;

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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class TradeHistoryMapperTest {
    private Configuration configuration() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/quant/TradeHistoryMapper.xml";
        try (InputStream input = org.apache.ibatis.io.Resources.getResourceAsStream(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private Connection database() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE quant_trade (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, strategy_id BIGINT, strategy_instance_id BIGINT, " +
                "symbol VARCHAR(32) NOT NULL, exchange_trade_id BIGINT NOT NULL, exchange_order_id BIGINT NOT NULL, side VARCHAR(10) NOT NULL, " +
                "quantity DECIMAL(20,10) NOT NULL, price DECIMAL(20,10) NOT NULL, commission DECIMAL(20,10), funding_fee DECIMAL(20,10), " +
                "realized_pnl DECIMAL(20,10), trade_time TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE quant_order (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "exchange_order_id BIGINT, client_order_id VARCHAR(128) NOT NULL, side VARCHAR(10) NOT NULL, position_side VARCHAR(20), " +
                "reduce_only CHAR(1) NOT NULL DEFAULT '0', leverage_snapshot INT, price_tick_size_snapshot DECIMAL(32,16), " +
                "post_fill_wallet_balance DECIMAL(32,16), post_fill_balance_at TIMESTAMP)");
            statement.execute("CREATE TABLE quant_symbol (symbol VARCHAR(32) PRIMARY KEY, price_tick_size DECIMAL(20,10) NOT NULL)");
            statement.execute("CREATE TABLE quant_execution_record (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "exchange_order_id BIGINT, operation_type VARCHAR(20) NOT NULL)");
            statement.execute("CREATE TABLE quant_strategy_order_intent (credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "exchange_order_id BIGINT NOT NULL, position_side VARCHAR(5) NOT NULL, fill_role VARCHAR(5) NOT NULL, " +
                "exchange_side VARCHAR(4) NOT NULL, client_order_id VARCHAR(80) NOT NULL)");
            statement.execute("CREATE TABLE quant_order_dispatch_ticket (id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "idempotency_key VARCHAR(160) NOT NULL, operation_type VARCHAR(16) NOT NULL, client_order_id VARCHAR(80) NOT NULL, " +
                "dispatch_status VARCHAR(32) NOT NULL, exchange_order_id BIGINT)");
            statement.execute("CREATE TABLE quant_strategy_pre_dispatch_intent (credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL, " +
                "idempotency_key VARCHAR(160) NOT NULL, client_order_id VARCHAR(80) NOT NULL, operation_type VARCHAR(5) NOT NULL, " +
                "position_side VARCHAR(5) NOT NULL, exchange_side VARCHAR(4) NOT NULL)");
        }
        return connection;
    }

    private List<Map<String, Object>> query(Connection connection, List<Long> ids, String symbol) throws Exception {
        MappedStatement mappedStatement = configuration().getMappedStatement(TradeHistoryMapper.class.getName() + ".selectFills");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ids", ids);
        parameters.put("symbol", symbol);
        BoundSql boundSql = mappedStatement.getBoundSql(parameters);
        try (PreparedStatement statement = connection.prepareStatement(boundSql.getSql())) {
            new org.apache.ibatis.scripting.defaults.DefaultParameterHandler(mappedStatement, parameters, boundSql).setParameters(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", resultSet.getLong("id"));
                    row.put("position_side", resultSet.getString("position_side"));
                    row.put("operation_type", resultSet.getString("operation_type"));
                    row.put("position_cycle_key", resultSet.getString("position_cycle_key"));
                    row.put("position_cycle_conflict", resultSet.getBoolean("position_cycle_conflict"));
                    row.put("price_tick_size", resultSet.getBigDecimal("current_price_tick_size"));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private void insertTrade(Connection connection, long id, long credentialId, String side, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO quant_trade (id,credential_id,symbol,exchange_trade_id,exchange_order_id,side,quantity,price,commission,realized_pnl,trade_time) " +
                "VALUES (?,?, 'BTCUSDT', ?, ?, ?, 1, 100, 0, 0, ?)")) {
            statement.setLong(1, id);
            statement.setLong(2, credentialId);
            statement.setLong(3, id + 1000);
            statement.setLong(4, orderId);
            statement.setString(5, side);
            statement.setTimestamp(6, Timestamp.valueOf("2026-10-10 00:00:00"));
            statement.executeUpdate();
        }
    }

    @Test
    void restoresOneWayDirectionAndOperationFromImmutablePreDispatchIntent() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            insertTrade(connection, 1, 10, "SELL", 101);
            statement.execute("INSERT INTO quant_order VALUES (1,10,'BTCUSDT',101,'QCLIENT101','SELL','BOTH','0',5,0.01,1000,'2026-10-10 00:00:01')");
            statement.execute("INSERT INTO quant_order_dispatch_ticket VALUES (1,10,'BTCUSDT','IDEMP101','OPEN','QCLIENT101','ACKED',101)");
            statement.execute("INSERT INTO quant_strategy_pre_dispatch_intent VALUES (10,'BTCUSDT','IDEMP101','QCLIENT101','OPEN','SHORT','SELL')");
            statement.execute("INSERT INTO quant_symbol VALUES ('BTCUSDT',0.01)");

            List<Map<String, Object>> rows = query(connection, List.of(10L), null);
            assertEquals(1, rows.size());
            assertEquals("SHORT", rows.getFirst().get("position_side"));
            assertEquals("OPEN", rows.getFirst().get("operation_type"));
            assertNull(rows.getFirst().get("position_cycle_key"));
            assertFalse((Boolean) rows.getFirst().get("position_cycle_conflict"));
            assertEquals(new java.math.BigDecimal("0.0100000000"), rows.getFirst().get("price_tick_size"));
        }
    }

    @Test
    void restoresHedgeDirectionAndCloseRoleFromAcceptedStrategyIntent() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            insertTrade(connection, 1, 10, "SELL", 102);
            statement.execute("INSERT INTO quant_order VALUES (1,10,'BTCUSDT',102,'QCLIENT102','SELL','BOTH','1',10,0.01,900,'2026-10-10 00:00:01')");
            statement.execute("INSERT INTO quant_strategy_order_intent VALUES (10,'BTCUSDT',102,'LONG','CLOSE','SELL','QCLIENT102')");

            List<Map<String, Object>> rows = query(connection, List.of(10L), "BTCUSDT");
            assertEquals(1, rows.size());
            assertEquals("LONG", rows.getFirst().get("position_side"));
            assertEquals("CLOSE", rows.getFirst().get("operation_type"));
            assertFalse((Boolean) rows.getFirst().get("position_cycle_conflict"));
        }
    }

    @Test
    void marksConflictingExecutionAndAcceptedIntentRolesAsIncompleteSource() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            insertTrade(connection, 1, 10, "BUY", 103);
            statement.execute("INSERT INTO quant_order VALUES (1,10,'BTCUSDT',103,'QCLIENT103','BUY','BOTH','0',5,0.01,1000,'2026-10-10 00:00:01')");
            statement.execute("INSERT INTO quant_execution_record VALUES (1,10,'BTCUSDT',103,'OPEN')");
            statement.execute("INSERT INTO quant_strategy_order_intent VALUES (10,'BTCUSDT',103,'LONG','CLOSE','BUY','QCLIENT103')");

            List<Map<String, Object>> rows = query(connection, List.of(10L), null);
            assertEquals(1, rows.size());
            assertEquals("CLOSE", rows.getFirst().get("operation_type"));
            assertTrue((Boolean) rows.getFirst().get("position_cycle_conflict"));
        }
    }

    @Test
    void leavesUnclassifiedOneWayTradesUnresolvedInsteadOfGuessing() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            insertTrade(connection, 1, 10, "BUY", 104);
            statement.execute("INSERT INTO quant_order VALUES (1,10,'BTCUSDT',104,'QCLIENT104','BUY','BOTH','0',5,0.01,1000,'2026-10-10 00:00:01')");
            List<Map<String, Object>> rows = query(connection, List.of(10L), null);
            assertEquals(1, rows.size());
            assertEquals("BOTH", rows.getFirst().get("position_side"));
            assertNull(rows.getFirst().get("operation_type"));
            assertNull(rows.getFirst().get("position_cycle_key"));
            assertFalse((Boolean) rows.getFirst().get("position_cycle_conflict"));
        }
    }

    @Test
    void doesNotSilentlyHideDuplicateOrderRows() throws Exception {
        try (Connection connection = database(); var statement = connection.createStatement()) {
            insertTrade(connection, 1, 10, "BUY", 105);
            statement.execute("INSERT INTO quant_order VALUES (1,10,'BTCUSDT',105,'QCLIENT-A','BUY','LONG','0',5,0.01,1000,'2026-10-10 00:00:01')");
            statement.execute("INSERT INTO quant_order VALUES (2,10,'BTCUSDT',105,'QCLIENT-B','BUY','LONG','0',5,0.01,1000,'2026-10-10 00:00:01')");

            List<Map<String, Object>> rows = query(connection, List.of(10L), null);
            assertEquals(2, rows.size());
            assertEquals(rows.get(0).get("id"), rows.get(1).get("id"));
        }
    }
}

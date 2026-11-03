package org.dromara.quant.account.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
public class TradeHistoryMapperTest {
    private static final AtomicInteger lookups = new AtomicInteger();

    public static boolean trackLookup(long id) {
        lookups.incrementAndGet();
        return true;
    }

    private String sql(List<Long> ids, String symbol) throws Exception {
        var parameters = new java.util.HashMap<String, Object>();
        parameters.put("ids", ids);
        parameters.put("symbol", symbol);
        var configuration = new Configuration();
        String resource = "mapper/quant/TradeHistoryMapper.xml";
        try (InputStream input = org.apache.ibatis.io.Resources.getResourceAsStream(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement(TradeHistoryMapper.class.getName() + ".selectFills").getBoundSql(parameters).getSql();
    }

    private Connection database() throws Exception {
        var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE quant_trade (id BIGINT PRIMARY KEY, credential_id BIGINT, symbol VARCHAR(32), exchange_order_id BIGINT, trade_time TIMESTAMP)");
            statement.execute("CREATE TABLE quant_order (id BIGINT PRIMARY KEY, credential_id BIGINT, symbol VARCHAR(32), exchange_order_id BIGINT, client_order_id VARCHAR(64), position_side VARCHAR(10), " +
                "reduce_only BOOLEAN, leverage_snapshot INT, price_tick_size_snapshot DECIMAL(32,16), post_fill_wallet_balance DECIMAL(32,16), post_fill_balance_at TIMESTAMP)");
            statement.execute("CREATE TABLE quant_symbol (symbol VARCHAR(32), price_tick_size DECIMAL(32,16))");
            statement.execute("CREATE TABLE quant_execution_record (id BIGINT PRIMARY KEY, credential_id BIGINT, symbol VARCHAR(32), exchange_order_id BIGINT, operation_type " +
                "VARCHAR(20))");
            statement.execute("CREATE TABLE quant_strategy_order_intent (credential_id BIGINT, symbol VARCHAR(32), exchange_order_id BIGINT, position_side VARCHAR(10), fill_role VARCHAR(20))");
            statement.execute("CREATE TABLE quant_order_dispatch_ticket (credential_id BIGINT, symbol VARCHAR(32), exchange_order_id BIGINT, operation_type VARCHAR(20), dispatch_status VARCHAR(20))");
            statement.execute("CREATE TABLE quant_strategy_pre_dispatch_intent (credential_id BIGINT, symbol VARCHAR(32), client_order_id VARCHAR(64), operation_type VARCHAR(20))");
            statement.execute("CREATE INDEX idx_order_history ON quant_order(credential_id, symbol, exchange_order_id)");
            statement.execute("CREATE INDEX idx_execution_history ON quant_execution_record(credential_id, symbol, exchange_order_id, operation_type)");
        }
        return connection;
    }


    @Test
    void restoresOneWayOrderRoleFromImmutableDispatchIntent() throws Exception {
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_trade VALUES (1,10,'BTCUSDT',101,'2026-10-10 00:00:00')");
            statement.execute("INSERT INTO quant_order (id,credential_id,symbol,exchange_order_id,client_order_id,position_side,reduce_only) " +
                "VALUES (1,10,'BTCUSDT',101,'QCLIENT101','BOTH',FALSE)");
            statement.execute("INSERT INTO quant_order_dispatch_ticket VALUES (10,'BTCUSDT',101,'OPEN','ACKED')");
            statement.execute("INSERT INTO quant_strategy_pre_dispatch_intent VALUES (10,'BTCUSDT','QCLIENT101','OPEN')");
            try (var prepared = connection.prepareStatement(sql(List.of(10L), "BTCUSDT"))) {
                prepared.setLong(1, 10L);
                prepared.setString(2, "BTCUSDT");
                try (var result = prepared.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("OPEN", result.getString("operation_type"));
                    assertEquals("BOTH", result.getString("position_side"));
                    assertFalse(result.next());
                }
            }
        }
    }

    @Test
    void restoresHedgeRoleFromImmutableStrategyOrderIntent() throws Exception {
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_trade VALUES (1,10,'BTCUSDT',102,'2026-10-10 00:00:00')");
            statement.execute("INSERT INTO quant_order (id,credential_id,symbol,exchange_order_id,position_side,reduce_only) " +
                "VALUES (1,10,'BTCUSDT',102,NULL,FALSE)");
            statement.execute("INSERT INTO quant_strategy_order_intent VALUES (10,'BTCUSDT',102,'SHORT','CLOSE')");
            try (var prepared = connection.prepareStatement(sql(List.of(10L), "BTCUSDT"))) {
                prepared.setLong(1, 10L);
                prepared.setString(2, "BTCUSDT");
                try (var result = prepared.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("CLOSE", result.getString("operation_type"));
                    assertEquals("SHORT", result.getString("position_side"));
                }
            }
        }
    }

    @Test
    void boundsOrderLookupsBeforeJoiningAnOversizedLedger() throws Exception {
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute("CREATE ALIAS track_lookup FOR 'org.dromara.quant.account.mapper.TradeHistoryMapperTest.trackLookup'");
            statement.execute("INSERT INTO quant_trade SELECT X, 10, 'BTCUSDT', X, DATEADD('SECOND', -X, TIMESTAMP '2026-10-10 00:00:00') FROM SYSTEM_RANGE(1, 25010)");
            statement.execute("INSERT INTO quant_order (id,credential_id,symbol,exchange_order_id,position_side,reduce_only) SELECT X, 10, 'BTCUSDT', X, 'LONG', FALSE FROM SYSTEM_RANGE(1, 25010)");
            // Count real join predicates, rather than assert a particular SQL spelling or elapsed time.
            String query = sql(List.of(10L), null).replace("AND o.symbol", "AND track_lookup(t.id) AND o.symbol");
            lookups.set(0);
            try (var prepared = connection.prepareStatement(query)) {
                prepared.setLong(1, 10L);
                try (var result = prepared.executeQuery()) {
                    int rows = 0;
                    long previous = Long.MAX_VALUE;
                    while (result.next()) {
                        assertTrue(result.getLong("id") < previous);
                        previous = result.getLong("id");
                        rows++;
                    }
                    assertEquals(20001, rows);
                }
            }
            assertTrue(lookups.get() <= 20001, "Order lookups exceeded the bounded trade window: " + lookups.get());
        }
    }

    @Test
    void preservesScopeMissingSourcesAndDuplicateSourceDetection() throws Exception {
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_trade VALUES (1,10,'BTCUSDT',101,'2026-10-10 00:00:00'),(2,10,'BTCUSDT',102,'2026-10-10 00:00:01'),(3,11,'BTCUSDT',101," +
                "'2026-10-10 00:00:02'),(4,10,'ETHUSDT',101,'2026-10-10 00:00:03')");
            statement.execute("INSERT INTO quant_order (id,credential_id,symbol,exchange_order_id,position_side,reduce_only) VALUES (1,10,'BTCUSDT',101,'LONG',FALSE),(2,10,'BTCUSDT',101,'LONG',FALSE),(3,11,'BTCUSDT',102,'SHORT',TRUE)");
            statement.execute("INSERT INTO quant_execution_record VALUES (1,10,'BTCUSDT',101,'OPEN'),(2,10,'BTCUSDT',102,'CANCEL'),(3,11,'BTCUSDT',102,'CLOSE')");
            try (var prepared = connection.prepareStatement(sql(List.of(10L), "BTCUSDT"))) {
                prepared.setLong(1, 10L);
                prepared.setString(2, "BTCUSDT");
                try (var result = prepared.executeQuery()) {
                    var ids = new ArrayList<Long>();
                    while (result.next()) {
                        ids.add(result.getLong("id"));
                        if (result.getLong("id") == 1) {
                            assertEquals("OPEN", result.getString("operation_type"));
                        } else {
                            assertNull(result.getString("position_side"));
                            assertNull(result.getString("operation_type"));
                        }
                    }
                    assertEquals(List.of(1L, 1L, 2L), ids);
                }
            }
        }
    }

    @Test
    void matchesOriginalQueryForMultipleAccountsWithAndWithoutSymbol() throws Exception {
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO quant_trade SELECT X, CASE WHEN MOD(X,2)=0 THEN 10 ELSE 11 END, CASE WHEN MOD(X,3)=0 THEN 'ETHUSDT' ELSE 'BTCUSDT' END, X, DATEADD" +
                "('SECOND', -FLOOR(X/2), TIMESTAMP '2026-10-10 00:00:00') FROM SYSTEM_RANGE(1,600)");
            statement.execute("INSERT INTO quant_order (id,credential_id,symbol,exchange_order_id,position_side,reduce_only) SELECT id,credential_id,symbol,exchange_order_id,'BOTH',FALSE FROM quant_trade WHERE MOD(id,5)<>0");
            statement.execute("INSERT INTO quant_order (id,credential_id,symbol,exchange_order_id,position_side,reduce_only) SELECT id+1000,credential_id,symbol,exchange_order_id,'BOTH',FALSE FROM quant_trade WHERE MOD(id,10)=0");
            statement.execute("INSERT INTO quant_execution_record SELECT id,credential_id,symbol,exchange_order_id,CASE WHEN MOD(id,3)=0 THEN 'CANCEL' WHEN MOD(id,2)=0 THEN " +
                "'OPEN' ELSE 'CLOSE' END FROM quant_trade");
            for (String symbol : new String[]{null, "BTCUSDT"}) {
                String original = "SELECT t.*, o.position_side, o.reduce_only, e.operation_type, o.leverage_snapshot, o.price_tick_size_snapshot, o.post_fill_wallet_balance, o.post_fill_balance_at, qs.price_tick_size AS current_price_tick_size FROM quant_trade t " + "LEFT JOIN quant_order o ON o.credential_id=t" +
                    ".credential_id AND o.symbol=t.symbol AND o.exchange_order_id=t.exchange_order_id " + "LEFT JOIN quant_execution_record e ON e.credential_id=t.credential_id " +
                    "AND e.symbol=t.symbol AND e.exchange_order_id=t.exchange_order_id AND e.operation_type IN ('OPEN','CLOSE') LEFT JOIN quant_symbol qs ON qs.symbol=t.symbol " + "WHERE t.credential_id IN (?,?) " + (symbol == null ? "" : "AND t.symbol=? ") + "ORDER BY t.trade_time,t.id LIMIT 20001";
                assertEquals(readRows(connection, original, symbol), readRows(connection, sql(List.of(10L, 11L), symbol), symbol));
            }
        }
    }

    private List<List<Object>> readRows(Connection connection, String query, String symbol) throws Exception {
        try (var prepared = connection.prepareStatement(query)) {
            prepared.setLong(1, 10L);
            prepared.setLong(2, 11L);
            if (symbol != null) {
                prepared.setString(3, symbol);
            }
            try (var result = prepared.executeQuery()) {
                var rows = new ArrayList<List<Object>>();
                while (result.next()) {
                    var row = new ArrayList<Object>();
                    for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                        row.add(result.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}

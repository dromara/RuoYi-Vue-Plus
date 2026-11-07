package org.dromara.quant.execution.mapper;

import org.apache.ibatis.session.Configuration;
import org.dromara.quant.execution.StrategyFillJournalRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("dev")
class StrategyFillJournalMapperTest {
    @Test
    void storesNullableRawFactsAndExposesRetryAndUnresolvedQueries() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:fill_journal_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("""
            CREATE TABLE quant_strategy_fill_journal (
              id BIGINT PRIMARY KEY, credential_id BIGINT NOT NULL, symbol VARCHAR(32) NOT NULL,
              exchange_trade_id BIGINT NOT NULL, exchange_order_id BIGINT NULL, side VARCHAR(10) NULL, position_side VARCHAR(20) NULL,
              quantity DECIMAL(32,16) NULL, price DECIMAL(32,16) NULL, commission DECIMAL(32,16) NULL,
              commission_asset VARCHAR(20) NULL, realized_pnl DECIMAL(32,16) NULL, trade_time DATETIME(3) NULL,
              attribution_status VARCHAR(24) NOT NULL, status_reason VARCHAR(500) NULL,
              attempt_count INT NOT NULL DEFAULT 0, last_attempt_at DATETIME(3) NULL,
              created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
              UNIQUE(credential_id,symbol,exchange_trade_id))
            """);
        var configuration = new Configuration();
        configuration.addMapper(StrategyFillJournalMapper.class);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        var mapper = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(StrategyFillJournalMapper.class);

        var raw = new StrategyFillJournalRecord();
        raw.setId(1L);
        raw.setCredentialId(2L);
        raw.setSymbol("BTCUSDT");
        raw.setExchangeTradeId(3L);
        raw.setExchangeOrderId(4L);
        raw.setSide("BUY");
        raw.setPositionSide("SHORT");
        raw.setQuantity(BigDecimal.ONE);
        raw.setPrice(BigDecimal.TEN);
        raw.setCommission(null);
        raw.setCommissionAsset(null);
        raw.setRealizedPnl(null);
        raw.setTradeTime(LocalDateTime.of(2026, 10, 17, 0, 0));

        assertEquals(1, mapper.insertRaw(raw));
        var stored = mapper.selectByKey(2L, "BTCUSDT", 3L);
        assertNull(stored.getCommission());
        assertNull(stored.getRealizedPnl());
        assertEquals("SHORT", stored.getPositionSide());
        assertEquals("CAPTURED", stored.getAttributionStatus());
        assertEquals(1, mapper.selectPending(2L, 100).size());
        assertEquals(1, mapper.updateStatus(1L, "WAITING_ORDER", "missing local order"));
        assertEquals(1, mapper.countUnresolved(2L));
        assertEquals("WAITING_ORDER", mapper.selectUnresolved(2L, "BTCUSDT", 10).getFirst().getAttributionStatus());
        assertEquals(1, jdbc.queryForObject("SELECT attempt_count FROM quant_strategy_fill_journal WHERE id=1", Integer.class));
        assertEquals(1, mapper.updateStatus(1L, "ATTRIBUTED", null));
        assertEquals(0, mapper.countUnresolved(2L));
    }
}

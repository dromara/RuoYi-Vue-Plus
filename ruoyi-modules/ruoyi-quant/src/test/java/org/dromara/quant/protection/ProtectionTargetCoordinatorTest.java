package org.dromara.quant.protection;

import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ProtectionTargetCoordinatorTest {
    private Fixture fixture() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE quant_protection_target (target_key VARCHAR(64) PRIMARY KEY, credential_id BIGINT NOT NULL, position_id BIGINT NOT NULL, position_cycle_key " +
            "VARCHAR(36) NOT NULL, position_side VARCHAR(8) NOT NULL, protection_type VARCHAR(24) NOT NULL, target_version BIGINT DEFAULT 0 NOT NULL, step_index INT DEFAULT 0 " +
            "NOT NULL, confirmed_step INT DEFAULT 0 NOT NULL, quantity DECIMAL(32,16), trigger_price DECIMAL(32,16), order_type VARCHAR(32), current_order_id BIGINT, " +
            "pending_order_id BIGINT, previous_order_id BIGINT, owner_token VARCHAR(36), lease_until TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE test_order (id BIGINT PRIMARY KEY, client_id VARCHAR(64))");
        var position = new QuantPosition();
        position.setId(10L);
        position.setPositionSide("LONG");
        position.setProtectionCycleKey(UUID.randomUUID().toString());
        try {
            var configuration = new org.apache.ibatis.session.Configuration();
            configuration.addMapper(org.dromara.quant.protection.mapper.ProtectionTargetMapper.class);
            var factoryBean = new org.mybatis.spring.SqlSessionFactoryBean();
            factoryBean.setDataSource(source);
            factoryBean.setConfiguration(configuration);
            var session = new org.mybatis.spring.SqlSessionTemplate(java.util.Objects.requireNonNull(factoryBean.getObject()));
            var mapper = session.getMapper(org.dromara.quant.protection.mapper.ProtectionTargetMapper.class);
            return new Fixture(jdbc, new ProtectionTargetCoordinator(mapper, new DataSourceTransactionManager(source)), new ProtectionTargetCoordinator(mapper,
                new DataSourceTransactionManager(source)), position);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot initialize MyBatis transactional protection fixture", ex);
        }
    }

    private QuantProtectionOrder order(long id) {
        var o = new QuantProtectionOrder();
        o.setId(id);
        o.setClientOrderId("client" + id);
        o.setQuantity(BigDecimal.ONE);
        o.setStopPrice(new BigDecimal("101"));
        o.setOrderType("STOP_MARKET");
        return o;
    }

    @Test
    void sameCycleIsExclusiveAndPendingIntentSurvivesRestart() {
        var f = fixture();
        try (var lease = f.first.acquire(1L, f.position, "STEP_TP")) {
            assertThrows(IllegalStateException.class, () -> f.second.acquire(1L, f.position, "STEP_TP"));
            var o = order(1);
            lease.prepare(o, 2, null, row -> f.jdbc.update("INSERT INTO test_order VALUES (?,?)", row.getId(), row.getClientOrderId()));
            assertEquals(1L, o.getTargetVersion());
            assertEquals(2, o.getStepIndex());
        }
        try (var lease = f.second.acquire(1L, f.position, "STEP_TP")) {
            assertEquals(1L, lease.state().pendingOrderId());
            assertThrows(ProtectionTargetCoordinator.PendingTargetException.class, () -> lease.prepare(order(2), 3, null, row -> fail("pending must be restored")));
            lease.confirmed(order(1), 2);
            lease.cleaned();
            assertEquals(2, lease.state().confirmedStep());
        }
        assertEquals(1, f.jdbc.queryForObject("SELECT COUNT(*) FROM test_order", Integer.class));
    }

    @Test
    void definitelyNotSentRetryReusesTheSameTargetVersionAndIntent() {
        var f = fixture();
        try (var lease = f.first.acquire(1L, f.position, "STEP_TP")) {
            var first = order(1);
            lease.prepare(first, 2, null, row -> f.jdbc.update("INSERT INTO test_order VALUES (?,?)", row.getId(), row.getClientOrderId()));
            assertEquals(1L, first.getTargetVersion());
            lease.terminal(first);

            var retry = order(1);
            retry.setTargetVersion(1L);
            lease.prepare(retry, 2, null, row -> f.jdbc.update("UPDATE test_order SET client_id=? WHERE id=?", row.getClientOrderId(), row.getId()));
            assertEquals(1L, retry.getTargetVersion());
            assertEquals(1L, lease.state().version());
            assertEquals(1L, lease.state().pendingOrderId());
        }
        assertEquals(1, f.jdbc.queryForObject("SELECT COUNT(*) FROM test_order", Integer.class));
    }

    @Test
    void stalePreviousTargetIsReportedAsRecoverablePendingState() {
        var f = fixture();
        try (var lease = f.first.acquire(1L, f.position, "HARD_STOP")) {
            f.jdbc.update("UPDATE quant_protection_target SET previous_order_id=77");
            var failure = assertThrows(ProtectionTargetCoordinator.PendingTargetException.class,
                () -> lease.prepare(order(2), 0, null, row -> fail("must not create a second protection intent")));
            assertTrue(failure.getMessage().contains("previousOrderId=77"));
            assertNull(lease.state().pendingOrderId());
            assertEquals(77L, lease.state().previousOrderId());
        }
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM test_order", Integer.class));
    }

    @Test
    void failedIntentInsertRollsBackVersionAndConfirmationCannotRegress() {
        var f = fixture();
        try (var lease = f.first.acquire(1L, f.position, "STEP_TP")) {
            assertThrows(IllegalStateException.class, () -> lease.prepare(order(1), 1, null, row -> {
                f.jdbc.update("INSERT INTO test_order VALUES (?,?)", row.getId(), row.getClientOrderId());
                throw new IllegalStateException("db failure");
            }));
            assertEquals(0, lease.state().version());
            assertNull(lease.state().pendingOrderId());
            assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM test_order", Integer.class));
            lease.confirmed(order(2), 3);
            lease.confirmed(order(2), 1);
            assertEquals(3, lease.state().confirmedStep());
            f.jdbc.update("UPDATE quant_protection_target SET owner_token='other'");
            assertThrows(IllegalStateException.class, lease::check);
            assertThrows(IllegalStateException.class, () -> lease.prepare(order(3), 4, null, row -> fail("lost lease must not submit")));
        }
    }

    private record Fixture(JdbcTemplate jdbc, ProtectionTargetCoordinator first, ProtectionTargetCoordinator second, QuantPosition position) {
    }
}

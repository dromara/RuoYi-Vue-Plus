package org.dromara.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.dromara.common.mybatis.config.properties.SqlLogProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlLogInterceptorTest {

    @Test
    void ignoredTableStillExecutesAndOtherTableStillLogs() throws Exception {
        SqlLogProperties properties = new SqlLogProperties();
        properties.setIgnoreTables(List.of("flow_event_wait"));
        SqlLogInterceptor interceptor = new SqlLogInterceptor(properties);
        String url = "jdbc:h2:mem:sql_log_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver", url, "sa", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE flow_event_wait (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE audit_record (id BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO flow_event_wait VALUES (1)");
            statement.execute("INSERT INTO audit_record VALUES (2)");
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("h2", new JdbcTransactionFactory(), dataSource));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(LogMapper.class);
        SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        PrintStream originalErr = System.err;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            try (SqlSession session = factory.openSession()) {
                assertEquals(1L, session.getMapper(LogMapper.class).eventWaitId());
                assertEquals(2L, session.getMapper(LogMapper.class).auditId());
            }
        } finally {
            System.setErr(originalErr);
        }
        String logs = output.toString(StandardCharsets.UTF_8);
        assertFalse(logs.contains("flow_event_wait"));
        assertTrue(logs.contains("audit_record"));
    }

    @Test
    void matchesTableReferencesWithoutMatchingLiteralsOrSimilarNames() {
        SqlLogProperties properties = new SqlLogProperties();
        properties.setIgnoreTables(List.of("flow_event_wait"));
        SqlLogInterceptor interceptor = new SqlLogInterceptor(properties);

        assertTrue(interceptor.isIgnoredTableSql("select * from flow_event_wait where id = ?"));
        assertTrue(interceptor.isIgnoredTableSql("select * from `app`.`flow_event_wait`"));
        assertTrue(interceptor.isIgnoredTableSql("select * from audit_record join FLOW_EVENT_WAIT w on w.id = audit_record.id"));
        assertTrue(interceptor.isIgnoredTableSql("update flow_event_wait set status = ?"));
        assertFalse(interceptor.isIgnoredTableSql("select 'from flow_event_wait' from audit_record"));
        assertFalse(interceptor.isIgnoredTableSql("select * from flow_event_wait_history"));
    }

    interface LogMapper {
        @Select("SELECT id FROM flow_event_wait")
        Long eventWaitId();

        @Select("SELECT id FROM audit_record")
        Long auditId();
    }
}

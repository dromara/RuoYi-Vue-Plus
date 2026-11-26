package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.workflow.command.*;
import org.dromara.warm.flow.core.workflow.context.OperatorContext;
import org.dromara.warm.flow.spring.boot.config.FlowAutoConfig;
import org.dromara.warm.plugin.modes.sb.utils.SpringUtil;
import org.dromara.workflow.service.impl.FlwEventWaitServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkflowEngineIntegrationTest {
    private final DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
        + ";MODE=MySQL", "sa", "");
    private final JdbcTemplate db = new JdbcTemplate(source);
    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    private Connection connection;

    @BeforeAll
    void initialize() throws Exception {
        connection = source.getConnection();
        new ResourceDatabasePopulator(new ClassPathResource("workflow-engine-h2.sql")).execute(source);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:warm/flow/*.xml"));
        var sessions = factory.getObject();
        context.registerBean(SqlSessionFactory.class, () -> sessions);
        context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(source));
        context.registerBean(org.dromara.warm.flow.core.spi.PermissionHandler.class,
            () -> new org.dromara.warm.flow.core.spi.PermissionHandler() {
                public List<String> permissions() { return List.of("7"); }
                public String getHandler() { return "7"; }
            });
        context.register(SpringUtil.class, FlowAutoConfig.class);
        context.refresh();
        db.update("insert into flow_definition(id,flow_code,flow_name,version,is_publish) values(4,'leave','请假','1',1)");
        db.update("""
            insert into flow_node(id,definition_id,node_type,node_code,node_name,permission_flag,node_ratio) values
            (10,4,0,'start','开始',null,null),(11,4,1,'apply','申请','7','0'),
            (12,4,1,'review','复核','7','0'),(13,4,1,'finance','财务','8','0'),(14,4,2,'end','结束',null,null)
            """);
        db.update("""
            insert into flow_skip(id,definition_id,now_node_code,now_node_type,next_node_code,next_node_type,skip_type) values
            (21,4,'start',0,'apply',1,'PASS'),(22,4,'apply',1,'review',1,'PASS'),
            (23,4,'review',1,'finance',1,'PASS'),(24,4,'finance',1,'end',2,'PASS')
            """);
    }

    @AfterAll
    void close() throws Exception {
        context.close();
        if (connection != null) connection.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void repeatedApproverAdvancesOnlyWhenDefinitionEnablesAutoPass(boolean enabled) {
        db.update("update flow_definition set def_config=? where id=4", "{\"autoPass\":" + enabled + "}");
        var start = new StartCommand();
        start.setBusinessId("A-" + enabled);
        start.setFlowCode("leave");
        start.setOperator(new OperatorContext("7"));
        var instance = FlowEngine.workflow().start(start).getInstanceId();
        var apply = db.queryForObject("select id from flow_task where instance_id=? and node_code='apply' and del_flag='0'", Long.class, instance);
        var complete = new CompleteCommand();
        complete.setTaskId(apply);
        complete.setOperator(new OperatorContext("7"));
        FlowEngine.workflow().complete(complete);
        assertEquals(List.of(enabled ? "finance" : "review"), db.queryForList(
            "select node_code from flow_task where instance_id=? and del_flag='0'", String.class, instance));
    }

    @Test
    void revivePersistsFreshScheduleAndRejectsSecondRevival() {
        db.update("insert into flow_instance(id,definition_id,business_id,node_type,node_code,flow_status) values(300,4,'timer',1,'wait','waiting')");
        db.update("""
            insert into flow_event_wait(id,definition_id,instance_id,node_code,event_type,trigger_type,trigger_interval,
                status,version,retry_count,last_error) values(400,4,300,'wait','TIMER_CATCH','AFTER','PT5M','DEAD',5,3,'failed')
            """);
        var monitor = new FlwEventWaitServiceImpl();
        var before = java.time.Instant.now().minusSeconds(1);
        monitor.revive(400L);
        var after = java.time.Instant.now().plusSeconds(1);
        assertEquals("WAITING", db.queryForObject("select status from flow_event_wait where id=400", String.class));
        var next = db.queryForObject("select next_trigger_at from flow_event_wait where id=400", java.sql.Timestamp.class).toInstant();
        assertFalse(next.isBefore(before.plusSeconds(300)));
        assertFalse(next.isAfter(after.plusSeconds(300)));
        assertEquals(6, db.queryForObject("select version from flow_event_wait where id=400", Integer.class));
        assertThrows(FlowException.class, () -> monitor.revive(400L));
        assertEquals(next, db.queryForObject("select next_trigger_at from flow_event_wait where id=400", java.sql.Timestamp.class).toInstant());
    }
}

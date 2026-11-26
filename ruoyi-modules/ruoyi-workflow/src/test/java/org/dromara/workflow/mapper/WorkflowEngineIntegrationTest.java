package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.domain.bo.FlowCancelBo;
import org.dromara.workflow.domain.context.CompleteTaskContext;
import org.dromara.workflow.service.impl.FlwInstanceServiceImpl;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.constant.ExceptionCons;
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

import com.baomidou.lock.*;
import com.baomidou.lock.aop.LockInterceptor;
import com.baomidou.lock.aop.LockAnnotationAdvisor;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import org.dromara.common.liteflow.utils.LiteFlowUtils;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.service.impl.FlwTaskServiceImpl;
import org.springframework.aop.framework.ProxyFactory;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
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
    void resubmittingCompletedTaskKeepsTheSameReviewTaskAssigneeAndHistory() {
        db.update("update flow_definition set def_config=? where id=4", "{\"autoPass\":false}");
        var start = new StartCommand();
        start.setBusinessId("duplicate-submit");
        start.setFlowCode("leave");
        start.setOperator(new OperatorContext("7"));
        var instance = FlowEngine.workflow().start(start).getInstanceId();
        String active = "select id from flow_task where instance_id=? and del_flag='0'";
        var apply = db.queryForObject(active, Long.class, instance);
        var complete = new CompleteCommand();
        complete.setTaskId(apply);
        complete.setOperator(new OperatorContext("7"));
        FlowEngine.workflow().complete(complete);
        var review = db.queryForObject(active + " and node_code='review'", Long.class, instance);
        assertEquals(ExceptionCons.NOT_FOUNT_TASK,
            assertThrows(FlowException.class, () -> FlowEngine.workflow().complete(complete)).getMessage());
        assertEquals(List.of(review), db.queryForList(active, Long.class, instance));
        assertEquals(List.of("review"), db.queryForList("select node_code from flow_instance where id=?", String.class, instance));
        assertEquals(List.of("7"), db.queryForList(
            "select processed_by from flow_user where associated=? and del_flag='0'", String.class, review));
        assertEquals(List.of("review"), db.queryForList(
            "select target_node_code from flow_his_task where task_id=? and del_flag='0'", String.class, apply));
    }
    private Long startManualApproval(String businessId) {
        db.update("update flow_definition set def_config=? where id=4", "{\"autoPass\":false}");
        var start = new StartCommand();
        start.setBusinessId(businessId);
        start.setFlowCode("leave");
        start.setOperator(new OperatorContext("7"));
        return FlowEngine.workflow().start(start).getInstanceId();
    }

    private CompleteCommand approval(long task) {
        var command = new CompleteCommand();
        command.setTaskId(task);
        command.setOperator(new OperatorContext("7"));
        return command;
    }

    @Test
    void revokedReviewCannotAdvanceFromAStaleTask() {
        long instance = startManualApproval("stale-review");
        String active = "select id from flow_task where instance_id=? and del_flag='0'";
        FlowEngine.workflow().complete(approval(db.queryForObject(active, Long.class, instance)));
        var stale = approval(db.queryForObject(active + " and node_code='review'", Long.class, instance));
        var mapper = new SqlSessionTemplate(context.getBean(SqlSessionFactory.class)).getMapper(FlowInstanceMapper.class);
        var service = new FlwInstanceServiceImpl(null, FlowEngine.defService(), null, null, mapper, null, null, null);
        var request = new FlowCancelBo("stale-review", "重新填写");
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserIdStr).thenReturn("8");
            assertThrows(ServiceException.class, () -> service.cancelProcessApply(request));
            assertEquals(List.of(stale.getTaskId()), db.queryForList(active, Long.class, instance));
            login.when(LoginHelper::getUserIdStr).thenReturn("7");
            assertTrue(service.cancelProcessApply(request));
        }
        var afterRevoke = db.queryForList(active, Long.class, instance);
        String history = "select id from flow_his_task where instance_id=? order by id";
        var revokedHistory = db.queryForList(history, Long.class, instance);
        assertThrows(FlowException.class, () -> FlowEngine.workflow().complete(stale));
        assertEquals("cancel", db.queryForObject("select flow_status from flow_instance where id=?", String.class, instance));
        assertEquals(afterRevoke, db.queryForList(active, Long.class, instance));
        assertEquals(revokedHistory, db.queryForList(history, Long.class, instance));
        assertTrue(db.queryForList(active + " and node_code in ('review','finance')", Long.class, instance).isEmpty());
    }

    @Test
    void overlappingApprovalsCommitOnlyOneTransition() throws Exception {
        long instance = startManualApproval("concurrent-review");
        long task = db.queryForObject("select id from flow_task where instance_id=? and del_flag='0'", Long.class, instance);
        var contention = new CountDownLatch(1);
        var service = approvalService(contention);
        Callable<Boolean> submit = () -> {
            try {
                return submitApproval(service, task, () -> assertTrue(contention.await(8, TimeUnit.SECONDS),
                    "second request must contend for the same task"));
            } catch (FlowException stale) {
                assertEquals(ExceptionCons.NOT_FOUNT_TASK, stale.getMessage());
                return false;
            }
        };
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(submit);
            var second = workers.submit(submit);
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
        assertSingleReview(instance, task);
    }

    @Test
    void distinctTasksAdvanceIndependently() throws Exception {
        long firstInstance = startManualApproval("independent-first");
        long secondInstance = startManualApproval("independent-second");
        String active = "select id from flow_task where instance_id=? and del_flag='0'";
        long firstTask = db.queryForObject(active, Long.class, firstInstance);
        long secondTask = db.queryForObject(active, Long.class, secondInstance);
        var contention = new CountDownLatch(1);
        var service = approvalService(contention);
        var firstEntered = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> submitApproval(service, firstTask, () -> {
                firstEntered.countDown();
                assertTrue(releaseFirst.await(25, TimeUnit.SECONDS));
            }));
            try {
                assertTrue(firstEntered.await(10, TimeUnit.SECONDS));
                var second = workers.submit(() -> submitApproval(service, secondTask, () -> {}));
                assertTrue(second.get(15, TimeUnit.SECONDS));
                assertSingleReview(secondInstance, secondTask);
                assertEquals(List.of(firstTask), db.queryForList(active, Long.class, firstInstance));
                assertEquals(1L, contention.getCount());
            } finally {
                releaseFirst.countDown();
            }
            assertTrue(first.get(10, TimeUnit.SECONDS));
        }
        assertSingleReview(firstInstance, firstTask);
    }

    private boolean submitApproval(IFlwTaskService service, long task, org.junit.jupiter.api.function.Executable beforeComplete) {
        var request = new CompleteTaskBo();
        request.setTaskId(task);
        try (var chain = mockStatic(LiteFlowUtils.class)) {
            chain.when(() -> LiteFlowUtils.execute(eq("completeTaskChain"), any())).thenAnswer(call -> {
                beforeComplete.execute();
                CompleteTaskContext submitted = call.getArgument(1);
                FlowEngine.workflow().complete(approval(submitted.getCompleteTaskBo().getTaskId()));
                return null;
            });
            return service.completeTask(request);
        }
    }

    private IFlwTaskService approvalService(CountDownLatch contention) throws Exception {
        var held = ConcurrentHashMap.<String>newKeySet();
        var executor = new LockExecutor<String>() {
            public String acquire(String key, String value, long expire, long timeout) {
                if (held.add(key)) return key;
                contention.countDown();
                return null;
            }
            public boolean releaseLock(String key, String value, String token) {
                return held.remove(token);
            }
        };
        var props = new Lock4jProperties();
        props.setAcquireTimeout(10000L);
        var template = new LockTemplate();
        template.setProperties(props);
        template.setExecutors(List.of(executor));
        template.afterPropertiesSet();
        var interceptor = new LockInterceptor(template, List.of(new DefaultLockKeyBuilder(context)),
            List.of(new DefaultLockFailureStrategy()), props);
        interceptor.afterPropertiesSet();
        var advisor = new LockAnnotationAdvisor(interceptor, 0);
        advisor.setBeanFactory(context);
        var target = new FlwTaskServiceImpl(null,null,null,null,null,null,null,null,null,null,null,null,null,null);
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvisor(advisor);
        return (IFlwTaskService) proxy.getProxy();
    }

    private void assertSingleReview(long instance, long task) {
        var review = db.queryForObject("select id from flow_task where instance_id=? and del_flag='0'", Long.class, instance);
        assertEquals("review", db.queryForObject("select node_code from flow_task where id=?", String.class, review));
        assertEquals(List.of("7"), db.queryForList("select processed_by from flow_user where associated=? and del_flag='0'", String.class, review));
        assertEquals(List.of("review"), db.queryForList("select target_node_code from flow_his_task where task_id=? and del_flag='0'", String.class, task));
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

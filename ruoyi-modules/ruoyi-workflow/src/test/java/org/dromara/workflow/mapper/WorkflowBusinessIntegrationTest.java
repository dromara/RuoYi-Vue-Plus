package org.dromara.workflow.mapper;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.workflow.api.WorkflowService;
import org.dromara.workflow.api.domain.StartProcessDTO;
import org.dromara.workflow.domain.FlowSpel;
import org.dromara.workflow.domain.TestLeave;
import org.dromara.workflow.domain.TestLeaveToTestLeaveVoMapperImpl;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.TestLeaveVo;
import org.dromara.workflow.service.ITestLeaveService;
import org.dromara.workflow.service.impl.*;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.dromara.workflow.event.WorkflowResultMessageEvent;
import org.dromara.workflow.event.WorkflowTaskMessageEvent;
import org.dromara.workflow.listener.WorkflowSideEffectListener;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.liteflow.utils.LiteFlowUtils;
import org.dromara.workflow.domain.context.StartProcessContext;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.liteflow.start.StartExecuteComponent;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.workflow.result.WorkflowResult;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowBusinessIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {}

    private final DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
    private final JdbcTemplate jdbc = new JdbcTemplate(source);
    private SqlSessionTemplate sessions;
    private Connection connection;

    @BeforeEach
    void connect() throws Exception {
        connection = source.getConnection();
        var config = new MybatisConfiguration();
        config.setEnvironment(new Environment("workflow-business", new SpringManagedTransactionFactory(), source));
        config.addMapper(FlwSpelMapper.class);
        config.addMapper(FlwCategoryMapper.class);
        config.addMapper(TestLeaveMapper.class);
        sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
    }

    @AfterEach
    void close() throws Exception {
        connection.close();
    }

    @Test
    void categoryNamesAreScopedToParentAndExcludeCurrentCategory() {
        jdbc.execute("""
            create table flow_category(category_id bigint primary key, parent_id bigint, category_name varchar(50),
            del_flag char(1) default '0')
            """);
        jdbc.update("insert into flow_category(category_id,parent_id,category_name) values (1,0,'财务'),(2,1,'报销')");
        var service = new FlwCategoryServiceImpl(null, sessions.getMapper(FlwCategoryMapper.class));
        var request = new FlowCategoryBo();
        request.setParentId(1L);
        request.setCategoryName("报销");
        assertFalse(service.checkCategoryNameUnique(request));
        request.setCategoryId(2L);
        assertTrue(service.checkCategoryNameUnique(request));
        request.setCategoryId(null);
        request.setParentId(0L);
        assertTrue(service.checkCategoryNameUnique(request));
        assertEquals(Map.of(1L, "财务", 2L, "报销"), service.selectCategoryNameByIds(Set.of(1L, 2L, 99L)));
    }

    @Test
    void duplicateExpressionRejectsInsertsAndRenamesButAllowsEditingItself() {
        jdbc.execute("""
            create table flow_spel(id bigint primary key, view_spel varchar(100), remark varchar(100),
            create_by bigint,create_time timestamp,create_dept bigint,update_by bigint,update_time timestamp,
            del_flag char(1) default '0')
            """);
        jdbc.update("insert into flow_spel(id,view_spel) values (1,'first'),(2,'second')");
        var service = new FlwSpelServiceImpl(sessions.getMapper(FlwSpelMapper.class));
        var request = new FlowSpelBo();
        request.setViewSpel("first");
        try (var spring = mockStatic(SpringUtil.class); var converter = mockStatic(MapstructUtils.class)) {
            converter.when(() -> MapstructUtils.convert(request, FlowSpel.class))
                .thenAnswer(call -> new FlowSpelBoToFlowSpelMapperImpl().convert(request));
            assertThrows(ServiceException.class, () -> service.insertByBo(request));
            request.setId(2L);
            assertThrows(ServiceException.class, () -> service.updateByBo(request));
            request.setId(1L);
            request.setRemark("更新说明");
            assertTrue(service.updateByBo(request));
        }
        assertEquals("更新说明", jdbc.queryForObject("select remark from flow_spel where id=1", String.class));
        assertEquals("second", jdbc.queryForObject("select view_spel from flow_spel where id=2", String.class));
    }

    @Test
    void failedLeaveSaveRejectsSubmissionBeforeWorkflowStarts() {
        var mapper = mock(TestLeaveMapper.class);
        var workflow = mock(WorkflowService.class);
        var service = new TestLeaveServiceImpl(mapper, workflow, null);
        var request = new TestLeaveBo();
        request.setId(18L);
        request.setStartDate(LocalDateTime.of(2026, 1, 3, 0, 0));
        request.setEndDate(request.getStartDate());
        var entity = new TestLeaveBoToTestLeaveMapperImpl().convert(request);
        when(mapper.insertOrUpdate(entity)).thenReturn(false);
        try (var spring = mockStatic(SpringUtil.class); var converter = mockStatic(MapstructUtils.class)) {
            converter.when(() -> MapstructUtils.convert(request, TestLeave.class)).thenReturn(entity);
            assertThrows(ServiceException.class, () -> service.submitAndFlowStart(request));
            verify(mapper).insertOrUpdate(entity);
            verifyNoInteractions(workflow);
        }
    }

    @Test
    void failedWorkflowRollsBackLeaveEditThenRetryCommitsOriginalId() {
        jdbc.execute("""
            create table test_leave(id bigint primary key, apply_code varchar(50), leave_type varchar(30),
            start_date timestamp,end_date timestamp,leave_days int,remark varchar(100),status varchar(30),
            create_by bigint,create_time timestamp,create_dept bigint,update_by bigint,update_time timestamp)
            """);
        var workflow = mock(WorkflowService.class);
        jdbc.execute("create table flow_instance(id bigint primary key, business_id varchar(50))");
        jdbc.execute("create table flow_instance_biz_ext(instance_id bigint primary key, business_id varchar(50))");
        try (var spring = mockStatic(SpringUtil.class); var converter = mockStatic(MapstructUtils.class);
             var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "workflow-test", Map.of("warm-flow.enabled", "true")));
            context.register(Transactions.class);
            context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(source));
            context.registerBean(TestLeaveServiceImpl.class, () -> new TestLeaveServiceImpl(sessions.getMapper(TestLeaveMapper.class), workflow, null));
            context.refresh();
            var service = context.getBean(ITestLeaveService.class);
            var leave = new TestLeaveBo();
            leave.setStartDate(LocalDateTime.of(2026, 1, 3, 0, 0));
            leave.setEndDate(leave.getStartDate().plusDays(1));
            converter.when(() -> MapstructUtils.convert(leave, TestLeave.class))
                .thenAnswer(call -> new TestLeaveBoToTestLeaveMapperImpl().convert(leave));
            converter.when(() -> MapstructUtils.convert(any(TestLeave.class), eq(TestLeaveVo.class)))
                .thenAnswer(call -> new TestLeaveToTestLeaveVoMapperImpl().convert((TestLeave) call.getArgument(0)));
            var draft = service.insertByBo(leave);
            assertEquals("draft", draft.getStatus());
            assertEquals(2, draft.getLeaveDays());
            leave.setRemark("提交审批");
            var attempts = new java.util.concurrent.atomic.AtomicInteger();
            when(workflow.startCompleteTask(any())).thenAnswer(call -> {
                StartProcessDTO command = call.getArgument(0);
                jdbc.update("insert into flow_instance values (31, ?)", command.getBusinessId());
                jdbc.update("insert into flow_instance_biz_ext values (31, ?)", command.getBusinessId());
                return attempts.incrementAndGet() == 2;
            });
            assertThrows(ServiceException.class, () -> service.submitAndFlowStart(leave));
            assertEquals(0, jdbc.queryForObject("select count(*) from flow_instance", Integer.class));
            assertEquals(0, jdbc.queryForObject("select count(*) from flow_instance_biz_ext", Integer.class));
            assertNull(jdbc.queryForObject("select remark from test_leave where id=?", String.class, draft.getId()));
            assertEquals(draft.getId(), service.submitAndFlowStart(leave).getId());
            assertEquals(draft.getId().toString(), jdbc.queryForObject("select business_id from flow_instance", String.class));
            assertEquals(draft.getId().toString(), jdbc.queryForObject("select business_id from flow_instance_biz_ext", String.class));
            verify(workflow, times(2)).startCompleteTask(argThat((StartProcessDTO dto) -> draft.getId().toString().equals(dto.getBusinessId())));
            assertEquals("提交审批", jdbc.queryForObject("select remark from test_leave where id=?", String.class, draft.getId()));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void firstTaskValidationRollsBackInstanceAndExtension(int count) {
        jdbc.execute("create table flow_instance(id bigint primary key, business_id varchar(50))");
        jdbc.execute("create table flow_instance_biz_ext(instance_id bigint primary key, business_id varchar(50))");
        var tasks = mock(TaskService.class);
        var instances = mock(InsService.class);
        var engineWorkflow = mock(org.dromara.warm.flow.core.workflow.WorkflowServiceImpl.class);
        var extensions = mock(FlwInstanceBizExtMapper.class);
        var execute = spy(new StartExecuteComponent(tasks, extensions));
        var instance = new FlowInstance().setBusinessId("A").setId(31L);
        var started = mock(WorkflowResult.class);
        when(started.getInstanceId()).thenReturn(31L);
        when(engineWorkflow.start(any())).thenAnswer(call -> {
            jdbc.update("insert into flow_instance values (31, 'A')");
            return started;
        });
        when(instances.getById(31L)).thenReturn(instance);
        when(tasks.list(any())).thenReturn(java.util.Collections.nCopies(count, new org.dromara.warm.flow.orm.entity.FlowTask()));
        doAnswer(call -> {
            FlowInstanceBizExt row = call.getArgument(0);
            jdbc.update("insert into flow_instance_biz_ext values (?, ?)", row.getInstanceId(), row.getBusinessId());
            return null;
        }).when(extensions).saveOrUpdateByInstanceId(any());
        try (var engine = mockStatic(FlowEngine.class); var chain = mockStatic(LiteFlowUtils.class);
             var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "workflow-test", Map.of("warm-flow.enabled", "true")));
            context.register(Transactions.class);
            context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(source));
            context.registerBean(FlwTaskServiceImpl.class, () -> new FlwTaskServiceImpl(tasks, instances, null, null,
                null, null, null, null, null, null, null, null, null, null));
            context.refresh();
            engine.when(FlowEngine::workflow).thenReturn(engineWorkflow);
            engine.when(FlowEngine::insService).thenReturn(instances);
            chain.when(() -> LiteFlowUtils.execute(eq("startProcessChain"), any(StartProcessContext.class))).thenAnswer(call -> {
                StartProcessContext state = call.getArgument(1);
                state.setBusinessId(state.getStartProcessBo().getBusinessId());
                state.setBizExt(new FlowInstanceBizExt());
                doReturn(state).when(execute).getContextBean(StartProcessContext.class);
                execute.process();
                return null;
            });
            var request = new StartProcessBo();
            request.setBusinessId("A");
            request.setHandler("7");
            var service = context.getBean(IFlwTaskService.class);
            var error = assertThrows(ServiceException.class, () -> service.startWorkFlow(request));
            assertEquals(count == 0 ? "流程启动失败，未生成任务" : "请检查流程第一个环节是否为申请人！", error.getMessage());
            verify(extensions).saveOrUpdateByInstanceId(any());
            assertEquals(0, jdbc.queryForObject("select count(*) from flow_instance", Integer.class));
            assertEquals(0, jdbc.queryForObject("select count(*) from flow_instance_biz_ext", Integer.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"commit", "rollback", "outside"})
    void taskAndResultNotificationsFollowTransactionOutcome(String outcome) {
        var messages = mock(IFlwCommonService.class);
        var users = mock(UserService.class);
        var user = new UserDTO();
        user.setUserId(7L);
        when(users.selectById(7L)).thenReturn(user);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "workflow-test", Map.of("warm-flow.enabled", "true")));
            context.register(Transactions.class);
            context.registerBean(DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(source));
            context.registerBean(WorkflowSideEffectListener.class, () -> new WorkflowSideEffectListener(null, messages, users));
            context.refresh();
            Runnable publish = () -> {
                context.publishEvent(new WorkflowTaskMessageEvent("请假", 31L, List.of("1"), "待审批"));
                context.publishEvent(new WorkflowResultMessageEvent("请假", "finish", "7", List.of("1")));
            };
            if ("outside".equals(outcome)) {
                publish.run();
            } else {
                new TransactionTemplate(context.getBean(DataSourceTransactionManager.class)).executeWithoutResult(status -> {
                    publish.run();
                    if ("rollback".equals(outcome)) {
                        status.setRollbackOnly();
                    } else {
                        verifyNoInteractions(messages);
                    }
                });
            }
            if ("rollback".equals(outcome)) {
                verifyNoInteractions(messages);
            } else {
                verify(messages).sendMessage("请假", 31L, List.of("1"), "待审批");
                verify(messages).sendResultMessage("请假", BusinessStatusEnum.FINISH, List.of("1"), List.of(user));
                verifyNoMoreInteractions(messages);
            }
        }
    }
}

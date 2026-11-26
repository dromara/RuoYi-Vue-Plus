package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.core.*;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.JoinMapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.injector.MPJSqlInjector;
import com.github.yulichang.interceptor.MPJInterceptor;
import com.github.yulichang.toolkit.SqlHelper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.workflow.domain.bo.FlowTaskBo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskPageIntegrationTest {
    interface Users extends MPJBaseMapper<FlowUser> {}
    interface Instances extends MPJBaseMapper<FlowInstance> {}
    interface Definitions extends MPJBaseMapper<FlowDefinition> {}
    private SqlSession session;
    private Connection connection;
    private MockedStatic<SqlHelper> registry;
    private FlwTaskMapper tasks;
    private FlwHisTaskMapper history;
    private final FlowTaskBo filter = new FlowTaskBo();
    private final List<String> categories = List.of("4", "5");

    @BeforeEach
    void prepare() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        connection = source.getConnection();
        var db = new JdbcTemplate(source);
        db.execute("create table flow_definition(id bigint, category varchar, flow_name varchar, flow_code varchar, form_custom varchar, form_path varchar, version varchar, del_flag char default '0')");
        db.execute("create table flow_instance(id bigint, definition_id bigint, business_id varchar, flow_status varchar, create_by varchar, del_flag char default '0')");
        db.execute("create table flow_instance_biz_ext(instance_id bigint, business_code varchar, business_title varchar, del_flag char default '0')");
        db.execute("create table flow_task(id bigint, definition_id bigint, instance_id bigint, node_type int, node_code varchar, node_name varchar, form_path varchar, create_time timestamp, update_time timestamp, del_flag char default '0')");
        db.execute("create table flow_user(id bigint, associated bigint, processed_by varchar, type varchar, create_time timestamp, del_flag char default '0')");
        db.execute("""
            create table flow_his_task(id bigint, definition_id bigint, instance_id bigint, node_type int, node_code varchar,
              node_name varchar, flow_status varchar, approver varchar, collaborator varchar, cooperate_type varchar,
              target_node_code varchar, target_node_name varchar, form_custom varchar, form_path varchar, message varchar,
              ext varchar, create_time timestamp, update_time timestamp, del_flag char default '0')
            """);
        db.update("insert into flow_definition(id,category,flow_code) values(1,'4','leave'),(2,'5','leave'),(3,'6','leave')");
        db.update("insert into flow_instance(id,definition_id,business_id,flow_status) values(10,1,'A','waiting'),(20,2,'B','waiting'),(30,3,'C','waiting')");
        db.update("insert into flow_task(id,definition_id,instance_id,node_type,create_time) values(11,1,10,1,'2026-01-03'),(21,2,20,1,'2026-01-02'),(31,3,30,1,'2026-01-01')");
        db.update("insert into flow_user(id,associated,processed_by,type) values(1,11,'7','1'),(2,11,'8','1'),(3,21,'8','1'),(4,31,'7','1'),(5,51,'7','4'),(6,52,'8','4')");
        db.update("insert into flow_his_task(id,definition_id,instance_id,node_type,approver,flow_status,create_time) values(41,1,10,1,'7','pass','2026-01-03'),(42,2,20,1,'8','pass','2026-01-02'),(51,1,10,1,'7','copy','2026-01-01'),(52,2,20,1,'8','copy','2026-01-01')");
        var config = new MybatisConfiguration();
        config.setEnvironment(new Environment("workflow-pages", new JdbcTransactionFactory(), source));
        var paging = new MybatisPlusInterceptor();
        paging.addInnerInterceptor(new PaginationInnerInterceptor());
        config.addInterceptor(paging);
        config.addInterceptor(new MPJInterceptor());
        GlobalConfigUtils.setGlobalConfig(config, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()).setSqlInjector(new MPJSqlInjector()));
        config.addMapper(FlwTaskMapper.class);
        config.addMapper(FlwHisTaskMapper.class);
        config.addMapper(Users.class);
        config.addMapper(Definitions.class);
        config.addMapper(Instances.class);
        config.addMapper(FlwInstanceBizExtMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession();
        tasks = session.getMapper(FlwTaskMapper.class);
        history = session.getMapper(FlwHisTaskMapper.class);
        registry = mockStatic(SqlHelper.class);
        bind(FlowTask.class, tasks);
        bind(FlowHisTask.class, history);
        bind(FlowUser.class, session.getMapper(Users.class));
    }

    private <T> void bind(Class<T> entity, JoinMapper<T> mapper) {
        registry.when(() -> SqlHelper.exec(eq(entity), any())).thenAnswer(call -> {
            Function<JoinMapper<T>, ?> query = call.getArgument(1);
            return query.apply(mapper);
        });
    }

    @AfterEach
    void close() throws Exception {
        if (registry != null) registry.close();
        if (session != null) session.close();
        if (connection != null) connection.close();
    }

    @Test
    void sharedTaskOccupiesOneRowAcrossPages() {
        var first = tasks.getListRunTask(new Page<>(1, 1), filter, categories, null);
        var second = tasks.getListRunTask(new Page<>(2, 1), filter, categories, null);
        var pastEnd = tasks.getListRunTask(new Page<>(3, 1), filter, categories, null);
        assertEquals(2, first.getTotal());
        assertEquals(List.of(11L), first.getRecords().stream().map(FlowTaskVo::getId).toList());
        assertEquals(List.of(21L), second.getRecords().stream().map(FlowTaskVo::getId).toList());
        assertTrue(pastEnd.getRecords().isEmpty());
        filter.setFlowCode("unmatched");
        assertEquals(0, tasks.getListRunTask(new Page<>(1, 10), filter, categories, null).getTotal());
    }

    @Test
    void personalQueuesKeepOtherUsersAndOtherCategoriesOut() {
        for (String user : List.of("7", "8")) {
            var waiting = tasks.getListRunTask(new Page<>(1, 10), filter, categories, user);
            var finished = history.getListFinishTask(new Page<>(1, 10), filter, categories, user);
            var copies = tasks.getTaskCopyByPage(new Page<>(1, 10), filter, categories, user);
            var expected = "7".equals(user) ? List.of(11L) : List.of(11L, 21L);
            assertEquals(expected, waiting.getRecords().stream().map(FlowTaskVo::getId).toList());
            assertEquals(expected.size(), waiting.getTotal());
            assertEquals(List.of("7".equals(user) ? 41L : 42L), finished.getRecords().stream().map(FlowHisTaskVo::getId).toList());
            assertEquals(List.of("7".equals(user) ? 51L : 52L), copies.getRecords().stream().map(FlowTaskVo::getId).toList());
            assertEquals(1, finished.getTotal());
            assertEquals(1, copies.getTotal());
        }
        var all = history.getListFinishTask(new Page<>(2, 1), filter, categories, null);
        assertEquals(2, all.getTotal());
        assertEquals(List.of(42L), all.getRecords().stream().map(FlowHisTaskVo::getId).toList());
    }
}

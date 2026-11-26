package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowBizExtIntegrationTest {
    @Test
    void resumedSubmissionReusesPersistedRowAndFailedTransactionKeepsOldTitle() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        try (var connection = source.getConnection()) {
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("""
                create table flow_instance_biz_ext (
                    id bigint primary key, instance_id bigint unique, business_id varchar(64), business_code varchar(64),
                    business_title varchar(100), del_flag char(1) default '0', create_by bigint, create_time timestamp,
                    update_by bigint, update_time timestamp, create_dept bigint
                )
                """);
            var configuration = new MybatisConfiguration();
            configuration.setEnvironment(new Environment("workflow", new SpringManagedTransactionFactory(), source));
            configuration.addMapper(FlwInstanceBizExtMapper.class);
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
            var mapper = sessions.getMapper(FlwInstanceBizExtMapper.class);
            var first = new FlowInstanceBizExt();
            first.setInstanceId(8L);
            first.setBusinessId("leave-3");
            first.setBusinessTitle("草稿");
            assertEquals(1, mapper.saveOrUpdateByInstanceId(first));
            var resumed = new FlowInstanceBizExt();
            resumed.setInstanceId(8L);
            resumed.setBusinessTitle("重新提交");
            assertEquals(1, mapper.saveOrUpdateByInstanceId(resumed));
            assertEquals(first.getId(), resumed.getId());
            var tx = new TransactionTemplate(new DataSourceTransactionManager(source));
            assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
                resumed.setBusinessTitle("未完成");
                mapper.saveOrUpdateByInstanceId(resumed);
                throw new IllegalStateException("首任务办理失败");
            }));
            assertEquals(1, jdbc.queryForObject("select count(*) from flow_instance_biz_ext", Integer.class));
            assertEquals("重新提交", jdbc.queryForObject(
                "select business_title from flow_instance_biz_ext where instance_id=8", String.class));
        }
    }
}

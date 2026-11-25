package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.system.service.ISysDataScopeService;
import org.dromara.system.service.ISysMenuService;
import org.dromara.system.service.impl.SysDataScopeServiceImpl;
import org.dromara.system.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SystemMapperIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableCaching
    static class Infrastructure {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.SYS_ROLE_CUSTOM);
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL", "Oracle", "MSSQLServer"})
    void bulkMenuDeletionRollsBackOnLinkFailureAndScopeReadsRespectCache(String mode) throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=" + mode, "sa", "");
        // Keep the isolated in-memory database alive until this test closes its context.
        try (var connection = source.getConnection(); var context = new AnnotationConfigApplicationContext()) {
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("create table sys_menu (menu_id bigint primary key)");
            jdbc.execute("create table sys_role_menu (role_id bigint primary key, menu_id bigint)");
            jdbc.execute("create table protected_link (role_id bigint references sys_role_menu(role_id))");
            jdbc.execute("create table sys_role_dept (role_id bigint, dept_id bigint)");
            jdbc.update("insert into sys_menu values (8)");
            jdbc.update("insert into sys_menu values (9)");
            jdbc.update("insert into sys_role_menu values (11, 8)");
            jdbc.update("insert into sys_role_menu values (12, 9)");
            jdbc.update("insert into protected_link values (11)");
            jdbc.update("insert into sys_role_dept values (11, 21)");
            jdbc.update("insert into sys_role_dept values (12, 22)");

            var configuration = new MybatisConfiguration();
            configuration.setEnvironment(new Environment("system", new SpringManagedTransactionFactory(), source));
            configuration.addMapper(SysMenuMapper.class);
            configuration.addMapper(SysRoleMenuMapper.class);
            configuration.addMapper(SysRoleDeptMapper.class);
            configuration.addMapper(SysRoleMapper.class);
            configuration.addMapper(SysDeptMapper.class);
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
            context.registerBean(DataSource.class, () -> source);
            context.register(Infrastructure.class);
            context.registerBean(SysMenuServiceImpl.class, () -> new SysMenuServiceImpl(sessions.getMapper(SysMenuMapper.class),
                sessions.getMapper(SysRoleMapper.class), sessions.getMapper(SysRoleMenuMapper.class)));
            context.registerBean(SysDataScopeServiceImpl.class, () -> new SysDataScopeServiceImpl(
                sessions.getMapper(SysRoleDeptMapper.class), sessions.getMapper(SysDeptMapper.class)));
            context.refresh();

            var scopes = context.getBean(ISysDataScopeService.class);
            assertEquals("21", scopes.getRoleCustom(11L));
            jdbc.update("update sys_role_dept set dept_id = 31 where role_id = 11");
            assertEquals("21", scopes.getRoleCustom(11L));
            context.getBean(CacheManager.class).getCache(CacheNames.SYS_ROLE_CUSTOM).evict(11L);
            assertEquals("31", scopes.getRoleCustom(11L));
            assertEquals("22", scopes.getRoleCustom(12L));
            assertEquals("-1", scopes.getRoleCustom(404L));

            var menus = context.getBean(ISysMenuService.class);
            assertThrows(DataIntegrityViolationException.class, () -> menus.deleteMenuById(List.of(8L)));
            assertEquals(List.of(8L, 9L), jdbc.queryForList("select menu_id from sys_menu order by menu_id", Long.class));
            jdbc.update("delete from protected_link where role_id = 11");
            menus.deleteMenuById(List.of(8L));
            assertEquals(List.of(9L), jdbc.queryForList("select menu_id from sys_menu", Long.class));
            assertEquals(List.of(12L), jdbc.queryForList("select role_id from sys_role_menu", Long.class));
        }
    }
}

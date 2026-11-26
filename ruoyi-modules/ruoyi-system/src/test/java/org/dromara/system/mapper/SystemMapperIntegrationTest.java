package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.system.service.ISysDataScopeService;
import org.dromara.system.service.ISysUserService;
import org.dromara.system.service.impl.SysUserServiceImpl;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
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
    @Test
    void routeUniquenessAllowsOwnMenuAndButtonsButRejectsCaseOnlyCollisions() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
        try (var connection = source.getConnection(); var beans = mockStatic(SpringUtil.class);
             var mapping = mockStatic(MapstructUtils.class)) {
            var db = new JdbcTemplate(source);
            db.execute("""
                create table sys_menu(menu_id bigint primary key,parent_id bigint,menu_name varchar,order_num int,
                path varchar,component varchar,query_param varchar,is_frame varchar,is_cache varchar,menu_type varchar,
                visible varchar,status varchar,perms varchar,icon varchar,active_menu varchar,ext varchar,remark varchar,
                create_dept bigint,create_by bigint,create_time timestamp,update_by bigint,update_time timestamp)
                """);
            db.update("insert into sys_menu(menu_id,parent_id,path,menu_type) values(1,20,'invoice','C')");
            var config = new MybatisConfiguration();
            config.setEnvironment(new Environment("menu", new SpringManagedTransactionFactory(), source));
            config.addMapper(SysMenuMapper.class);
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
            var service = new SysMenuServiceImpl(sessions.getMapper(SysMenuMapper.class), mock(), mock());
            var form = new org.dromara.system.domain.bo.SysMenuBo();
            var menu = new org.dromara.system.domain.SysMenu();
            mapping.when(() -> MapstructUtils.convert(form, org.dromara.system.domain.SysMenu.class)).thenReturn(menu);
            menu.setMenuId(1L);
            menu.setParentId(20L);
            menu.setPath("invoice");
            menu.setMenuType("C");
            assertTrue(service.checkRouteConfigUnique(form));
            menu.setMenuId(null);
            assertFalse(service.checkRouteConfigUnique(form));
            menu.setMenuType("F");
            assertTrue(service.checkRouteConfigUnique(form));
            menu.setMenuType("C");
            menu.setPath("INVOICE");
            assertFalse(service.checkRouteConfigUnique(form));
            menu.setParentId(30L);
            assertFalse(service.checkRouteConfigUnique(form));
            menu.setPath("reports");
            assertTrue(service.checkRouteConfigUnique(form));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableCaching
    static class Infrastructure {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void editingAccountPreservesOmittedAssignmentsAndRollsBackFailedClear() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
        try (var connection = source.getConnection(); var context = new AnnotationConfigApplicationContext();
             var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            var db = new JdbcTemplate(source);
            db.execute("create table sys_user_post (user_id bigint, post_id bigint)");
            db.execute("create table sys_user_role (user_id bigint, role_id bigint check (role_id <> 99))");
            db.update("insert into sys_user_post values (42,7),(42,8),(43,9)");
            db.update("insert into sys_user_role values (42,4),(43,5)");
            var config = new MybatisConfiguration();
            config.setEnvironment(new Environment("system", new SpringManagedTransactionFactory(), source));
            config.addMapper(SysUserRoleMapper.class);
            config.addMapper(SysUserPostMapper.class);
            var sessions = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(config));
            var users = mock(SysUserMapper.class);
            var roles = mock(SysRoleMapper.class);
            context.registerBean(DataSource.class, () -> source);
            context.register(Infrastructure.class);
            context.registerBean(SysUserServiceImpl.class, () -> new SysUserServiceImpl(users, mock(), roles, mock(),
                sessions.getMapper(SysUserRoleMapper.class), sessions.getMapper(SysUserPostMapper.class)));
            context.refresh();
            var service = context.getBean(ISysUserService.class);
            var form = new SysUserBo();
            form.setUserId(42L);
            var entity = new SysUser();
            entity.setUserId(42L);
            mapping.when(() -> MapstructUtils.convert(form, SysUser.class)).thenReturn(entity);
            when(users.updateById(entity)).thenReturn(1, 0, 1);
            String posts = "select post_id from sys_user_post order by post_id";
            String assignments = "select role_id from sys_user_role where user_id=42";
            assertEquals(1, service.updateUser(form));
            assertEquals(List.of(7L, 8L, 9L), db.queryForList(posts, Long.class));
            assertEquals(List.of(4L), db.queryForList(assignments, Long.class));

            form.setPostIds(new Long[0]);
            assertThrows(ServiceException.class, () -> service.updateUser(form));
            assertEquals(List.of(7L, 8L, 9L), db.queryForList(posts, Long.class));
            assertEquals(1, service.updateUser(form));
            assertEquals(List.of(9L), db.queryForList(posts, Long.class));

            // 编辑表单要求至少一个角色，单独授权入口收到空选择时保留既有授权。
            service.insertUserAuth(42L, new Long[0]);
            assertEquals(List.of(4L), db.queryForList(assignments, Long.class));
            when(roles.selectRoleCount(List.of(10L, 11L))).thenReturn(1L);
            assertThrows(ServiceException.class, () -> service.insertUserAuth(42L, new Long[]{10L, 11L}));
            assertEquals(List.of(4L), db.queryForList(assignments, Long.class));
            when(roles.selectRoleCount(List.of(10L, 11L))).thenReturn(2L);
            service.insertUserAuth(42L, new Long[]{10L, 11L});
            String ordered = assignments + " order by role_id";
            assertEquals(List.of(10L, 11L), db.queryForList(ordered, Long.class));
            when(roles.selectRoleCount(List.of(12L, 99L))).thenReturn(2L);
            var failure = assertThrows(org.apache.ibatis.exceptions.PersistenceException.class,
                () -> service.insertUserAuth(42L, new Long[]{12L, 99L}));
            assertInstanceOf(java.sql.BatchUpdateException.class,
                org.springframework.core.NestedExceptionUtils.getMostSpecificCause(failure));
            assertEquals(List.of(10L, 11L), db.queryForList(ordered, Long.class));
            assertEquals(List.of(5L), db.queryForList("select role_id from sys_user_role where user_id=43", Long.class));
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

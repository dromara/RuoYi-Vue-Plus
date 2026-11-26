package org.dromara.common.mybatis.handler;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.hutool.extra.spring.SpringUtil;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.domain.DataPermissionAccess;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.domain.RoleDTO;
import org.dromara.system.api.model.LoginUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.sql.DriverManager;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataScopeQueryTest {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void writesRecordTheCurrentOperatorAndKeepOriginalCreationTime(boolean loggedIn) {
        var entity = new org.dromara.common.mybatis.core.domain.BaseEntity();
        var created = java.time.LocalDateTime.of(2025, 3, 1, 12, 0);
        entity.setCreateTime(created);
        var user = new LoginUser();
        user.setUserId(42L);
        user.setDeptId(7L);
        var handler = new InjectionMetaObjectHandler();
        var meta = org.apache.ibatis.reflection.SystemMetaObject.forObject(entity);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(loggedIn ? user : null);
            handler.insertFill(meta);
            assertEquals(loggedIn ? 42L : -1L, entity.getCreateBy());
            assertEquals(loggedIn ? 7L : -1L, entity.getCreateDept());
            assertEquals(created, entity.getUpdateTime());
            handler.updateFill(meta);
            assertEquals(loggedIn ? 42L : -1L, entity.getUpdateBy());
            assertEquals(created, entity.getCreateTime());
            assertTrue(entity.getUpdateTime().isAfter(created));
        }
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void parsedReadAndWriteStatementsUseTheirRespectiveScope(boolean restricted) throws Exception {
        try (var spring = mockStatic(SpringUtil.class)) {
            var factory = new DefaultListableBeanFactory();
            spring.when(SpringUtil::getBeanFactory).thenReturn(factory);
            var interceptor = new org.dromara.common.mybatis.interceptor.PlusDataPermissionInterceptor();
            var handler = mock(PlusDataPermissionHandler.class);
            org.springframework.test.util.ReflectionTestUtils.setField(interceptor, "dataPermissionHandler", handler);
            if (restricted) {
                when(handler.getSqlSegment(null, true)).thenReturn(CCJSqlParserUtil.parseCondExpression("dept_id = 7"));
                when(handler.getSqlSegment(null, false)).thenReturn(CCJSqlParserUtil.parseCondExpression("owner_id = 4"));
            }
            String read = interceptor.parserSingle("select id from records union all select id from archive", "list");
            String edit = interceptor.parserSingle("update records set status = 1", "update");
            String delete = interceptor.parserSingle("delete from records", "delete");
            assertEquals(restricted ? 2 : 0, read.split("dept_id = 7", -1).length - 1);
            assertEquals(restricted, edit.contains("WHERE owner_id = 4"));
            assertEquals(restricted, delete.contains("WHERE owner_id = 4"));
        }
    }
    @DataPermission({@DataColumn, @DataColumn(key = "userName", value = "owner_id")})
    private void scopedMapper() {}

    @ParameterizedTest
    @CsvSource({
        "true, unrestricted, 1/2/3", "false, unrestricted, 1",
        "true, department, 1/2", "true, owner, 1/3", "true, unrelated, none",
        "false, department, 1/2", "false, owner, 1/3", "false, unrelated, none"
    })
    void returnedRowsRespectRoleScopeAndOperation(boolean select, String scope, String expected) throws Exception {
        var dept = new RoleDTO();
        dept.setRoleId(1L);
        dept.setRoleKey("warehouse");
        dept.setDataScope("3");
        var owner = new RoleDTO();
        owner.setRoleId(2L);
        owner.setRoleKey("operator");
        owner.setDataScope("5");
        var user = new LoginUser();
        user.setRoles(List.of(dept, owner));
        user.setUserId(11L);
        user.setDeptId(22L);
        user.setDataScopeRoleMap(Map.of("records:list", List.of(1L, 99L)));
        var access = switch (scope) {
            case "department" -> new DataPermissionAccess(Set.of("records:list"), Set.of());
            case "owner" -> new DataPermissionAccess(Set.of(), Set.of("operator"));
            case "unrelated" -> new DataPermissionAccess(Set.of("other:list"), Set.of());
            default -> DataPermissionAccess.EMPTY;
        };
        try (var spring = mockStatic(SpringUtil.class); var sa = mockStatic(SaHolder.class);
             var login = mockStatic(LoginHelper.class); var conn = DriverManager.getConnection("jdbc:h2:mem:")) {
            var factory = new DefaultListableBeanFactory();
            spring.when(SpringUtil::getBeanFactory).thenReturn(factory);
            var storage = mock(SaStorage.class);
            when(storage.get(anyString())).thenReturn(new HashMap<String, Object>());
            sa.when(SaHolder::getStorage).thenReturn(storage);
            login.when(LoginHelper::getLoginUser).thenReturn(user);
            DataPermissionHelper.setPermission(getClass().getDeclaredMethod("scopedMapper").getAnnotation(DataPermission.class));
            DataPermissionHelper.setAccess(access);
            var predicate = new PlusDataPermissionHandler().getSqlSegment(CCJSqlParserUtil.parseCondExpression("id < 5"), select);
            try (var statement = conn.createStatement()) {
                statement.execute("create table records(id int, dept_id int, owner_id int, changed boolean default false)");
                statement.execute("insert into records(id, dept_id, owner_id) values (1,22,11),(2,22,12),(3,23,11),(4,23,12),(5,22,11)");
                if (!select) {
                    statement.executeUpdate("update records set changed=true where " + predicate);
                }
                try (var rows = statement.executeQuery("select id from records where " + (select ? predicate : "changed") + " order by id")) {
                    var ids = new ArrayList<String>();
                    while (rows.next()) {
                        ids.add(rows.getString(1));
                    }
                    assertEquals(expected, ids.isEmpty() ? "none" : String.join("/", ids));
                }
            }
        } finally {
            DataPermissionHelper.removePermission();
        }
    }
}

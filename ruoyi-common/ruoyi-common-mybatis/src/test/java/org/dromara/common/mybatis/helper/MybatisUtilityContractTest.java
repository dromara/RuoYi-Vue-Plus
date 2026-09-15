package org.dromara.common.mybatis.helper;

import cn.hutool.core.exceptions.UtilException;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.dromara.common.mybatis.enums.DataBaseType;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.support.StaticApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("common-mybatis 工具契约单元测试")
class MybatisUtilityContractTest {

    private static DynamicRoutingDataSource dataSource;
    private static IdentifierGenerator identifierGenerator;

    /**
     * 注册数据库助手和 ID 工具的最小 Spring 依赖，避免连接真实数据库或使用应用数据源。
     */
    @BeforeAll
    static void initializeMybatisUtilities() {
        dataSource = mock(DynamicRoutingDataSource.class);
        identifierGenerator = mock(IdentifierGenerator.class);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("dynamicRoutingDataSource", dataSource);
        context.getBeanFactory().registerSingleton("identifierGenerator", identifierGenerator);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 每个用例前清理 mock 调用记录，保证数据库类型缓存断言只针对当前数据源名称。
     */
    @BeforeEach
    void resetMybatisMocks() {
        reset(dataSource, identifierGenerator);
    }

    /**
     * 验证指定数据源从 JDBC 元数据识别四种常用数据库，并缓存同一数据源的识别结果。
     */
    @Test
    @DisplayName("识别并缓存指定数据源类型")
    void shouldResolveAndCacheNamedDatabaseTypes() throws Exception {
        Map<String, DataSource> sources = Map.of(
            "mysql-contract", dataSource("MySQL"),
            "oracle-contract", dataSource("Oracle"),
            "postgres-contract", dataSource("PostgreSQL"),
            "sqlserver-contract", dataSource("Microsoft SQL Server"));
        when(dataSource.getDataSource(any(String.class))).thenAnswer(invocation -> sources.get(invocation.getArgument(0)));

        assertEquals(DataBaseType.MY_SQL, DataBaseHelper.getDataBaseType("mysql-contract"));
        assertEquals(DataBaseType.ORACLE, DataBaseHelper.getDataBaseType("oracle-contract"));
        assertEquals(DataBaseType.POSTGRE_SQL, DataBaseHelper.getDataBaseType("postgres-contract"));
        assertEquals(DataBaseType.SQL_SERVER, DataBaseHelper.getDataBaseType("sqlserver-contract"));
        assertEquals(DataBaseType.MY_SQL, DataBaseHelper.getDataBaseType("mysql-contract"));

        verify(dataSource, times(2)).getDataSource("mysql-contract");
        verify(sources.get("mysql-contract"), times(1)).getConnection();
    }

    /**
     * 验证当前线程数据源识别、默认 primary 键以及异常包装行为。
     */
    @Test
    @DisplayName("识别当前数据源并包装 JDBC 异常")
    void shouldResolveCurrentDataSourceAndWrapSqlException() throws Exception {
        DataSource current = dataSource("Oracle");
        when(dataSource.determineDataSource()).thenReturn(current);
        AtomicReference<String> currentKey = new AtomicReference<>("current-contract");
        when(dataSource.getDataSources()).thenReturn(Map.of("primary", current));

        try (MockedStatic<DynamicDataSourceContextHolder> holder = mockStatic(DynamicDataSourceContextHolder.class)) {
            holder.when(DynamicDataSourceContextHolder::peek).thenAnswer(invocation -> currentKey.get());
            assertEquals(DataBaseType.ORACLE, DataBaseHelper.getDataBaseType());

            currentKey.set(null);
            when(current.getConnection()).thenThrow(new SQLException("connection unavailable"));
            RuntimeException exception = assertThrows(RuntimeException.class, DataBaseHelper::getDataBaseType);
            assertEquals("获取数据库类型失败", exception.getMessage());
            assertInstanceOf(SQLException.class, exception.getCause());
        }
    }

    /**
     * 验证不同数据库方言生成对应 FIND_IN_SET 片段，并拒绝 SQL 关键字和引号注入。
     */
    @Test
    @DisplayName("生成数据库方言 FIND_IN_SET")
    void shouldBuildDialectSpecificFindInSetSql() throws Exception {
        Map<String, DataSource> sources = Map.of(
            "find-oracle", dataSource("Oracle"),
            "find-postgres", dataSource("PostgreSQL"),
            "find-sqlserver", dataSource("Microsoft SQL Server"),
            "find-mysql", dataSource("MySQL"));
        AtomicReference<String> currentKey = new AtomicReference<>();
        when(dataSource.determineDataSource()).thenAnswer(invocation -> sources.get(currentKey.get()));

        try (MockedStatic<DynamicDataSourceContextHolder> holder = mockStatic(DynamicDataSourceContextHolder.class)) {
            holder.when(DynamicDataSourceContextHolder::peek).thenAnswer(invocation -> currentKey.get());
            currentKey.set("find-oracle");
            assertEquals("instr(','||role_ids||',' , ',100,') <> 0", DataBaseHelper.findInSet(100, "role_ids"));
            currentKey.set("find-postgres");
            assertEquals("(select strpos(','||role_ids||',' , ',100,')) <> 0", DataBaseHelper.findInSet(100, "role_ids"));
            currentKey.set("find-sqlserver");
            assertEquals("charindex(',100,' , ','+role_ids+',') <> 0", DataBaseHelper.findInSet(100, "role_ids"));
            currentKey.set("find-mysql");
            assertEquals("find_in_set('100' , role_ids) <> 0", DataBaseHelper.findInSet(100, "role_ids"));
            assertThrows(UtilException.class, () -> DataBaseHelper.findInSet("100'", "role_ids"));
            assertThrows(UtilException.class, () -> DataBaseHelper.findInSet(100, "select role_ids"));
        }
    }

    /**
     * 验证数据源名称列表复制动态数据源集合，调用方修改返回列表不会污染路由器状态。
     */
    @Test
    @DisplayName("读取数据源名称列表")
    void shouldCopyDataSourceNameList() throws Exception {
        DataSource primary = dataSource("MySQL");
        DataSource archive = dataSource("Oracle");
        when(dataSource.getDataSources()).thenReturn(Map.of("primary", primary, "archive", archive));

        var names = DataBaseHelper.getDataSourceNameList();

        assertEquals(2, names.size());
        assertTrue(names.containsAll(java.util.List.of("primary", "archive")));
        names.clear();
        assertEquals(2, dataSource.getDataSources().size());
    }

    /**
     * 验证 ID 工具覆盖生成器的 Number、Long、String、实体、UUID 和前缀 API。
     */
    @Test
    @DisplayName("委托主键生成器和 UUID 生成")
    void shouldDelegateIdentifierGenerationApis() {
        when(identifierGenerator.nextId(any())).thenReturn(123456789L);
        when(identifierGenerator.nextUUID(any())).thenReturn("entity-uuid");
        Object entity = new Object();

        assertEquals("123456789", IdGeneratorUtil.nextId());
        assertEquals(123456789L, IdGeneratorUtil.nextLongId());
        assertEquals(123456789L, IdGeneratorUtil.nextNumberId());
        assertEquals(123456789L, IdGeneratorUtil.nextId(entity));
        assertEquals("123456789", IdGeneratorUtil.nextStringId(entity));
        assertEquals("entity-uuid", IdGeneratorUtil.nextUUID(entity));
        assertEquals("ORD123456789", IdGeneratorUtil.nextIdWithPrefix("ORD"));
        assertTrue(IdGeneratorUtil.nextUUIDWithPrefix("ID").startsWith("ID"));
        assertEquals(34, IdGeneratorUtil.nextUUIDWithPrefix("ID").length());
        assertEquals(32, IdGeneratorUtil.nextUUID().length());
        verify(identifierGenerator, atLeastOnce()).nextId(any());
    }

    /**
     * 创建返回指定数据库产品名的 JDBC mock，集中复用连接和元数据契约。
     *
     * @param productName 数据库产品名
     * @return JDBC 数据源 mock
     */
    private static DataSource dataSource(String productName) throws SQLException {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(metadata.getDatabaseProductName()).thenReturn(productName);
        when(connection.getMetaData()).thenReturn(metadata);
        when(source.getConnection()).thenReturn(connection);
        return source;
    }
}

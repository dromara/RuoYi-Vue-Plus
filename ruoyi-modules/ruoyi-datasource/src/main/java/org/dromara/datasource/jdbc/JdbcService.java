package org.dromara.datasource.jdbc;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.jdbc.core.DbType;
import org.dromara.datasource.utils.Assert;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 动态数据源执行服务
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcService {

    // 连接测试sql
    private static final String TEST_SQL = "SELECT 1";

    /**
     * 查询缓存，没必要每次都去获取dataSource
     */
    private final ConcurrentMap<String, JdbcQuery> jdbcQueryMap = new ConcurrentHashMap<>();

    /**
     * 动态数据源组件
     */
    private final DynamicRoutingDataSource dynamicRoutingDataSource;


    /**
     * 获取数据源
     */
    public DataSource getDataSource(String dataSourceName) {
        DataSource dataSource = dynamicRoutingDataSource.getDataSource(dataSourceName);
        Assert.notNull(dataSource, "数据源【%s】不存在".formatted(dataSourceName));
        Assert.isTrue(testConnection(dataSource), "数据源【%s】不存在".formatted(dataSourceName));

        return dataSource;
    }

    /**
     * 测试连接
     */
    public boolean testConnection(String url, String username, String password, String driverClassName) {
        DataSourceBuilder<HikariDataSource> builder = DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .driverClassName(driverClassName)
            .url(url)
            .username(username)
            .password(password);
        DataSource dataSource = builder.build();
        return testConnection(dataSource);
    }

    /**
     * 测试连接
     */
    public boolean testConnection(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet resultSet = stmt.executeQuery(TEST_SQL);
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 添加数据源
     */
    public void addDataSource(List<SysDatasource> datasourceList) {
        for (SysDatasource sysDatasource : datasourceList) {
            try {
                String dataSourceName = sysDatasource.getDsName();
                String url = sysDatasource.getConnUrl();
                String username = sysDatasource.getUsername();
                String password = sysDatasource.getPassword();
                String driverClassName = sysDatasource.getDriverClassName();
                addDataSource(dataSourceName, url, username, password, driverClassName);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    /**
     * 添加数据源
     */
    public void addDataSource(String dataSourceName, String url, String username, String password, String driverClassName) {
        Assert.isTrue(!dynamicRoutingDataSource.getDataSources().containsKey(dataSourceName), "数据源【%s】不存在".formatted(dataSourceName));
        DataSourceBuilder<HikariDataSource> builder = DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .driverClassName(driverClassName)
            .url(url)
            .username(username)
            .password(password);
        DataSource dataSource = builder.build();
        dynamicRoutingDataSource.addDataSource(dataSourceName, dataSource);
    }

    /**
     * 移除数据源
     */
    public void removeDataSource(String dataSourceName) {
        dynamicRoutingDataSource.removeDataSource(dataSourceName);
    }

    /**
     * 修改数据源
     */
    public void updateDataSource(String dataSourceName, String url, String username, String password, String driverClassName) {
        Assert.isTrue(dynamicRoutingDataSource.getDataSources().containsKey(dataSourceName), "数据源【%s】不存在".formatted(dataSourceName));
        addDataSource(dataSourceName, url, username, password, driverClassName);
    }

    /**
     * 查询sql
     */
    public <R> R query(String dsName, String dsType, Function<JdbcQuery, R> func) {
        try {
            DynamicDataSourceContextHolder.push(dsName);
            return func.apply(getJdbcQuery(dsName, dsType));
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    /**
     * 执行，没有返回值
     */
    public void execute(String dsName, String dsType, Consumer<JdbcQuery> consumer) {
        try {
            DynamicDataSourceContextHolder.push(dsName);
            consumer.accept(getJdbcQuery(dsName, dsType));
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    /**
     * 获取JdbcQuery
     */
    private JdbcQuery getJdbcQuery(String dsName, String dsType) {
        DataSource dataSource = getDataSource(dsName);

        JdbcQuery jdbcQuery = jdbcQueryMap.get(dsName);
        // 判断是否连接
        if (Objects.isNull(jdbcQuery) || !jdbcQuery.testConnection()) {
            jdbcQuery = new JdbcQueryBuilder.Builder()
                .setDbType(DbType.getDbType(dsType))
                .setDataSource(dataSource)
                .build();
            jdbcQueryMap.put(dsName, jdbcQuery);
        }
        return jdbcQuery;
    }
}

package org.dromara.datasource.jdbc;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.datasource.jdbc.core.DataType;
import org.dromara.datasource.jdbc.core.InternalFunction;
import org.dromara.datasource.jdbc.core.StorageEngine;
import org.dromara.datasource.utils.ExceptionUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * jdbc基础查询接口
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
public interface JdbcQuery {

    String SQL_CURRENT_DB = "select database()";

    /**
     * 获取JdbcTemplate
     */
    JdbcTemplate getJdbc();

    /**
     * 分页查询
     */
    default IPage<Map<String, Object>> queryForPage(IPage<Map<String, Object>> page, String sql) throws DatabaseException {
        if (StringUtils.isEmpty(sql)) {
            throw ExceptionUtils.create("sql should not empty");
        }
        JdbcTemplate jdbc = getJdbc();
        int total = Optional.ofNullable(jdbc.queryForObject("select count(1) from (" + sql + ") t", Integer.class))
            .orElse(0);
        page.setTotal(total);
        if (total > 0) {
            List<Map<String, Object>> maps = jdbc.queryForList(sql + " LIMIT " + page.offset() + StringPool.COMMA + page.getSize());
            page.setRecords(maps);
        }
        return page;
    }

    /**
     * 分页查询
     */
    default <T> IPage<T> queryForPage(IPage<T> page, String sql, Class<T> elementType) throws DatabaseException {
        if (StringUtils.isEmpty(sql)) {
            throw ExceptionUtils.create("sql should not empty");
        }
        JdbcTemplate jdbc = getJdbc();
        int total = Optional.ofNullable(jdbc.queryForObject("select count(1) from (" + sql + ") t", Integer.class))
            .orElse(0);
        page.setTotal(total);
        if (total > 0) {
            BeanPropertyRowMapper<T> rowMapper = BeanPropertyRowMapper.newInstance(elementType);
            List<T> list = jdbc.query(sql + " LIMIT " + page.offset() + StringPool.COMMA + page.getSize(), rowMapper);
            page.setRecords(list);
        }
        return page;
    }

    /**
     * 测试连接是否成功
     */
    default boolean testConnection() {
        try {
            getJdbc().queryForMap("select 1");
            return true;
        } catch (Exception ignored) {
            // ignored
        }
        return false;
    }

    /**
     * 查询数据库
     */
    default List<String> queryDatabases() throws DatabaseException {
        try {
            return getJdbc().queryForList("show databases", String.class);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * 查询表数据, 当前数据库
     */
    default List<Map<String, Object>> queryTables(String... tables) throws DatabaseException {
        return querySchemaTables(null, tables);
    }

    /**
     * 查询表数据
     */
    default List<Map<String, Object>> querySchemaTables(String schema, String... tables) throws DatabaseException {
        try {
            return getJdbc().queryForList(sqlQueryTable(schema, tables));
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * 分页查询表数据
     */
    default IPage<Map<String, Object>> queryTablePage(IPage<Map<String, Object>> page) throws DatabaseException {
        try {
            return queryForPage(page, sqlQueryTable());
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * 查询表sql
     */
    default String sqlQueryTable() {
        return sqlQueryTable(null);
    }

    /**
     * 查询表，可以指定表名
     */
    default String sqlQueryTable(String schema, String... tables) {
        throw ExceptionUtils.methodNotImplemented();
    }

    /**
     * 查询表字段
     */
    default List<Map<String, Object>> queryTableFields(String schema, String table) throws DatabaseException {
        throw ExceptionUtils.methodNotImplemented();
    }

    /**
     * 获取数据类型
     */
    default List<DataType> queryDateTypes() throws DatabaseException {
        throw ExceptionUtils.methodNotImplemented();
    }

    /**
     * 获取引擎类型
     */
    default List<StorageEngine> queryEngines() throws DatabaseException {
        throw ExceptionUtils.methodNotImplemented();
    }

    /**
     * 获取数据库内置方法
     */
    default List<InternalFunction> queryFunctions() throws DatabaseException {
        throw ExceptionUtils.methodNotImplemented();
    }

    /**
     * 获取数据库集群数据
     */
    default List<String> queryClusters() throws DatabaseException {
        throw ExceptionUtils.methodNotImplemented();
    }

    /**
     * 将map装换为javabean对象
     */
    default <T> T mapToBean(Map<String, Object> map, Class<T> clz) {
        try {
            return mapToBean(map, clz.newInstance());
        } catch (InstantiationException | IllegalAccessException ignored) {
            return null;
        }
    }

    default <T> T mapToBean(Map<String, Object> map, T bean) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                continue;
            }
            field.setAccessible(true);
            if (map.containsKey(field.getName())) {
                try {
                    Object value = map.get(field.getName());
                    if (value instanceof BigInteger) {
                        value = ((BigInteger) value).intValue();
                    }
                    field.set(bean, value);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return bean;
    }

    /**
     * bean转Map
     */
    default <T> Map<String, Object> beanToMap(T bean) {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(bean));
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }
}

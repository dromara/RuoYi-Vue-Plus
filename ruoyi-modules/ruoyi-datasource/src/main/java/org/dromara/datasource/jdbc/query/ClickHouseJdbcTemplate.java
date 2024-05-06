package org.dromara.datasource.jdbc.query;

import com.clickhouse.data.ClickHouseDataType;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.datasource.jdbc.DatabaseException;
import org.dromara.datasource.jdbc.JdbcQuery;
import org.dromara.datasource.jdbc.core.DataType;
import org.dromara.datasource.jdbc.core.InternalFunction;
import org.dromara.datasource.jdbc.core.StorageEngine;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClickHouse 查询
 *
 * @author xyxj xieyangxuejun@gmail.com
 * @since 2024/5/2
 */
public final class ClickHouseJdbcTemplate extends JdbcTemplate implements JdbcQuery {
    public ClickHouseJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ClickHouseJdbcTemplate getJdbc() {
        return this;
    }

    @Override
    public String sqlQueryTable(String schema, String... tables) {
        StringBuilder sb = new StringBuilder()
            .append("select name as tableName,\n")
            .append("database as tableSchema\n")
            .append("from system.tables\n");
        if (StringUtils.isEmpty(schema)) {
            sb.append("where database=(").append(SQL_CURRENT_DB).append(")\n");
        } else {
            sb.append("where database='").append(schema).append("'\n");
        }
        if (tables != null && tables.length != 0) {
            sb.append(" and name in ('").append(String.join("','", tables)).append("')\n");
        }
        sb.append(" and not startsWith(name, '.')\n")
            .append("order by database, name");
        return sb.toString();
    }

    @Override
    public List<Map<String, Object>> queryTableFields(String schema, String table) throws DatabaseException {
        try {
            StringBuilder sb = new StringBuilder()
                .append("select distinct c.table as tableName,\n")
                .append("c.name as columnName,\n")
                .append("c.type as columnType,\n")
                .append("c.type as dataType,\n")
                .append("int32(c.position) as sort\n")
                .append("from system.columns c,\n")
                .append("system.tables t\n")
                .append("where t.database = c.database and t.name = c.table\n");
            if (StringUtils.isEmpty(schema)) {
                sb.append(" and t.database=(" + SQL_CURRENT_DB + ")\n");
            } else {
                sb.append(" and t.database='").append(schema).append("'\n");
            }
            if (StringUtils.isNotEmpty(table)) {
                sb.append(" and t.name='").append(table).append("'");
            }
            return queryForList(sb.toString());
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<DataType> queryDateTypes() throws DatabaseException {
        return Arrays.stream(ClickHouseDataType.values()).map(type -> new DataType(type.name(), type.isSigned())).collect(Collectors.toList());
    }

    @Override
    public List<StorageEngine> queryEngines() throws DatabaseException {
        String sql = "select name, comment from system.storage_engines";
        return getJdbc().query(sql, BeanPropertyRowMapper.newInstance(StorageEngine.class));
    }

    @Override
    public List<InternalFunction> queryFunctions() throws DatabaseException {
        // 将alias to 合并
        // select arrayJoin(if(f.alias_to <> '', [f.name,f.alias_to], [f.name])) as name, f.* from system.functions f where f.alias_to <> '';
        String sql = String.format("select arrayJoin(if(f.alias_to <> '', [f.name,f.alias_to], [f.name])) as name, f.*\n" +
            "                   from system.functions f\n" +
            "                   where name <> %s", "''");
        return getJdbc().queryForList(sql).stream().map(v -> {
            InternalFunction functions = new InternalFunction();
            functions.setName(v.getOrDefault("name", "").toString());
            return functions;
        }).toList();
    }

    @Override
    public List<String> queryClusters() throws DatabaseException {
        String sql = "select distinct cluster from system.clusters";
        return getJdbc().queryForList(sql, String.class);
    }
}

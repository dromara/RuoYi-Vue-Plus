package org.dromara.datasource.jdbc.query;

import com.mysql.cj.MysqlType;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.datasource.jdbc.DatabaseException;
import org.dromara.datasource.jdbc.JdbcQuery;
import org.dromara.datasource.jdbc.core.DataType;
import org.dromara.datasource.jdbc.core.StorageEngine;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * mysql sql执行类
 *
 * @author xyxj xieyangxuejun@gmail.com
 */
public class MysqlJdbcTemplate extends JdbcTemplate implements JdbcQuery {

    public MysqlJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public MysqlJdbcTemplate getJdbc() {
        return this;
    }

    @Override
    public String sqlQueryTable(String schema, String... tables) {
        StringBuilder sb = new StringBuilder()
                .append("select table_name as tableName,\n")
                .append("table_comment as tableComment,\n")
                .append("create_time as createTime,\n")
                .append("update_time as updateTime,\n")
                .append("table_schema as tableSchema\n")
                .append("from information_schema.tables\n")
                .append("where table_type in ('BASE TABLE', 'SYSTEM VIEW')");
        if (StringUtils.isEmpty(schema)) {
            sb.append(" and table_schema=(" + SQL_CURRENT_DB + ")\n");
        } else {
            sb.append(" and table_schema='").append(schema).append("'\n");
        }
        if (tables != null && tables.length != 0) {
            sb.append(" and table_name in ('").append(String.join("','", tables)).append("')\n");
        }
        sb.append("order by 2, 1");
        return sb.toString();
    }

    @Override
    public List<Map<String, Object>> queryTableFields(String schema, String table) throws DatabaseException {
        try {
            StringBuilder sb = new StringBuilder()
                    .append("select distinct c.table_name as tableName,\n")
                    .append("c.column_name as columnName,\n")
                    .append("c.column_type as columnType,\n")
                    .append("c.column_comment as columnComment,\n")
                    .append("c.data_type as dataType,\n")
                    .append("(case when (c.is_nullable = 'no' && column_key != 'PRI') then '1' else '0' end) as isRequired,\n")
                    .append("(case when column_key = 'PRI' then '1' else '0' end) as isPk,\n")
                    .append("(case when extra = 'auto_increment' then '1' else '0' end) as isIncrement,\n")
                    .append("c.character_maximum_length as dataLength,\n")
                    .append("c.numeric_precision as dataPrecision,\n")
                    .append("c.numeric_scale as dataScale,\n")
                    .append("c.ordinal_position as sort\n")
                    .append("from information_schema.columns c,\n")
                    .append("information_schema.tables t\n")
                    .append("where t.table_name = c.table_name\n")
                    .append(" and t.table_type in ('BASE TABLE', 'SYSTEM VIEW')\n");
            if (StringUtils.isEmpty(schema)) {
                sb.append(" and t.table_schema=(" + SQL_CURRENT_DB + ")\n");
            } else {
                sb.append(" and t.table_schema='").append(schema).append("'\n");
            }
            if (StringUtils.isNotEmpty(table)) {
                sb.append(" and t.table_name='").append(table).append("'");
            }
            return queryForList(sb.toString());
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<DataType> queryDateTypes() throws DatabaseException {
        return Arrays.stream(MysqlType.values()).map(type -> new DataType(type.getName(), type.getCreateParams().contains("UNSIGNED"))).collect(Collectors.toList());
    }

    @Override
    public List<StorageEngine> queryEngines() throws DatabaseException {
        String sql = String.format("select e.engine as name,\n" +
                "                   if(e.SUPPORT='DEFAULT', 1, 0) as isDefault,\n" +
                "                   e.COMMENT as comment\n" +
                "            from information_schema.engines e\n" +
                "            where e.support in ('%s', '%s')", "DEFAULT", "YES");
        return getJdbc().queryForList(sql).stream()
                .map(v -> new StorageEngine(
                        v.getOrDefault("name", "").toString(),
                        Integer.parseInt(v.get("isDefault").toString()),
                        v.getOrDefault("comment", "").toString(),
                        null)
                ).collect(Collectors.toList());
    }
}

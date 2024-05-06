package org.dromara.datasource.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.*;
import org.dromara.datasource.domain.bo.FieldQueryBo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sql工具类
 *
 * @author xyxj xieyangxuejun@gmail.com
 */
public class SqlUtils {
    private SqlUtils() {
    }

    /**
     * 获取拼接sql
     */
    public static String buildSqlString(FieldQueryBo bo, String table) {
        SqlBuilder sqlBuilder = SqlBuilder.create();
        Collection<String> columnList = bo.getColumns();
        String[] columns = StrUtil.wrapAllWithPair(columnList.contains("*") ? "" : "`", columnList.toArray(new String[0]));
        sqlBuilder.select(columns).from(table);
        // 检索条件
        List<FieldQueryBo.FieldCondition> fieldConditions = bo.getFieldConditions();
        if (CollUtil.isNotEmpty(fieldConditions)) {
            sqlBuilder.where(fieldConditions.stream().filter(fc -> fc.getValue() != null && StrUtil.isNotEmpty(fc.getValue().toString())).map(fc -> {
                // value is wrapped
                Object value = fc.getValue();
                if (!StrUtil.isWrap(value.toString(), "'")) {
                    value = StrUtil.wrap(value.toString(), "'");
                }
                Condition condition = new Condition(fc.getField(), fc.getOperator(), value);
                condition.setPlaceHolder(false);
                condition.setSecondValue(fc.getSecondValue());
                if (StrUtil.isNotEmpty(fc.getLinkOperator())) {
                    condition.setLinkOperator(LogicalOperator.valueOf(fc.getLinkOperator().toUpperCase()));
                }
                return condition;
            }).toArray(Condition[]::new));
        }
        // 聚合
        if (CollUtil.isNotEmpty(bo.getGroupColumns())) {
            sqlBuilder.groupBy(bo.getGroupColumns().toArray(new String[0]));
        }
        // 排序
        List<FieldQueryBo.FieldOrder> fieldOrders = bo.getFieldOrders();
        if (CollUtil.isNotEmpty(fieldOrders)) {
            sqlBuilder.orderBy(fieldOrders.stream().map(fieldOrder -> {
                Order order = new Order();
                order.setField(fieldOrder.getField());
                order.setDirection(Direction.fromString(fieldOrder.getDirection()));
                return order;
            }).toArray(Order[]::new));
        }
        // 当有offset的时候，limit就不需要
        if (CollUtil.isEmpty(bo.getOffsets()) && bo.getLimit() != null) {
            sqlBuilder.append(" LIMIT " + bo.getLimit());
        }
        return sqlBuilder.build();
    }

    /**
     * 根据offsets查询数据
     *
     * @param jdbc    jdbc template
     * @param sql     sql string
     * @param offsets offsets
     * @return list
     */
    public static List<Map<String, Object>> queryByOffsets(@NonNull JdbcTemplate jdbc, @NonNull String sql, List<Integer> offsets) {
        if (CollUtil.isEmpty(offsets)) {
            return jdbc.queryForList(sql);
        }
        // 如果有offsets参数
        return offsets.parallelStream().map(offset -> {
            String append = " OFFSET " + (offset - 1) + " LIMIT 1";
            return jdbc.queryForMap(sql + append);
        }).toList();
    }
}

package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * 支持追加聚合查询字段的 Lambda 查询包装器。
 *
 * @param <T> 实体类型
 * @author Lion Li
 */
class AggregateLambdaQueryWrapper<T> extends LambdaQueryWrapper<T> {

    /**
     * 追加后的聚合查询字段 SQL。
     */
    private String aggregateSqlSelect;

    /**
     * 构造聚合查询包装器。
     *
     * @param entityClass 实体类型
     */
    AggregateLambdaQueryWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * 追加聚合查询字段。
     *
     * @param columnSql 查询字段 SQL
     */
    void appendSelectSql(String columnSql) {
        aggregateSqlSelect = AggregateSelectUtils.appendSelect(getSqlSelect(), columnSql);
    }

    /**
     * 追加子查询字段。
     *
     * @param subquerySql 子查询 SQL
     * @param alias       查询别名
     * @param params      子查询参数
     */
    void appendSelectSub(String subquerySql, String alias, Object... params) {
        appendSelectSql(AggregateSelectUtils.subquerySelect(formatSqlMaybeWithParam(subquerySql, params), alias));
    }

    /**
     * 格式化子查询 SQL。
     *
     * @param subquerySql 子查询 SQL
     * @param params      子查询参数
     * @return 格式化后的子查询 SQL
     */
    String formatSubquerySql(String subquerySql, Object... params) {
        return formatSqlMaybeWithParam(subquerySql, params);
    }

    /**
     * 格式化子查询参数。
     *
     * @param value 参数值
     * @return MyBatis 参数占位符
     */
    String formatSubqueryParam(Object value) {
        return formatParam(null, value);
    }

    /**
     * 清空聚合查询字段。
     */
    void resetAggregateSelect() {
        aggregateSqlSelect = null;
    }

    /**
     * 获取字段对应的数据库列名。
     *
     * @param column 字段
     * @return 数据库列名
     */
    String columnName(SFunction<T, ?> column) {
        return columnToString(column);
    }

    /**
     * 获取最终查询字段 SQL。
     *
     * @return 查询字段 SQL
     */
    @Override
    public String getSqlSelect() {
        if (aggregateSqlSelect != null) {
            return aggregateSqlSelect;
        }
        return super.getSqlSelect();
    }

    /**
     * 清空查询条件与聚合查询字段。
     */
    @Override
    public void clear() {
        super.clear();
        aggregateSqlSelect = null;
    }

}

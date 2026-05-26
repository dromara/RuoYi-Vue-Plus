package org.dromara.common.mybatis.core.query;

/**
 * SQL 标准聚合函数。
 *
 * @author Lion Li
 */
public enum SqlAggregateFunction {

    /**
     * 求和。
     */
    SUM("SUM"),

    /**
     * 最大值。
     */
    MAX("MAX"),

    /**
     * 最小值。
     */
    MIN("MIN"),

    /**
     * 平均值。
     */
    AVG("AVG"),

    /**
     * 计数。
     */
    COUNT("COUNT");

    private final String name;

    SqlAggregateFunction(String name) {
        this.name = name;
    }

    /**
     * 生成聚合函数 SQL 片段。
     *
     * @param expression 函数入参表达式
     * @return 聚合函数 SQL
     */
    public String format(String expression) {
        return name + "(" + expression + ")";
    }

}

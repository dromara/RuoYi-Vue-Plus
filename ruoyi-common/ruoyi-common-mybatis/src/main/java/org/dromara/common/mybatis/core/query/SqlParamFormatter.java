package org.dromara.common.mybatis.core.query;

/**
 * SQL 参数格式化器。
 *
 * @author Lion Li
 */
@FunctionalInterface
public interface SqlParamFormatter {

    /**
     * 格式化 SQL 参数。
     *
     * @param value 参数值
     * @return MyBatis 参数占位符
     */
    String format(Object value);

}

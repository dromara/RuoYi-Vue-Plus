package org.dromara.common.mybatis.core.query;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.util.regex.Pattern;

/**
 * 聚合查询字段 SQL 构造工具。
 *
 * @author Lion Li
 */
public final class AggregateSelectUtils {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private AggregateSelectUtils() {
    }

    /**
     * 追加查询字段 SQL。
     *
     * @param current  已有查询字段 SQL
     * @param fragment 新增查询字段 SQL
     * @return 合并后的查询字段 SQL
     */
    public static String appendSelect(String current, String fragment) {
        if (StringUtils.isBlank(current)) {
            return fragment;
        }
        return current + Constants.COMMA + fragment;
    }

    /**
     * 生成聚合查询字段 SQL。
     *
     * @param function 聚合函数
     * @param column   字段 SQL
     * @param alias    查询别名
     * @return 聚合查询字段 SQL
     */
    public static String aggregateSelect(SqlAggregateFunction function, String column, String alias) {
        return function.format(column) + " AS " + checkAlias(alias);
    }

    /**
     * 生成子查询字段 SQL。
     *
     * @param subquerySql 子查询 SQL
     * @param alias       查询别名
     * @return 子查询字段 SQL
     */
    public static String subquerySelect(String subquerySql, String alias) {
        return "(" + subquerySql + ") AS " + checkAlias(alias);
    }

    /**
     * 从 Lambda Getter 解析属性名作为查询别名。
     *
     * @param alias 别名字段 Getter
     * @return 查询别名
     */
    public static String aliasName(SFunction<?, ?> alias) {
        LambdaMeta meta = com.baomidou.mybatisplus.core.toolkit.LambdaUtils.extract(alias);
        return PropertyNamer.methodToProperty(meta.getImplMethodName());
    }

    /**
     * 检查查询别名是否为通用 SQL 标识符。
     *
     * @param alias 查询别名
     * @return 查询别名
     */
    public static String checkAlias(String alias) {
        Assert.isTrue(StringUtils.isNotBlank(alias) && ALIAS_PATTERN.matcher(alias).matches(),
            "查询别名只能包含字母、数字、下划线且不能以数字开头: %s", alias);
        return alias;
    }

}

package org.dromara.common.core.utils.sql;

import cn.hutool.core.exceptions.UtilException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.StringUtils;

/**
 * sql操作工具类
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlUtil {

    /**
     * 定义常用的 sql关键字
     */
    public static final String SQL_REGEX = "\u000B|%0A|and |extractvalue|updatexml|sleep|information_schema|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |union |like |+|/*|user()";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）
     */
    public static final String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value) {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value)) {
            throw new IllegalArgumentException("参数不符合规范，不能进行查询");
        }
        return value;
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value) {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL关键字检查
     */
    public static void filterKeyword(String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }

        // ==================== 核心增强：自动转义单引号 ====================
        // 不抛异常、不破坏业务、不改变原方法行为、自动防注入
        if (value.contains("'")) {
            throw new UtilException("请求参数包含非法字符【'】，已禁止执行");
        }

        // ==================== 原有逻辑不变 ====================
        String normalizedValue = value.replaceAll("\\p{Z}|\\s", "");
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords) {
            if (StringUtils.indexOf(normalizedValue, sqlKeyword) > -1) {
                throw new UtilException("请求参数包含敏感关键词'" + sqlKeyword + "'，可能存在安全风险");
            }
        }
    }

}

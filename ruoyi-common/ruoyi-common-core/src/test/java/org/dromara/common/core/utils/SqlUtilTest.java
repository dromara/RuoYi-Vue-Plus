package org.dromara.common.core.utils;

import cn.hutool.core.exceptions.UtilException;
import org.dromara.common.core.utils.sql.SqlUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SqlUtil 单元测试")
class SqlUtilTest {

    /**
     * 验证合法字段、逗号和表字段表达式可以通过排序校验。
     */
    @Test
    @DisplayName("接受合法的多字段排序表达式")
    void escapeOrderBySqlShouldAcceptSafeColumns() {
        assertEquals("user_name,create_time", SqlUtil.escapeOrderBySql("user_name,create_time"));
        assertTrue(SqlUtil.isValidOrderBySql("table_name.column_name"));
    }

    /**
     * 验证包含 SQL 分隔符等非法字符的排序参数会被拒绝。
     */
    @Test
    @DisplayName("拒绝包含非法字符的排序表达式")
    void escapeOrderBySqlShouldRejectUnsafeCharacters() {
        assertThrows(IllegalArgumentException.class,
            () -> SqlUtil.escapeOrderBySql("create_time desc;drop table sys_user"));
    }

    /**
     * 验证单引号、关键字及大小写空白变体无法绕过过滤。
     */
    @Test
    @DisplayName("拒绝单引号和 SQL 敏感关键词")
    void filterKeywordShouldRejectRiskyInput() {
        assertThrows(UtilException.class, () -> SqlUtil.filterKeyword("name='admin'"));
        assertThrows(UtilException.class, () -> SqlUtil.filterKeyword("union select password"));
        assertThrows(UtilException.class, () -> SqlUtil.filterKeyword("UNION\tSELECT password"));
    }

    /**
     * 验证普通业务文本和空值不会被误判为 SQL 注入。
     */
    @Test
    @DisplayName("允许普通业务查询文本")
    void filterKeywordShouldAcceptRegularText() {
        assertDoesNotThrow(() -> SqlUtil.filterKeyword("normal-business-value"));
        assertDoesNotThrow(() -> SqlUtil.filterKeyword(null));
    }

}

package org.dromara.gen.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RegExUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.gen.config.properties.GenProperties;
import org.dromara.gen.constant.GenConstants;
import org.dromara.gen.domain.GenTable;
import org.dromara.gen.domain.GenTableColumn;

import java.util.Arrays;

/**
 * 代码生成器 工具类
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenUtils {

    private final static GenProperties PROPERTIES = SpringUtils.getBean(GenProperties.class);

    /**
     * 初始化表信息
     *
     * @param genTable 待初始化的业务表对象
     */
    public static void initTable(GenTable genTable) {
        genTable.setClassName(convertClassName(genTable.getTableName()));
        genTable.setPackageName(PROPERTIES.getPackageName());
        genTable.setModuleName(getModuleName(PROPERTIES.getPackageName()));
        genTable.setBusinessName(getBusinessName(genTable.getTableName()));
        genTable.setFunctionName(replaceText(genTable.getTableComment()));
        genTable.setFunctionAuthor(PROPERTIES.getAuthor());
        genTable.setCreateTime(null);
        genTable.setUpdateTime(null);
    }

    /**
     * 初始化列属性字段
     *
     * @param column 待初始化的列对象
     * @param table  所属业务表对象
     */
    public static void initColumnField(GenTableColumn column, GenTable table) {
        String dataType = getDbType(column.getColumnType()).toLowerCase();
        // 统一转小写 避免有些数据库默认大写问题 如果需要特别书写方式 请在实体类增加注解标注别名
        String columnName = column.getColumnName().toLowerCase();
        Integer columnLength = getColumnLength(column.getColumnType());
        Integer columnScale = getColumnScale(column.getColumnType());
        column.setTableId(table.getTableId());
        column.setCreateTime(null);
        column.setUpdateTime(null);
        // 设置java字段名
        column.setJavaField(StringUtils.toCamelCase(columnName));
        // 设置默认类型
        column.setJavaType(GenConstants.TYPE_STRING);
        column.setQueryType(GenConstants.QUERY_EQ);

        if (arraysContains(GenConstants.COLUMNTYPE_STR, dataType) || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType)) {
            // 字符串长度超过500设置为文本域
            String htmlType = columnLength >= 500 || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType) ? GenConstants.HTML_TEXTAREA : GenConstants.HTML_INPUT;
            if (isBooleanColumn(dataType, columnLength, columnScale, columnName)) {
                column.setJavaType(GenConstants.TYPE_BOOLEAN);
                htmlType = GenConstants.HTML_SWITCH;
            }
            column.setHtmlType(htmlType);
        } else if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType)) {
            column.setJavaType(GenConstants.TYPE_DATE);
            column.setHtmlType(GenConstants.HTML_DATETIME);
        } else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType)) {
            column.setJavaType(resolveNumberJavaType(dataType, columnLength, columnScale, columnName));
            column.setHtmlType(GenConstants.TYPE_BOOLEAN.equals(column.getJavaType()) ? GenConstants.HTML_SWITCH : GenConstants.HTML_INPUT_NUMBER);
        }

        // BO对象 默认插入勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_ADD, columnName) && !column.isPk()) {
            column.setIsInsert(GenConstants.REQUIRE);
        }
        // BO对象 默认编辑勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName)) {
            column.setIsEdit(GenConstants.REQUIRE);
        }
        // VO对象 默认返回勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_LIST, columnName)) {
            column.setIsList(GenConstants.REQUIRE);
        }
        // BO对象 默认查询勾选
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_QUERY, columnName) && !column.isPk()) {
            column.setIsQuery(GenConstants.REQUIRE);
        }

        // 查询字段类型
        if (StringUtils.endsWithIgnoreCase(columnName, "name")) {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        if (GenConstants.HTML_DATETIME.equals(column.getHtmlType()) && column.isQuery()) {
            column.setQueryType(GenConstants.QUERY_BETWEEN);
        }
        // 状态字段设置单选框
        if (isSwitchColumn(columnName) || GenConstants.TYPE_BOOLEAN.equals(column.getJavaType())) {
            column.setHtmlType(GenConstants.HTML_SWITCH);
        }
        // 状态字段设置单选框/开关
        if (StringUtils.endsWithIgnoreCase(columnName, "status")) {
            column.setHtmlType(GenConstants.TYPE_BOOLEAN.equals(column.getJavaType()) ? GenConstants.HTML_SWITCH : GenConstants.HTML_RADIO);
        }
        // 类型&性别字段设置下拉框
        else if (StringUtils.endsWithIgnoreCase(columnName, "type")
            || StringUtils.endsWithIgnoreCase(columnName, "sex")) {
            column.setHtmlType(GenConstants.HTML_SELECT);
        }
        // 排序字段设置数字输入控件
        else if (isSortColumn(columnName)) {
            column.setHtmlType(GenConstants.HTML_INPUT_NUMBER);
        }
        // 图片字段设置图片上传控件
        else if (StringUtils.endsWithAny(columnName, "image", "avatar", "logo", "picture")) {
            column.setHtmlType(GenConstants.HTML_IMAGE_UPLOAD);
        }
        // 文件字段设置文件上传控件
        else if (StringUtils.endsWithAny(columnName, "file", "attachment")) {
            column.setHtmlType(GenConstants.HTML_FILE_UPLOAD);
        }
        // 备注描述类字段设置文本域
        else if (StringUtils.endsWithAny(columnName, "remark", "description", "desc", "note")) {
            column.setHtmlType(GenConstants.HTML_TEXTAREA);
        }
        // 内容字段设置富文本控件
        else if (StringUtils.endsWithAny(columnName, "content", "html", "body")) {
            column.setHtmlType(GenConstants.HTML_EDITOR);
        }
    }

    private static String resolveNumberJavaType(String dataType, Integer columnLength, Integer columnScale, String columnName) {
        if (isBooleanColumn(dataType, columnLength, columnScale, columnName)) {
            return GenConstants.TYPE_BOOLEAN;
        }
        if (arraysContains(new String[]{"decimal", "numeric", "money", "smallmoney"}, dataType)) {
            return columnScale > 0 ? GenConstants.TYPE_BIGDECIMAL : resolveIntegerJavaType(columnLength);
        }
        if (arraysContains(new String[]{"float", "float4", "float8", "double", "real", "double precision"}, dataType)) {
            return GenConstants.TYPE_DOUBLE;
        }
        if (arraysContains(new String[]{"bigint", "int8", "bigserial"}, dataType)) {
            return GenConstants.TYPE_LONG;
        }
        if (arraysContains(new String[]{"smallint", "mediumint", "int", "int2", "int4", "integer", "smallserial", "serial"}, dataType)) {
            return GenConstants.TYPE_INTEGER;
        }
        if (StringUtils.equals(dataType, "number")) {
            if (columnScale > 0) {
                return GenConstants.TYPE_BIGDECIMAL;
            }
            return resolveIntegerJavaType(columnLength);
        }
        return GenConstants.TYPE_LONG;
    }

    private static String resolveIntegerJavaType(Integer columnLength) {
        if (columnLength > 0 && columnLength <= 9) {
            return GenConstants.TYPE_INTEGER;
        }
        return GenConstants.TYPE_LONG;
    }

    private static boolean isBooleanColumn(String dataType, Integer columnLength, Integer columnScale, String columnName) {
        if (columnScale > 0) {
            return false;
        }
        if (StringUtils.equalsAny(dataType, "bit", "boolean", "bool")) {
            return true;
        }
        if (StringUtils.equalsAny(dataType, "tinyint", "number", "numeric", "decimal", "char", "nchar")
            && columnLength == 1 && isSwitchColumn(columnName)) {
            return true;
        }
        return false;
    }

    private static boolean isSwitchColumn(String columnName) {
        return StringUtils.endsWithAny(columnName, "status", "flag", "enabled", "disabled", "available", "visible")
            || columnName.startsWith("is_")
            || columnName.startsWith("has_")
            || columnName.startsWith("enable_")
            || columnName.startsWith("disable_");
    }

    private static boolean isSortColumn(String columnName) {
        return StringUtils.endsWithAny(columnName, "sort", "order_num", "order", "rank", "seq", "sequence");
    }

    /**
     * 校验数组是否包含指定值
     *
     * @param arr         数组
     * @param targetValue 值
     * @return 是否包含
     */
    public static boolean arraysContains(String[] arr, String targetValue) {
        return Arrays.asList(arr).contains(targetValue);
    }

    /**
     * 获取模块名
     *
     * @param packageName 包名
     * @return 模块名
     */
    public static String getModuleName(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        int nameLength = packageName.length();
        return StringUtils.substring(packageName, lastIndex + 1, nameLength);
    }

    /**
     * 获取业务名
     *
     * @param tableName 表名
     * @return 业务名
     */
    public static String getBusinessName(String tableName) {
        int firstIndex = tableName.indexOf("_");
        int nameLength = tableName.length();
        String businessName = StringUtils.substring(tableName, firstIndex + 1, nameLength);
        businessName = StringUtils.toCamelCase(businessName);
        return businessName;
    }

    /**
     * 表名转换成Java类名
     *
     * @param tableName 表名称
     * @return 类名
     */
    public static String convertClassName(String tableName) {
        boolean autoRemovePre = PROPERTIES.isAutoRemovePre();
        String tablePrefix = PROPERTIES.getTablePrefix();
        if (autoRemovePre && StringUtils.isNotEmpty(tablePrefix)) {
            String[] searchList = StringUtils.split(tablePrefix, StringUtils.SEPARATOR);
            tableName = replaceFirst(tableName, searchList);
        }
        return StringUtils.convertToCamelCase(tableName);
    }

    /**
     * 批量替换前缀
     *
     * @param replacementm 替换值
     * @param searchList   替换列表
     * @return 去除命中前缀后的字符串
     */
    public static String replaceFirst(String replacementm, String[] searchList) {
        String text = replacementm;
        for (String searchString : searchList) {
            if (replacementm.startsWith(searchString)) {
                text = StringUtils.removeStart(replacementm, searchString);
                break;
            }
        }
        return text;
    }

    /**
     * 关键字替换
     *
     * @param text 需要被替换的名字
     * @return 替换后的名字
     */
    public static String replaceText(String text) {
        return RegExUtils.replaceAll(text, "(?:表|若依)", "");
    }

    /**
     * 获取数据库类型字段
     *
     * @param columnType 列类型
     * @return 截取后的列类型
     */
    public static String getDbType(String columnType) {
        if (StringUtils.indexOf(columnType, "(") > 0) {
            return StringUtils.substringBefore(columnType, "(");
        } else {
            return columnType;
        }
    }

    /**
     * 获取字段长度
     *
     * @param columnType 列类型
     * @return 字段长度，未声明长度时返回 0
     */
    public static Integer getColumnLength(String columnType) {
        if (StringUtils.indexOf(columnType, "(") > 0) {
            String length = StringUtils.substringBetween(columnType, "(", ")").trim();
            // 处理 decimal(10,2) 这类带精度的类型，只取长度部分
            if (length.contains(",")) {
                length = StringUtils.substringBefore(length, ",").trim();
            }
            return Integer.valueOf(length);
        } else {
            return 0;
        }
    }

    /**
     * 获取字段精度
     *
     * @param columnType 列类型
     * @return 字段精度，未声明精度时返回 0
     */
    public static Integer getColumnScale(String columnType) {
        if (StringUtils.indexOf(columnType, "(") > 0) {
            String length = StringUtils.substringBetween(columnType, "(", ")").trim();
            if (length.contains(",")) {
                return Integer.valueOf(StringUtils.substringAfter(length, ",").trim());
            }
        }
        return 0;
    }
}

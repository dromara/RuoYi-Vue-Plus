package org.dromara.gen.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.gen.constant.GenConstants;

/**
 * 代码生成业务字段表 gen_table_column
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_table_column")
public class GenTableColumn extends BaseEntity {

    /**
     * 编号
     */
    @TableId(value = "column_id")
    private Long columnId;

    /**
     * 归属表编号
     */
    private Long tableId;

    /**
     * 列名称
     */
    private String columnName;

    /**
     * 列描述
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String columnComment;

    /**
     * 列类型
     */
    private String columnType;

    /**
     * JAVA类型
     */
    private String javaType;

    /**
     * JAVA字段名
     */
    @NotBlank(message = "Java属性不能为空")
    private String javaField;

    /**
     * 是否主键（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isPk;

    /**
     * 是否自增（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isIncrement;

    /**
     * 是否必填（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isRequired;

    /**
     * 是否为插入字段（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isInsert;

    /**
     * 是否编辑字段（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isEdit;

    /**
     * 是否列表字段（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isList;

    /**
     * 是否查询字段（1是）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String isQuery;

    /**
     * 查询方式（EQ等于、NE不等于、GT大于、LT小于、LIKE模糊、BETWEEN范围）
     */
    private String queryType;

    /**
     * 显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件、image图片上传控件、upload文件上传控件、editor富文本控件）
     */
    private String htmlType;

    /**
     * 字典类型
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, jdbcType = JdbcType.VARCHAR)
    private String dictType;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 获取首字母大写后的 Java 字段名。
     *
     * @return 首字母大写的 Java 字段名
     */
    public String getCapJavaField() {
        return StringUtils.capitalize(javaField);
    }

    /**
     * 判断当前列是否为主键列。
     *
     * @return 主键列返回 {@code true}
     */
    public boolean isPk() {
        return isPk(this.isPk);
    }

    /**
     * 根据标识判断是否为主键列。
     *
     * @param isPk 主键标识
     * @return 主键列返回 {@code true}
     */
    public boolean isPk(String isPk) {
        return isPk != null && StringUtils.equals("1", isPk);
    }

    /**
     * 判断当前列是否为自增列。
     *
     * @return 自增列返回 {@code true}
     */
    public boolean isIncrement() {
        return isIncrement(this.isIncrement);
    }

    /**
     * 根据标识判断是否为自增列。
     *
     * @param isIncrement 自增标识
     * @return 自增列返回 {@code true}
     */
    public boolean isIncrement(String isIncrement) {
        return isIncrement != null && StringUtils.equals("1", isIncrement);
    }

    /**
     * 判断当前列是否必填。
     *
     * @return 必填返回 {@code true}
     */
    public boolean isRequired() {
        return isRequired(this.isRequired);
    }

    /**
     * 根据标识判断当前列是否必填。
     *
     * @param isRequired 必填标识
     * @return 必填返回 {@code true}
     */
    public boolean isRequired(String isRequired) {
        return isRequired != null && StringUtils.equals("1", isRequired);
    }

    /**
     * 判断当前列是否参与新增。
     *
     * @return 参与新增返回 {@code true}
     */
    public boolean isInsert() {
        return isInsert(this.isInsert);
    }

    /**
     * 根据标识判断当前列是否参与新增。
     *
     * @param isInsert 新增标识
     * @return 参与新增返回 {@code true}
     */
    public boolean isInsert(String isInsert) {
        return isInsert != null && StringUtils.equals("1", isInsert);
    }

    /**
     * 判断当前列是否参与编辑。
     *
     * @return 参与编辑返回 {@code true}
     */
    public boolean isEdit() {
        return isEdit(this.isEdit);
    }

    /**
     * 根据标识判断当前列是否参与编辑。
     *
     * @param isEdit 编辑标识
     * @return 参与编辑返回 {@code true}
     */
    public boolean isEdit(String isEdit) {
        return isEdit != null && StringUtils.equals("1", isEdit);
    }

    /**
     * 判断当前列是否参与列表展示。
     *
     * @return 参与列表展示返回 {@code true}
     */
    public boolean isList() {
        return isList(this.isList);
    }

    /**
     * 根据标识判断当前列是否参与列表展示。
     *
     * @param isList 列表展示标识
     * @return 参与列表展示返回 {@code true}
     */
    public boolean isList(String isList) {
        return isList != null && StringUtils.equals("1", isList);
    }

    /**
     * 判断当前列是否参与查询条件。
     *
     * @return 参与查询返回 {@code true}
     */
    public boolean isQuery() {
        return isQuery(this.isQuery);
    }

    /**
     * 根据标识判断当前列是否参与查询条件。
     *
     * @param isQuery 查询标识
     * @return 参与查询返回 {@code true}
     */
    public boolean isQuery(String isQuery) {
        return isQuery != null && StringUtils.equals("1", isQuery);
    }

    /**
     * 判断当前列是否为基类公共字段。
     *
     * @return 基类公共字段返回 {@code true}
     */
    public boolean isSuperColumn() {
        return isSuperColumn(this.javaField);
    }

    /**
     * 根据字段名判断是否为基类公共字段。
     *
     * @param javaField Java 字段名
     * @return 基类公共字段返回 {@code true}
     */
    public static boolean isSuperColumn(String javaField) {
        return StringUtils.equalsAnyIgnoreCase(javaField,
            // BaseEntity
            "createBy", "createTime", "updateBy", "updateTime",
            // TreeEntity
            "parentName", "parentId");
    }

    /**
     * 判断当前列是否属于生成页面需要保留的白名单字段。
     *
     * @return 白名单字段返回 {@code true}
     */
    public boolean isUsableColumn() {
        return isUsableColumn(javaField);
    }

    /**
     * 根据字段名判断是否属于生成页面需要保留的白名单字段。
     *
     * @param javaField Java 字段名
     * @return 白名单字段返回 {@code true}
     */
    public static boolean isUsableColumn(String javaField) {
        // isSuperColumn()中的名单用于避免生成多余Domain属性，若某些属性在生成页面时需要用到不能忽略，则放在此处白名单
        return StringUtils.equalsAnyIgnoreCase(javaField, "parentId", "orderNum", "remark");
    }

    /**
     * 判断当前列是否需要显式声明 MP 字段映射。
     *
     * @return 字段名与下划线列名不一致时返回 {@code true}
     */
    public boolean isNeedTableField() {
        if (StringUtils.isAnyBlank(this.columnName, this.javaField)) {
            return false;
        }
        return !StringUtils.equalsIgnoreCase(this.columnName, StringUtils.toUnderScoreCase(this.javaField));
    }

    /**
     * 判断当前列是否属于字典控件列。
     *
     * @return 仅当已配置字典类型且显示类型支持字典时返回 {@code true}
     */
    public boolean isDictColumn() {
        return StringUtils.isNotBlank(this.dictType) && StringUtils.equalsAny(this.htmlType,
            GenConstants.HTML_SELECT,
            GenConstants.HTML_RADIO,
            GenConstants.HTML_CHECKBOX,
            GenConstants.HTML_SWITCH);
    }

    /**
     * 从字段注释中解析字典读转换表达式。
     *
     * @return 形如 `0=男,1=女` 的转换表达式，无法解析时返回原始注释
     */
    public String readConverterExp() {
        String remarks = StringUtils.substringBetween(this.columnComment, "（", "）");
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotEmpty(remarks)) {
            for (String value : remarks.split(" ")) {
                if (StringUtils.isNotEmpty(value)) {
                    Object startStr = value.subSequence(0, 1);
                    String endStr = value.substring(1);
                    sb.append(StringUtils.EMPTY).append(startStr).append("=").append(endStr).append(StringUtils.SEPARATOR);
                }
            }
            return sb.deleteCharAt(sb.length() - 1).toString();
        } else {
            return this.columnComment;
        }
    }
}

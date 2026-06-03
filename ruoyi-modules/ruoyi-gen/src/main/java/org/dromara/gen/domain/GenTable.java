package org.dromara.gen.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.gen.constant.GenConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务表 gen_table
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_table")
public class GenTable extends BaseEntity {

    /**
     * 编号
     */
    @TableId(value = "table_id")
    private Long tableId;

    /**
     * 数据源名称
     */
    @NotBlank(message = "数据源名称不能为空")
    private String dataName;

    /**
     * 表名称
     */
    @NotBlank(message = "表名称不能为空")
    private String tableName;

    /**
     * 表描述
     */
    @NotBlank(message = "表描述不能为空")
    private String tableComment;

    /**
     * 实体类名称(首字母大写)
     */
    @NotBlank(message = "实体类名称不能为空")
    private String className;

    /**
     * 使用的模板（crud单表操作 tree树表操作）
     */
    private String tplCategory;

    /**
     * 前端模板类型，对应 vm 下的模板目录
     */
    private String frontendType;

    /**
     * 生成包路径
     */
    @NotBlank(message = "生成包路径不能为空")
    private String packageName;

    /**
     * 生成模块名
     */
    @NotBlank(message = "生成模块名不能为空")
    private String moduleName;

    /**
     * 生成业务名
     */
    @NotBlank(message = "生成业务名不能为空")
    private String businessName;

    /**
     * 生成功能名
     */
    @NotBlank(message = "生成功能名不能为空")
    private String functionName;

    /**
     * 生成作者
     */
    @NotBlank(message = "作者不能为空")
    private String functionAuthor;

    /**
     * 主键信息
     */
    @TableField(exist = false)
    private GenTableColumn pkColumn;

    /**
     * 表列信息
     */
    @Valid
    @TableField(exist = false)
    private List<GenTableColumn> columns;

    /**
     * 其它生成选项
     */
    private String options;

    /**
     * 备注
     */
    private String remark;

    /**
     * 树编码字段
     */
    @TableField(exist = false)
    private String treeCode;

    /**
     * 树父编码字段
     */
    @TableField(exist = false)
    private String treeParentCode;

    /**
     * 树名称字段
     */
    @TableField(exist = false)
    private String treeName;

    /**
     * 菜单id列表
     */
    @TableField(exist = false)
    private List<Long> menuIds;

    /**
     * 上级菜单ID字段
     */
    @TableField(exist = false)
    private Long parentMenuId;

    /**
     * 上级菜单名称字段
     */
    @TableField(exist = false)
    private String parentMenuName;

    /**
     * 是否启用导出
     */
    @TableField(exist = false)
    private Boolean enableExport;

    /**
     * 是否启用状态切换
     */
    @TableField(exist = false)
    private Boolean enableStatus;

    /**
     * 状态字段
     */
    @TableField(exist = false)
    private String statusField;

    /**
     * 是否启用组合唯一校验
     */
    @TableField(exist = false)
    private Boolean enableUnique;

    /**
     * 组合唯一字段
     */
    @TableField(exist = false)
    private List<String> uniqueFields;

    /**
     * 是否启用排序调整
     */
    @TableField(exist = false)
    private Boolean enableSort;

    /**
     * 排序字段
     */
    @TableField(exist = false)
    private String sortField;

    /**
     * 树根节点值
     */
    @TableField(exist = false)
    private String treeRootValue;

    /**
     * 树祖级字段
     */
    @TableField(exist = false)
    private String treeAncestorsField;

    /**
     * 树排序字段
     */
    @TableField(exist = false)
    private String treeOrderField;

    /**
     * 请求参数
     */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

    /**
     * 判断当前业务表是否采用树表模板。
     *
     * @return 树表模板返回 {@code true}
     */
    public boolean isTree() {
        return isTree(this.tplCategory);
    }

    /**
     * 根据模板分类判断是否为树表模板。
     *
     * @param tplCategory 模板分类
     * @return 树表模板返回 {@code true}
     */
    public static boolean isTree(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_TREE, tplCategory);
    }

    /**
     * 判断当前业务表是否采用普通 CRUD 模板。
     *
     * @return 普通 CRUD 模板返回 {@code true}
     */
    public boolean isCrud() {
        return isCrud(this.tplCategory);
    }

    /**
     * 根据模板分类判断是否为普通 CRUD 模板。
     *
     * @param tplCategory 模板分类
     * @return 普通 CRUD 模板返回 {@code true}
     */
    public static boolean isCrud(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_CRUD, tplCategory);
    }

    /**
     * 判断指定 Java 字段是否属于基类公共字段。
     *
     * @param javaField Java 字段名
     * @return 基类公共字段返回 {@code true}
     */
    public boolean isSuperColumn(String javaField) {
        return isSuperColumn(this.tplCategory, javaField);
    }

    /**
     * 根据模板分类与字段名判断是否属于基类公共字段。
     *
     * @param tplCategory 模板分类
     * @param javaField   Java 字段名
     * @return 基类公共字段返回 {@code true}
     */
    public static boolean isSuperColumn(String tplCategory, String javaField) {
        return StringUtils.equalsAnyIgnoreCase(javaField, GenConstants.BASE_ENTITY);
    }
}

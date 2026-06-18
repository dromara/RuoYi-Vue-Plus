package org.dromara.gen.constant;

import cn.hutool.core.collection.CollUtil;

import java.util.Set;

/**
 * 代码生成通用常量
 *
 * @author ruoyi
 */
public interface GenConstants {
    /**
     * 单表（增删改查）
     */
    String TPL_CRUD = "crud";

    /**
     * 树表（增删改查）
     */
    String TPL_TREE = "tree";

    /**
     * 树编码字段
     */
    String TREE_CODE = "treeCode";

    /**
     * 树父编码字段
     */
    String TREE_PARENT_CODE = "treeParentCode";

    /**
     * 树名称字段
     */
    String TREE_NAME = "treeName";

    /**
     * 上级菜单ID字段
     */
    String PARENT_MENU_ID = "parentMenuId";

    /**
     * 上级菜单名称字段
     */
    String PARENT_MENU_NAME = "parentMenuName";

    /**
     * 是否启用导出能力
     */
    String ENABLE_EXPORT = "enableExport";

    /**
     * 是否启用状态切换能力
     */
    String ENABLE_STATUS = "enableStatus";

    /**
     * 状态字段
     */
    String STATUS_FIELD = "statusField";

    /**
     * 是否启用组合唯一校验
     */
    String ENABLE_UNIQUE = "enableUnique";

    /**
     * 组合唯一字段
     */
    String UNIQUE_FIELDS = "uniqueFields";

    /**
     * 是否启用排序调整能力
     */
    String ENABLE_SORT = "enableSort";

    /**
     * 排序字段
     */
    String SORT_FIELD = "sortField";

    /**
     * 默认前端模板类型，对应模板根目录下的 vue 目录。
     */
    String FRONTEND_TYPE_VUE = "vue";

    /**
     * 树根节点值
     */
    String TREE_ROOT_VALUE = "treeRootValue";

    /**
     * 树祖级字段
     */
    String TREE_ANCESTORS = "treeAncestors";

    /**
     * 树排序字段
     */
    String TREE_ORDER_FIELD = "treeOrderField";

    /**
     * 数据库字符串类型
     */
    String[] COLUMNTYPE_STR = {"char", "varchar", "enum", "set", "nchar", "nvarchar", "varchar2", "nvarchar2"};

    /**
     * 数据库文本类型
     */
    String[] COLUMNTYPE_TEXT = {"tinytext", "text", "mediumtext", "longtext", "binary", "varbinary", "blob",
        "ntext", "image", "bytea"};

    /**
     * 数据库时间类型
     */
    String[] COLUMNTYPE_TIME = {"datetime", "time", "date", "timestamp", "year", "interval",
        "smalldatetime", "datetime2", "datetimeoffset", "timestamptz"};

    /**
     * 数据库数字类型
     */
    String[] COLUMNTYPE_NUMBER = {"tinyint", "smallint", "mediumint", "int", "int2", "int4", "int8", "number", "integer",
        "bit", "bigint", "float", "float4", "float8", "double", "decimal", "numeric", "real", "double precision",
        "smallserial", "serial", "bigserial", "money", "smallmoney"};

    /**
     * BO对象 不需要添加字段
     */
    String[] COLUMNNAME_NOT_ADD = {"create_dept", "create_by", "create_time", "del_flag", "update_by",
        "update_time", "version"};

    /**
     * BO对象 不需要编辑字段
     */
    String[] COLUMNNAME_NOT_EDIT = {"create_dept", "create_by", "create_time", "del_flag", "update_by",
        "update_time", "version"};

    /**
     * VO对象 不需要返回字段
     */
    String[] COLUMNNAME_NOT_LIST = {"create_dept", "create_by", "create_time", "del_flag", "update_by",
        "update_time", "version"};

    /**
     * BO对象 不需要查询字段
     */
    String[] COLUMNNAME_NOT_QUERY = {"id", "create_dept", "create_by", "create_time", "del_flag", "update_by",
        "update_time", "remark", "version"};

    /**
     * Entity基类字段
     */
    String[] BASE_ENTITY = {"createDept", "createBy", "createTime", "updateBy", "updateTime"};

    /**
     * 文本框
     */
    String HTML_INPUT = "input";

    /**
     * 文本域
     */
    String HTML_TEXTAREA = "textarea";

    /**
     * 下拉框
     */
    String HTML_SELECT = "select";

    /**
     * 单选框
     */
    String HTML_RADIO = "radio";

    /**
     * 复选框
     */
    String HTML_CHECKBOX = "checkbox";

    /**
     * 日期控件
     */
    String HTML_DATETIME = "datetime";

    /**
     * 开关控件
     */
    String HTML_SWITCH = "switch";

    /**
     * 数字输入控件
     */
    String HTML_INPUT_NUMBER = "inputNumber";

    /**
     * 图片上传控件
     */
    String HTML_IMAGE_UPLOAD = "imageUpload";

    /**
     * 文件上传控件
     */
    String HTML_FILE_UPLOAD = "fileUpload";

    /**
     * 富文本控件
     */
    String HTML_EDITOR = "editor";

    /**
     * 字符串类型
     */
    String TYPE_STRING = "String";

    /**
     * 整型
     */
    String TYPE_INTEGER = "Integer";

    /**
     * 长整型
     */
    String TYPE_LONG = "Long";

    /**
     * 浮点型
     */
    String TYPE_DOUBLE = "Double";

    /**
     * 高精度计算类型
     */
    String TYPE_BIGDECIMAL = "BigDecimal";

    /**
     * 布尔类型
     */
    String TYPE_BOOLEAN = "Boolean";

    /**
     * 时间类型
     */
    String TYPE_DATE = "LocalDateTime";

    /**
     * 模糊查询
     */
    String QUERY_LIKE = "LIKE";

    /**
     * 相等查询
     */
    String QUERY_EQ = "EQ";
    /**
     * 范围查询
     */
    String QUERY_BETWEEN = "BETWEEN";

    /**
     * 必填标识，对应前端表单规则中的必填字段配置。
     */
    String REQUIRE = "1";

    // 模板根路径与后缀
    String TEMPLATE_ROOT_PATH = "fm";
    String TEMPLATE_FILE_SUFFIX = ".ftl";
    String TEMPLATE_RESOURCE_PREFIX = "classpath*:";
    String TEMPLATE_PAGE_PATTERN_SUFFIX = ".*" + TEMPLATE_FILE_SUFFIX;
    // 后端源码模板
    String JAVA_TEMPLATE_ROOT_PATH = TEMPLATE_ROOT_PATH + "/java";
    String JAVA_DOMAIN_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/domain.java" + TEMPLATE_FILE_SUFFIX;
    String JAVA_VO_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/vo.java" + TEMPLATE_FILE_SUFFIX;
    String JAVA_BO_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/bo.java" + TEMPLATE_FILE_SUFFIX;
    String JAVA_MAPPER_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/mapper.java" + TEMPLATE_FILE_SUFFIX;
    String JAVA_SERVICE_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/service.java" + TEMPLATE_FILE_SUFFIX;
    String JAVA_SERVICE_IMPL_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/serviceImpl.java" + TEMPLATE_FILE_SUFFIX;
    String JAVA_CONTROLLER_TEMPLATE_PATH = JAVA_TEMPLATE_ROOT_PATH + "/controller.java" + TEMPLATE_FILE_SUFFIX;
    // MyBatis MapperXML 模板
    String XML_TEMPLATE_ROOT_PATH = TEMPLATE_ROOT_PATH + "/xml";
    String XML_MAPPER_TEMPLATE_PATH = XML_TEMPLATE_ROOT_PATH + "/mapper.xml" + TEMPLATE_FILE_SUFFIX;
    // 前端源码模板
    String FRONTEND_API_TEMPLATE_NAME = "api.ts" + TEMPLATE_FILE_SUFFIX;
    String FRONTEND_TYPES_TEMPLATE_NAME = "types.ts" + TEMPLATE_FILE_SUFFIX;
    String FRONTEND_INDEX_TEMPLATE_PREFIX = "index";
    String FRONTEND_INDEX_TREE_TEMPLATE_PREFIX = "index-tree";
    // 数据库SQL模板
    String SQL_TEMPLATE_ROOT_PATH = TEMPLATE_ROOT_PATH + "/sql";
    String SQL_ORACLE_TEMPLATE_PATH = SQL_TEMPLATE_ROOT_PATH + "/oracle.sql" + TEMPLATE_FILE_SUFFIX;
    String SQL_POSTGRES_TEMPLATE_PATH = SQL_TEMPLATE_ROOT_PATH + "/postgres.sql" + TEMPLATE_FILE_SUFFIX;
    String SQL_SQLSERVER_TEMPLATE_PATH = SQL_TEMPLATE_ROOT_PATH + "/sqlserver.sql" + TEMPLATE_FILE_SUFFIX;
    String SQL_MYSQL_TEMPLATE_PATH = SQL_TEMPLATE_ROOT_PATH + "/mysql.sql" + TEMPLATE_FILE_SUFFIX;

    /**
     * 固定模板路径集合，前端模板按 frontendType 动态解析。
     */
    Set<String> TEMPLATE_PATHS = CollUtil.newHashSet(
        JAVA_DOMAIN_TEMPLATE_PATH
        , JAVA_VO_TEMPLATE_PATH
        , JAVA_BO_TEMPLATE_PATH
        , JAVA_MAPPER_TEMPLATE_PATH
        , JAVA_SERVICE_TEMPLATE_PATH
        , JAVA_SERVICE_IMPL_TEMPLATE_PATH
        , JAVA_CONTROLLER_TEMPLATE_PATH
        , XML_MAPPER_TEMPLATE_PATH
        , SQL_ORACLE_TEMPLATE_PATH
        , SQL_POSTGRES_TEMPLATE_PATH
        , SQL_SQLSERVER_TEMPLATE_PATH
        , SQL_MYSQL_TEMPLATE_PATH
    );
}

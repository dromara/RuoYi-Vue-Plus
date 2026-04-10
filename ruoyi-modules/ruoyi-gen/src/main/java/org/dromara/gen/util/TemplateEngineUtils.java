package org.dromara.gen.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.enums.DataBaseType;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.dromara.gen.config.properties.GenProperties;
import org.dromara.gen.constant.GenConstants;
import org.dromara.gen.domain.GenTable;
import org.dromara.gen.domain.GenTableColumn;
import org.dromara.gen.util.template.PathNamedTemplate;

import java.util.*;
import java.util.function.Consumer;

import static org.dromara.gen.constant.GenConstants.TS_TYPES_TEMPLATE_PATH;

/**
 * 模板引擎工具
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TemplateEngineUtils {

    /**
     * 项目空间路径
     */
    private static final String PROJECT_PATH = "main/java";

    /**
     * mybatis空间路径
     */
    private static final String MYBATIS_PATH = "main/resources/mapper";

    /**
     * 默认上级菜单，系统工具
     */
    private static final String DEFAULT_PARENT_MENU_ID = "3";

    // 模板引擎
    private static final TemplateEngine TEMPLATE_ENGINE;
    private static final Map<String, PathNamedTemplate> TEMPLATE_MAPPER;

    static {
        // 模板引擎初始化
        GenProperties properties = SpringUtils.getBean(GenProperties.class);
        TEMPLATE_ENGINE = TemplateUtil.createEngine(properties.getTemplateConfig());
        TEMPLATE_MAPPER = PathNamedTemplate.form(TEMPLATE_ENGINE, GenConstants.TEMPLATE_PATHS);
    }

    /**
     * 构建模板上下文
     *
     * @param contextInit 模板上下文初始化函数
     * @return 模板上下文
     */
    public static Dict buildContext(Consumer<Dict> contextInit) {
        Dict context = new Dict();
        contextInit.accept(context);
        return context;
    }

    /**
     * 构建模板上下文
     *
     * @param genTable 代码生成业务表对象
     * @return 模板上下文
     */
    public static Dict buildContext(GenTable genTable) {
        // 构建上下文
        Dict context = new Dict();
        String moduleName = genTable.getModuleName();
        String businessName = genTable.getBusinessName();
        String packageName = genTable.getPackageName();
        String tplCategory = genTable.getTplCategory();
        String functionName = genTable.getFunctionName();

        context.put("tplCategory", genTable.getTplCategory());
        context.put("tableName", genTable.getTableName());
        context.put("functionName", StringUtils.isNotEmpty(functionName) ? functionName : "【请填写功能名称】");
        context.put("ClassName", genTable.getClassName());
        context.put("className", StringUtils.uncapitalize(genTable.getClassName()));
        context.put("moduleName", moduleName);
        context.put("BusinessName", StringUtils.capitalize(businessName));
        context.put("businessName", businessName);
        context.put("basePackage", getPackagePrefix(packageName));
        context.put("packageName", packageName);
        context.put("author", genTable.getFunctionAuthor());
        context.put("datetime", DateUtils.getDate());
        context.put("pkColumn", genTable.getPkColumn());
        context.put("importList", getImportList(genTable));
        context.put("permissionPrefix", getPermissionPrefix(moduleName, businessName));
        context.put("columns", genTable.getColumns());
        context.put("table", genTable);
        context.put("dicts", getDicts(genTable));
        // 向模板上下文写入菜单相关变量
        String options = genTable.getOptions();
        Dict paramsObj = JsonUtils.parseMap(options);
        String parentMenuId = getParentMenuId(paramsObj);
        context.put("parentMenuId", parentMenuId);

        // 向树形模板上下文写入树字段相关变量
        if (GenConstants.TPL_TREE.equals(tplCategory)) {
            setTreeContext(context, genTable, paramsObj);
        }

        return context;
    }

    /**
     * 向树形模板上下文写入树字段相关变量。
     *
     * @param context    模板上下文
     * @param genTable   代码生成业务表对象
     * @param paramsObj  已解析的 options 参数（避免重复解析）
     */
    public static void setTreeContext(Dict context, GenTable genTable, Dict paramsObj) {
        String treeCode = getTreeCode(paramsObj);
        String treeParentCode = getTreeParentCode(paramsObj);
        String treeName = getTreeName(paramsObj);

        context.put("treeCode", treeCode);
        context.put("treeParentCode", treeParentCode);
        context.put("treeName", treeName);
        String expandTreeName = paramsObj.getStr(GenConstants.TREE_NAME);
        int expandColumn = 0;
        for (GenTableColumn column : genTable.getColumns()) {
            if (column.isList()) {
                expandColumn++;
                if (column.getColumnName().equals(expandTreeName)) {
                    break;
                }
            }
        }
        context.put("expandColumn", expandColumn);
        if (paramsObj.containsKey(GenConstants.TREE_PARENT_CODE)) {
            context.put("tree_parent_code", paramsObj.get(GenConstants.TREE_PARENT_CODE));
        }
        if (paramsObj.containsKey(GenConstants.TREE_NAME)) {
            context.put("tree_name", paramsObj.get(GenConstants.TREE_NAME));
        }
    }

    /**
     * 获取模板信息
     *
     * @return 模板列表
     */
    public static List<PathNamedTemplate> getTemplateList(String tplCategory, String dsName) {
        List<PathNamedTemplate> templates = new ArrayList<>();
        // 后端源码模板
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_DOMAIN_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_VO_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_BO_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_MAPPER_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_SERVICE_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_SERVICE_IMPL_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(GenConstants.JAVA_CONTROLLER_TEMPLATE_PATH));
        // MyBatis MapperXML 模板
        templates.add(TEMPLATE_MAPPER.get(GenConstants.XML_MAPPER_TEMPLATE_PATH));
        // 前端接口源码模板
        templates.add(TEMPLATE_MAPPER.get(GenConstants.TS_API_TEMPLATE_PATH));
        templates.add(TEMPLATE_MAPPER.get(TS_TYPES_TEMPLATE_PATH));
        // 数据库模板
        DataBaseType dataBaseType = DataBaseHelper.getDataBaseType(dsName);
        if (dataBaseType.isOracle()) {
            templates.add(TEMPLATE_MAPPER.get(GenConstants.SQL_ORACLE_TEMPLATE_PATH));
        } else if (dataBaseType.isPostgreSql()) {
            templates.add(TEMPLATE_MAPPER.get(GenConstants.SQL_POSTGRES_TEMPLATE_PATH));
        } else if (dataBaseType.isSqlServer()) {
            templates.add(TEMPLATE_MAPPER.get(GenConstants.SQL_SQLSERVER_TEMPLATE_PATH));
        } else {
            // 默认使用MySQL模板
            templates.add(TEMPLATE_MAPPER.get(GenConstants.SQL_MYSQL_TEMPLATE_PATH));
        }
        // 前端页面源码模板
        if (GenConstants.TPL_CRUD.equals(tplCategory)) {
            templates.add(TEMPLATE_MAPPER.get(GenConstants.VUE_INDEX_TEMPLATE_PATH));
        } else if (GenConstants.TPL_TREE.equals(tplCategory)) {
            templates.add(TEMPLATE_MAPPER.get(GenConstants.VUE_INDEX_TREE_TEMPLATE_PATH));
        }
        return templates;
    }

    /**
     * 获取文件名
     *
     * @param template 模板路径
     * @param genTable 代码生成业务表对象
     * @return 模板对应的目标文件相对路径
     */
    public static String getFileName(String template, GenTable genTable) {
        // 文件名称
        String fileName = "";
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 大写类名
        String className = genTable.getClassName();
        // 业务名称
        String businessName = genTable.getBusinessName();

        String javaPath = PROJECT_PATH + "/" + StringUtils.replace(packageName, ".", "/");
        String mybatisPath = MYBATIS_PATH + "/" + moduleName;
        String vuePath = "vue";
        // templatePath
        // genFilePathFormat
        if (template.contains("domain.java.vm")) {
            fileName = StringUtils.format("{}/domain/{}.java", javaPath, className);
        } else if (template.contains("vo.java.vm")) {
            fileName = StringUtils.format("{}/domain/vo/{}Vo.java", javaPath, className);
        } else if (template.contains("bo.java.vm")) {
            fileName = StringUtils.format("{}/domain/bo/{}Bo.java", javaPath, className);
        } else if (template.contains("mapper.java.vm")) {
            fileName = StringUtils.format("{}/mapper/{}Mapper.java", javaPath, className);
        } else if (template.contains("service.java.vm")) {
            fileName = StringUtils.format("{}/service/I{}Service.java", javaPath, className);
        } else if (template.contains("serviceImpl.java.vm")) {
            fileName = StringUtils.format("{}/service/impl/{}ServiceImpl.java", javaPath, className);
        } else if (template.contains("controller.java.vm")) {
            fileName = StringUtils.format("{}/controller/{}Controller.java", javaPath, className);
        } else if (template.contains("mapper.xml.vm")) {
            fileName = StringUtils.format("{}/{}Mapper.xml", mybatisPath, className);
        } else if (template.contains("sql.vm")) {
            fileName = businessName + "Menu.sql";
        } else if (template.contains("api.ts.vm")) {
            fileName = StringUtils.format("{}/api/{}/{}/index.ts", vuePath, moduleName, businessName);
        } else if (template.contains("types.ts.vm")) {
            fileName = StringUtils.format("{}/api/{}/{}/types.ts", vuePath, moduleName, businessName);
        } else if (template.contains("index.vue.vm")) {
            fileName = StringUtils.format("{}/views/{}/{}/index.vue", vuePath, moduleName, businessName);
        } else if (template.contains("index-tree.vue.vm")) {
            fileName = StringUtils.format("{}/views/{}/{}/index.vue", vuePath, moduleName, businessName);
        }
        return fileName;
    }

    /**
     * 获取包前缀
     *
     * @param packageName 包名称
     * @return 包前缀名称
     */
    public static String getPackagePrefix(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        return StringUtils.substring(packageName, 0, lastIndex);
    }

    /**
     * 根据列类型获取导入包
     *
     * @param genTable 业务表对象
     * @return 返回需要导入的包列表
     */
    public static HashSet<String> getImportList(GenTable genTable) {
        List<GenTableColumn> columns = genTable.getColumns();
        HashSet<String> importList = new HashSet<>();
        for (GenTableColumn column : columns) {
            if (!column.isSuperColumn() && GenConstants.TYPE_DATE.equals(column.getJavaType())) {
                importList.add("java.time.LocalDateTime");
                importList.add("com.fasterxml.jackson.annotation.JsonFormat");
            } else if (!column.isSuperColumn() && GenConstants.TYPE_BIGDECIMAL.equals(column.getJavaType())) {
                importList.add("java.math.BigDecimal");
            } else if (!column.isSuperColumn() && "imageUpload".equals(column.getHtmlType())) {
                importList.add("org.dromara.common.translation.annotation.Translation");
                importList.add("org.dromara.common.translation.constant.TransConstant");
            }
            if (!column.isSuperColumn() && GenConstants.QUERY_BETWEEN.equals(column.getQueryType())) {
                importList.add("java.util.HashMap");
                importList.add("java.util.Map");
            }
        }
        return importList;
    }

    /**
     * 根据列类型获取字典组
     *
     * @param genTable 业务表对象
     * @return 返回字典组
     */
    public static String getDicts(GenTable genTable) {
        List<GenTableColumn> columns = genTable.getColumns();
        Set<String> dicts = new HashSet<>();
        addDicts(dicts, columns);
        return StringUtils.join(dicts, ", ");
    }

    /**
     * 添加字典列表
     *
     * @param dicts 字典列表
     * @param columns 列集合
     */
    public static void addDicts(Set<String> dicts, List<GenTableColumn> columns) {
        for (GenTableColumn column : columns) {
            if (!column.isSuperColumn() && StringUtils.isNotEmpty(column.getDictType()) && StringUtils.equalsAny(
                column.getHtmlType(),
                new String[] { GenConstants.HTML_SELECT, GenConstants.HTML_RADIO, GenConstants.HTML_CHECKBOX })) {
                dicts.add("'" + column.getDictType() + "'");
            }
        }
    }

    /**
     * 获取权限前缀
     *
     * @param moduleName   模块名称
     * @param businessName 业务名称
     * @return 返回权限前缀
     */
    public static String getPermissionPrefix(String moduleName, String businessName) {
        return StringUtils.format("{}:{}", moduleName, businessName);
    }

    /**
     * 获取上级菜单ID字段
     *
     * @param paramsObj 生成其他选项
     * @return 上级菜单ID字段
     */
    public static String getParentMenuId(Dict paramsObj) {
        if (CollUtil.isNotEmpty(paramsObj) && paramsObj.containsKey(GenConstants.PARENT_MENU_ID)
            && StringUtils.isNotEmpty(paramsObj.getStr(GenConstants.PARENT_MENU_ID))) {
            return paramsObj.getStr(GenConstants.PARENT_MENU_ID);
        }
        return DEFAULT_PARENT_MENU_ID;
    }

    /**
     * 获取树编码
     *
     * @param paramsObj 生成其他选项
     * @return 树编码
     */
    public static String getTreeCode(Map<String, Object> paramsObj) {
        if (CollUtil.isNotEmpty(paramsObj) && paramsObj.containsKey(GenConstants.TREE_CODE)) {
            return StringUtils.toCamelCase(Convert.toStr(paramsObj.get(GenConstants.TREE_CODE)));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取树父编码
     *
     * @param paramsObj 生成其他选项
     * @return 树父编码
     */
    public static String getTreeParentCode(Dict paramsObj) {
        if (CollUtil.isNotEmpty(paramsObj) && paramsObj.containsKey(GenConstants.TREE_PARENT_CODE)) {
            return StringUtils.toCamelCase(paramsObj.getStr(GenConstants.TREE_PARENT_CODE));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取树名称
     *
     * @param paramsObj 生成其他选项
     * @return 树名称
     */
    public static String getTreeName(Dict paramsObj) {
        if (CollUtil.isNotEmpty(paramsObj) && paramsObj.containsKey(GenConstants.TREE_NAME)) {
            return StringUtils.toCamelCase(paramsObj.getStr(GenConstants.TREE_NAME));
        }
        return StringUtils.EMPTY;
    }
}

package org.dromara.gen.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

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
    private static final ResourcePatternResolver RESOURCE_PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();

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
        context.put("frontendType", resolveFrontendType(genTable.getFrontendType()));
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
        if (ObjectUtil.isNull(paramsObj)) {
            paramsObj = new Dict();
        }
        String parentMenuId = getParentMenuId(paramsObj);
        context.put("parentMenuId", parentMenuId);
        boolean enableExport = getBooleanOption(paramsObj, GenConstants.ENABLE_EXPORT, true);
        boolean enableStatus = getBooleanOption(paramsObj, GenConstants.ENABLE_STATUS, false);
        boolean enableUnique = getBooleanOption(paramsObj, GenConstants.ENABLE_UNIQUE, false);
        boolean enableSort = getBooleanOption(paramsObj, GenConstants.ENABLE_SORT, false);
        GenTableColumn statusColumn = getColumn(genTable, paramsObj.getStr(GenConstants.STATUS_FIELD));
        GenTableColumn sortColumn = getColumn(genTable, paramsObj.getStr(GenConstants.SORT_FIELD));
        List<GenTableColumn> uniqueColumns = getColumns(genTable, paramsObj.get(GenConstants.UNIQUE_FIELDS));
        context.put("enableExport", enableExport);
        context.put("enableStatus", enableStatus && ObjectUtil.isNotNull(statusColumn));
        context.put("statusColumn", statusColumn);
        context.put("statusField", ObjectUtil.isNotNull(statusColumn) ? statusColumn.getJavaField() : StringUtils.EMPTY);
        context.put("enableUnique", enableUnique && CollUtil.isNotEmpty(uniqueColumns));
        context.put("uniqueColumns", uniqueColumns);
        context.put("enableSort", enableSort && ObjectUtil.isNotNull(sortColumn));
        context.put("sortColumn", sortColumn);
        context.put("sortField", ObjectUtil.isNotNull(sortColumn) ? sortColumn.getJavaField() : StringUtils.EMPTY);

        // 向树形模板上下文写入树字段相关变量
        if (GenConstants.TPL_TREE.equals(tplCategory)) {
            setTreeContext(context, genTable, paramsObj);
        }

        return context;
    }

    /**
     * 向树形模板上下文写入树字段相关变量。
     *
     * @param context   模板上下文
     * @param genTable  代码生成业务表对象
     * @param paramsObj 已解析的 options 参数（避免重复解析）
     */
    public static void setTreeContext(Dict context, GenTable genTable, Dict paramsObj) {
        String treeCode = getTreeCode(paramsObj);
        String treeParentCode = getTreeParentCode(paramsObj);
        String treeName = getTreeName(paramsObj);
        GenTableColumn treeParentColumn = getColumn(genTable, paramsObj.getStr(GenConstants.TREE_PARENT_CODE));
        GenTableColumn treeAncestorsColumn = getColumn(genTable, paramsObj.getStr(GenConstants.TREE_ANCESTORS));
        GenTableColumn treeOrderColumn = getColumn(genTable, paramsObj.getStr(GenConstants.TREE_ORDER_FIELD));
        String treeRootValue = getTreeRootValue(paramsObj, treeParentColumn);

        context.put("treeCode", treeCode);
        context.put("treeParentCode", treeParentCode);
        context.put("treeName", treeName);
        context.put("treeParentColumn", treeParentColumn);
        context.put("treeAncestorsField", ObjectUtil.isNotNull(treeAncestorsColumn) ? treeAncestorsColumn.getJavaField() : StringUtils.EMPTY);
        context.put("treeOrderField", ObjectUtil.isNotNull(treeOrderColumn) ? treeOrderColumn.getJavaField() : StringUtils.EMPTY);
        context.put("treeOrderColumn", treeOrderColumn);
        context.put("treeRootValue", treeRootValue);
        context.put("treeRootValueJavaLiteral", getJavaLiteral(treeParentColumn, treeRootValue));
        context.put("treeRootValueTsLiteral", getTsLiteral(treeParentColumn, treeRootValue));
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
        return getTemplateList(tplCategory, dsName, GenConstants.FRONTEND_TYPE_VUE);
    }

    /**
     * 获取模板信息
     *
     * @param tplCategory  前端页面模板分类
     * @param dsName       数据源名称
     * @param frontendType 前端模板类型
     * @return 模板列表
     */
    public static List<PathNamedTemplate> getTemplateList(String tplCategory, String dsName, String frontendType) {
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
        // 前端 API 与类型模板
        templates.add(getTemplate(getFrontendApiTemplatePath(frontendType)));
        templates.add(getTemplate(getFrontendTypesTemplatePath(frontendType)));
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
        // 前端页面模板
        if (GenConstants.TPL_CRUD.equals(tplCategory)) {
            templates.add(getTemplate(getFrontendIndexTemplatePath(frontendType)));
        } else if (GenConstants.TPL_TREE.equals(tplCategory)) {
            templates.add(getTemplate(getFrontendIndexTreeTemplatePath(frontendType)));
        }
        return templates;
    }

    /**
     * 获取模板，支持按前端模板类型动态加载 vm/{frontendType} 下的模板。
     *
     * @param templatePath 模板路径
     * @return 带路径名的模板
     */
    private static PathNamedTemplate getTemplate(String templatePath) {
        PathNamedTemplate template = TEMPLATE_MAPPER.get(templatePath);
        if (ObjectUtil.isNotNull(template)) {
            return template;
        }
        return PathNamedTemplate.form(TEMPLATE_ENGINE, templatePath);
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
        String frontendPath = getFrontendPath(genTable.getFrontendType());
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
            fileName = StringUtils.format("{}/api/{}/{}/index.ts", frontendPath, moduleName, businessName);
        } else if (template.contains("types.ts.vm")) {
            fileName = StringUtils.format("{}/api/{}/{}/types.ts", frontendPath, moduleName, businessName);
        } else if (isFrontendPageTemplate(template, GenConstants.FRONTEND_INDEX_TEMPLATE_PREFIX)) {
            fileName = StringUtils.format("{}/views/{}/{}/index.{}", frontendPath, moduleName, businessName,
                getFrontendPageExtension(template, GenConstants.FRONTEND_INDEX_TEMPLATE_PREFIX));
        } else if (isFrontendPageTemplate(template, GenConstants.FRONTEND_INDEX_TREE_TEMPLATE_PREFIX)) {
            fileName = StringUtils.format("{}/views/{}/{}/index.{}", frontendPath, moduleName, businessName,
                getFrontendPageExtension(template, GenConstants.FRONTEND_INDEX_TREE_TEMPLATE_PREFIX));
        }
        return fileName;
    }

    /**
     * 获取前端输出目录。
     *
     * @param frontendType 前端模板类型
     * @return 前端输出目录
     */
    private static String getFrontendPath(String frontendType) {
        return resolveFrontendType(frontendType);
    }

    /**
     * 获取前端页面文件扩展名。
     *
     * @param template       模板路径
     * @param templatePrefix 模板文件名前缀
     * @return 文件扩展名
     */
    private static String getFrontendPageExtension(String template, String templatePrefix) {
        String fileName = getTemplateFileName(template);
        return fileName.substring((templatePrefix + ".").length(), fileName.length() - ".vm".length());
    }

    private static String getFrontendApiTemplatePath(String frontendType) {
        return getFrontendTemplatePath(frontendType, GenConstants.FRONTEND_API_TEMPLATE_NAME);
    }

    private static String getFrontendTypesTemplatePath(String frontendType) {
        return getFrontendTemplatePath(frontendType, GenConstants.FRONTEND_TYPES_TEMPLATE_NAME);
    }

    private static String getFrontendIndexTemplatePath(String frontendType) {
        return getFrontendPageTemplatePath(frontendType, GenConstants.FRONTEND_INDEX_TEMPLATE_PREFIX);
    }

    private static String getFrontendIndexTreeTemplatePath(String frontendType) {
        return getFrontendPageTemplatePath(frontendType, GenConstants.FRONTEND_INDEX_TREE_TEMPLATE_PREFIX);
    }

    /**
     * 解析前端模板类型。
     *
     * @param frontendType 前端模板类型
     * @return 已规范化的模板目录名
     */
    private static String resolveFrontendType(String frontendType) {
        String type = StringUtils.blankToDefault(frontendType, GenConstants.FRONTEND_TYPE_VUE);
        if (!type.matches("[A-Za-z0-9_-]+")) {
            throw new ServiceException("前端模板类型仅支持字母、数字、下划线和中划线");
        }
        return type;
    }

    private static String getFrontendTemplatePath(String frontendType, String templateName) {
        return StringUtils.format("vm/{}/{}", resolveFrontendType(frontendType), templateName);
    }

    /**
     * 获取前端页面模板路径，页面输出扩展名由模板文件名决定。
     *
     * @param frontendType   前端模板类型
     * @param templatePrefix 页面模板文件名前缀
     * @return 页面模板路径
     */
    private static String getFrontendPageTemplatePath(String frontendType, String templatePrefix) {
        String type = resolveFrontendType(frontendType);
        String pattern = StringUtils.format("classpath*:vm/{}/{}.*.vm", type, templatePrefix);
        try {
            Resource[] resources = RESOURCE_PATTERN_RESOLVER.getResources(pattern);
            if (resources.length == 0) {
                throw new ServiceException(StringUtils.format("未找到前端模板: vm/{}/{}.*.vm", type, templatePrefix));
            }
            return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(StringUtils::isNotBlank)
                .sorted()
                .findFirst()
                .map(fileName -> StringUtils.format("vm/{}/{}", type, fileName))
                .orElseThrow(() -> new ServiceException(StringUtils.format("未找到前端模板: vm/{}/{}.*.vm", type, templatePrefix)));
        } catch (IOException e) {
            throw new ServiceException(StringUtils.format("读取前端模板失败: vm/{}/{}.*.vm", type, templatePrefix), e);
        }
    }

    private static boolean isFrontendPageTemplate(String template, String templatePrefix) {
        String fileName = getTemplateFileName(template);
        return fileName.startsWith(templatePrefix + ".") && fileName.endsWith(".vm");
    }

    private static String getTemplateFileName(String template) {
        int index = Math.max(template.lastIndexOf('/'), template.lastIndexOf('\\'));
        return index >= 0 ? template.substring(index + 1) : template;
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
     * @param dicts   字典列表
     * @param columns 列集合
     */
    public static void addDicts(Set<String> dicts, List<GenTableColumn> columns) {
        for (GenTableColumn column : columns) {
            if (!column.isSuperColumn() && StringUtils.isNotEmpty(column.getDictType()) && StringUtils.equalsAny(
                column.getHtmlType(),
                GenConstants.HTML_SELECT, GenConstants.HTML_RADIO, GenConstants.HTML_CHECKBOX, GenConstants.HTML_SWITCH)) {
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

    /**
     * 获取树根节点值。
     *
     * @param paramsObj        其他选项
     * @param treeParentColumn 父节点字段
     * @return 树根节点值
     */
    public static String getTreeRootValue(Dict paramsObj, GenTableColumn treeParentColumn) {
        String defaultValue = "0";
        if (ObjectUtil.isNotNull(treeParentColumn) && StringUtils.equals(treeParentColumn.getJavaType(), GenConstants.TYPE_STRING)) {
            defaultValue = "0";
        }
        if (CollUtil.isNotEmpty(paramsObj) && paramsObj.containsKey(GenConstants.TREE_ROOT_VALUE)) {
            return StringUtils.blankToDefault(paramsObj.getStr(GenConstants.TREE_ROOT_VALUE), defaultValue);
        }
        return defaultValue;
    }

    /**
     * 读取布尔类型生成选项。
     *
     * @param paramsObj    生成选项
     * @param key          选项键
     * @param defaultValue 默认值
     * @return 选项值
     */
    private static boolean getBooleanOption(Dict paramsObj, String key, boolean defaultValue) {
        if (CollUtil.isEmpty(paramsObj) || !paramsObj.containsKey(key)) {
            return defaultValue;
        }
        return Convert.toBool(paramsObj.get(key), defaultValue);
    }

    /**
     * 根据字段名查找业务表字段。
     *
     * @param genTable 业务表
     * @param field    字段名称或 Java 属性名
     * @return 业务表字段
     */
    private static GenTableColumn getColumn(GenTable genTable, String field) {
        if (StringUtils.isBlank(field) || CollUtil.isEmpty(genTable.getColumns())) {
            return null;
        }
        for (GenTableColumn column : genTable.getColumns()) {
            if (StringUtils.equalsAny(field, column.getColumnName(), column.getJavaField())) {
                return column;
            }
        }
        return null;
    }

    /**
     * 根据字段配置查找业务表字段列表。
     *
     * @param genTable    业务表
     * @param fieldValues 字段配置值
     * @return 业务表字段列表
     */
    private static List<GenTableColumn> getColumns(GenTable genTable, Object fieldValues) {
        List<String> fields = new ArrayList<>();
        if (fieldValues instanceof Collection<?> collection) {
            collection.stream().map(Convert::toStr).forEach(fields::add);
        } else if (ObjectUtil.isNotNull(fieldValues)) {
            fields.addAll(StringUtils.str2List(Convert.toStr(fieldValues), StringUtils.SEPARATOR, true, true));
        }
        List<GenTableColumn> columns = new ArrayList<>();
        for (String field : fields) {
            GenTableColumn column = getColumn(genTable, field);
            if (ObjectUtil.isNotNull(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    /**
     * 转换为 Java 字面量。
     *
     * @param column 业务表字段
     * @param value  字段值
     * @return Java 字面量
     */
    private static String getJavaLiteral(GenTableColumn column, String value) {
        if (ObjectUtil.isNull(column) || StringUtils.isBlank(value)) {
            return "null";
        }
        if (StringUtils.equals(column.getJavaType(), GenConstants.TYPE_LONG)) {
            return value + "L";
        }
        if (StringUtils.equalsAny(column.getJavaType(), GenConstants.TYPE_INTEGER, GenConstants.TYPE_DOUBLE, GenConstants.TYPE_BIGDECIMAL)) {
            return value;
        }
        return "\"" + value + "\"";
    }

    /**
     * 转换为 TypeScript 字面量。
     *
     * @param column 业务表字段
     * @param value  字段值
     * @return TypeScript 字面量
     */
    private static String getTsLiteral(GenTableColumn column, String value) {
        if (ObjectUtil.isNull(column) || StringUtils.isBlank(value)) {
            return "undefined";
        }
        if (StringUtils.equalsAny(column.getJavaType(), GenConstants.TYPE_LONG, GenConstants.TYPE_INTEGER, GenConstants.TYPE_DOUBLE, GenConstants.TYPE_BIGDECIMAL)) {
            return value;
        }
        return "'" + value + "'";
    }
}

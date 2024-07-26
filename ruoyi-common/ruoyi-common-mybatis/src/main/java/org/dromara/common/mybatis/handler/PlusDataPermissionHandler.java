package org.dromara.common.mybatis.handler;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.io.Resources;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.enums.DataScopeType;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 数据权限过滤
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Slf4j
public class PlusDataPermissionHandler {

    /**
     * 方法或类(名称) 与 注解的映射关系缓存
     */
    private final Map<String, DataPermission> dataPermissionCacheMap = new ConcurrentHashMap<>();

    /**
     * spel 解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParserContext parserContext = new TemplateParserContext();
    /**
     * bean解析器 用于处理 spel 表达式中对 bean 的调用
     */
    private final BeanResolver beanResolver = new BeanFactoryResolver(SpringUtils.getBeanFactory());

    /**
     * 构造方法，扫描指定包下的 Mapper 类并初始化缓存
     *
     * @param mapperPackage Mapper 类所在的包路径
     */
    public PlusDataPermissionHandler(String mapperPackage) {
        scanMapperClasses(mapperPackage);
    }

    /**
     * 获取数据过滤条件的 SQL 片段
     *
     * @param where             原始的查询条件表达式
     * @param mappedStatementId Mapper 方法的 ID
     * @param isSelect          是否为查询语句
     * @return 数据过滤条件的 SQL 片段
     */
    public Expression getSqlSegment(Expression where, String mappedStatementId, boolean isSelect) {
        // 获取数据权限配置
        DataPermission dataPermission = getDataPermission(mappedStatementId);
        // 获取当前登录用户信息
        LoginUser currentUser = DataPermissionHelper.getVariable("user");
        if (ObjectUtil.isNull(currentUser)) {
            currentUser = LoginHelper.getLoginUser();
            DataPermissionHelper.setVariable("user", currentUser);
        }
        // 如果是超级管理员或租户管理员，则不过滤数据
        if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin()) {
            return where;
        }
        // 构造数据过滤条件的 SQL 片段
        String dataFilterSql = buildDataFilter(dataPermission, isSelect);
        if (StringUtils.isBlank(dataFilterSql)) {
            return where;
        }
        try {
            Expression expression = CCJSqlParserUtil.parseExpression(dataFilterSql);
            // 数据权限使用单独的括号 防止与其他条件冲突
            Parenthesis parenthesis = new Parenthesis(expression);
            if (ObjectUtil.isNotNull(where)) {
                return new AndExpression(where, parenthesis);
            } else {
                return parenthesis;
            }
        } catch (JSQLParserException e) {
            throw new ServiceException("数据权限解析异常 => " + e.getMessage());
        }
    }

    /**
     * 构建数据过滤条件的 SQL 语句
     *
     * @param dataPermission 数据权限注解
     * @param isSelect       标志当前操作是否为查询操作，查询操作和更新或删除操作在处理过滤条件时会有不同的处理方式
     * @return 构建的数据过滤条件的 SQL 语句
     * @throws ServiceException 如果角色的数据范围异常或者 key 与 value 的长度不匹配，则抛出 ServiceException 异常
     */
    private String buildDataFilter(DataPermission dataPermission, boolean isSelect) {
        // 更新或删除需满足所有条件
        String joinStr = isSelect ? " OR " : " AND ";
        if (StringUtils.isNotBlank(dataPermission.joinStr())) {
            joinStr = " " + dataPermission.joinStr() + " ";
        }
        LoginUser user = DataPermissionHelper.getVariable("user");
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(beanResolver);
        DataPermissionHelper.getContext().forEach(context::setVariable);
        Set<String> conditions = new HashSet<>();
        for (RoleDTO role : user.getRoles()) {
            user.setRoleId(role.getRoleId());
            // 获取角色权限泛型
            DataScopeType type = DataScopeType.findCode(role.getDataScope());
            if (ObjectUtil.isNull(type)) {
                throw new ServiceException("角色数据范围异常 => " + role.getDataScope());
            }
            // 全部数据权限直接返回
            if (type == DataScopeType.ALL) {
                return "";
            }
            boolean isSuccess = false;
            for (DataColumn dataColumn : dataPermission.value()) {
                if (dataColumn.key().length != dataColumn.value().length) {
                    throw new ServiceException("角色数据范围异常 => key与value长度不匹配");
                }
                // 不包含 key 变量 则不处理
                if (!StringUtils.containsAny(type.getSqlTemplate(),
                    Arrays.stream(dataColumn.key()).map(key -> "#" + key).toArray(String[]::new)
                )) {
                    continue;
                }
                // 包含权限标识符 这直接跳过
                if (StringUtils.isNotBlank(dataColumn.permission()) &&
                    CollUtil.contains(user.getMenuPermission(), dataColumn.permission())
                ) {
                    isSuccess = true;
                    continue;
                }
                // 设置注解变量 key 为表达式变量 value 为变量值
                for (int i = 0; i < dataColumn.key().length; i++) {
                    context.setVariable(dataColumn.key()[i], dataColumn.value()[i]);
                }

                // 解析sql模板并填充
                String sql = parser.parseExpression(type.getSqlTemplate(), parserContext).getValue(context, String.class);
                conditions.add(joinStr + sql);
                isSuccess = true;
            }
            // 未处理成功则填充兜底方案
            if (!isSuccess && StringUtils.isNotBlank(type.getElseSql())) {
                conditions.add(joinStr + type.getElseSql());
            }
        }

        if (CollUtil.isNotEmpty(conditions)) {
            String sql = StreamUtils.join(conditions, Function.identity(), "");
            return sql.substring(joinStr.length());
        }
        return "";
    }

    /**
     * 扫描指定包下的 Mapper 类，并查找其中带有特定注解的方法或类
     *
     * @param mapperPackage Mapper 类所在的包路径
     */
    private void scanMapperClasses(String mapperPackage) {
        // 创建资源解析器和元数据读取工厂
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
        // 将 Mapper 包路径按分隔符拆分为数组
        String[] packagePatternArray = StringUtils.splitPreserveAllTokens(mapperPackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        String classpath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;
        try {
            for (String packagePattern : packagePatternArray) {
                // 将包路径转换为资源路径
                String path = ClassUtils.convertClassNameToResourcePath(packagePattern);
                // 获取指定路径下的所有 .class 文件资源
                Resource[] resources = resolver.getResources(classpath + path + "/*.class");
                for (Resource resource : resources) {
                    // 获取资源的类元数据
                    ClassMetadata classMetadata = factory.getMetadataReader(resource).getClassMetadata();
                    // 获取资源对应的类对象
                    Class<?> clazz = Resources.classForName(classMetadata.getClassName());
                    // 查找类中的特定注解
                    findAnnotation(clazz);
                }
            }
        } catch (Exception e) {
            log.error("初始化数据安全缓存时出错:{}", e.getMessage());
        }
    }

    /**
     * 在指定的类中查找特定的注解 DataPermission，并将带有这个注解的方法或类存储到 dataPermissionCacheMap 中
     *
     * @param clazz 要查找的类
     */
    private void findAnnotation(Class<?> clazz) {
        DataPermission dataPermission;
        for (Method method : clazz.getMethods()) {
            if (method.isDefault() || method.isVarArgs()) {
                continue;
            }
            String mappedStatementId = clazz.getName() + "." + method.getName();
            if (AnnotationUtil.hasAnnotation(method, DataPermission.class)) {
                dataPermission = AnnotationUtil.getAnnotation(method, DataPermission.class);
                dataPermissionCacheMap.put(mappedStatementId, dataPermission);
            }
        }
        if (AnnotationUtil.hasAnnotation(clazz, DataPermission.class)) {
            dataPermission = AnnotationUtil.getAnnotation(clazz, DataPermission.class);
            dataPermissionCacheMap.put(clazz.getName(), dataPermission);
        }
    }

    /**
     * 根据映射语句 ID 或类名获取对应的 DataPermission 注解对象
     *
     * @param mapperId 映射语句 ID
     * @return DataPermission 注解对象，如果不存在则返回 null
     */
    public DataPermission getDataPermission(String mapperId) {
        // 检查缓存中是否包含映射语句 ID 对应的 DataPermission 注解对象
        if (dataPermissionCacheMap.containsKey(mapperId)) {
            return dataPermissionCacheMap.get(mapperId);
        }
        // 如果缓存中不包含映射语句 ID 对应的 DataPermission 注解对象，则尝试使用类名作为键查找
        String clazzName = mapperId.substring(0, mapperId.lastIndexOf("."));
        if (dataPermissionCacheMap.containsKey(clazzName)) {
            return dataPermissionCacheMap.get(clazzName);
        }
        return null;
    }

    /**
     * 检查给定的映射语句 ID 是否有效，即是否能够找到对应的 DataPermission 注解对象
     *
     * @param mapperId 映射语句 ID
     * @return 如果找到对应的 DataPermission 注解对象，则返回 false；否则返回 true
     */
    public boolean invalid(String mapperId) {
        return getDataPermission(mapperId) == null;
    }
}

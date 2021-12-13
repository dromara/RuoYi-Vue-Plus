package com.ruoyi.framework.handler;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.DataColumn;
import com.ruoyi.common.annotation.DataPermission;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.enums.DataScopeType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限过滤
 *
 * @author Lion Li
 */
@Slf4j
public class PlusDataPermissionHandler {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParserContext parserContext = new TemplateParserContext();
    private final BeanResolver beanResolver = new BeanFactoryResolver(SpringUtils.getBeanFactory());

    public Expression getSqlSegment(Expression where, String mappedStatementId, boolean isSelect) {
        DataColumn[] dataColumns = findAnnotation(mappedStatementId);
        if (ArrayUtil.isEmpty(dataColumns)) {
            return where;
        }
        SysUser currentUser = SpringUtils.getBean(UserService.class).selectUserById(SecurityUtils.getUserId());
        // 如果是超级管理员，则不过滤数据
        if (StringUtils.isNull(currentUser) || currentUser.isAdmin()) {
            return where;
        }
        String dataFilterSql = buildDataFilter(currentUser, dataColumns, isSelect);
        if (StringUtils.isBlank(dataFilterSql)) {
            return where;
        }
        try {
            Expression expression = CCJSqlParserUtil.parseExpression(dataFilterSql);
            if (ObjectUtil.isNotNull(where)) {
                return new AndExpression(where, expression);
            } else {
                return expression;
            }
        } catch (JSQLParserException e) {
            throw new ServiceException("数据权限解析异常 => " + e.getMessage());
        }
    }

    /**
     * 构造数据过滤sql
     */
    private String buildDataFilter(SysUser user, DataColumn[] dataColumns, boolean isSelect) {
        StringBuilder sqlString = new StringBuilder();

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(beanResolver);
        context.setVariable("user", user);

        for (DataColumn dataColumn : dataColumns) {
            // 设置注解变量 key 为表达式变量 value 为变量值
            context.setVariable(dataColumn.key(), dataColumn.value());
            for (SysRole role : user.getRoles()) {
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
                // 不包含 key 变量 则不处理
                if (!StringUtils.contains(type.getSql(), "#" + dataColumn.key())) {
                    continue;
                }
                // 更新或删除需满足所有条件
                sqlString.append(isSelect ? " OR " : " AND ");
                // 解析sql模板并填充
                String sql = parser.parseExpression(type.getSql(), parserContext).getValue(context, String.class);
                sqlString.append(sql);
            }
        }

        if (StringUtils.isNotBlank(sqlString.toString())) {
            return sqlString.substring(isSelect ? 4 : 5);
        }
        return "";
    }

    private DataColumn[] findAnnotation(String mappedStatementId) {
        StringBuilder sb = new StringBuilder(mappedStatementId);
        int index = sb.lastIndexOf(".");
        String clazzName = sb.substring(0, index);
        String methodName = sb.substring(index + 1, sb.length());
        Class<?> clazz = ClassUtil.loadClass(clazzName);
        List<Method> methods = Arrays.stream(ClassUtil.getDeclaredMethods(clazz))
            .filter(method -> method.getName().equals(methodName)).collect(Collectors.toList());
        DataPermission dataPermission;
        for (Method method : methods) {
            if (AnnotationUtil.hasAnnotation(method, DataPermission.class)) {
                dataPermission = AnnotationUtil.getAnnotation(method, DataPermission.class);
                return dataPermission.value();
            }
        }
        return null;
    }
}

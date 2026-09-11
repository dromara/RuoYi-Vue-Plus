/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.expression;

import org.dromara.warm.flow.exception.FlowException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * 条件表达式 spel
 *
 * @author warm,battcn
 */
@Configuration
public class SpelHelper implements ApplicationContextAware {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private final static ParserContext PARSER_CONTEXT = new TemplateParserContext();

    private static ApplicationContext applicationContext;

    /**
     * bean解析器 用于处理 spel 表达式中对 bean 的调用
     */
    private static BeanResolver beanResolver = null;

    public static BeanResolver beanResolver() {
        if (beanResolver == null) {
            beanResolver = new BeanFactoryResolver(getBeanFactory());
        }
        return beanResolver;
    }

    /**
     * @param expression expression
     * @return  Object
     */
    public static Object parseExpression(String expression, Map<String, Object> variable) {

        // 创建带沙箱保护的评估上下文
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 设置 Bean 解析器（支持 @beanName 引用）
        context.setBeanResolver(beanResolver());

        // 设置变量
        context.setVariables(variable);

        // 设置沙箱：限制类型访问（白名单机制）
        context.setTypeLocator(new SafeTypeLocator());

        // 设置沙箱：限制方法调用
        context.addMethodResolver(new SafeMethodResolver());

        return PARSER.parseExpression(expression, PARSER_CONTEXT).getValue(context, Object.class);
    }

    /**
     * 执行 spel 表达式并按布尔结果返回
     *
     * @param expression 表达式
     * @param variable   流程变量
     * @return 布尔结果
     */
    public static boolean evalBool(String expression, Map<String, Object> variable) {
        return Boolean.TRUE.equals(parseExpression(expression, variable));
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        if (SpelHelper.applicationContext == null) {
            SpelHelper.applicationContext = applicationContext;
        }
    }

    public static ListableBeanFactory getBeanFactory() {
        if (null == applicationContext) {
            throw new FlowException("No ConfigurableListableBeanFactory or ApplicationContext injected, maybe not in the Spring environment?");
        } else {
            return applicationContext;
        }
    }

    public static String replace(String expression, Map<String, Object> variable) {
        expression = expression.replace("$", "#");
        for (Map.Entry<String, Object> entry : variable.entrySet()) {
            if (expression.contains(entry.getKey())) {
                expression = expression.replace(entry.getKey(), "#" + entry.getKey());
            }
        }
        return expression;
    }

}

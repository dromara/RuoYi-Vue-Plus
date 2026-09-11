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
package org.dromara.warm.flow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ArrayUtil;

import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.enums.Condition;
import org.dromara.warm.flow.enums.Handler;
import org.dromara.warm.flow.enums.VoteSign;
import org.dromara.warm.flow.dto.FlowParams;
import org.dromara.warm.flow.entity.FlowTask;
import org.dromara.warm.flow.expression.ListenerStrategySpel;
import org.dromara.warm.flow.handler.PermissionHandler;
import org.dromara.warm.flow.strategy.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表达式工具类
 *
 * @author warm
 */
public class ExpressionUtil {

    static {
        // 注册条件表达式（比较类 + spel）
        for (Condition condition : Condition.values()) {
            setExpression(condition);
        }

        // 注册会签票签表达式
        for (VoteSign voteSign : VoteSign.values()) {
            setExpression(voteSign);
        }

        // 注册监听器表达式
        setExpression(new ListenerStrategySpel());

        // 注册办理人表达式
        for (Handler handler : Handler.values()) {
            setExpression(handler);
        }
    }

    /**
     * 设置表达式
     *
     * @param expressionStrategy 表达式策略
     * @param <T>                表达式类型
     */
    public static <T> void setExpression(ExpressionStrategy<T> expressionStrategy) {
        expressionStrategy.setExpression(expressionStrategy);
    }

    /**
     * 条件表达式替换
     *
     * @param expression 条件表达式，比如“eq@@flag|4” ，或者自定义策略
     * @param variable   变量
     * @return boolean
     */
    public static boolean evalCondition(String expression, Map<String, Object> variable) {
        return Boolean.TRUE.equals(getValue(ConditionStrategy.EXPRESSION_STRATEGY_LIST, expression, variable));
    }

    /**
     * 办理人表达式替换
     *
     * @param addTasks   任务列表
     * @param flowParams 流程变量
     */
    public static void evalVariable(List<FlowTask> addTasks, FlowParams flowParams) {
        if (CollUtil.isEmpty(addTasks)) {
            return;
        }
        Map<String, Object> variable = flowParams.getVariable();
        addTasks.forEach(addTask -> {
            List<String> permissions = addTask.getPermissionList().stream()
                    .map(s -> evalVariable(s, variable)).filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            // 转换办理人，比如设计器中预设了能办理的人，如果其中包含角色或者部门id等，可以通过此接口进行转换成用户id
            PermissionHandler permissionHandler = FlowEngine.permissionHandler();
            if (permissionHandler != null) {
                try {
                    permissions = permissionHandler.convertPermissions(permissions);
                } catch (Exception ignored) {
                }
            }
            // 自定义下个任务的处理人 下个任务处理人配置类型 和 执行的下个任务的办理人
            permissions = nextHandle(flowParams.isNextHandlerAppend(), flowParams.getNextHandler(), permissions);

            addTask.setPermissionList(permissions);
        });
    }

    /**
     * 办理人表达式替换
     *
     * @param expression 表达式，比如“${flag}或者# { &#064;user.notify(#listenerVariable) } ” ，或者自定义策略
     * @param variable   流程变量
     * @return List<String>
     */
    public static List<String> evalVariable(String expression, Map<String, Object> variable) {
        List<String> value = getValue(HandlerStrategy.EXPRESSION_STRATEGY_LIST, expression, variable);
        if (CollUtil.isNotEmpty(value)) {
            return value;
        }
        return List.of(expression);
    }

    /**
     * 监听器表达式替换
     *
     * @param expression 条件表达式，比如“# { &#064;user.notify(#listenerVariable) } ” ，或者自定义策略
     * @param variable   变量
     */
    public static boolean evalListener(String expression, Map<String, Object> variable) {
        return Boolean.TRUE.equals(getValue(ListenerStrategy.EXPRESSION_STRATEGY_LIST, expression, variable));
    }

    /**
     * 票签表达式替换
     *
     * @param expression 表达式，比如“${flag}或者# { &#064;user.notify(#listenerVariable) } ” ，或者自定义策略
     * @param variable   流程变量
     * @return boolean
     */
    public static boolean evalVoteSign(String expression, Map<String, Object> variable) {
        return Boolean.TRUE.equals(getValue(VoteSignStrategy.EXPRESSION_STRATEGY_LIST, expression, variable));
    }

    /**
     * 获取表达式对应的值
     *
     * @param strategyList 表达式策略列表
     * @param expression   变量表达式
     * @param variable     流程变量
     * @return 执行结果
     */
    private static <T> T getValue(List<ExpressionStrategy<T>> strategyList, String expression
            , Map<String, Object> variable) {
        if (StrUtil.isNotEmpty(expression)) {
            // 倒叙遍历，优先匹配最后注入的策略实现类
            for (int i = strategyList.size() - 1; i >= 0; i--) {
                ExpressionStrategy<T> strategy = strategyList.get(i);
                if (expression.startsWith(strategy.getType())) {
                    if (StrUtil.isNotEmpty(strategy.interceptStr())) {
                        expression = expression.replace(strategy.getType() + strategy.interceptStr(), "");
                    }
                    return strategy.eval(expression, MapUtil.isEmpty(variable) ? new HashMap<>() : variable);
                }
            }
        }
        return null;
    }

    /**
     * 处理下个任务的处理人
     *
     * @param nextHandlerAppend 下个任务处理人配置类型
     * @param nextHandler       下个任务的处理人
     * @param permissions       节点配置的原下个任务的处理人
     * @author xiarg
     * @since 2025/06/03 15:33:38
     */
    private static List<String> nextHandle(boolean nextHandlerAppend, String[] nextHandler, List<String> permissions) {
        if (ArrayUtil.isEmpty(nextHandler)) {
            return permissions;
        }
        if (nextHandlerAppend) {
            permissions.addAll(Arrays.asList(nextHandler));
        } else {
            permissions = new ArrayList<>(Arrays.asList(nextHandler));
        }
        return permissions;
    }
}

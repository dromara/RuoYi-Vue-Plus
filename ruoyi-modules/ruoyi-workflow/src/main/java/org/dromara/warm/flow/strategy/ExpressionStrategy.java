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
package org.dromara.warm.flow.strategy;

import java.util.Map;

/**
 * 表达式策略类接口
 *
 * @author warm
 */
public interface ExpressionStrategy<T> {

    /**
     * 获取策略类型
     *
     * @return 类型
     */
    String getType();

    /**
     * 当选择截取，并且希望拼接上某些字符串，在进行截取
     *
     * @return 类型
     */
    default String interceptStr() {
        return "";
    }

    /**
     * 执行表达式
     *
     * @param expression 表达式
     * @param variable   流程变量
     * @return 执行结果
     */
    T eval(String expression, Map<String, Object> variable);


    /**
     * 设置表达式
     *
     * @param expressionStrategy 表达式
     */
    void setExpression(ExpressionStrategy<T> expressionStrategy);

}

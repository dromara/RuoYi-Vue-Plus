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

import org.dromara.warm.flow.constant.FlowCons;

import java.util.ArrayList;
import java.util.List;

/**
 * 票签表达式接口
 *
 * @author warm
 */
public interface VoteSignStrategy extends ExpressionStrategy<Boolean> {

    /**
     * 票签表达式策略实现类集合
     */
    List<ExpressionStrategy<Boolean>> EXPRESSION_STRATEGY_LIST = new ArrayList<>();

    @Override
    default void setExpression(ExpressionStrategy<Boolean> expressionStrategy) {
        EXPRESSION_STRATEGY_LIST.add(expressionStrategy);
    }

    @Override
    default String interceptStr() {
        return FlowCons.SPLIT_AT;
    }
}

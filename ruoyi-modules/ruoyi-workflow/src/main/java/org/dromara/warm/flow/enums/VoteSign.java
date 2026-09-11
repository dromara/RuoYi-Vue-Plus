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
package org.dromara.warm.flow.enums;

import org.dromara.warm.flow.constant.FlowCons;
import org.dromara.warm.flow.expression.SpelHelper;
import org.dromara.warm.flow.strategy.VoteSignStrategy;

import java.util.Map;

/**
 * 内置会签票签表达式策略，合并原 VoteSignStrategySpel/VoteSignStrategyDefault 两个策略类
 * 表达式格式：spel@@#{@user.eval()} 或 default@@${flag == 5 && flag > 4}
 *
 * @author warm
 */
public enum VoteSign implements VoteSignStrategy {

    /**
     * spel 会签表达式 spel@@#{@user.eval()}
     */
    SPEL(FlowCons.SPEL) {
        @Override
        public Boolean eval(String expression, Map<String, Object> variable) {
            return SpelHelper.evalBool(expression, variable);
        }
    },

    /**
     * 默认会签表达式 default@@${flag == 5 && flag > 4}，基于 spel 简化使用
     */
    DEFAULT(FlowCons.DEFAULT) {
        @Override
        public Boolean eval(String expression, Map<String, Object> variable) {
            return SPEL.eval(SpelHelper.replace(expression, variable), variable);
        }
    };

    private final String key;

    VoteSign(String key) {
        this.key = key;
    }

    @Override
    public String getType() {
        return key;
    }
}

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

import org.dromara.warm.flow.expression.SpelHelper;
import org.dromara.warm.flow.strategy.HandlerStrategy;

import java.util.Map;

/**
 * 内置办理人表达式策略，合并原 DefaultHandlerStrategy/HandlerStrategySpel 两个策略类
 * 表达式格式：${flag} 或 #{@user.evalVar()}
 *
 * @author warm,battcn
 */
public enum Handler implements HandlerStrategy {

    /**
     * 默认办理人表达式 ${flag}
     */
    DEFAULT("$") {
        @Override
        public Object preEval(String expression, Map<String, Object> variable) {
            String result = expression.replace("${", "").replace("}", "");
            return variable.get(result);
        }
    },

    /**
     * spel 办理人表达式 #{@user.evalVar()}
     */
    SPEL("#") {
        @Override
        public Object preEval(String expression, Map<String, Object> variable) {
            return SpelHelper.parseExpression(expression, variable);
        }
    };

    private final String key;

    Handler(String key) {
        this.key = key;
    }

    @Override
    public String getType() {
        return key;
    }
}

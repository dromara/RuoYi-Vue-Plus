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

import org.dromara.warm.flow.constant.ExceptionCons;
import org.dromara.warm.flow.constant.FlowCons;
import org.dromara.warm.flow.expression.SpelHelper;
import org.dromara.warm.flow.strategy.ConditionStrategy;
import org.dromara.warm.flow.utils.AssertUtil;
import org.dromara.warm.flow.utils.MathUtil;

import java.util.Map;

/**
 * 内置条件表达式策略，合并原 eq/ne/gt/ge/lt/le/like/notLike/spel/default 十个策略类及 ConditionType 枚举
 * 表达式格式：eq@@flag|4（前缀 eq@@ 由 {@link ConditionStrategy} 截取，进入 eval 的为 flag|4）
 *
 * @author warm
 */
public enum Condition implements ConditionStrategy {

    /**
     * 等于
     */
    EQ("eq") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            if (MathUtil.isNumeric(value)) {
                return MathUtil.determineSize(variableValue, value) == 0;
            }
            return variableValue.equals(value);
        }
    },

    /**
     * 不等于
     */
    NE("ne") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            if (MathUtil.isNumeric(value)) {
                return MathUtil.determineSize(variableValue, value) != 0;
            }
            return !variableValue.equals(value);
        }
    },

    /**
     * 大于（仅数值比较）
     */
    GT("gt") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            return MathUtil.isNumeric(value) && MathUtil.determineSize(variableValue, value) > 0;
        }
    },

    /**
     * 大于等于（仅数值比较）
     */
    GE("ge") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            return MathUtil.isNumeric(value) && MathUtil.determineSize(variableValue, value) >= 0;
        }
    },

    /**
     * 小于（仅数值比较）
     */
    LT("lt") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            return MathUtil.isNumeric(value) && MathUtil.determineSize(variableValue, value) < 0;
        }
    },

    /**
     * 小于等于（仅数值比较）
     */
    LE("le") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            return MathUtil.isNumeric(value) && MathUtil.determineSize(variableValue, value) <= 0;
        }
    },

    /**
     * 包含
     */
    LIKE("like") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            return variableValue.contains(value);
        }
    },

    /**
     * 不包含
     */
    NOT_LIKE("notLike") {
        @Override
        public Boolean afterEval(String value, String variableValue) {
            return !variableValue.contains(value);
        }
    },

    /**
     * spel 条件表达式 spel@@#{@user.eval()}
     */
    SPEL(FlowCons.SPEL) {
        @Override
        public Boolean eval(String expression, Map<String, Object> variable) {
            return SpelHelper.evalBool(expression, variable);
        }

        @Override
        public Boolean afterEval(String value, String variableValue) {
            throw new UnsupportedOperationException("spel 条件表达式不走值比较");
        }
    },

    /**
     * 默认条件表达式 default@@${flag == 5 && flag > 4}，基于 spel 简化使用
     */
    DEFAULT(FlowCons.DEFAULT) {
        @Override
        public Boolean eval(String expression, Map<String, Object> variable) {
            return SPEL.eval(SpelHelper.replace(expression, variable), variable);
        }

        @Override
        public Boolean afterEval(String value, String variableValue) {
            throw new UnsupportedOperationException("默认条件表达式不走值比较");
        }
    };

    private final String key;

    Condition(String key) {
        this.key = key;
    }

    @Override
    public String getType() {
        return key;
    }

    @Override
    public Boolean eval(String expression, Map<String, Object> variable) {
        AssertUtil.isEmpty(variable, ExceptionCons.NULL_CONDITION_VALUE);
        String[] split = expression.split(FlowCons.SPLIT_VERTICAL);
        String name = split[0].trim();
        Object o = variable.get(name);
        AssertUtil.isNull(o, ExceptionCons.NULL_CONDITION_VALUE);
        return afterEval(split[1].trim(), String.valueOf(o));
    }

    /**
     * 比较表达式值与流程变量值
     *
     * @param value         表达式最后一个参数，比如：eq@@flag|5 的 [5]
     * @param variableValue 流程变量值
     * @return 比较结果
     */
    public abstract Boolean afterEval(String value, String variableValue);
}

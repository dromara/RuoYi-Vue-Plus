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

import cn.hutool.core.util.StrUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 审批动作
 *
 * @author warm
 * @since 2023/3/31 12:16
 */
@Getter
@AllArgsConstructor
public enum SkipType {
    /**
     * 审批动作
     */
    PASS("PASS", "审批通过"),

    REJECT("REJECT", "退回"),

    NONE("NONE", "无动作");

    private final String key;
    private final String value;

    public static String getKeyByValue(String value) {
        for (SkipType item : SkipType.values()) {
            if (item.getValue().equals(value)) {
                return item.getKey();
            }
        }
        return null;
    }

    public static String getValueByKey(String key) {
        for (SkipType item : SkipType.values()) {
            if (item.getKey().equals(key)) {
                return item.getValue();
            }
        }
        return null;
    }

    public static SkipType getByKey(String key) {
        for (SkipType item : SkipType.values()) {
            if (item.getKey().equals(key)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 判断是否通过类型
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isPass(String key) {
        return StrUtil.isNotEmpty(key) && (SkipType.PASS.getKey().equals(key));
    }

    /**
     * 判断是否退回类型
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isReject(String key) {
        return StrUtil.isNotEmpty(key) && (SkipType.REJECT.getKey().equals(key));
    }

    /**
     * 判断是否无动作类型
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isNone(String key) {
        return StrUtil.isNotEmpty(key) && (SkipType.NONE.getKey().equals(key));
    }

}

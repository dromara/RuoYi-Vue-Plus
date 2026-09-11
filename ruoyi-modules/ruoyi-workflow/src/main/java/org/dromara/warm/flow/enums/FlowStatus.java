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

import cn.hutool.core.util.ObjectUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 流程状态
 *
 * @author warm
 * @since 2023/3/31 12:16
 */
@Getter
@AllArgsConstructor
public enum FlowStatus {
    /**
     * 流程状态
     */
    TOBESUBMIT("0", "待提交"),

    APPROVAL("1", "审批中"),

    PASS("2", "审批通过"),

    AUTO_PASS("3", "自动完成"),

    TERMINATE("4", "终止"),

    NULLIFY("5", "作废"),

    CANCEL("6", "撤销"),

    RETRIEVE("7", "取回"),

    FINISHED("8", "已完成"),

    REJECT("9", "已退回"),

    INVALID("10", "失效"),

    TASK_BACK("11", "拿回"),

    RE_START("12", "重启"),

    PENDING("13", "暂存");

    private final String key;
    private final String value;

    public static String getKeyByValue(String value) {
        for (FlowStatus item : FlowStatus.values()) {
            if (item.getValue().equals(value)) {
                return item.getKey();
            }
        }
        return null;
    }

    public static String getValueByKey(String key) {
        for (FlowStatus item : FlowStatus.values()) {
            if (item.getKey().equals(key)) {
                return item.getValue();
            }
        }
        return null;
    }

    public static FlowStatus getByKey(String key) {
        for (FlowStatus item : FlowStatus.values()) {
            if (item.getKey().equals(key)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 判断是否结束节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isFinished(String key) {
        return ObjectUtil.isNotNull(key) && (FlowStatus.FINISHED.getKey().equals(key));
    }

}

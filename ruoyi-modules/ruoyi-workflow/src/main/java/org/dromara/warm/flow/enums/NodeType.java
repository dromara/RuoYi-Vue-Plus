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
 * 节点类型
 *
 * @author warm
 * @since 2023/3/31 12:16
 */
@AllArgsConstructor
@Getter
public enum NodeType {
    /**
     * 开始节点
     */
    START(0, "start"),
    /**
     * 中间节点
     */
    BETWEEN(1, "between"),
    /**
     * 结束节点
     */
    END(2, "end"),

    /**
     * 互斥网关
     */
    SERIAL(3, "serial"),

    /**
     * 并行网关
     */
    PARALLEL(4, "parallel"),

    /**
     * 包容网关
     */
    INCLUSIVE(5, "inclusive");

    private final Integer key;
    private final String value;

    public static Integer getKeyByValue(String value) {
        for (NodeType item : NodeType.values()) {
            if (item.getValue().equals(value)) {
                return item.getKey();
            }
        }
        return null;
    }

    public static String getValueByKey(Integer key) {
        for (NodeType item : NodeType.values()) {
            if (item.getKey().equals(key)) {
                return item.getValue();
            }
        }
        return null;
    }

    public static NodeType getByKey(Integer key) {
        for (NodeType item : NodeType.values()) {
            if (item.getKey().equals(key)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 判断是否开始节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isStart(Integer key) {
        return ObjectUtil.isNotNull(key) && (NodeType.START.getKey().equals(key));
    }

    /**
     * 判断是否中间节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isBetween(Integer key) {
        return ObjectUtil.isNotNull(key) && (NodeType.BETWEEN.getKey().equals(key));
    }

    /**
     * 判断是否结束节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isEnd(Integer key) {
        return ObjectUtil.isNotNull(key) && (NodeType.END.getKey().equals(key));
    }

    /**
     * 判断是否网关节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isGateWay(Integer key) {
        return ObjectUtil.isNotNull(key) && (NodeType.SERIAL.getKey().equals(key)
            || NodeType.PARALLEL.getKey().equals(key)|| NodeType.INCLUSIVE.getKey().equals(key));
    }

    /**
     * 判断是否互斥网关节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isGateWaySerial(Integer key) {
        return ObjectUtil.isNotNull(key) && NodeType.SERIAL.getKey().equals(key);
    }

    /**
     * 判断是否并行网关节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isGateWayParallel(Integer key) {
        return ObjectUtil.isNotNull(key) && NodeType.PARALLEL.getKey().equals(key);
    }

    /**
     * 判断是否包容网关节点
     *
     * @param key 枚举key
     * @return  boolean
     */
    public static Boolean isGateWayInclusive(Integer key) {
        return ObjectUtil.isNotNull(key) && NodeType.INCLUSIVE.getKey().equals(key);
    }

}

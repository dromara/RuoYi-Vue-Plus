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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * map工具类（仅保留 hutool MapUtil 没有的方法，其余请直接使用 cn.hutool.core.map.MapUtil）
 *
 * @author warm
 * @since 2023/5/18 9:46
 */
public class MapUtil {

    /**
     * * 判断一个Map是否为空
     *
     * @param map 要判断的Map
     * @return true：为空 false：非空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return ObjectUtil.isNull(map) || map.isEmpty();
    }

    /**
     * * 判断一个Map是否为空
     *
     * @param map 要判断的Map
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 合并多个map（不修改入参，返回新map）
     *
     * @param maps 需要合并的map
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeAll(Map<K, V>... maps) {
        Map<K, V> map = new HashMap<>();
        for (Map<K, V> m : maps) {
            if (isNotEmpty(m)) {
                map.putAll(m);
            }
        }
        return map;
    }

    /**
     * 合并多个参数为 Map，每两个参数为一组 key-value
     *
     * @param values 参数
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mergeAll(Object... values) {
        Map<K, V> map = new HashMap<>();
        if (ArrayUtil.isNotEmpty(values)) {
            for (int i = 0; i < values.length - 1; i += 2) {
                K key = (K) values[i];
                V value = (V) values[i + 1];
                map.put(key, value);
            }
        }
        return map;
    }

    public static <K, V> Map<K, V> newAndPut(K k, V v) {
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    /**
     * 克隆一个Map
     *
     * @param map 要克隆的Map
     * @return 克隆后的Map
     */
    public static <K, V> Map<K, V> clone(Map<K, V> map) {
        if (isEmpty(map)) {
            return new HashMap<>();
        }
        return new HashMap<>(map);
    }
}

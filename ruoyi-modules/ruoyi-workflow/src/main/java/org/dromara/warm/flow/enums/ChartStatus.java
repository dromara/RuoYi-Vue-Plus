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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;




import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程图状态
 *
 * @author warm
 * @since 2023/3/31 12:16
 */
@Getter
@AllArgsConstructor
public enum ChartStatus {
    /**
     * 流程图状态
     */
    NOT_DONE(0, "未办理", new Color(107,114,128)),

    TO_DO(1, "待办理", new Color(245,158,11)),

    DONE(2, "已办理", new Color(56,161,105));

    private final Integer key;
    private final String value;
    private final Color color;

    private static final Map<Integer, Color> CUSTOM_COLOR = new HashMap<>();
    private static final Map<Integer, Color> CUSTOM_COLOR_CLASSICS = new HashMap<>();
    private static final Map<Integer, Color> CUSTOM_COLOR_MIMIC = new HashMap<>();

    public static void initCustomColor(List<String> chartStatusColor, List<String> chartStatusColorClassics,
                                       List<String> chartStatusColorMimic) {
        if (CollUtil.isNotEmpty(chartStatusColor) && chartStatusColor.size() == 3) {
            for (int i = 0; i < chartStatusColor.size(); i++) {
                String statusColor = chartStatusColor.get(i);
                if (StrUtil.isNotEmpty(statusColor)) {
                    String[] colorArr = statusColor.split(",");
                    if (colorArr.length == 3) {
                        ChartStatus.CUSTOM_COLOR.put(i, new Color(Integer.parseInt(colorArr[0]), Integer.parseInt(colorArr[1]), Integer.parseInt(colorArr[2])));
                    }
                }
            }
        }
        if (CollUtil.isNotEmpty(chartStatusColorClassics) && chartStatusColorClassics.size() == 3) {
            for (int i = 0; i < chartStatusColorClassics.size(); i++) {
                String statusColor = chartStatusColorClassics.get(i);
                if (StrUtil.isNotEmpty(statusColor)) {
                    String[] colorArr = statusColor.split(",");
                    if (colorArr.length == 3) {
                        ChartStatus.CUSTOM_COLOR_CLASSICS.put(i, new Color(Integer.parseInt(colorArr[0]), Integer.parseInt(colorArr[1]), Integer.parseInt(colorArr[2])));
                    }
                }
            }
        }
        if (CollUtil.isNotEmpty(chartStatusColorMimic) && chartStatusColorMimic.size() == 3) {
            for (int i = 0; i < chartStatusColorMimic.size(); i++) {
                String statusColor = chartStatusColorMimic.get(i);
                if (StrUtil.isNotEmpty(statusColor)) {
                    String[] colorArr = statusColor.split(",");
                    if (colorArr.length == 3) {
                        ChartStatus.CUSTOM_COLOR_MIMIC.put(i, new Color(Integer.parseInt(colorArr[0]), Integer.parseInt(colorArr[1]), Integer.parseInt(colorArr[2])));
                    }
                }
            }
        }
    }

    public static Color getNotDone(String modelValue) {
        return getColorByKey(ChartStatus.NOT_DONE, modelValue);
    }

    public static Color getToDo(String modelValue) {
        return getColorByKey(ChartStatus.TO_DO, modelValue);
    }

    public static Color getDone(String modelValue) {
        return getColorByKey(ChartStatus.DONE, modelValue);
    }

    public static Color getColorByKey(ChartStatus chartStatus, String modelValue) {
        Color color = null;
        if (ModelEnum.CLASSICS.name().equals(modelValue)) {
            color = ChartStatus.CUSTOM_COLOR_CLASSICS.get(chartStatus.getKey());
        } else if (ModelEnum.MIMIC.name().equals(modelValue)) {
            color = ChartStatus.CUSTOM_COLOR_MIMIC.get(chartStatus.getKey());
        }
        if (ObjectUtil.isNull(color)) {
            color = ChartStatus.CUSTOM_COLOR.get(chartStatus.getKey());
        }
        return ObjectUtil.defaultIfNull(color, chartStatus.getColor());
    }

    public static Color getColorByKey(Integer key) {
        for (ChartStatus item : ChartStatus.values()) {
            if (item.getKey().equals(key)) {
                Color color = ChartStatus.CUSTOM_COLOR.get(key);
                return ObjectUtil.defaultIfNull(color, item.getColor());
            }
        }
        return null;
    }

    /**
     * 判断是否未办理
     *
     * @param key 状态
     */
    public static Boolean isNotDone(Integer key) {
        return ObjectUtil.isNotNull(key) && (ChartStatus.NOT_DONE.getKey().equals(key));
    }

    /**
     * 判断是否待办理
     *
     * @param key 状态
     */
    public static Boolean isToDo(Integer key) {
        return ObjectUtil.isNotNull(key) && (ChartStatus.TO_DO.getKey().equals(key));
    }

    /**
     * 判断是否已办理
     *
     * @param key 状态
     */
    public static Boolean isDone(Integer key) {
        return ObjectUtil.isNotNull(key) && (ChartStatus.DONE.getKey().equals(key));
    }

}

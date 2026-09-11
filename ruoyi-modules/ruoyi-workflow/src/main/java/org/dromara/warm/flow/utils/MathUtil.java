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

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;

/**
 * 数字工具类
 *
 * @author warm
 */
public class MathUtil {

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private MathUtil() {
    }

    /**
     * 判断是否为数字
     *
     * @param str 字符串
     */
    public static boolean isNumeric(String str) {
        // 检查字符串是否为空
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            // 使用Double.parseDouble()方法尝试将字符串转换为double
            Double.parseDouble(str.trim());
            // 转换成功，字符串是数字
            return true;
        } catch (NumberFormatException e) {
            // 转换失败，字符串不是数字
            return false;
        }
    }

    /**
     * 判断数字大小
     *
     * @param n1 字符串
     * @param n2 字符串
     */
    public static int determineSize(String n1, String n2) {
        BigDecimal a = new BigDecimal(n1);
        BigDecimal b = new BigDecimal(n2);
        return a.compareTo(b);
    }


    /**
     * 判断是否为0
     *
     * @param str 字符串
     */
    public static boolean isZero(String str) {
        if (StrUtil.isEmpty(str)) {
            return false;
        }
        try {
            BigDecimal value = new BigDecimal(str.trim());
            return value.compareTo(BigDecimal.ZERO) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否等于 100
     *
     * @param str 字符串
     * @return true：等于 100；false：不等于
     */
    public static boolean isHundred(String str) {
        if (StrUtil.isEmpty(str)) {
            return false;
        }
        try {
            BigDecimal value = new BigDecimal(str.trim());
            return value.compareTo(ONE_HUNDRED) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串表示的数值是否在 0 到 100 之间（含边界）
     *
     * @param str 字符串
     * @return true：在 [0, 100] 范围内；false：不在范围内
     */
    public static boolean isBetweenZeroAndHundred(String str) {
        if (StrUtil.isEmpty(str)) {
            return false;
        }
        try {
            BigDecimal value = new BigDecimal(str.trim());
            return value.compareTo(BigDecimal.ZERO) >= 0 && value.compareTo(ONE_HUNDRED) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}

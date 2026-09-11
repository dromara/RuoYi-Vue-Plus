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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import org.dromara.warm.flow.exception.FlowException;

import java.util.Collection;
import java.util.Map;

/**
 * Assert类
 *
 * @author warm
 * @since 2023/3/30 14:05
 */
public class AssertUtil {
    private AssertUtil() {

    }

    public static void isNull(Object obj, String errorMsg) {
        if (obj == null) {
            throw new FlowException(errorMsg);
        }
    }

    public static void isNotNull(Object obj, String errorMsg) {
        if (obj != null) {
            throw new FlowException(errorMsg);
        }
    }

    /**
     * 为true不抛异常
     *
     * @param obj
     * @param errorMsg
     */
    public static void isFalse(boolean obj, String errorMsg) {
        if (!obj) {
            throw new FlowException(errorMsg);
        }
    }

    /**
     * 为false不抛异常
     *
     * @param obj
     * @param errorMsg
     */
    public static void isTrue(boolean obj, String errorMsg) {
        if (obj) {
            throw new FlowException(errorMsg);
        }
    }

    public static void isNotEmpty(Object obj, String errorMsg) {
        if (obj != null) {
            if (obj instanceof String) {
                AssertUtil.isTrue(StrUtil.isNotEmpty((String) obj), errorMsg);
            } else if (obj instanceof Collection) {
                AssertUtil.isTrue(CollUtil.isNotEmpty((Collection<?>) obj), errorMsg);
            } else if (obj instanceof Map) {
                AssertUtil.isTrue(MapUtil.isNotEmpty((Map<?, ?>) obj), errorMsg);
            } else {
                throw new FlowException("Unsupported type: " + obj.getClass().getName());
            }
        }
    }


    public static void isEmpty(Object obj, String errorMsg) {
        if (obj == null) {
            throw new FlowException(errorMsg);
        } else if (obj instanceof String) {
            AssertUtil.isTrue(StrUtil.isEmpty((String) obj), errorMsg);
        } else if (obj instanceof Collection) {
            AssertUtil.isTrue(CollUtil.isEmpty((Collection<?>) obj), errorMsg);
        } else if (obj instanceof Map) {
            AssertUtil.isTrue(MapUtil.isEmpty((Map<?, ?>) obj), errorMsg);
        } else {
            throw new FlowException("Unsupported type: " + obj.getClass().getName());
        }
    }

    public static <T> void contains(Collection<T> a, T b, String errorMsg) {
        if (CollUtil.isNotEmpty(a) && a.contains(b)) {
            throw new FlowException(errorMsg);
        }
    }

    public static <T> void notContains(Collection<T> a, T b, String errorMsg) {
        if (CollUtil.isEmpty(a) || !a.contains(b)) {
            throw new FlowException(errorMsg);
        }
    }

}

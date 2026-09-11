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
package org.dromara.warm.flow.expression;

import cn.hutool.core.util.StrUtil;

import org.dromara.warm.flow.exception.FlowException;

import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardTypeLocator;

import java.util.HashSet;
import java.util.Set;

/**
 * 自定义类型定位器，用于限制访问的类
 *
 * @author warm
 * @since 2026/3/31
 */
// ... existing code ...

/**
 * 自定义类型定位器，用于限制访问的类
 *
 * @author warm
 * @since 2026/3/31
 */
public class SafeTypeLocator implements TypeLocator {
    private final StandardTypeLocator defaultTypeLocator;
    private final Set<String> allowedClasses;
    private static final Set<String> DANGEROUS_CLASSES;

    static {
        // 初始化危险类黑名单（使用全限定名便于前缀匹配）
        DANGEROUS_CLASSES = new HashSet<>();
        DANGEROUS_CLASSES.add("java.lang.Runtime");
        DANGEROUS_CLASSES.add("java.lang.ProcessBuilder");
        DANGEROUS_CLASSES.add("java.lang.System");
        DANGEROUS_CLASSES.add("java.lang.Class");
        DANGEROUS_CLASSES.add("java.lang.reflect");
        DANGEROUS_CLASSES.add("sun.misc.Unsafe");
        DANGEROUS_CLASSES.add("sun.reflect");
    }

    {
        allowedClasses = new HashSet<>();
        allowedClasses.add("java.lang.String");
        allowedClasses.add("java.lang.Integer");
        allowedClasses.add("java.lang.Long");
        allowedClasses.add("java.lang.Double");
        allowedClasses.add("java.lang.Float");
        allowedClasses.add("java.lang.Boolean");
        allowedClasses.add("java.lang.Byte");
        allowedClasses.add("java.lang.Short");
        allowedClasses.add("java.lang.Character");
        allowedClasses.add("java.lang.Object");
        allowedClasses.add("java.util.List");
        allowedClasses.add("java.util.ArrayList");
        allowedClasses.add("java.util.Map");
        allowedClasses.add("java.util.HashMap");
        allowedClasses.add("java.util.Set");
        allowedClasses.add("java.util.HashSet");
        allowedClasses.add("java.util.Date");
    }

    public SafeTypeLocator() {
        this.defaultTypeLocator = new StandardTypeLocator();
    }

    @Override
    public Class<?> findType(String typeName) {
        if (StrUtil.isEmpty(typeName)) {
            throw new FlowException("Type name cannot be null or empty");
        }

        // 检查是否为危险类（黑名单）
        if (isDangerousClass(typeName)) {
            throw new FlowException("Forbidden dangerous class: " + typeName);
        }

        // 检查是否在白名单内
        if (!allowedClasses.contains(typeName)) {
            throw new FlowException("Forbidden class not in whitelist: " + typeName);
        }

        return defaultTypeLocator.findType(typeName);
    }

    private boolean isDangerousClass(String typeName) {
        for (String dangerousClass : DANGEROUS_CLASSES) {
            if (typeName.equals(dangerousClass) || typeName.startsWith(dangerousClass + ".")) {
                return true;
            }
        }
        return false;
    }
}


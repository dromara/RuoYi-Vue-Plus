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
package org.dromara.warm.flow;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.config.WarmFlowProperties;
import org.dromara.warm.flow.handler.DataFillHandler;
import org.dromara.warm.flow.handler.PermissionHandler;
import org.dromara.warm.flow.listener.GlobalListener;
import org.dromara.warm.flow.service.*;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * 流程引擎，通过静态方法驱动流程流转
 */
public class FlowEngine {

    private static WarmFlowProperties flowConfig;
    private static DataFillHandler dataFillHandler;
    private static PermissionHandler permissionHandler;
    private static GlobalListener globalListener;

    public static DefService defService() {
        return SpringUtils.getBeanOrNull(DefService.class);
    }

    public static NodeService nodeService() {
        return SpringUtils.getBeanOrNull(NodeService.class);
    }

    public static SkipService skipService() {
        return SpringUtils.getBeanOrNull(SkipService.class);
    }

    public static InsService insService() {
        return SpringUtils.getBeanOrNull(InsService.class);
    }

    public static TaskService taskService() {
        return SpringUtils.getBeanOrNull(TaskService.class);
    }

    public static HisTaskService hisTaskService() {
        return SpringUtils.getBeanOrNull(HisTaskService.class);
    }

    public static UserService userService() {
        return SpringUtils.getBeanOrNull(UserService.class);
    }

    public static ChartService chartService() {
        return SpringUtils.getBeanOrNull(ChartService.class);
    }

    public static WarmFlowProperties getFlowConfig() {
        return flowConfig;
    }

    public static void setFlowConfig(WarmFlowProperties flowConfig) {
        FlowEngine.flowConfig = flowConfig;
    }

    public static void initDataFillHandler(String handlerPath) {
        dataFillHandler = initBean(DataFillHandler.class, handlerPath, () -> new DataFillHandler() {});
    }

    public static void initPermissionHandler(String handlerPath) {
        permissionHandler = initBean(PermissionHandler.class, handlerPath, null);
    }

    public static void initGlobalListener(String handlerPath) {
        globalListener = initBean(GlobalListener.class, handlerPath, null);
    }

    /**
     * 获取填充类
     */
    public static DataFillHandler dataFillHandler() {
        return dataFillHandler;
    }

    /**
     * 获取填充类
     */
    public static PermissionHandler permissionHandler() {
        return permissionHandler;
    }

    /**
     * 获取全局监听器
     */
    public static GlobalListener globalListener() {
        return globalListener;
    }

    /**
     * 获取数据库类型
     */
    public static String dataSourceType() {
        return flowConfig.getDataSourceType();
    }

    /**
     * 初始化bean，先从yml配置获取bean的全包名路径，否则从spring容器获取bean，如果都没有，则通过supplier获取bean
     *
     * @param tClazz   bean的class类型
     * @param beanPath bean全包名路径
     * @param supplier 获取bean的lambda
     * @param <T>      bean类型
     * @return bean
     */
    private static <T> T initBean(Class<T> tClazz, String beanPath, Supplier<T> supplier) {
        T handler = null;
        try {
            if (StrUtil.isNotBlank(beanPath)) {
                Class<?> clazz = ClassUtil.loadClass(beanPath);
                if (clazz != null && tClazz.isAssignableFrom(clazz)) {
                    Constructor<?> constructor = clazz.getConstructor();
                    handler = tClazz.cast(constructor.newInstance());
                }
            }
        } catch (Exception ignored) {
        }
        if (handler == null) {
            handler = SpringUtils.getBeanOrNull(tClazz);
        }
        if (handler == null && supplier != null) {
            handler = supplier.get();
        }
        return handler;
    }

}

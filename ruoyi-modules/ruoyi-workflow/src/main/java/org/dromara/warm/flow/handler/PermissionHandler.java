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
package org.dromara.warm.flow.handler;

import org.dromara.warm.flow.dto.FlowParams;

import java.util.List;

/**
 * 办理人权限处理器
 * 用户获取工作流中用到的permissionFlag和handler
 * permissionFlag: 办理人权限标识，比如用户，角色，部门等，用于校验是否有权限办理任务
 * handler: 当前办理人唯一标识，就是确定唯一用的，如用户id，通常用来入库，记录流程实例创建人，办理人
 *
 * @author shadow
 */
public interface PermissionHandler {

    /**
     * 办理人权限标识，比如用户，角色，部门等，用于校验是否有权限办理任务
     * 后续在{@link FlowParams#getPermissionFlag}  中获取
     * 返回当前用户权限集合
     *
     */
    List<String> permissions();

    /**
     * 获取当前办理人：就是确定唯一用的，如用户id，通常用来入库，记录流程实例创建人，办理人
     * 后续在{@link FlowParams#getHandler()}  中获取
     *
     * @return 当前办理人
     */
    String getHandler();

    /**
     * 转换办理人，比如设计器中预设了能办理的人，如果其中包含角色或者部门id等，可以通过此接口进行转换成用户id
     */
    default List<String> convertPermissions(List<String> permissions) {
        return permissions;
    }

}

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
package org.dromara.warm.flow.ui.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 流程设计器-办理人权限设置列表查询参数
 * 办理人权限列表选择框，可能存在多个，比如：部门、角色、用户的情况
 *
 * @author warm
 */
@Getter
@Setter
public class HandlerQuery {

    /**
     * 权限编码，如：zhang、roleAdmin、deptAdmin等编码
     */
    private String handlerCode;

    /**
     * 权限名称，如：管理员、角色管理员、部门管理员等名称
     */
    private String handlerName;

    /**
     * 办理权限类型，比如用户/角色/部门等
     */
    private String handlerType;

    /**
     * 页面左侧树权限分组主键，如：角色、部门等主键
     */
    private String groupId;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页显示条数
     */
    private Integer pageSize;

    /**
     * 开始时间
     */
    private String beginTime;

    /**
     * 结束时间
     */
    private String endTime;

}

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
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.Function;

/**
 * 办理人权限设置列表Function
 *
 * @author warm
 */
@Getter
@Setter
@Accessors(chain = true)
public class HandlerFunDto<T> {
    /**
     * 权限列表
     */
    private List<T> list;

    /**
     * 权限列表list总数
     */
    private long total;

    /**
     * 获取入库主键集合Function
     */
    private Function<T, String> storageId;

    /**
     * 获取权限编码Function
     */
    private Function<T, String> handlerCode;

    /**
     * 获取权限名称Function
     */
    private Function<T, String> handlerName;

    /**
     * 获取权限分组名称Function
     */
    private Function<T, String> groupName;

    /**
     * 获取创建时间Function
     */
    private Function<T, String> createTime;

    public HandlerFunDto(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }
}

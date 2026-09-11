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
 * 页面左侧树列表Function
 *
 * @author warm
 */
@Getter
@Setter
@Accessors(chain = true)
public class TreeFunDto<T> {

    /**
     * 左侧树列表
     */
    private List<T> list;

    /**
     * 获取左侧树ID Function
     */
    private Function<T, String> id;

    /**
     * 获取左侧树名称 Function
     */
    private Function<T, String> name;

    /**
     * 获取左侧树父级ID Function
     */
    private Function<T, String> parentId;

    public TreeFunDto(List<T> list) {
        this.list = list;
    }

}

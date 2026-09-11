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
package org.dromara.warm.flow.ui.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 监听器列表-设计器页面，监听器列表下拉选使用
 *
 * @author warm
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListenerVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 监听器类型，如：start、assignment、finish、create，如果未设置，选择监听器后，监听器类型不联动
     */
    private String type;

    /**
     * 监听器全限定类路径
     */
    private String path;

    /**
     * 监听器描述
     */
    private String description;

}
